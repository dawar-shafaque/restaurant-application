package com.restaurant.model;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;


@Data
@AllArgsConstructor
@NoArgsConstructor
@DynamoDbBean
public class WaiterMetrics {

    private String locationId;
    private String email;
    // Now a String (ISO-8601)
    private String startDate; // Changed from Instant to String (ISO-8601)
    // Now a String (ISO-8601)
    private String endDate;   // Changed from Instant to String (ISO-8601)
    private Double workHours;
    private Integer ordersProcessed;
    private Double delta_ordersProcessedInPercent;
    private Double totalFeedback;
    private Double averageServiceFeedback;
    private Double minServiceFeedback;
    private Double delta_averageServiceFeedbackInPercent;

    @DynamoDbPartitionKey
    public String getEmail() {
        return email;
    }

    @DynamoDbSortKey
    public String getStartDate() { // Now a String (ISO-8601)
        return startDate;
    }



}