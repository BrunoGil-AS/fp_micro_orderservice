package com.aspiresys.fp_micro_orderservice.kafka.config;

import com.aspiresys.fp_micro_orderservice.kafka.dto.ProductMessage;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for Kafka Producer setup.
 * <p>
 * This class configures the Kafka producer for sending product messages
 * to Kafka topics.
 * </p>
 *
 * @author bruno.gil
 */
@Configuration
@EnableKafka
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    /**
     * Producer factory configuration for creating Kafka producers.
     *
     * @return ProducerFactory configured for String keys and ProductMessage values
     */
    @Bean
    public ProducerFactory<String, ProductMessage> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Kafka template for sending ProductMessage objects to Kafka topics.
     *
     * @return KafkaTemplate configured with producer factory
     */
    @Bean
    public KafkaTemplate<String, ProductMessage> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
