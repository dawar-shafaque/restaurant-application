package com.restaurant.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dish {

    private int id;
    private String name;
    private String price;
    private String weight;
    private String imageUrl;
    @Setter
    private boolean popular;
    private String locationId;
    private String dishType;
    private String description;
    private String calories;
    private String fats;
    private String carbohydrates;
    private String proteins;
    private String vitamins;
    @Setter
    private boolean available;

    @DynamoDbPartitionKey
    public int getId() {
        return id;
    }

    @DynamoDbAttribute("isPopular")
    public boolean isPopular() {
        return popular;
    }

    @DynamoDbAttribute("isAvailable")
    public boolean isAvailable() {
        return available;
    }

}