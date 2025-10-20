package com.restaurant.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateReservationByWaiterResponse {

    private String id;
    private String date;
    private String timeSlot;  // formatted as "timeFrom-timeTo"
    private String tableNumber;
    private Integer guestsNumber;
    private String status;
    private Object preOrder;  // Using Object type since the structure isn't specified
    private String feedbackId;
    private String assignedWaiter;
    private String userInfo;
    private String assignedWaiterName;
    private String locationAddress;
    private String message;
}
