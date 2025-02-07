package com.example.product.service;

import com.example.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductService
{
    private final ProductRepository repository;

    @Autowired
    public ProductService(ProductRepository repository)
    {
        this.repository = repository;
    }


}
