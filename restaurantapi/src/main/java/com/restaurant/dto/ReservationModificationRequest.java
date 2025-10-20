package com.restaurant.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReservationModificationRequest {

    private String reservationId;
    private String timeFrom;
    private String timeTo;
    private String guestsNumber;

}