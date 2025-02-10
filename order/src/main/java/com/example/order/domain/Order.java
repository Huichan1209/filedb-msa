package com.example.order.domain;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class Order
{
    private Long id;

    private Long productId;

    private int count;
}
