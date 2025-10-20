package com.restaurant.service;

import com.restaurant.exception.BadRequestException;
import com.restaurant.exception.InternalServerErrorException;
import com.restaurant.exception.NotFoundException;
import com.epam.edai.run8.team16.model.*;
import com.restaurant.dto.AvailableTable;
import com.restaurant.model.Booking;
import com.restaurant.model.Location;
import com.restaurant.utils.DateSlot;
import com.restaurant.utils.TimeSlot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final DynamoDbTable<Booking> bookingsTable;
    private final DynamoDbTable<Location> locationTable;
    private static final String TIME_FORMAT = "HH:mm";


    public List<AvailableTable> getAvailableTables(String locationId, String date, String requestedTime, String guestsStr) {
        int guests = Integer.parseInt(guestsStr);

        validateDateAndTime(date, requestedTime);

        List<Booking> filteredBookings = getFilteredBookings(locationId, guests);
        Location location = getLocation(locationId);
        String locationAddress = location != null ? location.getAddress() : "Unknown Location";

        boolean isToday = LocalDate.now().equals(LocalDate.parse(date, DateTimeFormatter.ISO_DATE));

        return buildAvailableTables(filteredBookings, locationId, locationAddress, date, requestedTime, isToday);
    }

    public List<AvailableTable> getAvailableTablesTimeSlots(String locationId, String date, String requestedTime, String guestsStr) {
        int guests = Integer.parseInt(guestsStr);

        validateDateAndTime(date, requestedTime);

        List<Booking> filteredBookings = getFilteredBookingsTimeSlots(locationId, guests);
        Location location = getLocation(locationId);
        String locationAddress = location != null ? location.getAddress() : "Unknown Location";

        boolean isToday = LocalDate.now().equals(LocalDate.parse(date, DateTimeFormatter.ISO_DATE));

        return buildAvailableTables(filteredBookings, locationId, locationAddress, date, requestedTime, isToday);
    }

    private void validateDateAndTime(String date, String requestedTime) {
        LocalDate requestedLocalDate = LocalDate.parse(date, DateTimeFormatter.ISO_DATE);
        LocalDate today = LocalDate.now();
        LocalDate limitDate = today.plusDays(30);

        // Check if date is within valid range
        if (requestedLocalDate.isBefore(today) || requestedLocalDate.isAfter(limitDate)) {
            throw new BadRequestException("Booking date must be between today and 30 days in the future");
        }

        boolean isToday = requestedLocalDate.equals(today);

        // Check if the requested date is today and the requested time has already passed
        if (isToday && requestedTime != null && !requestedTime.isEmpty()) {
            LocalTime currentTime = LocalTime.now();
            LocalTime requestedLocalTime = LocalTime.parse(requestedTime);

            if (requestedLocalTime.isBefore(currentTime)) {
                throw new BadRequestException("Cannot book a table for a time that has already passed");
            }
        }
    }

    private List<Booking> getFilteredBookings(String locationId, int guests) {
        List<Booking> bookings = getBookingsByLocationId(locationId);
        return bookings.stream()
                .filter(booking -> booking.getGuestCapacity() >= guests)
                .toList();
    }

    private List<Booking> getFilteredBookingsTimeSlots(String locationId, int guests) {
        List<Booking> bookings = getBookingsByLocationId(locationId);
        return bookings.stream()
                .toList();
    }

    private List<AvailableTable> buildAvailableTables(List<Booking> bookings, String locationId,
                                                      String locationAddress, String date,
                                                      String requestedTime, boolean isToday) {
        List<AvailableTable> availableTables = new ArrayList<>();

        for (Booking booking : bookings) {
            List<String> timeSlots = getFilteredTimeSlots(booking, date, requestedTime, isToday);

            if (timeSlots.isEmpty()) {
                continue;
            }

            AvailableTable availableTable = new AvailableTable(
                    locationId,
                    locationAddress,
                    booking.getTableNumber(),
                    booking.getGuestCapacity().toString(),
                    timeSlots
            );

            availableTables.add(availableTable);
        }

        if(availableTables.isEmpty()) {
            throw new NotFoundException("No table found");
        }

        return availableTables;
    }

    private List<String> getFilteredTimeSlots(Booking booking, String date, String requestedTime, boolean isToday) {
        List<String> availableTimeSlots = getAvailableTimeSlotsForDate(booking, date, isToday);

        if (availableTimeSlots.isEmpty()) {
            return Collections.emptyList();
        }

        if (requestedTime != null && !requestedTime.isEmpty()) {
            List<String> filteredSlots = TimeSlot.getTimeSlotsAtOrAfter(requestedTime);
            return availableTimeSlots.stream()
                    .filter(filteredSlots::contains)
                    .toList();
        } else if (isToday) {
            String currentTimeStr = LocalTime.now().format(DateTimeFormatter.ofPattern(TIME_FORMAT));
            List<String> filteredSlots = TimeSlot.getTimeSlotsAtOrAfter(currentTimeStr);
            return availableTimeSlots.stream()
                    .filter(filteredSlots::contains)
                    .toList();
        }

        return availableTimeSlots;
    }

    private List<String> getAvailableTimeSlotsForDate(Booking booking, String requestedDate, boolean isToday) {
        // First check if there are specific time slots defined for this date
        List<String> specificSlots = getSpecificTimeSlots(booking, requestedDate, isToday);
        if (!specificSlots.isEmpty()) {
            return specificSlots;
        }

        // If no specific slots, generate default time slots
        return generateDefaultTimeSlots(requestedDate, isToday);
    }

    private List<String> getSpecificTimeSlots(Booking booking, String requestedDate, boolean isToday) {
        DateSlot[] availableSlots = booking.getAvailableSlots();
        if (availableSlots == null) {
            return Collections.emptyList();
        }

        for (DateSlot dateSlot : availableSlots) {
            if (dateSlot != null && dateSlot.getDate() != null && dateSlot.getDate().equals(requestedDate)) {
                String[] timeSlots = dateSlot.getAvailableTimeSlots();
                if (timeSlots != null) {
                    List<String> slots = Arrays.asList(timeSlots);
                    return filterTodaySlots(slots, isToday);
                }
            }
        }

        return Collections.emptyList();
    }

    private List<String> generateDefaultTimeSlots(String requestedDate, boolean isToday) {
        LocalDate today = LocalDate.now();
        LocalDate requestedLocalDate = LocalDate.parse(requestedDate, DateTimeFormatter.ISO_DATE);
        LocalDate limitDate = today.plusDays(30);

        if (requestedLocalDate.isAfter(today.minusDays(1)) && !requestedLocalDate.isAfter(limitDate)) {
            List<String> allTimeSlots = TimeSlot.getAllTimeSlots();
            return filterTodaySlots(allTimeSlots, isToday);
        }

        return Collections.emptyList();
    }

    private List<String> filterTodaySlots(List<String> slots, boolean isToday) {
        if (isToday) {
            String currentTimeStr = LocalTime.now().format(DateTimeFormatter.ofPattern(TIME_FORMAT));
            return slots.stream()
                    .filter(slot -> slot.compareTo(currentTimeStr) > 0)
                    .toList();
        }
        return slots;
    }

    public List<Booking> getBookingsByLocationId(String locationId) {
        QueryConditional queryConditional = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(locationId).build());

        return bookingsTable.query(queryConditional)
                .items()
                .stream()
                .toList();
    }

    public Location getLocation(String locationId) {
        Key key = Key.builder().partitionValue(locationId).build();
        return locationTable.getItem(key);
    }

    public int getTableCapacity(String locationId, String tableNumber) {

        if (locationId == null && tableNumber == null) {
            throw new BadRequestException("Location ID and table number are required");
        }

        if (locationId == null){
            throw new BadRequestException("Location ID is required");

        }

        if(tableNumber == null) {
            throw new BadRequestException("Table number is required");
        }

        try {
            Key key = Key.builder()
                    .partitionValue(locationId)
                    .sortValue(tableNumber)
                    .build();

            Booking booking = bookingsTable.getItem(key);

            if (booking == null) {
                throw new BadRequestException(
                        String.format("Table %s not found at location %s", tableNumber, locationId)
                );
            }

            Integer capacity = booking.getGuestCapacity();

            if (capacity == null) {
                throw new BadRequestException(
                        String.format("Capacity not defined for table %s at location %s", tableNumber, locationId)
                );
            }

            return capacity;
        } catch (DynamoDbException e) {
            throw new InternalServerErrorException(
                    String.format("Error retrieving capacity for table %s: %s", tableNumber, e.getMessage())
            );
        }
    }
}