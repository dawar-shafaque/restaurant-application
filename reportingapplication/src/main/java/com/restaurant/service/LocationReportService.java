package com.restaurant.service;

import com.restaurant.exception.BadRequestException;
import com.restaurant.model.Reservation;
import com.restaurant.model.Feedback;
import com.restaurant.model.LocationMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.time.*;

@Service
public class LocationReportService {

    private final DynamoDbTable<LocationMetrics> locationMetricsTable;
    private final DynamoDbTable<Reservation> reservationTable;

    @Autowired
    public LocationReportService(DynamoDbTable<LocationMetrics> locationMetricsTable, DynamoDbTable<Reservation> reservationTable) {
        this.locationMetricsTable = locationMetricsTable;
        this.reservationTable = reservationTable;
    }

    public void processLocationReport(Feedback feedback) {
        Reservation reservation = reservationTable.getItem(r -> r.key(k -> k.partitionValue(feedback.getReservationId())));
        if (reservation == null) {
            throw new BadRequestException("Reservation is null");
        }

        String reportingStartDate = getReportingStartDate(reservation.getDate());
        String reportingEndDate = getReportingEndDate(reservation.getDate());

        LocationMetrics locationMetrics = locationMetricsTable.getItem(r ->
                r.key(k ->
                        k.partitionValue(reservation.getLocationId())
                                .sortValue(reportingStartDate)
                )
        );
        if (locationMetrics == null) {
            locationMetrics = createNewLocationMetrics(reservation, reportingStartDate);
        }

        if ("FINISHED".equalsIgnoreCase(reservation.getStatus())) {
            positiveReport(locationMetrics, feedback);
        } else if ("CANCELLED".equalsIgnoreCase(reservation.getStatus())) {
            negativeReport(locationMetrics, feedback);
        }

        locationMetricsTable.putItem(locationMetrics);
    }

    private LocationMetrics createNewLocationMetrics(Reservation reservation, String reportingStartDate) {
        LocationMetrics locationMetrics = new LocationMetrics();
        locationMetrics.setLocationId(reservation.getLocationId());
        locationMetrics.setStartDate(getReportingStartDate(reservation.getDate()));
        locationMetrics.setEndDate(getReportingEndDate(reservation.getDate()));
        locationMetrics.setOrdersProcessed(0);
        locationMetrics.setTotalFeedback(0.0);
        locationMetrics.setAverageCuisineFeedback(0.0);
        locationMetrics.setMinCuisineFeedback(Double.MAX_VALUE);
        locationMetrics.setDeltaOrdersProcessedInPercent(0.0);
        locationMetrics.setDeltaAverageCuisineFeedbackInPercent(0.0);
        locationMetrics.setPreviousOrdersProcessed(0);
        locationMetrics.setPreviousAverageFeedback(0.0);

        return locationMetrics;
    }

