package com.shopflow.order.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

// ── Event sent TO payment service ─────────────────────────────────
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestEvent {
    private UUID orderId;
    private String customerId;
    private BigDecimal amount;
    private String product;
}


