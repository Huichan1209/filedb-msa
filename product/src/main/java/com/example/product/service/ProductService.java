package com.example.product.service;

import com.example.product.db.paging.Page;
import com.example.product.db.paging.Pageable;
import com.example.product.domain.Product;
import com.example.product.dto.req.ProductReqDto;
import com.example.product.dto.res.ProductResDto;
import com.example.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService
{
    private final ProductRepository repository;

    @Autowired
    public ProductService(ProductRepository repository)
    {
        this.repository = repository;
    }

    public ProductResDto addProduct (ProductReqDto reqDto) throws Exception
    {
        Product product = new Product(null, reqDto.getName(), reqDto.getPrice());
        repository.save(product);
        return new ProductResDto(product.getId(), product.getName(), product.getPrice(), true);
    }

    public ProductResDto updateProduct(ProductReqDto reqDto) throws Exception
    {
        Product product = new Product(reqDto.getId(), reqDto.getName(), reqDto.getPrice());
        if(!repository.findById(reqDto.getId()).isPresent())
        {
            System.out.println("[Service.update] 해당 데이터가 존재하지 않음.");
            return new ProductResDto(reqDto.getId(), reqDto.getName(), reqDto.getPrice(), false);
        }

        Product resultProduct = repository.save(product);
        return new ProductResDto(resultProduct.getId(), resultProduct.getName(), resultProduct.getPrice(), true);
    }

    public ProductResDto deleteProduct(Long id) throws Exception
    {
        if(!repository.findById(id).isPresent())
        {
            System.out.println("[Service.delete] 해당 데이터가 존재하지 않음.");
            return new ProductResDto(id, "", 0, false);
        }

        repository.delete(id);
        return new ProductResDto(id, "", 0, true);
    }

    public ProductResDto getProductById(Long id) throws IOException
    {
        Optional<Product> findProduct = repository.findById(id);
        return findProduct.map(p -> new ProductResDto(p.getId(), p.getName(), p.getPrice(), true))
                .orElse(new ProductResDto(id, "", 0, false));
    }

    public List<ProductResDto> getAllProducts(Pageable pageable) throws IOException
    {
        Page<Product> productPage = repository.findAll(pageable);
        return productPage.getContent().stream()
                .map(p -> new ProductResDto(p.getId(), p.getName(), p.getPrice(), true))
                .collect(Collectors.toList());
    }
}