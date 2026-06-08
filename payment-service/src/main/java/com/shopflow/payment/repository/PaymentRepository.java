package com.shopflow.payment.repository;

import com.shopflow.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    boolean existsByOrderId(UUID orderId);
    List<Payment> findAllByOrderId(UUID orderId);
    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(UUID orderId);
    List<Payment> findByStatus(Payment.PaymentStatus status);
}
