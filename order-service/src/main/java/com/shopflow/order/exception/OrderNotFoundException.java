package com.shopflow.order.exception;

import java.util.UUID;

public class OrderNotFoundException extends OrderException {
    public OrderNotFoundException(UUID id) {
        super("ORDER_NOT_FOUND", "Order not found with id: " + id);
    }
}
