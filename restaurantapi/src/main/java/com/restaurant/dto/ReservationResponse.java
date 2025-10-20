package com.restaurant.dto;

import com.restaurant.utils.ViewReservation;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReservationResponse implements ViewReservation {

    private String id;
    private String locationId;
    private String tableNumber;
    private String date;
    private String timeFrom;
    private String timeTo;
    private String guestsNumber;
    private String status;
    private String waiterEmail;
    private String preOrder;
    private String feedbackId;
    private String message;

}