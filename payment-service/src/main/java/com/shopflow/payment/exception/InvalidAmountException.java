package com.shopflow.payment.exception;

public class InvalidAmountException extends PaymentException {
    public InvalidAmountException(String message) {
        super("INVALID_AMOUNT", message);
    }
}
