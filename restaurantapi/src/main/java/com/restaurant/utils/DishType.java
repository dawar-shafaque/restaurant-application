package com.restaurant.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.Set;

@Getter
@AllArgsConstructor
public enum DishType {
    APPETIZERS("Appetizers"),
    MAINCOURSES("Main Courses"),
    DESSERTS("Desserts");

    private final String dishes;

    public static Set<String> getAllDishTypes() {
        return Set.of(APPETIZERS.getDishes(),
                      MAINCOURSES.getDishes(),
                      DESSERTS.getDishes());
    }
}
