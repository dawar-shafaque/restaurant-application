package com.restaurant.model;

import com.restaurant.model.converter.DateSlotArrayConverter;
import com.restaurant.utils.DateSlot;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    private String locationId;
    private String tableNumber;
    private Integer guestCapacity;
    private DateSlot[] availableSlots;

    @DynamoDbPartitionKey
    public String getLocationId() {
        return locationId;
    }

    @DynamoDbSortKey
    public String getTableNumber() {
        return tableNumber;
    }

    @DynamoDbConvertedBy(DateSlotArrayConverter.class)
    public DateSlot[] getAvailableSlots() {
        return availableSlots;
    }
}