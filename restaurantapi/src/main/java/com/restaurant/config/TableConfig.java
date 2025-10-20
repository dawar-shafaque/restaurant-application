package com.restaurant.config;

import com.epam.edai.run8.team16.model.*;
import com.restaurant.model.*;
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

    private String userTableName;
    private String waiterTableName;
    private String dishTableName;
    private String locationTableName;
    private String feedbackTableName;
    private String reservationTableName;
    private String bookingsTableName;

    @Bean
    public DynamoDbTable<User> userTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(userTableName, fromBean(User.class));
    }

    @Bean
    public DynamoDbTable<Waiter> waiterTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(waiterTableName, fromBean(Waiter.class));
    }

    @Bean
    public DynamoDbTable<Dish> dishTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(dishTableName, fromBean(Dish.class));
    }

    @Bean
    public DynamoDbTable<Location> locationTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(locationTableName, fromBean(Location.class));
    }

    @Bean
    public DynamoDbTable<Reservation> reservationTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(reservationTableName, fromBean(Reservation.class));
    }

    @Bean
    public DynamoDbTable<Feedback> feedbackTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(feedbackTableName, fromBean(Feedback.class));
    }

    @Bean
    public DynamoDbTable<Booking> bookingsTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(bookingsTableName, fromBean(Booking.class));
    }
}
