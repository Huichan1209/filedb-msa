package com.example.product.kafka.consumer;

import com.example.product.domain.Product;
import com.example.product.kafka.event.OrderCreatedEvent;
import com.example.product.kafka.event.StockDecreasedEvent;
import com.example.product.kafka.producer.ProductEventProducer;
import com.example.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ProductEventListener
{
    private final ProductRepository repository;
    private final ProductEventProducer producer;

    @Autowired
    public ProductEventListener(ProductRepository repository, ProductEventProducer producer)
    {
        this.repository = repository;
        this.producer = producer;
    }

    @KafkaListener(topics = "order-created", groupId = "product-service")
    public void handleOrderCreated(OrderCreatedEvent event)
    {
        System.out.println("order-created 이벤트 수신. event: " + event.toString());

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
            producer.sendStockDecreaseFailed(new StockDecreasedEvent(event.getOrderId()));
        }
    }
}