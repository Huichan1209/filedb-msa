package com.example.product.kafka.consumer;

import com.example.product.domain.Product;
import com.example.product.kafka.event.*;
import com.example.product.kafka.producer.ProductEventProducer;
import com.example.product.repository.ProductRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ProductEventListener
{
    private final ProductRepository repository;
    private final ProductEventProducer producer;
    private final ObjectMapper objectMapper;

    @Autowired
    public ProductEventListener(ProductRepository repository, ProductEventProducer producer, ObjectMapper objectMapper)
    {
        this.repository = repository;
        this.producer = producer;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "order-created", groupId = "product-service")
    public void handleOrderCreated(String msg)
    {
        OrderCreatedEvent event = null;
        try
        {
            event = objectMapper.readValue(msg, OrderCreatedEvent.class);
            System.out.println("order-created 이벤트 수신. event: " + event);
        }
        catch (JsonProcessingException e)
        {
            System.out.println("event 메시지 파싱 중 에러");
            e.printStackTrace();
        }

        try
        {
            // 재고 차감 로직
            Product product = repository.findById(event.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product가 존재하지 않음."));

            if (product.getStock() >= event.getCount())
            {
                product.setStock(product.getStock() - event.getCount());
                repository.save(product);

                // 재고 차감 성공 이벤트 발행
                producer.sendStockDecreasedEvent(new StockDecreasedEvent(event.getOrderId()));
            }
            else
            {
                throw new RuntimeException("상품의 재고가 충분하지 않음.");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();

            // 재고 차감 실패 이벤트 발행
            producer.sendStockDecreaseFailedEvent(new StockDecreasedEvent(event.getOrderId()));
        }
    }

    @KafkaListener(topics = "order-cancelled", groupId = "product-service")
    public void handleOrderCancelled(String msg)
    {
        OrderCancelledEvent event = null;
        try
        {
            event = objectMapper.readValue(msg, OrderCancelledEvent.class);
            System.out.println("order-cancelled 이벤트 수신. event: " + event);
        }
        catch (JsonProcessingException e)
        {
            System.out.println("event 메시지 파싱 중 에러");
            e.printStackTrace();
        }

        try
        {
            // 재고 복구 로직
            Product product = repository.findById(event.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product가 존재하지 않음."));

            product.setStock(product.getStock() + event.getCount());
            repository.save(product);

            // 재고 복구 성공 이벤트 발행
            producer.sendStockRestoredEvent(new StockRestoredEvent(event.getOrderId()));
        }
        catch (Exception e)
        {
            e.printStackTrace();

            // 주문 취소 실패 이벤트 발행
            producer.sendStockRestoreFailedEvent(new StockRestoreFailedEvent(event.getOrderId()));
        }
    }
}