package com.example.order.db.paging;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@NoArgsConstructor(force = true)
@AllArgsConstructor
@Getter
@ToString
public class Page<T>
{
    private final List<T> content;
    private final int pageNumber;
    private final int pageSize;
    private final long totalElements;
    private final Sort sort;

    public int getTotalPages()
    {
        return (int) Math.ceil((double) totalElements / pageSize);
    }

    public boolean hasNext()
    {
        return pageNumber + 1 < getTotalPages();
    }

    public boolean hasPrevious()
    {
        return pageNumber > 0;
    }

    public boolean isFirst()
    {
        return pageNumber == 0;
    }

    public boolean isLast()
    {
        return pageNumber + 1 == getTotalPages();
    }
}