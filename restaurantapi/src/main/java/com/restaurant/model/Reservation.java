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
public class Reservation {

    private String reservationId;
    private String userId;
    private String locationId;
    private String tableNumber;
    private String date;
    private String timeFrom;
    private String timeTo;
    private String guestsNumber;
    private String status;
    private String preOrder;
    private String feedbackId;
    private String waiterEmail;

    @DynamoDbPartitionKey
    public String getReservationId() {
        return reservationId;
    }

}