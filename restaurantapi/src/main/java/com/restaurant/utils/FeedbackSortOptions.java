package com.restaurant.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public enum FeedbackSortOptions {
    BEST("best"),WORST("worst"),NEWEST("newest"),OLDEST("oldest");

    private final String sortOption;

    public static List<String> getAllSortOptions(){
        return Arrays.asList(BEST.getSortOption(),WORST.getSortOption(),NEWEST.getSortOption(),OLDEST.getSortOption());
    }
}
