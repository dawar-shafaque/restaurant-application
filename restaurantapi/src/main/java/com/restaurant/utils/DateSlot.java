package com.restaurant.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DateSlot {

    private String date;
    private String[] availableTimeSlots;

}