package com.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModifyReservationRequest {
    String timeFrom;
    String timeTo;
    String guestsNumber;
}
