package com.example.order.dto.req;

import com.example.order.domain.OrderStatus;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class OrderReqDto
{
    private Long id;
    private Long productId;
    private int count;
    private OrderStatus status;
}