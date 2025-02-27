package com.rentalplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PageDto<T> {
    private List<T> content;
    private int totalPages;
    private long totalElements;
    private int size;
    private int number;

    public PageDto(Page<T> page) {
        this.content = page.getContent();
        this.totalPages = page.getTotalPages();
        this.totalElements = page.getTotalElements();
        this.size = page.getSize();
        this.number = page.getNumber();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageDto<?> pageDto = (PageDto<?>) o;
        return totalPages == pageDto.totalPages && totalElements == pageDto.totalElements && size == pageDto.size;
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalPages, totalElements, size);
    }
}
