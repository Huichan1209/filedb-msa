package com.example.order.dto.req;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString // AOP로 간단하게 매개변수 내용 toString해서 로그 찍을 용도
public class OrderReqDto
{
    private Long id;
    private Long productId;
    private int count;
}