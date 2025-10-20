package com.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WaiterReservationResponse {

    private String reservationId;
    private String locationId;
    private String location;         // Location name (not just ID)
    private String tableNumber;
    private String date;
    private String timeSlot;         // Combined timeFrom and timeTo
    private String preOrder;         // Default "0" if empty
    private String customerName;     // Customer's name
    private String guestsNumber;
    private String status;
    private String waiterEmail;
    private String feedbackId;
    private String userId;           // Customer ID/email

}