package com.example.product.db.paging;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class Pageable
{
    private final int pageNumber;     // 페이지 번호
    private final int pageSize;       // 페이지 크기
    private final Sort sort;          // 정렬 정보

    public Pageable(int pageNumber, int pageSize, Sort sort)
    {
        if (pageNumber < 0) throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        if (pageSize < 1) throw new IllegalArgumentException("페이지 크기는 1 이상이어야 합니다.");

        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.sort = (sort != null) ? sort : Sort.unsorted();
    }

    public long getOffset()
    {
        return (long) pageNumber * pageSize;
    }

    public boolean isPaged()
    {
        return true;
    }
}
