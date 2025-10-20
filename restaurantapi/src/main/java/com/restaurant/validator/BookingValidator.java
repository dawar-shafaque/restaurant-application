package com.restaurant.validator;

import com.restaurant.exception.BadRequestException;
import com.restaurant.exception.MissingParameterException;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class BookingValidator {

    public void validateAvailableTablesRequest(String locationId, String date, String guests, String requestedTime) {
        validateRequiredParameters(locationId, date, guests);
        LocalDate requestedDate = validateDateFormat(date);
        validateGuestsNumber(guests);
        validateDateRange(requestedDate);
        validateTimeForSameDayBooking(requestedDate, requestedTime);
    }

    // Overloaded method to maintain backward compatibility with existing code
    public void validateAvailableTablesRequest(String locationId, String date, String guests) {
        validateAvailableTablesRequest(locationId, date, guests, null);
    }

    private void validateRequiredParameters(String locationId, String date, String guests) {
        if (locationId == null || locationId.trim().isEmpty()) {
            throw new MissingParameterException("locationId", "String");
        }

        if (date == null || date.trim().isEmpty()) {
            throw new MissingParameterException("date", "String");
        }

        if (guests == null || guests.trim().isEmpty()) {
            throw new MissingParameterException("guests", "String");
        }
    }

    private LocalDate validateDateFormat(String date) {
        try {
            return LocalDate.parse(date, DateTimeFormatter.ISO_DATE);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid date format. Use ISO format (YYYY-MM-DD)");
        }
    }

    private void validateGuestsNumber(String guests) {
        try {
            int guestsCount = Integer.parseInt(guests);
            if (guestsCount <= 0) {
                throw new BadRequestException("Number of guests must be positive");
            }
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid guests format. Must be a number");
        }
    }

    private void validateDateRange(LocalDate requestedDate) {
        LocalDate today = LocalDate.now();
        if (requestedDate.isBefore(today) || requestedDate.isAfter(today.plusDays(30))) {
            throw new BadRequestException("Bookings can only be made between today and 30 days from today");
        }
    }

    private void validateTimeForSameDayBooking(LocalDate requestedDate, String requestedTime) {
        if (!isBookingForToday(requestedDate) || !hasRequestedTime(requestedTime)) {
            return;
        }

        try {
            LocalTime currentTime = LocalTime.now();
            LocalTime requestedLocalTime = LocalTime.parse(requestedTime);
            LocalTime cutoffTime = currentTime.plusMinutes(30);

            if (requestedLocalTime.isBefore(cutoffTime)) {
                throw new BadRequestException("For same-day reservations, booking must be at least 30 minutes in advance.");
            }
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid time format. Use HH:mm format");
        }
    }

    private boolean isBookingForToday(LocalDate requestedDate) {
        return requestedDate.equals(LocalDate.now());
    }

    private boolean hasRequestedTime(String requestedTime) {
        return requestedTime != null && !requestedTime.trim().isEmpty();
    }
}