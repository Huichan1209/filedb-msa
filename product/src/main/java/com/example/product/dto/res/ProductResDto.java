package com.example.product.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProductResDto
{
    private Long id;
    private String name;
    private int price;
    private boolean success = false;
}
