package com.shopflow.order.exception;

public class OrderException extends RuntimeException {
    private final String errorCode;

    public OrderException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public OrderException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