    private void positiveReport(LocationMetrics locationMetrics, Feedback feedback) {
        try {
            double feedbackRate = validateRate(feedback.getRate());

            locationMetrics.setOrdersProcessed(locationMetrics.getOrdersProcessed() + 1);
            locationMetrics.setTotalFeedback((locationMetrics.getTotalFeedback() + feedbackRate));
            locationMetrics.setAverageCuisineFeedback(
                    (locationMetrics.getTotalFeedback() / locationMetrics.getOrdersProcessed())
            );
            locationMetrics.setMinCuisineFeedback(Math.min(locationMetrics.getMinCuisineFeedback(), feedbackRate));

            String lastWeekStartDate = calculateLastWeekMonday(locationMetrics.getStartDate());
            LocationMetrics previousWeekMetrics = locationMetricsTable.getItem(r ->
                    r.key(k -> k.partitionValue(locationMetrics.getLocationId())
                            .sortValue(lastWeekStartDate))
            );

            locationMetrics.setDeltaOrdersProcessedInPercent(
                    previousWeekMetrics != null && previousWeekMetrics.getOrdersProcessed() > 0
                            ? ((double) locationMetrics.getOrdersProcessed() - previousWeekMetrics.getOrdersProcessed()) / previousWeekMetrics.getOrdersProcessed() * 100
                            : 0.0
            );

            locationMetrics.setDeltaAverageCuisineFeedbackInPercent(
                    previousWeekMetrics != null && previousWeekMetrics.getAverageCuisineFeedback() > 0
                            ? ((locationMetrics.getAverageCuisineFeedback() - previousWeekMetrics.getAverageCuisineFeedback()) / previousWeekMetrics.getAverageCuisineFeedback()) * 100
                            : 0.0
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Failed during rate parsing in positive aggregation: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Error during positive aggregation: " + e.getMessage());
        }
    }

    private void negativeReport(LocationMetrics locationMetrics, Feedback feedback) {
        try {
            double feedbackRate = validateRate(feedback.getRate());

            locationMetrics.setOrdersProcessed(Math.max(0, locationMetrics.getOrdersProcessed() - 1));
            locationMetrics.setTotalFeedback(Math.max(0, (locationMetrics.getTotalFeedback() - feedbackRate)));
            locationMetrics.setAverageCuisineFeedback(
                    (locationMetrics.getTotalFeedback() / locationMetrics.getOrdersProcessed())
            );
            locationMetrics.setMinCuisineFeedback(Math.min(locationMetrics.getMinCuisineFeedback(), feedbackRate));

            String lastWeekStartDate = calculateLastWeekMonday(locationMetrics.getStartDate());
            LocationMetrics previousWeekMetrics = locationMetricsTable.getItem(r ->
                    r.key(k -> k.partitionValue(locationMetrics.getLocationId())
                            .sortValue(lastWeekStartDate))
            );

            locationMetrics.setDeltaOrdersProcessedInPercent(
                    previousWeekMetrics != null && previousWeekMetrics.getOrdersProcessed() > 0
                            ? ((double) locationMetrics.getOrdersProcessed() - previousWeekMetrics.getOrdersProcessed()) / previousWeekMetrics.getOrdersProcessed() * 100
                            : 0.0
            );

            locationMetrics.setDeltaAverageCuisineFeedbackInPercent(
                    previousWeekMetrics != null && previousWeekMetrics.getAverageCuisineFeedback() > 0
                            ? ((locationMetrics.getAverageCuisineFeedback() - previousWeekMetrics.getAverageCuisineFeedback()) / previousWeekMetrics.getAverageCuisineFeedback()) * 100
                            : 0.0
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Failed during rate parsing in negative aggregation: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Error during negative aggregation: " + e.getMessage());
        }
    }

    private double validateRate(String rate) {
        try {
            double feedbackRate = Double.parseDouble(rate);
            if (feedbackRate < 0 || feedbackRate > 5) {  // Assuming rates are 0-5
                throw new IllegalArgumentException("Rate is out of bounds: " + feedbackRate);
            }
            return feedbackRate;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid rate format: " + rate, e);
        }
    }

    private String calculateLastWeekMonday(String currentStartDate) {
        LocalDate currentStartDateDate = LocalDate.parse(currentStartDate);
        LocalDate lastWeekMonday = currentStartDateDate.minusWeeks(1).with(DayOfWeek.MONDAY);
        return lastWeekMonday.toString();
    }

    private static String getReportingStartDate(String reservationDate) {
        try {
            LocalDate date = LocalDate.parse(reservationDate);
            return date.with(DayOfWeek.MONDAY).toString();
        } catch (Exception e) {
            throw new RuntimeException("Error calculating reporting start date. Reservation Date: " + reservationDate, e);
        }
    }

    private String getReportingEndDate(String reservationDate) {
        try {
            LocalDate date = LocalDate.parse(reservationDate);
            return date.with(DayOfWeek.SUNDAY).toString();
        } catch (Exception e) {
            throw new RuntimeException("Error calculating reporting end date: " + reservationDate, e);
        }
    }
}