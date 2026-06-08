package com.shopflow.payment.exception;

import java.util.UUID;

public class DuplicatePaymentException extends PaymentException {
    public DuplicatePaymentException(UUID orderId) {
        super("DUPLICATE_PAYMENT", "Payment already exists for orderId: " + orderId);
    }
}
