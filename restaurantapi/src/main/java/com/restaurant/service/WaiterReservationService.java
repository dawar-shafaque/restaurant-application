package com.restaurant.service;

import com.epam.edai.run8.team16.dto.*;
import com.restaurant.dto.*;
import com.restaurant.exception.BadRequestException;
import com.restaurant.exception.ForbiddenException;
import com.restaurant.exception.InternalServerErrorException;
import com.restaurant.model.Reservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaiterReservationService {

    private final DynamoDbTable<Reservation> reservationTable;
    private final WaiterService waiterService;
    private final LocationService locationService;
    private final UserService userService;
    private final UserReservationService userReservationService;
    private final BookingService bookingService;

    //Create Reservation By Waiter for Customer
    public CreateReservationByWaiterResponse createReservationByWaiterForCustomer(CreateReservationByWaiterRequest waiterRequest, String waiterEmail) {

        String userRole = userService.getUserRole(waiterEmail);
        if (!"WAITER".equalsIgnoreCase(userRole)) {
            throw new ForbiddenException("Forbidden: Only waiters can create reservations through this endpoint");
        }

        // Get waiter's assigned location
        String waiterLocationId = waiterService.getWaiterLocation(waiterEmail);
        if (waiterLocationId == null) {
            throw new BadRequestException("Waiter is not assigned to any location");
        }

        // Parse request
        if (waiterRequest == null) {
            throw new BadRequestException("Request body is required");
        }

        if (!waiterLocationId.equals(waiterRequest.getLocationId())) {
            throw new ForbiddenException("Waiter can only create reservations for their assigned location");
        }
        if(waiterRequest.getDate().isEmpty()) {
            throw new BadRequestException("Date cannot be empty");
        }
        if(waiterRequest.getTableNumber().isEmpty()) {
            throw new BadRequestException("Table number cannot be empty");
        }
        // Validate date format
        LocalDate requestedDate;
        try {
            requestedDate = LocalDate.parse(waiterRequest.getDate(), DateTimeFormatter.ISO_DATE);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid date format. Use ISO format (YYYY-MM-DD)");
        }

        // Validate guests number
        try {
            int guestsCount = Integer.parseInt(waiterRequest.getGuestsNumber());
            if (guestsCount <= 0) {
                throw new BadRequestException("Number of guests must be positive");
            }
        } catch (DynamoDbException e) {
            throw new BadRequestException("Invalid guests format. Must be a number");
        }

        // Validate date range
        LocalDate today = LocalDate.now();

        // Update date range validation to allow today's date
        if (requestedDate.isBefore(today) || requestedDate.isAfter(today.plusDays(30))) {
            throw new BadRequestException("Bookings can only be made between today and 30 days from today");
        }

        // Add check for today's bookings to ensure time hasn't passed
        if (requestedDate.equals(today)) {
            LocalTime currentTime = LocalTime.now();
            LocalTime requestedTime = LocalTime.parse(waiterRequest.getTimeFrom());

            // Add buffer time (30 minutes) to ensure there's enough time to prepare
            LocalTime cutoffTime = currentTime.plusMinutes(30);

            if (requestedTime.isBefore(cutoffTime)) {
                throw new BadRequestException("For same-day reservations, booking must be at least 30 minutes in advance.");
            }
        }

        // Check if the table is available
        List<AvailableTable> availableTables = bookingService.getAvailableTablesTimeSlots(
                waiterRequest.getLocationId(),
                waiterRequest.getDate(),
                waiterRequest.getTimeFrom(),
                waiterRequest.getGuestsNumber()
        );

        // Check if the requested table is available
        boolean tableAvailable = false;
        boolean validGuestsCount = false;
        int tableCapacity = bookingService.getTableCapacity(waiterRequest.getLocationId(),waiterRequest.getTableNumber());
        for (AvailableTable table : availableTables) {
            if (table.getTableNumber().equals(waiterRequest.getTableNumber())) {
                // Check if the time slot is available
                String requestedTimeSlot = waiterRequest.getTimeFrom() + " - " + waiterRequest.getTimeTo();
                if (table.getAvailableSlots().contains(requestedTimeSlot)) {
                    tableAvailable = true;
                }
            }
        }

        // Check if the guest capacity is sufficient
        int guestRequested = Integer.parseInt(waiterRequest.getGuestsNumber());
        if (tableCapacity >= guestRequested) {
            validGuestsCount = true ;
        }

        if(!tableAvailable && !validGuestsCount){
            throw new BadRequestException("The requested table is not available for the specified time slot and number of guests");
        }

        if (!tableAvailable) {
            throw new BadRequestException("The requested table is not available for the specified time slot.");
        }
        if (!validGuestsCount){
            throw new BadRequestException("The requested table cannot accommodate " + waiterRequest.getGuestsNumber() +" guests (capacity: " + tableCapacity + ")");

        }

        // Extract customer email from the format "Name, email@example.com"
        String customerEmail = extractCustomerEmail(waiterRequest.getCustomerName());
        if (customerEmail == null) {
            throw new IllegalArgumentException("Invalid customer format. Expected format: 'Name, email@example.com'");
        }

        // Verify customer exists
        try {
            if (!userService.userExists(customerEmail)) {
                throw new IllegalArgumentException("Customer with email " + customerEmail + " does not exist");
            }
        } catch (DynamoDbException e) {
            log.error("Error verifying customer existence: {}", e.getMessage());
            throw new InternalServerErrorException("Error verifying customer: " + e.getMessage());
        }

        // Convert waiter request to regular reservation request
        ReservationRequest request = convertToReservationRequest(waiterRequest);

        // Find the least busy waiter
        String assignedWaiterEmail;
        try {
            assignedWaiterEmail = waiterService.findLeastBusyWaiter(
                    request.getLocationId(),
                    request.getDate(),
                    request.getTimeFrom()
            );
        } catch (Exception e) {
            log.error("Failed to find least busy waiter: {}", e.getMessage());
            throw new InternalServerErrorException("Failed to find available waiter: " + e.getMessage());
        }

        // Create the reservation using the customer's email as userId
        Reservation reservation = createReservationEntity(request, customerEmail);

        // Set the assigned waiter email
        reservation.setWaiterEmail(assignedWaiterEmail);

        try {
            reservationTable.putItem(reservation);
        } catch (Exception e) {
            log.error("Failed to save reservation: {}", e.getMessage());
            throw new InternalServerErrorException("Failed to save reservation: " + e.getMessage());
        }

        try {
            waiterService.updateWaiterAvailability(assignedWaiterEmail, request.getDate(), request.getTimeFrom());
        } catch (Exception e) {
            log.warn("Failed to update waiter availability: {}", e.getMessage());
            // Continue despite error - we'll handle this as a partial success
        }

        try {
            userReservationService.updateBookingAvailability(request.getLocationId(), request.getTableNumber(),
                    request.getDate(), request.getTimeFrom(), request.getTimeTo());
        } catch (Exception e) {
            log.warn("Failed to update booking availability: {}", e.getMessage());
            // Continue despite error - we'll handle this as a partial success
        }

        ReservationResponse reservationResponse = UserReservationService.getReservationResponse(reservation);
        CreateReservationByWaiterResponse result = new CreateReservationByWaiterResponse();

        result.setId(reservationResponse.getId());
        result.setDate(reservationResponse.getDate());
        result.setTimeSlot(reservationResponse.getTimeFrom() + "-" + reservationResponse.getTimeTo());
        result.setTableNumber(reservationResponse.getTableNumber());
        result.setGuestsNumber(Integer.valueOf(reservationResponse.getGuestsNumber()));
        result.setStatus(reservationResponse.getStatus());
        result.setPreOrder(reservationResponse.getPreOrder());
        result.setFeedbackId(reservationResponse.getFeedbackId());
        result.setAssignedWaiter(reservationResponse.getWaiterEmail());
        result.setUserInfo("Customer " + waiterRequest.getCustomerName());

        String waiterName = userService.getUserName(reservationResponse.getWaiterEmail());
        result.setAssignedWaiterName(waiterName);
        result.setLocationAddress("Reserved at " + waiterLocationId);
        result.setMessage("Your booking is complete. Thank you!");
        return result;
    }

    private String extractCustomerEmail(String customerName) {
        if (customerName == null || customerName.isEmpty()) {
            return null;
        }

        Pattern pattern = Pattern.compile(".*,\\s*([\\w.]+@[\\w.]+)");
        Matcher matcher = pattern.matcher(customerName);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private ReservationRequest convertToReservationRequest(CreateReservationByWaiterRequest waiterRequest) {
        ReservationRequest request = new ReservationRequest();
        request.setLocationId(waiterRequest.getLocationId());
        request.setTableNumber(waiterRequest.getTableNumber());
        request.setDate(waiterRequest.getDate());
        request.setTimeFrom(waiterRequest.getTimeFrom());
        request.setTimeTo(waiterRequest.getTimeTo());
        request.setGuestsNumber(waiterRequest.getGuestsNumber());
        return request;
    }

    private Reservation createReservationEntity(ReservationRequest request, String userId) {
        userReservationService.validateReservationRequest(request);

        Reservation reservation = new Reservation();
        reservation.setReservationId(UUID.randomUUID().toString());
        reservation.setUserId(userId);
        reservation.setLocationId(request.getLocationId());
        reservation.setTableNumber(request.getTableNumber());
        reservation.setDate(request.getDate());
        reservation.setTimeFrom(request.getTimeFrom());
        reservation.setTimeTo(request.getTimeTo());
        reservation.setGuestsNumber(request.getGuestsNumber());
        reservation.setStatus("RESERVED");
        reservation.setPreOrder("");
        reservation.setFeedbackId("");

        return reservation;
    }

    //Create Reservation By Waiter for Visitor
    public CreateReservationByWaiterResponse createReservationByWaiterForVisitor(CreateReservationByWaiterRequest waiterRequest, String waiterEmail) {

        String userRole = userService.getUserRole(waiterEmail);
        if (!"WAITER".equalsIgnoreCase(userRole)) {
            throw new ForbiddenException("Forbidden: Only waiters can create reservations through this endpoint");
        }

        // Get waiter's assigned location
        String waiterLocationId = waiterService.getWaiterLocation(waiterEmail);
        if (waiterLocationId == null) {
            throw new BadRequestException("Waiter is not assigned to any location");
        }

        // Parse request
        if (waiterRequest == null) {
            throw new BadRequestException("Request body is required");
        }

        if (!waiterLocationId.equals(waiterRequest.getLocationId())) {
            throw new ForbiddenException("Waiter can only create reservations for their assigned location");
        }

        if(waiterRequest.getDate().isEmpty()) {
            throw new BadRequestException("Date cannot be empty");
        }
        if(waiterRequest.getTableNumber().isEmpty()) {
            throw new BadRequestException("Table number cannot be empty");
        }
        // Validate date format
        LocalDate requestedDate;
        try {
            requestedDate = LocalDate.parse(waiterRequest.getDate(), DateTimeFormatter.ISO_DATE);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid date format. Use ISO format (YYYY-MM-DD)");
        }

        // Validate guests number
        try {
            int guestsCount = Integer.parseInt(waiterRequest.getGuestsNumber());
            if (guestsCount <= 0) {
                throw new BadRequestException("Number of guests must be positive");
            }
        } catch (DynamoDbException e) {
            throw new BadRequestException("Invalid guests format. Must be a number");
        }

        // Validate date range
        LocalDate today = LocalDate.now();

        // Update date range validation to allow today's date
        if (requestedDate.isBefore(today) || requestedDate.isAfter(today.plusDays(30))) {
            throw new BadRequestException("Bookings can only be made between today and 30 days from today");
        }

        // Add check for today's bookings to ensure time hasn't passed
        if (requestedDate.equals(today)) {
            LocalTime currentTime = LocalTime.now();
            LocalTime requestedTime = LocalTime.parse(waiterRequest.getTimeFrom());

            // Add buffer time (30 minutes) to ensure there's enough time to prepare
            LocalTime cutoffTime = currentTime.plusMinutes(30);

            if (requestedTime.isBefore(cutoffTime)) {
                throw new BadRequestException("For same-day reservations, booking must be at least 30 minutes in advance.");
            }
        }

        // Check if the table is available
        List<AvailableTable> availableTables = bookingService.getAvailableTablesTimeSlots(
                waiterRequest.getLocationId(),
                waiterRequest.getDate(),
                waiterRequest.getTimeFrom(),
                waiterRequest.getGuestsNumber()
        );

        // Check if the requested table is available
        boolean tableAvailable = false;
        boolean validGuestsCount = false;
        int tableCapacity = bookingService.getTableCapacity(waiterRequest.getLocationId(),waiterRequest.getTableNumber());
        for (AvailableTable table : availableTables) {
            if (table.getTableNumber().equals(waiterRequest.getTableNumber())) {
                // Check if the time slot is available
                String requestedTimeSlot = waiterRequest.getTimeFrom() + " - " + waiterRequest.getTimeTo();
                if (table.getAvailableSlots().contains(requestedTimeSlot)) {
                    tableAvailable = true;
                }
            }
        }

        // Check if the guest capacity is sufficient
        int guestRequested = Integer.parseInt(waiterRequest.getGuestsNumber());
        if (tableCapacity >= guestRequested) {
            validGuestsCount = true ;
        }

        if(!tableAvailable && !validGuestsCount){
            throw new BadRequestException("The requested table is not available for the specified time slot and number of guests");
        }

        if (!tableAvailable) {
            throw new BadRequestException("The requested table is not available for the specified time slot.");
        }
        if (!validGuestsCount){
            throw new BadRequestException("The requested table cannot accommodate " + waiterRequest.getGuestsNumber() +" guests (capacity: " + tableCapacity + ")");

        }

        // For visitors, we use "VISITOR" as the userId
        String visitorId = "VISITOR";

        // Convert waiter request to regular reservation request
        ReservationRequest request = convertToReservationRequest(waiterRequest);

        // Find the least busy waiter
        String assignedWaiterEmail;
        try {
            assignedWaiterEmail = waiterService.findLeastBusyWaiter(
                    request.getLocationId(),
                    request.getDate(),
                    request.getTimeFrom()
            );
        } catch (Exception e) {
            log.error("Failed to find least busy waiter: {}", e.getMessage());
            throw new InternalServerErrorException("Failed to find available waiter: " + e.getMessage());
        }

        // Create the reservation using "VISITOR" as userId
        Reservation reservation = createReservationEntity(request, visitorId);

        // Set the assigned waiter email
        reservation.setWaiterEmail(assignedWaiterEmail);

        try {
            reservationTable.putItem(reservation);
        } catch (Exception e) {
            log.error("Failed to save reservation: {}", e.getMessage());
            throw new InternalServerErrorException("Failed to save reservation: " + e.getMessage());
        }

        try {
            waiterService.updateWaiterAvailability(assignedWaiterEmail, request.getDate(), request.getTimeFrom());
        } catch (Exception e) {
            log.warn("Failed to update waiter availability: {}", e.getMessage());
            // Continue despite error - we'll handle this as a partial success
        }

        try {
            userReservationService.updateBookingAvailability(request.getLocationId(), request.getTableNumber(),
                    request.getDate(), request.getTimeFrom(), request.getTimeTo());
        } catch (Exception e) {
            log.warn("Failed to update booking availability: {}", e.getMessage());
            // Continue despite error - we'll handle this as a partial success
        }

        ReservationResponse reservationResponse =  UserReservationService.getReservationResponse(reservation);
        CreateReservationByWaiterResponse result = new CreateReservationByWaiterResponse();

        result.setId(reservationResponse.getId());
        result.setDate(reservationResponse.getDate());
        result.setTimeSlot(reservationResponse.getTimeFrom() + "-" + reservationResponse.getTimeTo());
        result.setTableNumber(reservationResponse.getTableNumber());
        result.setGuestsNumber(Integer.valueOf(reservationResponse.getGuestsNumber()));
        result.setStatus(reservationResponse.getStatus());
        result.setPreOrder(reservationResponse.getPreOrder());
        result.setFeedbackId(reservationResponse.getFeedbackId());
        result.setAssignedWaiter(reservationResponse.getWaiterEmail());
        result.setUserInfo("Visitor " + waiterRequest.getCustomerName());

        String waiterName = userService.getUserName(reservationResponse.getWaiterEmail());
        result.setAssignedWaiterName(waiterName);
        result.setLocationAddress("Reserved at " + waiterLocationId);
        result.setMessage("Your booking is complete. Thank you!");
        return result;
    }

    //View Waiter Reservations
    public List<WaiterReservationResponse> getWaiterReservations(String waiterEmail, String locationId,
                                                                 String date, String time, String tableNumber) {
        if (waiterEmail == null || waiterEmail.isEmpty()) {
            throw new IllegalArgumentException("Waiter email is required");
        }

        try {
            updateReservationStatuses();

            // Get all reservations for this waiter at this location
            List<Reservation> waiterReservations = new ArrayList<>();
            reservationTable.scan().items().forEach(reservation -> {
                if (waiterEmail.equals(reservation.getWaiterEmail()) &&
                        (locationId == null || locationId.isEmpty() || locationId.equals(reservation.getLocationId())) &&
                        // Only include RESERVED reservations
                        "RESERVED".equals(reservation.getStatus())) {
                    waiterReservations.add(reservation);
                }
            });

            // Apply date filter if provided
            List<Reservation> filteredReservations = waiterReservations;
            if (date != null && !date.isEmpty()) {
                filteredReservations = filteredReservations.stream()
                        .filter(reservation -> reservation.getDate().equals(date))
                        .toList();
            }

            // Apply table filter if not "Any Table"
            if (tableNumber != null && !tableNumber.isEmpty() && !"Any Table".equals(tableNumber)) {
                filteredReservations = filteredReservations.stream()
                        .filter(reservation -> reservation.getTableNumber().equals(tableNumber))
                        .toList();
            }

            // Apply time filter if not "00:00"
            if (time != null && !time.isEmpty() && !"00:00".equals(time)) {
                filteredReservations = filteredReservations.stream()
                        .filter(reservation -> reservation.getTimeFrom().equals(time))
                        .toList();
            }

            // Filter out cancelled reservations
            filteredReservations = filteredReservations.stream()
                    .filter(reservation -> "RESERVED".equals(reservation.getStatus()))
                    .toList();

            // Get the tables assigned to this waiter
            List<String> assignedTables = waiterService.getWaiterAssignedTables(waiterEmail);

            // Initialize visitor counter
            final AtomicInteger visitorCounter = new AtomicInteger(0);

            // Convert reservations to detailed responses
            return filteredReservations.stream()
                    .map(reservation -> {
                        // For VISITOR reservations, increment the counter
                        int visitorNumber = 0;
                        if ("VISITOR".equals(reservation.getUserId())) {
                            visitorNumber = visitorCounter.incrementAndGet();
                        }
                        return convertToWaiterReservationResponse(reservation, assignedTables, visitorNumber);
                    })
                    .toList();
        } catch (Exception e) {
            log.error("Failed to get reservations for waiter {}: {}", waiterEmail, e.getMessage());
            throw new InternalServerErrorException("Failed to get waiter reservations: " + e.getMessage());
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

    private WaiterReservationResponse convertToWaiterReservationResponse(
            Reservation reservation, List<String> assignedTables, int visitorNumber) {
        WaiterReservationResponse response =  WaiterReservationResponse
                .builder()
                .reservationId(reservation.getReservationId())
                .locationId(reservation.getLocationId())
                .tableNumber(reservation.getTableNumber())
                .date(reservation.getDate())
                .timeSlot(reservation.getTimeFrom()+" - "+reservation.getTimeTo())
                .guestsNumber(reservation.getGuestsNumber())
                .status(reservation.getStatus())
                .waiterEmail(reservation.getWaiterEmail())
                .build();


        // Set pre-order (default to "0" if empty)
        String preOrder = reservation.getPreOrder();
        response.setPreOrder(preOrder != null && !preOrder.isEmpty() ? (preOrder.split(",").length) + " dishes" : "0 dishes");

        response.setFeedbackId(reservation.getFeedbackId() != null ? reservation.getFeedbackId() : "");
        response.setUserId(reservation.getUserId());

        // Get location name from location service
        try {
            String locationName = locationService.getLocationName(reservation.getLocationId());
            response.setLocation(locationName);
        } catch (Exception e) {
            log.warn("Failed to get location name for ID {}: {}",
                    reservation.getLocationId(), e.getMessage());
            response.setLocation(reservation.getLocationId()); // Fallback to ID
        }

        // Get customer name from user service
        if ("VISITOR".equals(reservation.getUserId())) {
            try {
                // Get waiter name
                String waiterName = userService.getUserName(reservation.getWaiterEmail());

                // Use the sequential counter passed in
                response.setCustomerName("Waiter " + waiterName + " (Visitor " + visitorNumber + ")");
            } catch (Exception e) {
                log.warn("Failed to format visitor name for reservation {}: {}",
                        reservation.getReservationId(), e.getMessage());
                response.setCustomerName("Visitor"); // Fallback
            }
        } else {
            // Regular customer name handling
            try {
                String customerName = userService.getUserName(reservation.getUserId());
                response.setCustomerName("Customer " + customerName);
            } catch (Exception e) {
                log.warn("Failed to get customer name for ID {}: {}",
                        reservation.getUserId(), e.getMessage());
                response.setCustomerName("Anonymous"); // Fallback
            }
        }

        return response;
    }
}
