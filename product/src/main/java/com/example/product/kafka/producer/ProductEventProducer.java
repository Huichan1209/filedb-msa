package com.example.product.kafka.producer;

import com.example.product.kafka.event.StockDecreasedEvent;
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

    public void sendStockDecreaseFailed(StockDecreasedEvent event)
    {
        kafkaTemplate.send("stock-decrease-failed", event.getOrderId().toString(), event);
    }
}