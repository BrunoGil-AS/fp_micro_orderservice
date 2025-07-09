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
                .csrf(csrf -> csrf.disable()) // Desactiva CSRF para APIs REST
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Habilita CORS
                .authorizeHttpRequests(authz -> authz
                        // Endpoints públicos
                        .requestMatchers("/actuator/**").permitAll() // Health checks
                        
                        // Endpoints específicos del usuario (deben ir ANTES que los generales)
                        .requestMatchers(HttpMethod.GET, "/orders/me").hasRole("USER") // Obtener pedidos del usuario
                        .requestMatchers(HttpMethod.POST, "/orders/me").hasRole("USER") // Crear pedido
                        .requestMatchers(HttpMethod.PUT, "/orders/me").hasRole("USER") // Actualizar pedido
                        .requestMatchers(HttpMethod.DELETE, "/orders/me").hasRole("USER") // Eliminar pedido
                        
                        // Endpoints que requieren rol ADMIN (DESPUÉS de los específicos)
                        .requestMatchers(HttpMethod.GET, "/orders/**").hasRole("ADMIN") // Todos los demás endpoints de orders
                        
                        // Cualquier otra petición requiere autenticación
                        .anyRequest().authenticated()
                )
                // Configurar como servidor de recursos OAuth2 con JWT
                .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .build();
    }

    /**
     * Configuración del convertidor de autenticación JWT para extraer roles/authorities.
     * Extrae los roles del claim 'authorities', 'roles' o 'scope' del JWT.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Intentar obtener authorities desde diferentes claims posibles
            Collection<String> authorities = null;
            
            // Primero intentar con 'authorities'
            if (jwt.hasClaim("authorities")) {
                authorities = jwt.getClaimAsStringList("authorities");
            }
            // Si no existe, intentar con 'roles'
            else if (jwt.hasClaim("roles")) {
                authorities = jwt.getClaimAsStringList("roles");
            }
            // Si no existe, intentar con 'scope' (separado por espacios)
            else if (jwt.hasClaim("scope")) {
                String scope = jwt.getClaimAsString("scope");
                authorities = Arrays.asList(scope.split(" "));
            }
            
            log.info("Authorities extracted from JWT: " + authorities);
            log.info("JWT Claims: " + jwt.getClaims());
            
            // Convertir a SimpleGrantedAuthority y asegurar prefijo ROLE_
            if (authorities != null) {
                return authorities.stream()
                        .map(authority -> authority.startsWith("ROLE_") ? authority : "ROLE_" + authority)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }
            
            return Arrays.asList(); // Retornar lista vacía si no hay authorities
        });
        
        return converter;
    }

    /**
     * Configuración CORS para permitir el acceso desde el frontend.
     * Permite requests desde http://localhost:3000 (React frontend) y desde el gateway.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Permitir el origen del frontend y del gateway
        configuration.setAllowedOrigins(Arrays.asList(
            frontendUrl, // Frontend React
            gatewayUrl  // Gateway
        ));
        
        // Permitir todos los métodos HTTP necesarios
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Permitir todos los headers
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Permitir cookies y credenciales
        configuration.setAllowCredentials(true);
        
        // Configurar para todas las rutas
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    /**
     * Bean JwtDecoder para decodificar tokens JWT.
     * Este bean es necesario para que Spring Security pueda validar los tokens JWT.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

}
