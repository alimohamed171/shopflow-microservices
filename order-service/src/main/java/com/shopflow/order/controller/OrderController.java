package com.shopflow.order.controller;

import com.shopflow.order.exception.OrderNotFoundException;
import com.shopflow.order.model.Order;
import com.shopflow.order.repository.OrderRepository;
import com.shopflow.order.saga.OrderSagaOrchestrator;
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
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderSagaOrchestrator saga;
    private final OrderRepository orderRepository;

    @PostMapping
    public ResponseEntity<Order> createOrder(@Valid @RequestBody CreateOrderRequest req) {
        log.info("[ORDER-CTRL] POST /api/orders product='{}' customerId='{}' amount={}",
                req.product(), req.customerId(), req.amount());
        Order order = saga.startSaga(req.product(), req.customerId(), req.amount());
        return ResponseEntity.ok(order);
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable UUID id) {
        return orderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("service", "service-order", "total", orderRepository.count());
    }

    public record CreateOrderRequest(
            @NotBlank(message = "product is required") String product,
            @NotBlank(message = "customerId is required") String customerId,
            @NotNull(message = "amount is required") @Positive(message = "amount must be positive") BigDecimal amount
    ) {}
}
