package com.restaurant.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class PaginatedResponse<T> {

    private int totalPages;
    private int totalElements;
    private int size;
    private List<T> content;
    private int number;
    private boolean first;
    private boolean last;
    private int numberOfElements;
    private boolean empty;
    private Pageable pageable;
    private List<Sort> sort;

    @Data
    @NoArgsConstructor
    public static class Pageable {

        private int offset;
        private List<Sort> sort;
        private boolean paged;
        private int pageSize;
        private int pageNumber;
        private boolean unPaged;

    }

    @Data
    @NoArgsConstructor
    public static class Sort {
        private String direction;
        private String nullHandling;
        private boolean ascending;
        private String property;
        private boolean ignoreCase;

    }
}