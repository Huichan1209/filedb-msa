package com.example.order.kafka.producer;

import com.example.order.kafka.event.StockDecreasedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductEventProducer
{
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public ProductEventProducer(KafkaTemplate<String, Object> kafkaTemplate)
    {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendStockDecreasedEvent(StockDecreasedEvent event)
    {
        kafkaTemplate.send("stock-decreased", event.getOrderId().toString(), event);
    }
}