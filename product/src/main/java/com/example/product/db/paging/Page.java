package com.example.product.db.paging;

import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
public class Page<T>
{
    private final List<T> content;
    private final int pageNumber;
    private final int pageSize;
    private final long totalElements;
    private final Sort sort;

    public Page(List<T> content, int pageNumber, int pageSize, long totalElements, Sort sort)
    {
        this.content = content;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.sort = sort;
    }

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