package com.restaurant.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateReservationByWaiterRequest {

    private String clientType; // "CUSTOMER" or "VISITOR"
    private String customerName; // For visitors or format "Name, email" for customers
    private String locationId;
    private String tableNumber;
    private String date;
    private String timeFrom;
    private String timeTo;
    private String guestsNumber;

}