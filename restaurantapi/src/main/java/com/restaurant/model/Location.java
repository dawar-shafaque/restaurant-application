package com.restaurant.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Location {
    private String id;
    private String address;
    private String description;
    private Integer totalCapacity;
    private String averageOccupancy;
    private String imageUrl;
    private String rating;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }
}