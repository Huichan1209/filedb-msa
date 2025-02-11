package com.example.order.kafka.consumer;

import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import com.example.order.kafka.event.StockDecreasedEvent;
import com.example.order.repository.OrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class OrderEventListener
{
    private final OrderRepository repository;
    private final ObjectMapper objectMapper;

    @Autowired
    public OrderEventListener(OrderRepository repository, ObjectMapper objectMapper)
    {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "stock-decreased", groupId = "order-service")
    public void handleStockDecreased(String msg)
    {
        StockDecreasedEvent event = null;
        try
        {
            event = objectMapper.readValue(msg, StockDecreasedEvent.class);
            System.out.println("stock-decreased 이벤트 수신" + event);
        }
        catch (JsonProcessingException e)
        {
            System.out.println("event 메시지 파싱 중 에러");
            e.printStackTrace();
        }

        // 주문 상태를 COMPLETE(채결)로 변경
        updateOrderStatus(event.getOrderId(), OrderStatus.COMPLETED);
    }

    @KafkaListener(topics = "stock-decrease-failed", groupId = "order-service")
    public void handleStockDecreaseFailed(String msg)
    {
        StockDecreasedEvent event = null;
        try
        {
            event = objectMapper.readValue(msg, StockDecreasedEvent.class);
            System.out.println("stock-decrease-failed 이벤트 수신" + event);
        }
        catch (JsonProcessingException e)
        {
            System.out.println("event 메시지 파싱 중 에러");
            e.printStackTrace();
        }

        // 주문 상태를 FAILED(실패)로 변경
        updateOrderStatus(event.getOrderId(), OrderStatus.FAILED);
    }

    // 주문 상태 변경
    private void updateOrderStatus(Long orderId, OrderStatus status)
    {
        try
        {
            repository.findById(orderId).ifPresent(order ->
            {
                order.setStatus(status);
                try
                {
                    repository.merge(order);
                }
                catch (Exception e)
                {
                    System.out.println("[OrderEventListener.updateOrderStatus] merge 실패");
                    e.printStackTrace();
                }
            });
        }
        catch (IOException e)
        {
            System.out.println("[OrderEventListener.updateOrderStatus] find 실패");
            e.printStackTrace();
        }
    }
}