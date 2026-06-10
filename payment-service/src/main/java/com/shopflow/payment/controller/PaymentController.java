package com.shopflow.payment.controller;

import com.shopflow.payment.model.Payment;
import com.shopflow.payment.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    @GetMapping
    public List<Payment> getAllPayments() {
        log.debug("[PAYMENT-CTRL] GET /api/payments");
        return paymentService.getAllPayments();
    }
    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPayment(@PathVariable UUID id) {
        log.debug("[PAYMENT-CTRL] GET /api/payments/{}", id);
        return ResponseEntity.ok(paymentService.getPayment(id));
    }

    @GetMapping("/order/{orderId}")
    public List<Payment> getByOrder(@PathVariable UUID orderId) {
        log.debug("[PAYMENT-CTRL] GET /api/payments/order/{}", orderId);
        return paymentService.getPaymentsByOrderId(orderId);
    }

    @PostMapping
    public ResponseEntity<Payment> createPayment(@Valid @RequestBody CreatePaymentRequest req) {
        log.info("[PAYMENT-CTRL] Manual payment request orderId={} amount={}", req.orderId(), req.amount());
        Payment payment = paymentService.processPayment(req.orderId(), req.customerId(), req.amount(), req.product());
        return ResponseEntity.ok(payment);
    }
    @PostMapping("/order/{orderId}/retry")
    public ResponseEntity<Payment> retryPayment(
            @PathVariable UUID orderId,
            @Valid @RequestBody RetryPaymentRequest req) {
        log.info("[PAYMENT-CTRL] Retry request for orderId={}", orderId);
        Payment payment = paymentService.retryPayment(orderId, req.customerId(), req.amount());
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        long total = paymentService.getAllPayments().size();
        return Map.of(
                "service", "service-payment",
                "total", total
        );
    }

    public record CreatePaymentRequest(
            @NotNull UUID orderId,
            @NotBlank String customerId,
            @NotNull @Positive BigDecimal amount,
            String product
    ) {}

    public record RetryPaymentRequest(
            @NotBlank String customerId,
            @NotNull @Positive BigDecimal amount
    ) {}
}
