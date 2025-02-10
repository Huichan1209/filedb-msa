package com.example.product.controller;

import com.example.product.db.paging.Page;
import com.example.product.db.paging.PageRequest;
import com.example.product.db.paging.Pageable;
import com.example.product.db.paging.Sort;
import com.example.product.dto.req.ProductReqDto;
import com.example.product.dto.res.ProductResDto;
import com.example.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ProductController
{
    private final ProductService service;

    @Autowired
    public ProductController(ProductService service)
    {
        this.service = service;
    }

    @PostMapping
    public ProductResDto addProduct(@RequestBody ProductReqDto dto) throws Exception
    {
        return service.addProduct(dto);
    }

    @GetMapping("/{id}")
    public ProductResDto getProductById(@PathVariable Long id) throws Exception
    {
        return service.getProductById(id);
    }

    @GetMapping("/list")
    public Page<ProductResDto> getAllProducts(@RequestParam(required = false) Integer page,
                                              @RequestParam(required = false) Integer size,
                                              @RequestParam(required = false) String sortBy,
                                              @RequestParam(required = false) String direction) throws Exception
    {
        page = (page != null) ? page : 0;
        size = (size != null) ? size : 10;
        sortBy = (sortBy != null) ? sortBy : "id";
        direction = (direction != null) ? direction : "ASC";

        Pageable pageable = PageRequest.of(page, size, new Sort(sortBy, Sort.Direction.fromString(direction)));
        return service.getAllProducts(pageable);
    }

    @PutMapping("/{id}")
    public ProductResDto updateProduct(@PathVariable Long id, @RequestBody ProductReqDto dto) throws Exception
    {
        dto.setId(id);
        return service.updateProduct(dto);
    }

    @DeleteMapping("/{id}")
    public ProductResDto deleteProduct(@PathVariable Long id) throws Exception
    {
        return service.deleteProduct(id);
    }
}