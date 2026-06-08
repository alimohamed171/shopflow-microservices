package com.shopflow.payment.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor
public class PaymentResponseEvent {
    private UUID orderId;
    private boolean success;
    private String reason;
}
