package com.restaurant.controller;

import com.epam.edai.run8.team16.dto.*;
import com.restaurant.dto.*;
import com.restaurant.exception.BadRequestException;
import com.restaurant.exception.InternalServerErrorException;
import com.restaurant.exception.UnAuthorizedException;
import com.epam.edai.run8.team16.service.*;
import com.restaurant.service.*;
import com.restaurant.utils.ViewReservation;
import com.restaurant.validator.ViewReservationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final UserReservationService userReservationService;
    private final UserService userService;
    private final WaiterService waiterService;
    private final ViewReservationValidator validator;
    private final WaiterReservationService waiterReservationService;
    private final TokenContextService tokenContextService;

    @GetMapping
    public ResponseEntity<List<ViewReservation>> getReservations(
            @RequestParam(required = false) String date,
            @RequestParam(required = false, defaultValue = "00:00") String time,
            @RequestParam(required = false, defaultValue = "Any Table") String tableNumber) {

        String email = tokenContextService.getEmailFromToken();

        if (email == null) {
            throw new IllegalArgumentException("Email parameter is required");
        }

        validator.validateEmail(email);

        String userRole = tokenContextService.getRoleFromToken();
        boolean isWaiter = validator.validateWaiterParameters(email, userRole);
        List<ViewReservation> result;

        if (isWaiter) {
            String locationId = waiterService.getWaiterLocation(email);
            List<WaiterReservationResponse> waiterReservations =
                    waiterReservationService.getWaiterReservations(email, locationId, date, time, tableNumber);
            List<ViewWaiterReservationResponse> formattedReservations = waiterReservations.stream()
                    .map(ViewWaiterReservationResponse::fromWaiterReservationResponse)
                    .toList();
            result = new ArrayList<>(formattedReservations);
        } else {
            List<ReservationResponse> reservations = userReservationService.getUserReservations(email);
            result = new ArrayList<>(reservations);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ReservationResponse> modifyReservation(@PathVariable String id, @RequestBody ModifyReservationRequest modifyReservationRequest){

        String userEmail = tokenContextService.getEmailFromToken();
        if (userEmail == null || userEmail.isEmpty()) {
            throw new UnAuthorizedException("Please log in to modify a reservation");
        }

        if (modifyReservationRequest == null) {
            throw new BadRequestException("Request body is required");
        }

        ReservationModificationRequest request = new ReservationModificationRequest();
        request.setReservationId(id);
        request.setTimeFrom(modifyReservationRequest.getTimeFrom());
        request.setTimeTo(modifyReservationRequest.getTimeTo());
        request.setGuestsNumber(modifyReservationRequest.getGuestsNumber());

        if (request.getReservationId() == null || request.getReservationId().isEmpty()) {
            throw new BadRequestException("Reservation ID is required");
        }

        // Validate time slot if provided
        if ((request.getTimeFrom() == null || request.getTimeFrom().isEmpty()) ||
                (request.getTimeTo() == null || request.getTimeTo().isEmpty())) {
            throw new BadRequestException("Both start time and end time must be provided together");
        }

        // Validate guests number if provided
        if (request.getGuestsNumber() != null && !request.getGuestsNumber().isEmpty()) {
            int guestsCount = Integer.parseInt(request.getGuestsNumber());
            if (guestsCount <= 0) {
                throw new BadRequestException("Number of guests must be positive");
            }
        }

        ReservationResponse response = userReservationService.modifyReservation(request, userEmail);
        return new ResponseEntity<>(response,HttpStatus.OK);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Response> deleteReservation (@PathVariable String id){

        if (id == null) {
            throw new BadRequestException("Reservation ID is required");
        }

        String userEmail = tokenContextService.getEmailFromToken();

        if (userEmail == null) {
            throw new UnAuthorizedException("Unauthorized: User not signed in");
        }

        boolean result = userReservationService.cancelReservation(id, userEmail);

        if(!result)
            throw new InternalServerErrorException("Failed to Cancel Reservation.");
        Response response = new Response();
        response.setMessage("Reservation cancelled successfully");
        return new ResponseEntity<>(response,HttpStatus.OK);
    }

    @GetMapping("{id}/details")
    public ResponseEntity<ReservationDetailsResponse> viewReservationDetails (@PathVariable String id){

        if (id == null) {
            throw new BadRequestException("Reservation ID is required");
        }

        String userEmail = tokenContextService.getEmailFromToken();

        if (userEmail == null || userEmail.isEmpty()) {
            throw new UnAuthorizedException("Unauthorized: User not signed in");
        }

        ReservationDetailsResponse response = userReservationService.getReservationDetails(id, userEmail);

        return new ResponseEntity<>(response,HttpStatus.OK);
    }
}