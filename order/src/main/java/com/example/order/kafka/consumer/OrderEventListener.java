package com.example.order.kafka.consumer;

import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import com.example.order.kafka.event.StockDecreasedEvent;
import com.example.order.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class OrderEventListener
{
    private final OrderRepository repository;

    @Autowired
    public OrderEventListener(OrderRepository repository)
    {
        this.repository = repository;
    }

    @KafkaListener(topics = "stock-decreased", groupId = "order-service")
    public void handleStockDecreased(StockDecreasedEvent event)
    {
        System.out.println("stock-decreased 이벤트 수신" + event);

        // 주문 상태를 COMPLETE(채결)로 변경
        updateOrderStatus(event.getOrderId(), OrderStatus.COMPLETED);
    }

    @KafkaListener(topics = "stock-decrease-failed", groupId = "order-service")
    public void handleStockDecreaseFailed(StockDecreasedEvent event)
    {
        System.out.println("stock-decrease-failed 이벤트 수신" + event);

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