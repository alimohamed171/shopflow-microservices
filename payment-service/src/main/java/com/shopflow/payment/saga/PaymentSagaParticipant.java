package com.shopflow.payment.saga;

import com.shopflow.payment.config.RabbitMqConfig;
import com.shopflow.payment.event.PaymentRequestEvent;
import com.shopflow.payment.event.PaymentResponseEvent;
import com.shopflow.payment.exception.DuplicatePaymentException;
import com.shopflow.payment.exception.InvalidAmountException;
import com.shopflow.payment.exception.PaymentException;
import com.shopflow.payment.model.Payment;
import com.shopflow.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentSagaParticipant {

    private final PaymentService paymentService;
    private final RabbitTemplate rabbitTemplate;

    /**
     * SAGA PARTICIPANT — listens for payment requests, delegates to PaymentService,
     * then sends back a PaymentResponseEvent with success/failure.
     *
     * Exception handling strategy:
     *  - DuplicatePaymentException → idempotent failure (already processed), send failure
     *  - InvalidAmountException    → business validation failure, send failure
     *  - PaymentException          → general domain error, send failure
     *  - Any other exception       → unexpected error, send failure (don't re-throw so message is ack'd)
     */
    @RabbitListener(queues = RabbitMqConfig.PAYMENT_REQUEST_QUEUE)
    public void processPaymentRequest(PaymentRequestEvent event) {
        log.info("[SAGA-PARTICIPANT] Received payment request orderId={} customerId={} amount={}",
                event.getOrderId(), event.getCustomerId(), event.getAmount());

        PaymentResponseEvent response;

        try {
            Payment payment = paymentService.processPayment(
                    event.getOrderId(),
                    event.getCustomerId(),
                    event.getAmount(),
                    event.getProduct()
            );

            boolean success = payment.getStatus() == Payment.PaymentStatus.SUCCESS;
            String reason = success ? "Payment processed successfully" : payment.getFailureReason();
            response = new PaymentResponseEvent(event.getOrderId(), success, reason);

            if (success) {
                log.info("[SAGA-PARTICIPANT] Payment SUCCESS orderId={} paymentId={}",
                        event.getOrderId(), payment.getId());
            } else {
                log.warn("[SAGA-PARTICIPANT] Payment DECLINED orderId={} reason={}",
                        event.getOrderId(), reason);
            }

        } catch (DuplicatePaymentException ex) {
            log.warn("[SAGA-PARTICIPANT] Duplicate payment for orderId={} — sending failure response",
                    event.getOrderId());
            response = new PaymentResponseEvent(event.getOrderId(), false, ex.getMessage());

        } catch (InvalidAmountException ex) {
            log.warn("[SAGA-PARTICIPANT] Invalid amount for orderId={} amount={} reason={}",
                    event.getOrderId(), event.getAmount(), ex.getMessage());
            response = new PaymentResponseEvent(event.getOrderId(), false, ex.getMessage());

        } catch (PaymentException ex) {
            log.error("[SAGA-PARTICIPANT] Payment domain error orderId={} code={} message={}",
                    event.getOrderId(), ex.getErrorCode(), ex.getMessage());
            response = new PaymentResponseEvent(event.getOrderId(), false,
                    "Payment error: " + ex.getMessage());

        } catch (Exception ex) {
            log.error("[SAGA-PARTICIPANT] Unexpected error processing orderId={}: {}",
                    event.getOrderId(), ex.getMessage(), ex);
            response = new PaymentResponseEvent(event.getOrderId(), false,
                    "Internal payment processing error");
        }

        // Always reply so the saga orchestrator can move forward
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.SAGA_EXCHANGE,
                RabbitMqConfig.PAYMENT_RESPONSE_QUEUE,
                response);

        log.info("[SAGA-PARTICIPANT] Response sent orderId={} success={}",
                response.getOrderId(), response.isSuccess());
    }
}
