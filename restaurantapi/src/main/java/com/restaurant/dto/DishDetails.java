package com.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DishDetails {

    private int id;
    private String name;
    private String description;
    private String imageUrl;
    private String price;
    private String weight;
    private String dishType;
    private String calories;
    private String fats;
    private String carbohydrates;
    private String proteins;
    private String vitamins;
    private boolean state;
    private boolean popular;
    private String locationId;
}
