package com.example.order.kafka.producer;

import com.example.order.kafka.event.OrderCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventProducer
{
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public OrderEventProducer(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper)
    {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendOrderCreatedEvent(OrderCreatedEvent event)
    {
        try
        {
            kafkaTemplate.send("order-created", event.getProductId().toString(), objectMapper.writeValueAsString(event));
        }
        catch (JsonProcessingException e)
        {
            e.printStackTrace();
        }
    }
}