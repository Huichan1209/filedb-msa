package com.example.order.service;

import com.example.order.db.paging.Page;
import com.example.order.db.paging.Pageable;
import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import com.example.order.dto.req.OrderReqDto;
import com.example.order.dto.res.OrderResDto;
import com.example.order.kafka.event.OrderCreatedEvent;
import com.example.order.kafka.producer.OrderEventProducer;
import com.example.order.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderService
{
    private final OrderRepository repository;
    private final OrderEventProducer eventProducer;

    @Autowired
    public OrderService(OrderRepository repository, OrderEventProducer eventProducer)
    {
        this.repository = repository;
        this.eventProducer = eventProducer;
    }

    public OrderResDto addOrder (OrderReqDto reqDto) throws Exception
    {
        // 1. PENDING(보류) 상태로 주문 insert
        Order order = new Order(null, reqDto.getProductId(), reqDto.getCount(), OrderStatus.PENDING);
        repository.save(order);

        // 2. Kafka로 order-created 이벤트 발행 (Product의 ProductEventListener에서 수신)
        OrderCreatedEvent event = new OrderCreatedEvent(order.getId(), order.getProductId(), order.getCount());
        eventProducer.sendOrderCreatedEvent(event);

        return new OrderResDto(order.getId(), order.getProductId(), order.getCount(), order.getStatus(), true);
    }

//    public OrderResDto updateOrder(OrderReqDto reqDto) throws Exception
//    {
//        Order order = new Order(reqDto.getId(), reqDto.getProductId(), reqDto.getCount(), reqDto.getStatus());
//        if(!repository.findById(reqDto.getId()).isPresent())
//        {
//            System.out.println("[Service.update] 해당 데이터가 존재하지 않음. id:" + reqDto.getId());
//            return new OrderResDto(reqDto.getId(), reqDto.getProductId(), reqDto.getCount(), reqDto.getStatus(), false);
//        }
//
//        Order resultOrder = repository.save(order);
//        return new OrderResDto(resultOrder.getId(), resultOrder.getProductId(), resultOrder.getCount(), reqDto.getStatus(), true);
//    }

    public OrderResDto deleteOrder(Long id) throws Exception
    {
        if(!repository.findById(id).isPresent())
        {
            System.out.println("[Service.delete] 해당 데이터가 존재하지 않음. id:" + id);
            return new OrderResDto(id, null, 0, OrderStatus.PENDING, false);
        }

        repository.delete(id);
        return new OrderResDto(id, null, 0, OrderStatus.PENDING, true);
    }

    public OrderResDto getOrderById(Long id) throws IOException
    {
        Optional<Order> findOrder = repository.findById(id);
        return findOrder.map(p -> new OrderResDto(p.getId(), p.getProductId(), p.getCount(), p.getStatus(), true))
                .orElse(new OrderResDto(id, null, 0, OrderStatus.PENDING, false));
    }

    public Page<OrderResDto> getAllOrders(Pageable pageable) throws IOException
    {
        List<OrderResDto> contents = repository.findAll(pageable).stream()
                .map(p -> new OrderResDto(p.getId(), p.getProductId(), p.getCount(), p.getStatus(), true))
                .collect(Collectors.toList());

        return new Page<OrderResDto>(contents, pageable.getPageNumber(), pageable.getPageSize(), repository.getTotalElements(), pageable.getSort());
    }
}