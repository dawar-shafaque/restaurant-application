package com.restaurant.service;

import com.restaurant.model.Feedback;
import com.restaurant.model.Reservation;
import com.restaurant.model.WaiterMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.time.*;

@Service
public class WaiterReportService {

    private final DynamoDbTable<WaiterMetrics> waiterMetricsTable;
    private final DynamoDbTable<Reservation> reservationTable;

    @Autowired
    public WaiterReportService(DynamoDbTable<WaiterMetrics> waiterMetricsTable, DynamoDbTable<Reservation> reservationTable) {
        this.waiterMetricsTable = waiterMetricsTable;
        this.reservationTable = reservationTable;
    }

    public void processWaiterReport(Feedback feedback) {

        // Fetch tables
        Reservation reservation = reservationTable.getItem(r ->
                r.key(k -> k.partitionValue(feedback.getReservationId()))
        );

        if (reservation == null) {
            return;
        }

        // Calculate reporting period
        String reportingStartDate = getReportingStartDate(reservation.getDate());
        String reportingEndDate = getReportingEndDate(reservation.getDate());

        // Fetch existing metrics
        WaiterMetrics waiterMetrics = waiterMetricsTable.getItem(r ->
                r.key(k -> k.partitionValue(reservation.getWaiterEmail())
                        .sortValue(reportingStartDate))
        );

        if (waiterMetrics == null) {
            waiterMetrics = createNewWaiterMetrics(reservation, reportingStartDate, reportingEndDate);
        }

        // Perform aggregation based on reservation status
        if ("FINISHED".equalsIgnoreCase(reservation.getStatus())) {
            positiveAggregate(waiterMetrics,reservation, feedback);
        } else if ("CANCELLED".equalsIgnoreCase(reservation.getStatus())) {
            negativeAggregate(waiterMetrics, reservation, feedback);
        }

        waiterMetricsTable.putItem(waiterMetrics);
    }

    private WaiterMetrics createNewWaiterMetrics(Reservation reservation, String reportingStartDate, String reportingEndDate) {
        WaiterMetrics waiterMetrics = new WaiterMetrics();
        waiterMetrics.setLocationId(reservation.getLocationId());
        waiterMetrics.setEmail(reservation.getWaiterEmail());
        waiterMetrics.setStartDate(reportingStartDate);
        waiterMetrics.setEndDate(reportingEndDate);
        waiterMetrics.setWorkHours(0.0);
        waiterMetrics.setTotalFeedback(0.0);
        waiterMetrics.setOrdersProcessed(0);
        waiterMetrics.setAverageServiceFeedback(0.0);
        waiterMetrics.setMinServiceFeedback(Double.MAX_VALUE);
        waiterMetrics.setDelta_ordersProcessedInPercent(0.0);
        waiterMetrics.setDelta_averageServiceFeedbackInPercent(0.0);

        return waiterMetrics;
    }

