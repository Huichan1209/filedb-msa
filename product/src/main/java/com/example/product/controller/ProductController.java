package com.example.product.controller;

import com.example.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController
{
    private final ProductService service;

    @Autowired
    public ProductController(ProductService service)
    {
        this.service = service;
    }


}
