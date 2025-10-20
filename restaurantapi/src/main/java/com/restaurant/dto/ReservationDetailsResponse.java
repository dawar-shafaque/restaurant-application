package com.restaurant.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReservationDetailsResponse {

    private String reservationId;
    private String timeFrom;
    private String timeTo;
    private String guestsNumber;
    private String date;
    private String locationId;
    private String tableNumber;

}