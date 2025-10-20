package com.restaurant.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;


@Data
@RequiredArgsConstructor
@DynamoDbBean
public class LocationMetrics {

    private String locationId;
    private String email;       //email id of the waiter
    private String startDate; // Report Period Start
    private String endDate;   // Report Period End


    private int ordersProcessed; // Orders processed in the current period
    private int previousOrdersProcessed; // Orders processed in the previous period
    private double totalFeedback; // Total feedback sum (used to calculate average)

    private double averageCuisineFeedback; // Current period average feedback
    private double previousAverageFeedback; // Previous period average feedback
    private double minCuisineFeedback; // Minimum feedback score in the current period

    private double deltaOrdersProcessedInPercent; // Delta (% change) of orders processed
    private double deltaAverageCuisineFeedbackInPercent; // Delta (% change) of average feedback

    // Add constructor(s), getters, setters, and utility methods as required
    @DynamoDbPartitionKey
    public String getLocationId() {
        return locationId;
    }

    @DynamoDbSortKey
    public String getStartDate() { // Changed from Instant to String
        return startDate;
    }
}