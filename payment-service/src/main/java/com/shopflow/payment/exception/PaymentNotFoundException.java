package com.shopflow.payment.exception;

import java.util.UUID;

public class PaymentNotFoundException extends PaymentException {
    public PaymentNotFoundException(UUID id) {
        super("PAYMENT_NOT_FOUND", "Payment not found with id: " + id);
    }
    public PaymentNotFoundException(String message) {
        super("PAYMENT_NOT_FOUND", message);
    }
}
