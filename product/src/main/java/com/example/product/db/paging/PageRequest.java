package com.example.product.db.paging;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class PageRequest
{
    private final int pageNumber;
    private final int pageSize;
    private final Sort sort;

    private PageRequest(int pageNumber, int pageSize, Sort sort)
    {
        if (pageNumber < 0) throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        if (pageSize < 1) throw new IllegalArgumentException("페이지 크기는 1 이상이어야 합니다.");

        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.sort = (sort != null) ? sort : Sort.unsorted();
    }

    public static Pageable of(int pageNumber, int pageSize)
    {
        return new Pageable(pageNumber, pageSize, Sort.unsorted());
    }

    public static Pageable of(int pageNumber, int pageSize, Sort sort)
    {
        return new Pageable(pageNumber, pageSize, sort);
    }

    public long getOffset() {
        return (long) pageNumber * pageSize;
    }
}
