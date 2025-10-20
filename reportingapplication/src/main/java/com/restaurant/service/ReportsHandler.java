package com.restaurant.service;

import com.restaurant.model.Feedback;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReportsHandler {

    private final WaiterReportService waiterReportService;
    private final LocationReportService locationReportService;
    private final ObjectMapper objectMapper;

    @Autowired
    public ReportsHandler(WaiterReportService waiterReportService, LocationReportService locationReportService, ObjectMapper objectMapper) {
        this.waiterReportService = waiterReportService;
        this.locationReportService = locationReportService;
        this.objectMapper = objectMapper;
    }

    @SqsListener("tm16-waiter-report-dev6")
    public void handleReport(String message) {
        try {
            Feedback feedback = objectMapper.readValue(message, Feedback.class);

            if ("cuisine".equalsIgnoreCase(feedback.getType())) {
                waiterReportService.processWaiterReport(feedback);
            } else if ("service".equalsIgnoreCase(feedback.getType())) {
                locationReportService.processLocationReport(feedback);
            } else {
                throw new IllegalArgumentException("Unknown feedback type: " + feedback.getType());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}