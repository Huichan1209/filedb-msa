package com.example.order.service;

import com.example.order.db.paging.Page;
import com.example.order.db.paging.Pageable;
import com.example.order.domain.Order;
import com.example.order.dto.req.OrderReqDto;
import com.example.order.dto.res.OrderResDto;
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
    private OrderRepository repository;

    @Autowired
    public OrderService(OrderRepository repository)
    {
        this.repository = repository;
    }

    public OrderResDto addOrder (OrderReqDto reqDto) throws Exception
    {
        Order order = new Order(null, reqDto.getProductId(), reqDto.getCount());
        repository.save(order);
        return new OrderResDto(order.getId(), order.getProductId(), order.getCount(), true);
    }

    public OrderResDto updateOrder(OrderReqDto reqDto) throws Exception
    {
        Order order = new Order(reqDto.getId(), reqDto.getProductId(), reqDto.getCount());
        if(!repository.findById(reqDto.getId()).isPresent())
        {
            System.out.println("[Service.update] 해당 데이터가 존재하지 않음. id:" + reqDto.getId());
            return new OrderResDto(reqDto.getId(), reqDto.getProductId(), reqDto.getCount(), false);
        }

        Order resultOrder = repository.save(order);
        return new OrderResDto(resultOrder.getId(), resultOrder.getProductId(), resultOrder.getCount(), true);
    }

    public OrderResDto deleteOrder(Long id) throws Exception
    {
        if(!repository.findById(id).isPresent())
        {
            System.out.println("[Service.delete] 해당 데이터가 존재하지 않음. id:" + id);
            return new OrderResDto(id, null, 0, false);
        }

        repository.delete(id);
        return new OrderResDto(id, null, 0, true);
    }

    public OrderResDto getOrderById(Long id) throws IOException
    {
        Optional<Order> findOrder = repository.findById(id);
        return findOrder.map(p -> new OrderResDto(p.getId(), p.getProductId(), p.getCount(), true))
                .orElse(new OrderResDto(id, null, 0, false));
    }

    public Page<OrderResDto> getAllOrders(Pageable pageable) throws IOException
    {
        List<OrderResDto> contents = repository.findAll(pageable).stream()
                .map(p -> new OrderResDto(p.getId(), p.getProductId(), p.getCount(), true))
                .collect(Collectors.toList());

        return new Page<OrderResDto>(contents, pageable.getPageNumber(), pageable.getPageSize(), repository.getTotalElements(), pageable.getSort());
    }
}
