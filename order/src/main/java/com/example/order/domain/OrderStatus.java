package com.example.order.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.ToString;

public enum OrderStatus
{
    PENDING, // 보류
    COMPLETED, // 채결(정상 주문)
    FAILED; // 실패(재고 부족 등으로 실패한 상태)

    @JsonCreator
    public static OrderStatus fromString(String str)
    {
        switch (str.toUpperCase())
        {
            // case "PENDING": return OrderStatus.PENDING;
            case "COMPLETED": return OrderStatus.COMPLETED;
            case "FAILED": return OrderStatus.FAILED;
            default: return OrderStatus.PENDING;
        }
    }
}
