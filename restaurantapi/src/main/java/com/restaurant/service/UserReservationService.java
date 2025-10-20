package com.restaurant.service;

import com.restaurant.dto.ReservationDetailsResponse;
import com.restaurant.dto.ReservationModificationRequest;
import com.restaurant.dto.ReservationRequest;
import com.restaurant.dto.ReservationResponse;
import com.epam.edai.run8.team16.exception.*;
import com.restaurant.exception.*;
import com.restaurant.model.Location;
import com.restaurant.utils.TimeSlot;
import com.restaurant.model.Booking;
import com.restaurant.utils.DateSlot;
import com.restaurant.model.Reservation;
import com.restaurant.model.Waiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserReservationService {

    private final WaiterService waiterService;
    private final UserService userService;
    private final DynamoDbTable<Booking> bookingTable;
    private final DynamoDbTable<Reservation> reservationTable;
    private final DynamoDbTable<Waiter> waiterTable;
    private final DynamoDbTable<Location> locationTable;

    //Create Reservation by User
    public ReservationResponse createReservation(ReservationRequest request, String userId) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        log.info("Starting reservation creation process for user: {}, location: {}", userId, request.getLocationId());

        if (request.getLocationId() == null) {
            throw new BadRequestException("Location is required.");
        }

        if(request.getTableNumber().isBlank()){
            throw new BadRequestException("Table Number is required.");
        }

        if(request.getDate().isBlank()){
            throw new BadRequestException("Date cannot be empty.");
        }

        // Check if the user has the CUSTOMER role
        String userRole = userService.getUserRole(userId);
        if (userRole == null) {
            throw new NotFoundException("User not found");
        }

        if (!"Customer".equalsIgnoreCase(userRole)) {
            log.warn("Unauthorized reservation attempt by non-customer user: {}, role: {}", userId, userRole);
            throw new ForbiddenException("Only customers can make reservations");
        }

        validateReservationRequest(request);

        // Check if the location exists
        try {
            Location location = locationTable.getItem(Key.builder()
                    .partitionValue(request.getLocationId())
                    .build());

            if (location == null) {
                log.error("Location not found for locationId: {}", request.getLocationId());
                throw new NotFoundException("Location with ID " + request.getLocationId() + " not found");
            }

            log.info("Location validated: {} - {}", location.getId(), location.getAddress());
        } catch (DynamoDbException e) {
            log.error("Error checking location existence: {}", e.getMessage(), e);
            throw new InternalServerErrorException("Failed to validate location: " + e.getMessage());
        }

        // Check if the requested time slot exists in the bookings table
        Booking booking = bookingTable.getItem(Key.builder()
                .partitionValue(request.getLocationId())
                .sortValue(request.getTableNumber())
                .build());

        if (booking == null) {
            log.error("Table not found for locationId: {} and tableNumber: {}",
                    request.getLocationId(), request.getTableNumber());
            throw new NotFoundException("Table not found");
        }

        int requestedGuests;
        requestedGuests = Integer.parseInt(request.getGuestsNumber());

        // Check if the number of guests is less than 1
        if (requestedGuests < 1) {
            log.error("Invalid number of guests: {}. Must be at least 1", request.getGuestsNumber());
            throw new BadRequestException("Number of guests must be at least 1");
        }

        Integer tableCapacity = booking.getGuestCapacity();
        if (tableCapacity == null) {
            log.error("Table capacity not defined for table {} at location {}",
                    request.getTableNumber(), request.getLocationId());
            throw new InternalServerErrorException("Table capacity not defined");
        }

        if (requestedGuests > tableCapacity) {
            log.warn("Requested guests ({}) exceeds table capacity ({}) for table {} at location {}",
                    requestedGuests, tableCapacity, request.getTableNumber(), request.getLocationId());
            throw new BadRequestException("Number of guests (" + requestedGuests +
                    ") exceeds table capacity of " + tableCapacity);
        }

        // Check if the requested time slot is available
        String requestedTimeSlot = request.getTimeFrom() + " - " + request.getTimeTo();
        boolean isSlotAvailable = false;

        DateSlot[] availableSlots = booking.getAvailableSlots();
// The converter returns empty array instead of null, but we'll keep the null check for robustness
        if (availableSlots != null && availableSlots.length > 0) {
            for (DateSlot dateSlot : availableSlots) {
                if (dateSlot != null && dateSlot.getDate() != null &&
                        dateSlot.getDate().equals(request.getDate())) {
                    String[] timeSlots = dateSlot.getAvailableTimeSlots();
                    if (timeSlots != null && timeSlots.length > 0) {
                        for (String slot : timeSlots) {
                            if (slot != null && slot.equals(requestedTimeSlot)) {
                                isSlotAvailable = true;
                                break;
                            }
                        }
                    }
                    // We found the matching date slot, no need to check other dates
                    break;
                }
            }
        }

        if (!isSlotAvailable) {
            log.warn("Requested time slot {} is not available for table {} at location {} on date {}",
                    requestedTimeSlot, request.getTableNumber(), request.getLocationId(), request.getDate());
            throw new BadRequestException("The requested time slot is not available for this table");
        }

        String waiterEmail;
        try {
            log.info("Finding least busy waiter for location: {}, date: {}, time: {}",
                    request.getLocationId(), request.getDate(), request.getTimeFrom());
            waiterEmail = waiterService.findLeastBusyWaiter(
                    request.getLocationId(),
                    request.getDate(),
                    request.getTimeFrom()
            );
            log.info("Least busy waiter found: {}", waiterEmail);
        } catch (Exception e) {
            log.error("Failed to find least busy waiter: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to find available waiter: " + e.getMessage());
        }

        // Create reservation
        Reservation reservation = new Reservation();
        reservation.setReservationId(UUID.randomUUID().toString().substring(0, 8));
        reservation.setUserId(userId);
        reservation.setLocationId(request.getLocationId());
        reservation.setTableNumber(request.getTableNumber());
        reservation.setDate(request.getDate());
        reservation.setTimeFrom(request.getTimeFrom());
        reservation.setTimeTo(request.getTimeTo());
        reservation.setGuestsNumber(request.getGuestsNumber());
        reservation.setStatus("RESERVED");
        reservation.setWaiterEmail(waiterEmail);
        reservation.setPreOrder("");
        reservation.setFeedbackId("");

        log.info("Creating reservation: {}", reservation);

        try {
            log.info("Saving reservation to DynamoDB table.");
            reservationTable.putItem(reservation);
            log.info("Reservation successfully saved: {}", reservation.getReservationId());
        } catch (DynamoDbException e) {
            log.error("Failed to save reservation: {}", e.getMessage(), e);
            throw new InternalServerErrorException("Failed to save reservation: " + e.getMessage());
        }

        try {
            log.info("Updating waiter availability for waiter: {}, date: {}, time: {}",
                    waiterEmail, request.getDate(), request.getTimeFrom());
            waiterService.updateWaiterAvailability(waiterEmail, request.getDate(), request.getTimeFrom());
            log.info("Waiter availability successfully updated.");
        } catch (DynamoDbException e) {
            log.warn("Failed to update waiter availability: {}", e.getMessage());
        }

        try {
            log.info("Updating booking availability for location: {}, table: {}, date: {}, timeFrom: {}, timeTo: {}",
                    request.getLocationId(), request.getTableNumber(), request.getDate(), request.getTimeFrom(), request.getTimeTo());
            updateBookingAvailability(request.getLocationId(), request.getTableNumber(),
                    request.getDate(), request.getTimeFrom(), request.getTimeTo());
            log.info("Booking availability successfully updated.");
        } catch (DynamoDbException e) {
            log.warn("Failed to update booking availability: {}", e.getMessage());
        }

        log.info("Reservation successfully created: {}", reservation.getReservationId());
        return getReservationResponse(reservation);
    }

    public void validateReservationRequest(ReservationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Reservation request cannot be null");
        }

        if (request.getLocationId() == null || request.getLocationId().isEmpty()) {
            throw new IllegalArgumentException("Location ID is required");
        }
        if (request.getTableNumber() == null || request.getTableNumber().isEmpty()) {
            throw new IllegalArgumentException("Table number is required");
        }
        if (request.getDate() == null || request.getDate().isEmpty()) {
            throw new IllegalArgumentException("Date is required");
        }
        if (request.getTimeFrom() == null || request.getTimeFrom().isEmpty()) {
            throw new IllegalArgumentException("Start time is required");
        }
        if (request.getTimeTo() == null || request.getTimeTo().isEmpty()) {
            throw new IllegalArgumentException("End time is required");
        }
        if (request.getGuestsNumber() == null || request.getGuestsNumber().isEmpty()) {
            throw new IllegalArgumentException("Number of guests is required");
        }

        String timeFrom = request.getTimeFrom();
        String timeTo = request.getTimeTo();

        boolean validTimeSlot = false;
        for (String slot : TimeSlot.getAllTimeSlots()) {
            String[] parts = slot.split(" - ");
            if (parts.length == 2 && parts[0].equals(timeFrom) && parts[1].equals(timeTo)) {
                validTimeSlot = true;
                break;
            }
        }

        if (!validTimeSlot) {
            throw new IllegalArgumentException("Invalid time slot. Start time: " + timeFrom + ", End time: " + timeTo);
        }

        LocalDate requestDate;
        try {
            requestDate = LocalDate.parse(request.getDate(), DateTimeFormatter.ISO_DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Use YYYY-MM-DD format.");
        }

        // Check if the reservation date is between today and 30 days from now
        LocalDate today = LocalDate.now();
        LocalDate maxDate = LocalDate.now().plusDays(30);

        if (requestDate.isBefore(today)) {
            throw new IllegalArgumentException("Reservations can only be made for today or future dates.");
        }

        if (requestDate.isAfter(maxDate)) {
            throw new IllegalArgumentException("Reservations can only be made up to 30 days in advance.");
        }

        // Add check for today's reservations to ensure time hasn't passed
        if (requestDate.equals(today)) {
            if (!isValidTimeForTodayBooking(request.getTimeFrom())) {
                throw new IllegalArgumentException("For same-day reservations, booking must be at least 30 minutes in advance.");
            }
        }
    }

    private boolean isValidTimeForTodayBooking(String requestedTime) {
        LocalTime currentTime = LocalTime.now();
        LocalTime requestedLocalTime = LocalTime.parse(requestedTime);

        // Add buffer time (e.g., 30 minutes) to ensure there's enough time to prepare
        LocalTime cutoffTime = currentTime.plusMinutes(30);

        return requestedLocalTime.isAfter(cutoffTime);
    }

    public void updateBookingAvailability(String locationId, String tableNumber, String date, String timeFrom, String timeTo) {
        String timeSlot = timeFrom + " - " + timeTo;

        try {
            Booking booking = bookingTable.getItem(Key.builder()
                    .partitionValue(locationId)
                    .sortValue(tableNumber)
                    .build());

            if (booking != null) {
                DateSlot[] updatedSlots = removeSlotFromBookingAvailableSlots(
                        booking.getAvailableSlots(),
                        date,
                        timeSlot
                );

                booking.setAvailableSlots(updatedSlots);
                bookingTable.updateItem(booking);
                log.debug("Updated booking availability for locationId: {} and tableNumber: {}",
                        locationId, tableNumber);
            } else {
                log.warn("Booking not found for locationId: {} and tableNumber: {}",
                        locationId, tableNumber);
            }
        } catch (Exception e) {
            log.error("Failed to update booking availability: {}", e.getMessage());
            throw new InternalServerErrorException("Failed to update booking availability: " + e.getMessage());
        }
    }

    public static ReservationResponse getReservationResponse(Reservation reservation) {
        log.debug("Generating reservation response for reservationId: {}", reservation.getReservationId());
        ReservationResponse response = ReservationResponse.builder()
                .id(reservation.getReservationId())
                .locationId(reservation.getLocationId())
                .tableNumber(reservation.getTableNumber())
                .date(reservation.getDate())
                .timeFrom(reservation.getTimeFrom())
                .timeTo(reservation.getTimeTo())
                .guestsNumber(reservation.getGuestsNumber())
                .status(reservation.getStatus())
                .waiterEmail(reservation.getWaiterEmail())
                .message("Reservation confirmed! See you soon.")
                .build();

        log.debug("Setting pre-order and feedback fields in the response.");
        response.setPreOrder(reservation.getPreOrder());
        response.setFeedbackId(reservation.getFeedbackId());
        log.debug("Reservation response successfully generated: {}", response);
        return response;
    }

    public DateSlot[] removeSlotFromBookingAvailableSlots(DateSlot[] availableSlots, String date, String timeSlot) {
        if (availableSlots == null || availableSlots.length == 0) {
            return availableSlots;
        }

        for (int i = 0; i < availableSlots.length; i++) {
            DateSlot dateSlot = availableSlots[i];
            if (dateSlot != null && dateSlot.getDate() != null && dateSlot.getDate().equals(date)) {
                String[] existingTimeSlots = dateSlot.getAvailableTimeSlots();
                if (existingTimeSlots == null || existingTimeSlots.length == 0) {
                    continue;
                }

                List<String> timeSlotList = new ArrayList<>(Arrays.asList(existingTimeSlots));

                timeSlotList.remove(timeSlot);

                if (timeSlotList.isEmpty()) {
                    DateSlot[] newSlots = new DateSlot[availableSlots.length - 1];
                    System.arraycopy(availableSlots, 0, newSlots, 0, i);
                    if (i < availableSlots.length - 1) {
                        System.arraycopy(availableSlots, i + 1, newSlots, i, availableSlots.length - i - 1);
                    }
                    return newSlots;
                } else {
                    dateSlot.setAvailableTimeSlots(timeSlotList.toArray(new String[0]));
                    return availableSlots;
                }
            }
        }

        return availableSlots;
    }

    //View Reservation by User
    public List<ReservationResponse> getUserReservations(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }

        try {
            updateReservationStatuses();
            List<Reservation> userReservations = reservationTable.scan().items().stream()
                    .filter(reservation -> reservation.getUserId().equals(userId))
                    .toList();

            log.debug("Found {} reservations for user {}", userReservations.size(), userId);
            return userReservations.stream()
                    .map(this::convertToReservationResponse)
                    .toList();
        } catch (Exception e) {
            log.error("Failed to get reservations for user {}: {}", userId, e.getMessage());
            throw new InternalServerErrorException("Failed to get user reservations: " + e.getMessage());
        }
    }

    public void updateReservationStatuses() {
        try {
            LocalDateTime now = LocalDateTime.now();

            List<Reservation> allReservations = reservationTable.scan().items().stream()
                    .filter(reservation -> "RESERVED".equals(reservation.getStatus()))
                    .toList();

            log.debug("Updating statuses for {} reservations", allReservations.size());

            for (Reservation reservation : allReservations) {
                try {
                    LocalDate reservationDate = LocalDate.parse(reservation.getDate());
                    LocalTime reservationTimeFrom = LocalTime.parse(reservation.getTimeFrom());
                    LocalTime reservationTimeTo = LocalTime.parse(reservation.getTimeTo());

                    LocalDateTime reservationStart = LocalDateTime.of(reservationDate, reservationTimeFrom);
                    LocalDateTime reservationEnd = LocalDateTime.of(reservationDate, reservationTimeTo);

                    String originalStatus = reservation.getStatus();

                    if (now.isAfter(reservationStart) && now.isBefore(reservationEnd)) {
                        reservation.setStatus("IN_PROGRESS");
                    } else if (now.isAfter(reservationEnd)) {
                        reservation.setStatus("FINISHED");
                    }

                    if (!originalStatus.equals(reservation.getStatus())) {
                        reservationTable.updateItem(reservation);
                        log.debug("Updated reservation {} status from {} to {}",
                                reservation.getReservationId(), originalStatus, reservation.getStatus());
                    }
                } catch (DateTimeParseException e) {
                    log.warn("Invalid date/time format in reservation {}: {}",
                            reservation.getReservationId(), e.getMessage());
                } catch (Exception e) {
                    log.warn("Failed to update status for reservation {}: {}",
                            reservation.getReservationId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to update reservation statuses: {}", e.getMessage());
            throw new InternalServerErrorException("Failed to update reservation statuses: " + e.getMessage());
        }
    }

    private ReservationResponse convertToReservationResponse(Reservation reservation) {
        ReservationResponse response = ReservationResponse.builder()
                .id(reservation.getReservationId())
                .locationId(reservation.getLocationId())
                .tableNumber(reservation.getTableNumber())
                .date(reservation.getDate())
                .timeFrom(reservation.getTimeFrom())
                .timeTo(reservation.getTimeTo())
                .guestsNumber(reservation.getGuestsNumber())
                .status(reservation.getStatus())
                .waiterEmail(reservation.getWaiterEmail())
                .message("Reservations successfully retrieved!")
                .build();

        response.setPreOrder(reservation.getPreOrder() != null ? reservation.getPreOrder() : "");
        response.setFeedbackId(reservation.getFeedbackId() != null ? reservation.getFeedbackId() : "");

        return response;
    }

    //Cancel Reservation
    public boolean cancelReservation(String reservationId, String userEmail){
        try {
            Reservation reservation = reservationTable.getItem(Key.builder()
                    .partitionValue(reservationId)
                    .build());

            if (reservation == null) {
                log.info("Reservation not found: {}", reservationId);
                throw new BadRequestException("Reservation not found");
            }

            if (!isAuthorizedToCancel(reservation, userEmail)) {
                log.warn("Unauthorized cancellation attempt for reservation {} by user {}",
                        reservationId, userEmail);
                throw new UnAuthorizedException("Not authorized to cancel this reservation");
            }

            if (!canCancelByTime(reservation)) {
                log.info("Cancellation time limit exceeded for reservation {}", reservationId);
                throw new BadRequestException("Reservations can only be cancelled at least 30 minutes before the reservation time");
            }

            boolean isVisitorReservation = "VISITOR".equals(reservation.getUserId());

            if (isVisitorReservation) {
                // For visitor reservations, delete the item completely
                log.info("Deleting visitor reservation: {}", reservationId);
                reservationTable.deleteItem(Key.builder()
                        .partitionValue(reservationId)
                        .build());
                log.info("Visitor reservation {} deleted successfully", reservationId);
            } else {
                // For regular reservations, just update the status
                reservation.setStatus("CANCELLED");
                reservationTable.updateItem(reservation);
                log.info("Reservation {} cancelled successfully", reservationId);
            }

            boolean hasWarnings = false;
            StringBuilder warningMessages = new StringBuilder();

            try {
                updateBookingSlots(reservation);
            } catch (Exception e) {
                hasWarnings = true;
                log.warn("Error updating booking slots for reservation {}: {}",
                        reservationId, e.getMessage());
                warningMessages.append("Error updating booking slots: ").append(e.getMessage()).append("; ");
            }

            try {
                updateWaiterAvailability(reservation);
            } catch (Exception e) {
                hasWarnings = true;
                log.warn("Error updating waiter availability for reservation {}: {}",
                        reservationId, e.getMessage());
                warningMessages.append("Error updating waiter availability: ").append(e.getMessage());
            }

            if (hasWarnings) {
                throw new InternalServerErrorException("Reservation " +
                        (isVisitorReservation ? "deleted" : "cancelled") +
                        " but with warnings: " + warningMessages);
            }

            return true;
        } catch (DynamoDbException e) {
            log.error("Failed to cancel reservation {}: {}", reservationId, e.getMessage());
            throw new InternalServerErrorException("Failed to cancel reservation: " + e.getMessage());
        }

    }

    public boolean isAuthorizedToCancel(Reservation reservation, String userEmail) {
        return reservation.getUserId().equals(userEmail) ||
                (reservation.getWaiterEmail() != null && reservation.getWaiterEmail().equals(userEmail));
    }

    public boolean canCancelByTime(Reservation reservation) {
        try {
            LocalDate reservationDate = LocalDate.parse(reservation.getDate());
            LocalTime reservationTime = LocalTime.parse(reservation.getTimeFrom());
            LocalDateTime reservationDateTime = LocalDateTime.of(reservationDate, reservationTime);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime cutoffTime = reservationDateTime.minusMinutes(30);
            return now.isBefore(cutoffTime);
        } catch (DateTimeParseException e) {
            log.error("Invalid date/time format in reservation {}: {}",
                    reservation.getReservationId(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Error checking cancellation time for reservation {}: {}",
                    reservation.getReservationId(), e.getMessage());
            return false;
        }
    }

    private void updateBookingSlots(Reservation reservation) {
        try {
            Booking booking = bookingTable.getItem(Key.builder()
                    .partitionValue(reservation.getLocationId())
                    .sortValue(reservation.getTableNumber())
                    .build());

            if (booking == null) {
                log.warn("Booking was not found for locationId: {} and tableNumber: {}",
                        reservation.getLocationId(), reservation.getTableNumber());
                return;
            }

            DateSlot[] updatedSlots = addSlotToBookingAvailableSlots(
                    booking.getAvailableSlots(),
                    reservation.getDate(),
                    reservation.getTimeFrom(),
                    reservation.getTimeTo()
            );

            booking.setAvailableSlots(updatedSlots);
            bookingTable.updateItem(booking);
            log.debug("Updated booking slots for locationId: {} and tableNumber: {}",
                    reservation.getLocationId(), reservation.getTableNumber());
        } catch (Exception e) {
            log.error("Failed to update booking slots: {}", e.getMessage());
            throw new InternalServerErrorException("Failed to update booking slots: " + e.getMessage());
        }
    }

    private void updateWaiterAvailability(Reservation reservation) {
        try {
            if (reservation.getWaiterEmail() == null || reservation.getWaiterEmail().isEmpty()) {
                log.warn("No waiter email found in reservation {}", reservation.getReservationId());
                return;
            }

            Waiter waiter = waiterTable.getItem(Key.builder()
                    .partitionValue(reservation.getWaiterEmail())
                    .build());

            if (waiter == null) {
                log.warn("Waiter not found with email: {}", reservation.getWaiterEmail());
                return;
            }

            if (waiter.getAvailableSlots() != null) {
                DateSlot[] updatedWaiterSlots = addSlotToBookingAvailableSlots(
                        waiter.getAvailableSlots(),
                        reservation.getDate(),
                        reservation.getTimeFrom(),
                        reservation.getTimeTo()
                );
                waiter.setAvailableSlots(updatedWaiterSlots);
                waiterTable.updateItem(waiter);
                log.debug("Updated availability for waiter: {}", reservation.getWaiterEmail());
            }
        } catch (Exception e) {
            log.error("Failed to update waiter availability: {}", e.getMessage());
            throw new InternalServerErrorException("Failed to update waiter availability: " + e.getMessage());
        }
    }

    public DateSlot[] addSlotToBookingAvailableSlots(DateSlot[] availableSlots, String date, String timeFrom, String timeTo) {
        String timeSlot = timeFrom + " - " + timeTo;

        if (availableSlots == null || availableSlots.length == 0) {
            DateSlot newDateSlot = new DateSlot();
            newDateSlot.setDate(date);
            newDateSlot.setAvailableTimeSlots(new String[]{timeSlot});
            return new DateSlot[]{newDateSlot};
        }

        for (DateSlot dateSlot : availableSlots) {
            if (dateSlot.getDate().equals(date)) {
                String[] existingTimeSlots = dateSlot.getAvailableTimeSlots();
                String[] newTimeSlots = Arrays.copyOf(existingTimeSlots, existingTimeSlots.length + 1);
                newTimeSlots[existingTimeSlots.length] = timeSlot;

                Arrays.sort(newTimeSlots);

                dateSlot.setAvailableTimeSlots(newTimeSlots);
                return availableSlots;
            }
        }

        DateSlot newDateSlot = new DateSlot();
        newDateSlot.setDate(date);
        newDateSlot.setAvailableTimeSlots(new String[]{timeSlot});

        DateSlot[] newAvailableSlots = Arrays.copyOf(availableSlots, availableSlots.length + 1);
        newAvailableSlots[availableSlots.length] = newDateSlot;

        return newAvailableSlots;
    }

    //View Existing Reservation details before modification
    public ReservationDetailsResponse getReservationDetails(String reservationId, String userEmail) {
        if (reservationId == null || reservationId.isEmpty()) {
            throw new IllegalArgumentException("Reservation ID is required");
        }

        try {
            Reservation reservation = reservationTable.getItem(Key.builder()
                    .partitionValue(reservationId)
                    .build());

            if (reservation == null) {
                log.info("Reservation not found: {}", reservationId);
                throw new IllegalArgumentException("Reservation not found");
            }

            if (!isAuthorizedToModify(reservation, userEmail)) {
                log.warn("Unauthorized access attempt for reservation {} by user {}",
                        reservationId, userEmail);
                throw new UnAuthorizedException("Not authorized to access this reservation");
            }

            if (!canModifyByTime(reservation)) {
                log.info("Modification time limit exceeded for reservation {}", reservationId);
                throw new IllegalArgumentException("Reservations can only be modified at least 2 hours before the reservation time");
            }

            if (!"RESERVED".equals(reservation.getStatus())) {
                throw new IllegalArgumentException("Only reservations with status 'RESERVED' can be modified");
            }

            ReservationDetailsResponse response = new ReservationDetailsResponse();
            response.setReservationId(reservation.getReservationId());
            response.setTimeFrom(reservation.getTimeFrom());
            response.setTimeTo(reservation.getTimeTo());
            response.setGuestsNumber(reservation.getGuestsNumber());
            response.setDate(reservation.getDate());
            response.setLocationId(reservation.getLocationId());
            response.setTableNumber(reservation.getTableNumber());

            return response;
        } catch (DynamoDbException e) {
            log.error("Failed to get reservation details {}: {}", reservationId, e.getMessage());
            throw new InternalServerErrorException("Failed to get reservation details: " + e.getMessage());
        }
    }

    //Modify Reservation
    public ReservationResponse modifyReservation(ReservationModificationRequest request, String userEmail) {
        if (request == null || request.getReservationId() == null || request.getReservationId().isEmpty()) {
            throw new IllegalArgumentException("Reservation ID is required");
        }

        try {
            Reservation reservation = reservationTable.getItem(Key.builder()
                    .partitionValue(request.getReservationId())
                    .build());

            if (reservation == null) {
                log.info("Reservation not found: {}", request.getReservationId());
                throw new IllegalArgumentException("Reservation not found");
            }

            if (!isAuthorizedToModify(reservation, userEmail)) {
                log.warn("Unauthorized modification attempt for reservation {} by user {}",
                        request.getReservationId(), userEmail);
                throw new UnAuthorizedException("Not authorized to modify this reservation");
            }

            if (!canModifyByTime(reservation)) {
                log.info("Modification time limit exceeded for reservation {}", request.getReservationId());
                throw new IllegalArgumentException("Reservations can only be modified at least 2 hours before the reservation time");
            }

            if (!"RESERVED".equals(reservation.getStatus())) {
                throw new IllegalArgumentException("Only reservations with status 'RESERVED' can be modified");
            }

            // Save old values for updating availability
            String oldTimeFrom = reservation.getTimeFrom();
            String oldTimeTo = reservation.getTimeTo();
            String oldGuestsNumber = reservation.getGuestsNumber();
            String oldWaiterEmail = reservation.getWaiterEmail();

            // Check if time slot is changing
            boolean isTimeChanging = (request.getTimeFrom() != null && !request.getTimeFrom().isEmpty() &&
                    !request.getTimeFrom().equals(oldTimeFrom)) ||
                    (request.getTimeTo() != null && !request.getTimeTo().isEmpty() &&
                            !request.getTimeTo().equals(oldTimeTo));

            // Check if guests number is changing
            boolean isGuestsChanging = request.getGuestsNumber() != null &&
                    !request.getGuestsNumber().isEmpty() &&
                    !request.getGuestsNumber().equals(oldGuestsNumber);

            // If time or guests are changing, verify that the table is available
            if (isTimeChanging || isGuestsChanging) {
                String newTimeFrom = (request.getTimeFrom() != null && !request.getTimeFrom().isEmpty())
                        ? request.getTimeFrom() : oldTimeFrom;
                String newTimeTo = (request.getTimeTo() != null && !request.getTimeTo().isEmpty())
                        ? request.getTimeTo() : oldTimeTo;
                String newGuestsNumber = (request.getGuestsNumber() != null && !request.getGuestsNumber().isEmpty())
                        ? request.getGuestsNumber() : oldGuestsNumber;

                // Validate the new time slot format
                validateTimeSlot(newTimeFrom, newTimeTo);

                // Check if the new time slot is available for this table
                // Check if the new time slot is available for this table
                if (!isTableAvailableForModification(
                        reservation.getLocationId(),
                        reservation.getTableNumber(),
                        reservation.getDate(),
                        newTimeFrom,
                        Integer.parseInt(newGuestsNumber),
                        reservation.getReservationId())) {
                    throw new InternalServerErrorException("Failed to find available slot with given guest number for this table.");
                }
            }

            // Create a copy of the reservation with old values for later use
            Reservation oldReservation = new Reservation();
            oldReservation.setReservationId(reservation.getReservationId());
            oldReservation.setUserId(reservation.getUserId());
            oldReservation.setLocationId(reservation.getLocationId());
            oldReservation.setTableNumber(reservation.getTableNumber());
            oldReservation.setDate(reservation.getDate());
            oldReservation.setTimeFrom(oldTimeFrom);
            oldReservation.setTimeTo(oldTimeTo);
            oldReservation.setGuestsNumber(oldGuestsNumber);
            oldReservation.setStatus(reservation.getStatus());
            oldReservation.setWaiterEmail(oldWaiterEmail);
            oldReservation.setPreOrder(reservation.getPreOrder());
            oldReservation.setFeedbackId(reservation.getFeedbackId());

            // Update reservation with new values
            if (request.getTimeFrom() != null && !request.getTimeFrom().isEmpty()) {
                reservation.setTimeFrom(request.getTimeFrom());
            }

            if (request.getTimeTo() != null && !request.getTimeTo().isEmpty()) {
                reservation.setTimeTo(request.getTimeTo());
            }

            if (request.getGuestsNumber() != null && !request.getGuestsNumber().isEmpty()) {
                reservation.setGuestsNumber(request.getGuestsNumber());
            }

            // If time is changing, assign a new waiter
            if (isTimeChanging) {
                try {
                    String newWaiterEmail = waiterService.findLeastBusyWaiter(
                            reservation.getLocationId(),
                            reservation.getDate(),
                            reservation.getTimeFrom()
                    );

                    // Update the reservation with the new waiter
                    reservation.setWaiterEmail(newWaiterEmail);

                    // Log the waiter change
                    if (!newWaiterEmail.equals(oldWaiterEmail)) {
                        log.info("Waiter reassigned from {} to {} for reservation {}",
                                oldWaiterEmail, newWaiterEmail, reservation.getReservationId());
                    }
                } catch (Exception e) {
                    log.error("Failed to find waiter for modified reservation: {}", e.getMessage());
                    throw new InternalServerErrorException("Failed to find available waiter: " + e.getMessage());
                }
            }

            // Update reservation in database
            reservationTable.updateItem(reservation);
            log.info("Reservation {} modified successfully", request.getReservationId());

            boolean hasWarnings = false;
            StringBuilder warningMessages = new StringBuilder();

            // Update booking and waiter availability if time changed
            if (isTimeChanging) {
                try {
                    // First free up the old time slot
                    updateBookingSlotsForModification(oldReservation, true);
                    // Then book the new time slot
                    updateBookingSlotsForModification(reservation, false);
                } catch (Exception e) {
                    hasWarnings = true;
                    log.warn("Error updating booking slots for reservation {}: {}",
                            request.getReservationId(), e.getMessage());
                    warningMessages.append("Error updating booking slots: ").append(e.getMessage()).append("; ");
                }

                try {
                    // First free up the old waiter's time slot
                    if (oldWaiterEmail != null && !oldWaiterEmail.isEmpty()) {
                        updateWaiterAvailabilityForModification(oldReservation, true);
                    }

                    // Then book the new waiter's time slot
                    if (reservation.getWaiterEmail() != null && !reservation.getWaiterEmail().isEmpty()) {
                        updateWaiterAvailabilityForModification(reservation, false);
                    }
                } catch (Exception e) {
                    hasWarnings = true;
                    log.warn("Error updating waiter availability for reservation {}: {}",
                            request.getReservationId(), e.getMessage());
                    warningMessages.append("Error updating waiter availability: ").append(e.getMessage());
                }
            }

            if (hasWarnings) {
                log.warn("Reservation modified but with warnings: {}", warningMessages);
                // We don't throw an exception here as we want to return the modified reservation
            }

            return getReservationResponse(reservation);
        } catch (DynamoDbException e) {
            log.error("Failed to modify reservation {}: {}", request.getReservationId(), e.getMessage());
            throw new InternalServerErrorException("Failed to modify reservation: " + e.getMessage());
        }
    }

    public boolean isAuthorizedToModify(Reservation reservation, String userEmail) {
        return reservation.getUserId().equals(userEmail) ||
                (reservation.getWaiterEmail() != null && reservation.getWaiterEmail().equals(userEmail));
    }

    public boolean canModifyByTime(Reservation reservation) {
        try {
            LocalDate reservationDate = LocalDate.parse(reservation.getDate());
            LocalTime reservationTime = LocalTime.parse(reservation.getTimeFrom());
            LocalDateTime reservationDateTime = LocalDateTime.of(reservationDate, reservationTime);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime cutoffTime = reservationDateTime.minusHours(2);
            return now.isBefore(cutoffTime);
        } catch (DateTimeParseException e) {
            log.error("Invalid date/time format in reservation {}: {}",
                    reservation.getReservationId(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Error checking modification time for reservation {}: {}",
                    reservation.getReservationId(), e.getMessage());
            return false;
        }
    }

    public void validateTimeSlot(String timeFrom, String timeTo) {
        if ((timeFrom == null || timeFrom.isEmpty()) && (timeTo == null || timeTo.isEmpty())) {
            return; // No time changes requested
        }

        if ((timeFrom != null && !timeFrom.isEmpty()) && (timeTo == null || timeTo.isEmpty())) {
            throw new IllegalArgumentException("If start time is provided, end time must also be provided");
        }

        if ((timeTo != null && !timeTo.isEmpty()) && (timeFrom == null || timeFrom.isEmpty())) {
            throw new IllegalArgumentException("If end time is provided, start time must also be provided");
        }

        String timeSlot = timeFrom + " - " + timeTo;
        boolean validTimeSlot = false;
        for (String slot : TimeSlot.getAllTimeSlots()) {
            if (slot.equals(timeSlot)) {
                validTimeSlot = true;
                break;
            }
        }

        if (!validTimeSlot) {
            throw new IllegalArgumentException("Invalid time slot. Start time: " + timeFrom + ", End time: " + timeTo);
        }
    }

    public boolean isTableAvailableForModification(String locationId, String tableNumber,
                                                    String date, String timeFrom, int guestsCount,
                                                    String reservationId) {
        try {
            // Get the booking for this table
            Booking booking = bookingTable.getItem(Key.builder()
                    .partitionValue(locationId)
                    .sortValue(tableNumber)
                    .build());

            if (booking == null) {
                log.warn("Booking not found for locationId: {} and tableNumber: {}", locationId, tableNumber);
                throw new NotFoundException("Booking not found for locationId: " + locationId + " and tableNumber: " + tableNumber);
            }

            // Check if the guest capacity is sufficient
            if (booking.getGuestCapacity() < guestsCount) {
                log.info("Table {} at location {} cannot accommodate {} guests (capacity: {})",
                        tableNumber, locationId, guestsCount, booking.getGuestCapacity());
                throw new BadRequestException("Table " + tableNumber + " at location " + locationId + " cannot accommodate " + guestsCount + " guests (capacity: " + booking.getGuestCapacity() + ")");
            }

            // Get the current reservation to check if we're modifying the same time slot
            Reservation currentReservation = reservationTable.getItem(Key.builder()
                    .partitionValue(reservationId)
                    .build());

            if (currentReservation == null) {
                throw new NotFoundException("Current reservation not found with ID: " + reservationId);
            }

            if (currentReservation.getTimeFrom().equals(timeFrom)) {
                return true;
            }

            // If we're changing the time, check if the new time slot is available
            String requestedTimeSlot = timeFrom + " - " + getEndTimeFromStartTime(timeFrom);

            // Check if the requested time slot is available for this date
            DateSlot[] availableSlots = booking.getAvailableSlots();
            if (availableSlots == null) {
                throw new NotFoundException("No available slots found for table " + tableNumber);
            }

            boolean dateFound = false;
            for (DateSlot dateSlot : availableSlots) {
                if (dateSlot != null && dateSlot.getDate() != null && dateSlot.getDate().equals(date)) {
                    dateFound = true;
                    String[] timeSlots = dateSlot.getAvailableTimeSlots();
                    if (timeSlots == null || timeSlots.length == 0) {
                        throw new NotFoundException("No time slots available for date " + date);
                    }

                    // Look for the exact time slot
                    for (String slot : timeSlots) {
                        if (slot.equals(requestedTimeSlot)) {
                            return true;
                        }
                    }
                }
            }

            if (!dateFound) {
                throw new NotFoundException("Date " + date + " not found in available slots");
            }

            // Special case: check if any other reservation (except this one) is using this time slot
            boolean isTimeSlotAvailable = !isTimeSlotBookedByAnotherReservation(
                    locationId, tableNumber, date, timeFrom, requestedTimeSlot, reservationId);

            if (!isTimeSlotAvailable) {
                throw new BadRequestException("Time slot " + requestedTimeSlot +
                        " is already booked by another reservation");
            }

            return isTimeSlotAvailable;

        } catch (DynamoDbException e) {
            log.error("Error checking table availability for modification: {}", e.getMessage());
            return false;
        }
    }

    private void updateBookingSlotsForModification(Reservation reservation, boolean makeAvailable) {
        try {
            Booking booking = bookingTable.getItem(Key.builder()
                    .partitionValue(reservation.getLocationId())
                    .sortValue(reservation.getTableNumber())
                    .build());

            if (booking == null) {
                log.warn("Booking not found for locationId: {} and tableNumber: {}",
                        reservation.getLocationId(), reservation.getTableNumber());
                return;
            }

            DateSlot[] availableSlots = booking.getAvailableSlots();
            if (availableSlots == null) {
                if (makeAvailable) {
                    // Initialize with a single date slot if we're making a slot available
                    availableSlots = new DateSlot[1];
                    DateSlot newDateSlot = new DateSlot();
                    newDateSlot.setDate(reservation.getDate());
                    newDateSlot.setAvailableTimeSlots(new String[]{reservation.getTimeFrom() + " - " + reservation.getTimeTo()});
                    availableSlots[0] = newDateSlot;
                    booking.setAvailableSlots(availableSlots);
                    bookingTable.updateItem(booking);
                }
                return;
            }

            String timeSlot = reservation.getTimeFrom() + " - " + reservation.getTimeTo();

            // Find the date slot for this reservation
            boolean dateFound = false;
            for (DateSlot dateSlot : availableSlots) {
                if (dateSlot.getDate().equals(reservation.getDate())) {
                    dateFound = true;
                    String[] timeSlots = dateSlot.getAvailableTimeSlots();

                    if (makeAvailable) {
                        // Adding the slot back to available slots
                        if (timeSlots == null) {
                            dateSlot.setAvailableTimeSlots(new String[]{timeSlot});
                        } else {
                            // Check if the slot already exists
                            boolean slotExists = false;
                            for (String slot : timeSlots) {
                                if (slot.equals(timeSlot)) {
                                    slotExists = true;
                                    break;
                                }
                            }

                            if (!slotExists) {
                                // Add the slot
                                String[] newTimeSlots = Arrays.copyOf(timeSlots, timeSlots.length + 1);
                                newTimeSlots[timeSlots.length] = timeSlot;
                                dateSlot.setAvailableTimeSlots(newTimeSlots);
                            }
                        }
                    } else {
                        // Removing the slot from available slots
                        if (timeSlots != null && timeSlots.length > 0) {
                            List<String> updatedSlots = new ArrayList<>();
                            for (String slot : timeSlots) {
                                if (!slot.equals(timeSlot)) {
                                    updatedSlots.add(slot);
                                }
                            }
                            dateSlot.setAvailableTimeSlots(updatedSlots.toArray(new String[0]));
                        }
                    }
                    break;
                }
            }

            if (!dateFound && makeAvailable) {
                // Add a new date slot if we're making a slot available
                DateSlot[] newAvailableSlots = Arrays.copyOf(availableSlots, availableSlots.length + 1);
                DateSlot newDateSlot = new DateSlot();
                newDateSlot.setDate(reservation.getDate());
                newDateSlot.setAvailableTimeSlots(new String[]{timeSlot});
                newAvailableSlots[availableSlots.length] = newDateSlot;
                availableSlots = newAvailableSlots;
            }

            booking.setAvailableSlots(availableSlots);
            bookingTable.updateItem(booking);
            log.debug("{} time slot {} for table {} at location {}",
                    makeAvailable ? "Added" : "Removed", timeSlot,
                    reservation.getTableNumber(), reservation.getLocationId());
        } catch (Exception e) {
            log.error("Failed to update booking slots: {}", e.getMessage());
            throw new InternalServerErrorException("Failed to update booking availability: " + e.getMessage());
        }
    }

    private void updateWaiterAvailabilityForModification(Reservation reservation, boolean makeAvailable) {
        String waiterEmail = reservation.getWaiterEmail();
        if (waiterEmail == null || waiterEmail.isEmpty()) {
            return;
        }

        try {
            Waiter waiter = waiterTable.getItem(Key.builder()
                    .partitionValue(waiterEmail)
                    .build());

            if (waiter == null) {
                log.warn("Waiter {} not found", waiterEmail);
                return;
            }

            DateSlot[] availableSlots = waiter.getAvailableSlots();
            if (availableSlots == null) {
                if (makeAvailable) {
                    // Initialize with a single date slot if we're making a slot available
                    availableSlots = new DateSlot[1];
                    DateSlot newDateSlot = new DateSlot();
                    newDateSlot.setDate(reservation.getDate());
                    newDateSlot.setAvailableTimeSlots(new String[]{reservation.getTimeFrom() + " - " + reservation.getTimeTo()});
                    availableSlots[0] = newDateSlot;
                    waiter.setAvailableSlots(availableSlots);
                    waiterTable.updateItem(waiter);
                }
                return;
            }

            String timeSlot = reservation.getTimeFrom() + " - " + reservation.getTimeTo();

            // Find the date slot for this reservation
            boolean dateFound = false;
            for (DateSlot dateSlot : availableSlots) {
                if (dateSlot.getDate().equals(reservation.getDate())) {
                    dateFound = true;
                    String[] timeSlots = dateSlot.getAvailableTimeSlots();

                    if (makeAvailable) {
                        // Adding the slot back to available slots
                        if (timeSlots == null) {
                            dateSlot.setAvailableTimeSlots(new String[]{timeSlot});
                        } else {
                            // Check if the slot already exists
                            boolean slotExists = false;
                            for (String slot : timeSlots) {
                                if (slot.equals(timeSlot)) {
                                    slotExists = true;
                                    break;
                                }
                            }

                            if (!slotExists) {
                                // Add the slot
                                String[] newTimeSlots = Arrays.copyOf(timeSlots, timeSlots.length + 1);
                                newTimeSlots[timeSlots.length] = timeSlot;
                                dateSlot.setAvailableTimeSlots(newTimeSlots);
                            }
                        }
                    } else {
                        // Removing the slot from available slots
                        if (timeSlots != null && timeSlots.length > 0) {
                            List<String> updatedSlots = new ArrayList<>();
                            for (String slot : timeSlots) {
                                if (!slot.equals(timeSlot)) {
                                    updatedSlots.add(slot);
                                }
                            }
                            dateSlot.setAvailableTimeSlots(updatedSlots.toArray(new String[0]));
                        }
                    }
                    break;
                }
            }

            if (!dateFound && makeAvailable) {
                // Add a new date slot if we're making a slot available
                DateSlot[] newAvailableSlots = Arrays.copyOf(availableSlots, availableSlots.length + 1);
                DateSlot newDateSlot = new DateSlot();
                newDateSlot.setDate(reservation.getDate());
                newDateSlot.setAvailableTimeSlots(new String[]{timeSlot});
                newAvailableSlots[availableSlots.length] = newDateSlot;
                availableSlots = newAvailableSlots;
            }

            waiter.setAvailableSlots(availableSlots);
            waiterTable.updateItem(waiter);
            log.debug("{} time slot {} for waiter {}",
                    makeAvailable ? "Added" : "Removed", timeSlot, waiterEmail);
        } catch (Exception e) {
            log.error("Failed to update waiter availability: {}", e.getMessage());
            throw new InternalServerErrorException("Failed to update waiter availability: " + e.getMessage());
        }
    }

    private String getEndTimeFromStartTime(String startTime) {
        for (String slot : TimeSlot.getAllTimeSlots()) {
            String[] parts = slot.split(" - ");
            if (parts.length == 2 && parts[0].equals(startTime)) {
                return parts[1];
            }
        }
        // Default to 2 hours later if not found (should not happen with validation)
        return startTime;
    }

    private boolean isTimeSlotBookedByAnotherReservation(String locationId, String tableNumber,
                                                         String date, String timeFrom, String timeSlot,
                                                         String currentReservationId) {
        try {
            // Scan all reservations for this location, table, and date
            List<Reservation> reservations = reservationTable.scan().items().stream()
                    .filter(r -> r.getLocationId().equals(locationId) &&
                            r.getTableNumber().equals(tableNumber) &&
                            r.getDate().equals(date) &&
                            "RESERVED".equals(r.getStatus()) &&
                            !r.getReservationId().equals(currentReservationId)) // Exclude current reservation
                    .toList();

            // Check if any other reservation is using this time slot
            for (Reservation r : reservations) {
                String reservationTimeSlot = r.getTimeFrom() + " - " + r.getTimeTo();
                if (reservationTimeSlot.equals(timeSlot)) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            log.error("Error checking if time slot is booked by another reservation: {}", e.getMessage());
            return true; // Assume booked if there's an error
        }
    }
}