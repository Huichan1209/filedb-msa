package com.example.product.db.paging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Sort
{
    private final String property;
    private final Direction direction;

    private static final Sort UNSORTED = new Sort(null, null);

    public Sort(String property, Direction direction)
    {
        this.property = property;
        this.direction = direction;
    }

    public String getProperty()
    {
        return property;
    }

    public Direction getDirection()
    {
        return direction;
    }

    public boolean isAscending()
    {
        return this.direction == Direction.ASC;
    }

    public static Sort by(String property)
    {
        return new Sort(property, Direction.ASC);
    }

    public static Sort by(String property, Direction direction)
    {
        return new Sort(property, direction);
    }

    public static Sort byDescending(String property)
    {
        return new Sort(property, Direction.DESC);
    }

    public static Sort unsorted()
    {
        return UNSORTED;
    }

    public boolean isUnsorted()
    {
        return this.property == null && this.direction == null;
    }

    public enum Direction
    {
        ASC,
        DESC;

        public static Direction fromString(String value)
        {
            if (value == null || value.trim().isEmpty())
            {
                throw new IllegalArgumentException("Direction 값은 null 또는 빈 문자열일 수 없습니다.");
            }
            switch (value.trim().toUpperCase())
            {
                case "ASC":
                    return ASC;
                case "DESC":
                    return DESC;
                default:
                    return ASC;
            }
        }
    }
}