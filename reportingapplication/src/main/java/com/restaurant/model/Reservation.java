package com.restaurant.model;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
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

}