package com.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailableTable {
    private String locationId;
    private String locationAddress;
    private String tableNumber;
    private String guestCapacity;
    private List<String> availableSlots;
}