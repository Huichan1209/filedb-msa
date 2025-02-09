package com.example.product.db.paging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Sort
{
    private final List<Order> orders;

    public Sort(List<Order> orders)
    {
        this.orders = orders;
    }

    public static Sort by(Direction direction, String... properties)
    {
        List<Order> orders = new ArrayList<>();
        for (String property : properties)
        {
            orders.add(new Order(direction, property));
        }

        return new Sort(orders);
    }

    public static Sort unsorted()
    {
        return new Sort(Collections.emptyList());
    }

    public List<Order> getOrders()
    {
        return orders;
    }

    public static class Order
    {
        private final Direction direction;
        private final String property;

        public Order(Direction direction, String property)
        {
            this.direction = direction;
            this.property = property;
        }

        public Direction getDirection()
        {
            return direction;
        }

        public String getProperty()
        {
            return property;
        }
    }

    public enum Direction
    {
        ASC,
        DESC;
    }
}
