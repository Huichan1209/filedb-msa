package com.example.order.controller;

import com.example.order.db.paging.Page;
import com.example.order.db.paging.PageRequest;
import com.example.order.db.paging.Pageable;
import com.example.order.db.paging.Sort;
import com.example.order.dto.req.OrderReqDto;
import com.example.order.dto.res.OrderResDto;
import com.example.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class OrderController
{
    private OrderService service;

    @Autowired
    public OrderController(OrderService service)
    {
        this.service = service;
    }

    @PostMapping
    public OrderResDto addProduct(@RequestBody OrderReqDto dto) throws Exception
    {
        return service.addOrder(dto);
    }

    @GetMapping("/{id}")
    public OrderResDto getOrderById(@PathVariable Long id) throws Exception
    {
        return service.getOrderById(id);
    }

    @GetMapping("/list")
    public Page<OrderResDto> getAllProducts(@RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer size,
                                            @RequestParam(required = false) String sortBy,
                                            @RequestParam(required = false) String direction) throws Exception
    {
        page = (page != null) ? page : 0;
        size = (size != null) ? size : 10;
        sortBy = (sortBy != null) ? sortBy : "id";
        direction = (direction != null) ? direction : "ASC";

        Pageable pageable = PageRequest.of(page, size, new Sort(sortBy, Sort.Direction.fromString(direction)));
        return service.getAllOrders(pageable);
    }

    @PutMapping("/{id}")
    public OrderResDto updateOrder(@PathVariable Long id, @RequestBody OrderReqDto dto) throws Exception
    {
        dto.setId(id);
        return service.updateOrder(dto);
    }

    @DeleteMapping("/{id}")
    public OrderResDto deleteOrder(@PathVariable Long id) throws Exception
    {
        return service.deleteOrder(id);
    }
}