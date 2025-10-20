package com.restaurant.controller;

import com.epam.edai.run8.team16.dto.*;
import com.restaurant.dto.*;
import com.restaurant.exception.BadRequestException;
import com.restaurant.exception.UnAuthorizedException;
import com.epam.edai.run8.team16.service.*;
import com.restaurant.service.BookingService;
import com.restaurant.service.TokenContextService;
import com.restaurant.service.UserReservationService;
import com.restaurant.service.WaiterReservationService;
import com.restaurant.validator.BookingValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final UserReservationService userReservationService;
    private final WaiterReservationService waiterReservationService;
    private final BookingValidator bookingValidator;
    private final TokenContextService tokenContextService;

    @GetMapping("/tables")
    public ResponseEntity<List<AvailableTable>> getAvailableTables(
            @RequestParam(required = true) String locationId,
            @RequestParam(required = true) String date,
            @RequestParam(required = true) String guests,
            @RequestParam(required = false, defaultValue = "00:00") String time) {

        bookingValidator.validateAvailableTablesRequest(locationId, date, guests);

        List<AvailableTable> availableTables = bookingService.getAvailableTables(locationId, date, time, guests);
        return new ResponseEntity<>(availableTables,HttpStatus.OK);
    }

    @PostMapping("/client")
    public ResponseEntity<ReservationResponse> createReservationByUser(@RequestBody ReservationRequest request) {

        String userEmail = tokenContextService.getEmailFromToken();
        if (userEmail == null || userEmail.isEmpty()) {
            throw new UnAuthorizedException("Please log in to make a reservation");
        }

        ReservationResponse response = userReservationService.createReservation(request, userEmail);
        return new ResponseEntity<>(response,HttpStatus.CREATED);
    }

    @PostMapping("/waiter")
    public ResponseEntity<CreateReservationByWaiterResponse> createReservationByWaiter(@RequestBody CreateReservationByWaiterRequest response) {

        String waiterEmail = tokenContextService.getEmailFromToken();
        if (waiterEmail == null || waiterEmail.isEmpty()) {
            throw new UnAuthorizedException("Unauthorized: Waiter not signed in");
        }

        CreateReservationByWaiterResponse result;
        if ("CUSTOMER".equalsIgnoreCase(response.getClientType())) {
            result = waiterReservationService.createReservationByWaiterForCustomer(response, waiterEmail);
        } else if ("VISITOR".equalsIgnoreCase(response.getClientType())) {
            result = waiterReservationService.createReservationByWaiterForVisitor(response, waiterEmail);
        } else {
            throw new BadRequestException("Invalid client type. Must be either CUSTOMER or VISITOR");
        }

        return new ResponseEntity<>(result,HttpStatus.CREATED);
    }
}