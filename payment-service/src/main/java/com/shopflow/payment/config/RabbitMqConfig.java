package com.shopflow.payment.config;


import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String PAYMENT_REQUEST_QUEUE  = "payment.request";
    public static final String PAYMENT_RESPONSE_QUEUE = "payment.response";
    public static final String SAGA_EXCHANGE          = "saga.exchange";

    @Bean
    public TopicExchange sagaExchange() {
        return new TopicExchange(SAGA_EXCHANGE, true, false);
    }

    @Bean
    public Queue paymentRequestQueue() {
        return QueueBuilder.durable(PAYMENT_REQUEST_QUEUE).build();
    }

    @Bean
    public Queue paymentResponseQueue() {
        return QueueBuilder.durable(PAYMENT_RESPONSE_QUEUE).build();
    }

    @Bean
    public Binding paymentRequestBinding(Queue paymentRequestQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(paymentRequestQueue).to(sagaExchange).with("payment.request");
    }

    @Bean
    public Binding paymentResponseBinding(Queue paymentResponseQueue, TopicExchange sagaExchange) {
        return BindingBuilder.bind(paymentResponseQueue).to(sagaExchange).with("payment.response");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}