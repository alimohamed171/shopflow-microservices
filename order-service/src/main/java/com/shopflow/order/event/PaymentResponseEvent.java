package com.shopflow.order.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

// ── Event received FROM payment service ───────────────────────────
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseEvent {
    private UUID orderId;
    private boolean success;
    private String reason;
}