    public void positiveAggregate(WaiterMetrics waiterMetrics, Reservation reservation, Feedback feedback) {
        try {
            double feedbackRate = validateRate(feedback.getRate());
            waiterMetrics.setWorkHours(waiterMetrics.getWorkHours() + computeWorkedHours(reservation));
            waiterMetrics.setOrdersProcessed(Math.max(0, waiterMetrics.getOrdersProcessed() + 1));
            waiterMetrics.setTotalFeedback(waiterMetrics.getTotalFeedback() + feedbackRate);
            waiterMetrics.setAverageServiceFeedback(
                    waiterMetrics.getOrdersProcessed() > 0
                            ? (waiterMetrics.getTotalFeedback() / waiterMetrics.getOrdersProcessed())
                            : 0.0
            );
            waiterMetrics.setMinServiceFeedback(Math.min(waiterMetrics.getMinServiceFeedback(), feedbackRate));

            String lastWeekStartDate = calculateLastWeekMonday(waiterMetrics.getStartDate());
            WaiterMetrics previousWeekMetrics = waiterMetricsTable.getItem(r ->
                    r.key(k -> k.partitionValue(waiterMetrics.getEmail())
                            .sortValue(lastWeekStartDate))
            );

            waiterMetrics.setDelta_ordersProcessedInPercent(
                    previousWeekMetrics != null && previousWeekMetrics.getOrdersProcessed() > 0
                            ? ((double) waiterMetrics.getOrdersProcessed() - previousWeekMetrics.getOrdersProcessed()) / previousWeekMetrics.getOrdersProcessed() * 100
                            : 0.0
            );

            waiterMetrics.setDelta_averageServiceFeedbackInPercent(
                    previousWeekMetrics != null && previousWeekMetrics.getAverageServiceFeedback() > 0
                            ? ((waiterMetrics.getAverageServiceFeedback() - previousWeekMetrics.getAverageServiceFeedback()) / previousWeekMetrics.getAverageServiceFeedback()) * 100
                            : 0.0
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Failed during rate parsing in positive aggregation: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Error during positive aggregation: " + e.getMessage());
        }
    }

    public void negativeAggregate(WaiterMetrics waiterMetrics, Reservation reservation, Feedback feedback) {
        try {
            double feedbackRate = validateRate(feedback.getRate());
            waiterMetrics.setWorkHours(Math.max(0, waiterMetrics.getWorkHours() - computeWorkedHours(reservation)));
            waiterMetrics.setOrdersProcessed(Math.max(0, waiterMetrics.getOrdersProcessed() - 1));
            waiterMetrics.setTotalFeedback(Math.max(0, waiterMetrics.getTotalFeedback() - feedbackRate));
            waiterMetrics.setAverageServiceFeedback(
                    waiterMetrics.getOrdersProcessed() > 0
                            ? (waiterMetrics.getTotalFeedback() / waiterMetrics.getOrdersProcessed())
                            : 0.0
            );
            waiterMetrics.setMinServiceFeedback(Math.min(waiterMetrics.getMinServiceFeedback(), feedbackRate));

            String lastWeekStartDate = calculateLastWeekMonday(waiterMetrics.getStartDate());
            WaiterMetrics previousWeekMetrics = waiterMetricsTable.getItem(r ->
                    r.key(k -> k.partitionValue(waiterMetrics.getEmail())
                            .sortValue(lastWeekStartDate))
            );

            waiterMetrics.setDelta_ordersProcessedInPercent(
                    previousWeekMetrics != null && previousWeekMetrics.getOrdersProcessed() > 0
                            ? ((double) waiterMetrics.getOrdersProcessed() - previousWeekMetrics.getOrdersProcessed()) / previousWeekMetrics.getOrdersProcessed() * 100
                            : 0.0
            );

            waiterMetrics.setDelta_averageServiceFeedbackInPercent(
                    previousWeekMetrics != null && previousWeekMetrics.getAverageServiceFeedback() > 0
                            ? ((waiterMetrics.getAverageServiceFeedback() - previousWeekMetrics.getAverageServiceFeedback()) / previousWeekMetrics.getAverageServiceFeedback()) * 100
                            : 0.0
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Failed during rate parsing in negative aggregation: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Error during negative aggregation: " + e.getMessage());
        }
    }

    private double computeWorkedHours(Reservation reservation) {
        return Duration.between(
                LocalTime.parse(reservation.getTimeFrom()),
                LocalTime.parse(reservation.getTimeTo())
        ).toHours();
    }

    private double validateRate(String rate) {
        double feedbackRate = Double.parseDouble(rate);
        if (feedbackRate < 0 || feedbackRate > 5) {
            throw new IllegalArgumentException("Rate is out of bounds: " + feedbackRate);
        }
        return feedbackRate;
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

    private String calculateLastWeekMonday(String currentStartDate) {
        LocalDate currentStartDateDate = LocalDate.parse(currentStartDate);
        LocalDate lastWeekMonday = currentStartDateDate.minusWeeks(1).with(DayOfWeek.MONDAY);

        return lastWeekMonday.toString();
    }
}