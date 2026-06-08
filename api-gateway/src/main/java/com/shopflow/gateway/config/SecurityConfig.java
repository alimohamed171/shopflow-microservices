package com.shopflow.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * SecurityConfig for the API Gateway.
 * <p>
 * IMPORTANT: The Gateway uses Spring WebFlux (reactive), not Spring MVC.
 * Use @EnableWebFluxSecurity and ServerHttpSecurity — NOT HttpSecurity.
 * <p>
 * We disable Spring Security's own auth mechanisms here because:
 * - JWT validation is handled by our custom JwtAuthFilter (GatewayFilter)
 * - Spring Security's reactive filter would conflict with our setup
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                // Let all requests through — our JwtAuthFilter handles auth per-route
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                .build();
    }
    @Bean
    public UserDetailsService userDetailsService() {
        // Suppress Spring Security's auto-generated password warning.
        // Auth is handled entirely by JwtAuthFilter per-route.
        return username -> { throw new UsernameNotFoundException("No local users"); };
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("*");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
