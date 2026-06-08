package com.shopflow.gateway.filter;

import com.shopflow.gateway.config.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * JwtAuthFilter — applied to every route that requires authentication.
 *
 * What it does:
 *  1. Extracts Bearer token from Authorization header.
 *  2. Validates the JWT using the shared secret.
 *  3. On success: forwards request, injecting X-User-Id and X-User-Role headers
 *     so downstream services don't need to re-parse the JWT.
 *  4. On failure: returns 401 immediately.
 *
 * Routes that are public (e.g. /api/auth/login) bypass this filter
 * via the route config in application.yml.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter() {
        super(Config.class);
        this.jwtUtil = null; // Spring will inject via field after construction
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest()
                    .getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Missing or malformed Authorization header on path: {}",
                        exchange.getRequest().getPath());
                return unauthorised(exchange);
            }

            String token = authHeader.substring(7);

            if (!jwtUtil.isValid(token)) {
                log.warn("Invalid or expired JWT on path: {}",
                        exchange.getRequest().getPath());
                return unauthorised(exchange);
            }

            // Inject user context into downstream request headers
            Claims claims = jwtUtil.validateAndExtractClaims(token);
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(r -> r
                        .header("X-User-Id",   claims.getSubject())
                        .header("X-User-Role",  claims.get("role", String.class))
                        .header("X-User-Email", claims.get("email", String.class))
                    )
                    .build();

            return chain.filter(mutatedExchange);
        };
    }

    private Mono<Void> unauthorised(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        // Config fields can be added here if needed (e.g. role requirements per route)
    }
}
