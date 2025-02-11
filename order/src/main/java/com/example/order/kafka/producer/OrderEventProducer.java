package com.example.order.kafka.producer;

import com.example.order.kafka.event.OrderCreatedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventProducer
{
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public OrderEventProducer(KafkaTemplate<String, Object> kafkaTemplate)
    {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendOrderCreatedEvent(OrderCreatedEvent event)
    {
        kafkaTemplate.send("order-created", event.getProductId().toString(), event);
    }
}