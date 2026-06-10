package com.shopflow.payment.service;

import com.shopflow.payment.exception.DuplicatePaymentException;
import com.shopflow.payment.exception.InvalidAmountException;
import com.shopflow.payment.exception.PaymentNotFoundException;
import com.shopflow.payment.model.Payment;
import com.shopflow.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Value("${payment.fail-threshold:500.00}")
    private BigDecimal failThreshold;

    @Value("${payment.max-amount:10000.00}")
    private BigDecimal maxAmount;

    @Transactional
    public Payment processPayment(UUID orderId, String customerId, BigDecimal amount, String product) {
        log.info("[PAYMENT-SVC] Processing payment orderId={} customerId={} amount={}", orderId, customerId, amount);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero, got: " + amount);
        }
        if (amount.compareTo(maxAmount) > 0) {
            throw new InvalidAmountException("Amount " + amount + " exceeds the maximum allowed " + maxAmount);
        }
    if (paymentRepository.existsByOrderId(orderId)) {
            log.warn("[PAYMENT-SVC] Duplicate payment attempt for orderId={}", orderId);
            throw new DuplicatePaymentException(orderId);
        }

        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setCustomerId(customerId);
        payment.setAmount(amount);
        if (amount.compareTo(failThreshold) > 0) {
            String reason = "Amount " + amount + " exceeds configured limit of " + failThreshold;
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason(reason);
            Payment saved = paymentRepository.save(payment);
            log.warn("[PAYMENT-SVC] FAILED orderId={} reason={}", orderId, reason);
            return saved;
        }

        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        Payment saved = paymentRepository.save(payment);
        log.info("[PAYMENT-SVC] SUCCESS orderId={} paymentId={}", orderId, saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Payment getPayment(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByOrderId(UUID orderId) {
        log.debug("[PAYMENT-SVC] Looking up payments for orderId={}", orderId);
        return paymentRepository.findAllByOrderId(orderId);
    }
    @Transactional(readOnly = true)
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @Transactional
    public Payment retryPayment(UUID orderId, String customerId, BigDecimal amount) {
        log.info("[PAYMENT-SVC] Retry requested for orderId={}", orderId);
        paymentRepository.findAllByOrderId(orderId).stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.FAILED)
                .forEach(p -> {
                    log.info("[PAYMENT-SVC] Removing failed payment id={} before retry", p.getId());
                    paymentRepository.delete(p);
                });

        return processPayment(orderId, customerId, amount, null);
    }
}
