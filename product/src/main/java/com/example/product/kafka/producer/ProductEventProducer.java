package com.example.product.kafka.producer;

import com.example.product.kafka.event.StockDecreasedEvent;
import com.example.product.kafka.event.StockRestoreFailedEvent;
import com.example.product.kafka.event.StockRestoredEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductEventProducer
{
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public ProductEventProducer(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper)
    {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendStockDecreasedEvent(StockDecreasedEvent event)
    {
        try
        {
            kafkaTemplate.send("stock-decreased", event.getOrderId().toString(), objectMapper.writeValueAsString(event));
        }
        catch (JsonProcessingException e)
        {
            e.printStackTrace();
        }
    }

    public void sendStockDecreaseFailedEvent (StockDecreasedEvent event)
    {
        try
        {
            kafkaTemplate.send("stock-decrease-failed", event.getOrderId().toString(), objectMapper.writeValueAsString(event));
        }
        catch (JsonProcessingException e)
        {
            e.printStackTrace();
        }
    }

    public void sendStockRestoredEvent (StockRestoredEvent event)
    {
        try
        {
            kafkaTemplate.send("stock-restored", event.getOrderId().toString(), objectMapper.writeValueAsString(event));
        }
        catch (JsonProcessingException e)
        {
            e.printStackTrace();
        }
    }

    public void sendStockRestoreFailedEvent (StockRestoreFailedEvent event)
    {
        try
        {
            kafkaTemplate.send("stock-restore-failed", event.getOrderId().toString(), objectMapper.writeValueAsString(event));
        }
        catch (JsonProcessingException e)
        {
            e.printStackTrace();
        }
    }
}