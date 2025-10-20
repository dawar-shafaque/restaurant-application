package com.restaurant.dto;

import com.restaurant.utils.ViewReservation;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewWaiterReservationResponse implements ViewReservation {
    private String reservationId;
    private String location;
    private String tableNumber;
    private String date;
    private String timeSlot;
    private Object preOrder;
    private String customerName;
    private String guestsNumber;
    private String status;

    public static ViewWaiterReservationResponse fromWaiterReservationResponse(WaiterReservationResponse response) {
        return ViewWaiterReservationResponse.builder()
                .reservationId(response.getReservationId())
                .location(response.getLocation())
                .tableNumber("Table " + response.getTableNumber())
                .date(response.getDate())
                .timeSlot(response.getTimeSlot())
                .preOrder(response.getPreOrder())
                .customerName(response.getCustomerName())
                .guestsNumber(response.getGuestsNumber() + " Guests")
                .status(response.getStatus())
                .build();
    }
}