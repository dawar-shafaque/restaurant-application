package com.restaurant.model;

import com.restaurant.model.converter.DateSlotArrayConverter;
import com.restaurant.utils.DateSlot;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Waiter {

    private String email;
    private String locationId;
    private DateSlot[] availableSlots;

    @DynamoDbPartitionKey
    public String getEmail() {
        return email;
    }

    @DynamoDbConvertedBy(DateSlotArrayConverter.class) // Use the same custom converter for time slots
    public DateSlot[] getAvailableSlots() {
        return availableSlots;
    }
}