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
public class Feedback {

    private String id;
    private String locationId;
    private String rate;
    private String comment;
    private String userName;
    private String userAvatarUrl;
    private String date;
    private String type;
    private String reservationId;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

}