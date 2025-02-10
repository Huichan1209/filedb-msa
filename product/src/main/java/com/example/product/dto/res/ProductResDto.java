package com.example.product.dto.res;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString // AOP로 간단하게 매개변수 내용 toString해서 로그 찍을 용도
public class ProductResDto
{
    private Long id;
    private String name;
    private int price;
    private boolean success = false;
}
