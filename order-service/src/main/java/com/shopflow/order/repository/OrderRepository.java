package com.shopflow.order.repository;

import com.shopflow.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByCustomerIdOrderByCreatedAtDesc(String customerId);
    List<Order> findByStatusOrderByCreatedAtDesc(Order.OrderStatus status);
    long countByStatus(Order.OrderStatus status);
}
