package com.restaurant.service;

import com.restaurant.model.LocationMetrics;
import com.restaurant.model.WaiterMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(ReportSchedulerService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Autowired
    private ReportSenderService reportSenderService;

    private final DynamoDbTable<LocationMetrics> locationMetricsTable;
    private final DynamoDbTable<WaiterMetrics> waiterMetricsTable;

    public ReportSchedulerService(DynamoDbEnhancedClient enhancedClient) {
        this.locationMetricsTable = enhancedClient.table("LocationMetrics",
                TableSchema.fromBean(LocationMetrics.class));
        this.waiterMetricsTable = enhancedClient.table("WaiterMetrics",
                TableSchema.fromBean(WaiterMetrics.class));
    }


    // Run every Monday at 8:00 AM
    @Scheduled(fixedRate = 60000)       //runs every minute
    public void generateAndSendWeeklyReport() {
        logger.info("Starting the weekly report generation process...");

        try {
            // Calculate last Sunday's date
            LocalDate currentDate = LocalDate.now();
            LocalDate lastSunday = currentDate.with(DayOfWeek.SUNDAY).minusWeeks(1);
            LocalDate lastMonday = lastSunday.minusDays(6);  // Monday of last week

            String endDate = lastSunday.format(DATE_FORMATTER);
            String startDate = lastMonday.format(DATE_FORMATTER);

            logger.info("Fetching metrics for period: {} to {}", startDate, endDate);

            // Fetch all metrics from Dynamo
            List<WaiterMetrics> allWaiterMetrics =  new ArrayList<>();
            waiterMetricsTable.scan(ScanEnhancedRequest.builder().build())
                    .items().forEach(allWaiterMetrics::add);

            List<LocationMetrics> allLocationMetrics = new ArrayList<>();
            locationMetricsTable.scan(ScanEnhancedRequest.builder().build())
                    .items().forEach(allLocationMetrics::add);

            // Filter metrics for last week
            List<WaiterMetrics> weeklyWaiterMetrics = allWaiterMetrics.stream()
                    .filter(metrics -> metrics.getEndDate().equals(endDate))
                    .collect(Collectors.toList());

            List<LocationMetrics> weeklyLocationMetrics = allLocationMetrics.stream()
                    .filter(metrics -> metrics.getEndDate().equals(endDate))
                    .collect(Collectors.toList());

            logger.info("Found {} waiter metrics and {} location metrics for last week",
                    weeklyWaiterMetrics.size(), weeklyLocationMetrics.size());

            // Send the report
            reportSenderService.sendReport(weeklyWaiterMetrics, weeklyLocationMetrics);

            logger.info("Weekly report sent successfully");
        } catch (Exception e) {
            logger.error("Error generating or sending weekly report: {}", e.getMessage(), e);
        }
    }
}