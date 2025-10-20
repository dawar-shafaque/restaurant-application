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
public class User {

    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String role;
    private String createdAt;
    private String userAvatarUrl;

    @DynamoDbPartitionKey
    public String getEmail() {
        return email;
    }
}