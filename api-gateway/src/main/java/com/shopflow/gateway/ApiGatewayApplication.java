package com.shopflow.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import java.util.Map;

@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}

//@RestController
//@RequestMapping("/fallback")
//class FallbackController {
//    @GetMapping("/orders")
//    public Mono<Map<String, String>> ordersFallback() {
//        return Mono.just(Map.of("error", "Order service unavailable", "status", "SERVICE_DOWN"));
//    }
//    @GetMapping("/payments")
//    public Mono<Map<String, String>> paymentsFallback() {
//        return Mono.just(Map.of("error", "Payment service unavailable", "status", "SERVICE_DOWN"));
//    }
//}