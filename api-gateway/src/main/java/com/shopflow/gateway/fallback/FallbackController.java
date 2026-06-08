package com.shopflow.gateway.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> ordersFallback() {
        log.warn("[GATEWAY] Circuit breaker open for order-service");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "service", "order-service",
                "status", "UNAVAILABLE",
                "message", "Order service is temporarily unavailable. Please try again shortly.",
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/payments")
    public ResponseEntity<Map<String, Object>> paymentsFallback() {
        log.warn("[GATEWAY] Circuit breaker open for payment-service");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "service", "payment-service",
                "status", "UNAVAILABLE",
                "message", "Payment service is temporarily unavailable. Please try again shortly.",
                "timestamp", Instant.now().toString()
        ));
    }
}
