package com.aspiresys.fp_micro_orderservice.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.extern.java.Log;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;


/**
 * Security configuration class for the Order Service microservice.
 * <p>
 * This class configures Spring Security for the application, including:
 * <ul>
 *   <li>Enabling web security and method-level security annotations.</li>
 *   <li>Configuring CORS to allow requests from the frontend and gateway servers.</li>
 *   <li>Disabling CSRF protection (suitable for stateless APIs).</li>
 *   <li>Defining authorization rules for HTTP endpoints, restricting access based on user roles.</li>
 *   <li>Setting up JWT-based OAuth2 resource server authentication.</li>
 *   <li>Customizing JWT authority extraction to support different claim names ("authorities", "roles", "scope").</li>
 *   <li>Providing a bean for JWT decoding using a JWK set URI.</li>
 * </ul>
 * <p>
 * Environment properties required:
 * <ul>
 *   <li><b>service.env.frontend.server</b>: URL of the frontend application allowed for CORS.</li>
 *   <li><b>service.env.gateway.server</b>: URL of the gateway allowed for CORS.</li>
 *   <li><b>spring.security.oauth2.resourceserver.jwt.jwk-set-uri</b>: JWK Set URI for JWT decoding.</li>
 * </ul>
 *
 * @author bruno.gil
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Habilita @PreAuthorize y @PostAuthorize
@Log
public class SecurityConfig {

    @Value("${service.env.frontend.server}")
    private String frontendUrl;

    @Value("${service.env.gateway.server}")
    private String gatewayUrl;
    
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable()) // disable CSRF for stateless APIs, like frontend and gateway
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) 
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/actuator/**").permitAll() 
                        .requestMatchers(HttpMethod.GET, "/orders/me").hasRole("USER") 
                        .requestMatchers(HttpMethod.POST, "/orders/me").hasRole("USER") 
                        .requestMatchers(HttpMethod.PUT, "/orders/me").hasRole("USER") 
                        .requestMatchers(HttpMethod.DELETE, "/orders/me").hasRole("USER") 

                        .requestMatchers(HttpMethod.GET, "/orders/**").hasRole("ADMIN") 
                        .anyRequest().authenticated() // All other requests require authentication
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .build();
    }

    
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<String> authorities = null;
            
            if (jwt.hasClaim("authorities")) {
                authorities = jwt.getClaimAsStringList("authorities");
            }

            else if (jwt.hasClaim("roles")) {
                authorities = jwt.getClaimAsStringList("roles");
            }

            else if (jwt.hasClaim("scope")) {
                String scope = jwt.getClaimAsString("scope");
                authorities = Arrays.asList(scope.split(" "));
            }
            
            log.info("Authorities extracted from JWT: " + authorities);
            log.info("JWT Claims: " + jwt.getClaims());
            
            if (authorities != null) {
                return authorities.stream()
                        .map(authority -> authority.startsWith("ROLE_") ? authority : "ROLE_" + authority)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }
            
            return Arrays.asList();
        });
        
        return converter;
    }

    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        configuration.setAllowedOrigins(Arrays.asList(
            frontendUrl, // Frontend React
            gatewayUrl  // Gateway
        ));
    
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        
        configuration.setAllowCredentials(true);
        
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

}
