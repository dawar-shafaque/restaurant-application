package com.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DishSummary {

    private int id;
    private String name;
    private String previewImageUrl;
    private String price;
    private String weight;
    private boolean available;
}
