package com.restaurant.config;

import com.restaurant.model.Feedback;
import com.restaurant.model.LocationMetrics;
import com.restaurant.model.Reservation;
import com.restaurant.model.WaiterMetrics;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import static software.amazon.awssdk.enhanced.dynamodb.TableSchema.fromBean;

@Data
@Configuration
@ConfigurationProperties(prefix = "dynamodb.tables")
public class TableConfig {

    private String waiterReportTableName;
    private String locationReportTableName;
    private String feedbackTableName;
    private String reservationTableName;

    @Bean
    public DynamoDbTable<WaiterMetrics> waiterTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(waiterReportTableName, fromBean(WaiterMetrics.class));
    }

    @Bean
    public DynamoDbTable<LocationMetrics> locationTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(locationReportTableName, fromBean(LocationMetrics.class));
    }

    @Bean
    public DynamoDbTable<Reservation> reservationTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(reservationTableName, fromBean(Reservation.class));
    }

    @Bean
    public DynamoDbTable<Feedback> feedbackTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(feedbackTableName, fromBean(Feedback.class));
    }

}
