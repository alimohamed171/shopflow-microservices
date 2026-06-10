package com.shopflow.order.saga;

import com.shopflow.order.config.RabbitMqConfig;
import com.shopflow.order.event.PaymentRequestEvent;
import com.shopflow.order.event.PaymentResponseEvent;
import com.shopflow.order.model.Order;
import com.shopflow.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSaga {

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public Order startSaga(String product, String customerId, BigDecimal amount) {
        log.info("[SAGA] Starting saga product='{}' customerId='{}' amount={}", product, customerId, amount);

        Order order = new Order();
        order.setProduct(product);
        order.setCustomerId(customerId);
        order.setAmount(amount);
        order.setStatus(Order.OrderStatus.PENDING);
        order = orderRepository.save(order);

        log.info("[SAGA] Order persisted orderId={} status=PENDING", order.getId());
        order.setStatus(Order.OrderStatus.PAYMENT_PROCESSING);
        orderRepository.save(order);
        PaymentRequestEvent event = new PaymentRequestEvent(
                order.getId(), customerId, amount, product);
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.SAGA_EXCHANGE,
                    RabbitMqConfig.PAYMENT_REQUEST_QUEUE,
                    event);
            log.info("[SAGA] PaymentRequestEvent published orderId={}", order.getId());
        } catch (AmqpException ex) {
            log.error("[SAGA] Failed to publish PaymentRequestEvent for orderId={}: {}",
                    order.getId(), ex.getMessage(), ex);
            throw ex;
        }

        return order;
    }

    @RabbitListener(queues = RabbitMqConfig.PAYMENT_RESPONSE_QUEUE)
    @Transactional
    public void handlePaymentResponse(PaymentResponseEvent event) {
        log.info("[SAGA] Received payment response orderId={} success={} reason={}",
                event.getOrderId(), event.isSuccess(), event.getReason());

        try {
            orderRepository.findById(event.getOrderId()).ifPresentOrElse(order -> {
                if (event.isSuccess()) {
                    order.setStatus(Order.OrderStatus.COMPLETED);
                    orderRepository.save(order);
                    log.info("[SAGA]  Order COMPLETED orderId={}", order.getId());
                } else {
                    order.setStatus(Order.OrderStatus.FAILED);
                    orderRepository.save(order);
                    log.warn("[SAGA]  Order FAILED (compensated) orderId={} reason={}",
                            order.getId(), event.getReason());
                }
            }, () -> log.error("[SAGA] Order not found for payment response orderId={} — cannot apply state change",
                    event.getOrderId()));
        } catch (Exception ex) {
            log.error("[SAGA] Unexpected error handling payment response for orderId={}: {}",
                    event.getOrderId(), ex.getMessage(), ex);
        }
    }
}
