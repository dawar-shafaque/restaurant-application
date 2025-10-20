package com.epam.edp.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.epam.edp.demo")
@EnableScheduling
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}

package com.epam.edp.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;

@Data
@Configuration
@ConfigurationProperties(prefix = "aws.dynamodb")
public class AwsConfig {
    private String accessKey;
    private String secretKey;
    private String sessionToken;
    private String region;
    private String roleArn;

    @Bean
    public DynamoDbClient dynamoDbClient() {

        // Step 1: Use initial temporary session credentials
        AwsSessionCredentials sessionCredentials = AwsSessionCredentials.create(
                accessKey,
                secretKey,
                sessionToken
        );

        // Step 2: Create STS client using the initial session credentials
        StsClient stsClient = StsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(sessionCredentials))
                .build();

        // Step 3: Assume Role credentials provider
        StsAssumeRoleCredentialsProvider assumeRoleCredentialsProvider = StsAssumeRoleCredentialsProvider.builder()
                .refreshRequest(r -> r.roleArn(roleArn).roleSessionName("assume-role-session"))
                .stsClient(stsClient)
                .build();

        // Step 4: Create DynamoDB client using assumed credentials
        return DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(assumeRoleCredentialsProvider)
                .build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}
package com.epam.edp.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
public class SesConfig {


    // Load AWS credentials and region from application properties
    @Value("${aws.dynamodb.accessKey}")
    private String accessKey;

    @Value("${aws.dynamodb.secretKey}")
    private String secretKey;

    @Value("${aws.dynamodb.sessionToken}") // Optional session token
    private String sessionToken;

    @Value("${aws.ses.region}")
    private String sesRegion;

    @Bean
    public SesClient sesClient() {
        if (accessKey == null || secretKey == null || sesRegion == null || sesRegion.isEmpty()) {
            throw new IllegalArgumentException("AWS credentials and SES region must be properly set.");
        }

        // Create SES client with AWS SDK v2
        if (sessionToken != null && !sessionToken.isEmpty()) {
            // With session token
            return SesClient.builder()
                    .region(Region.of(sesRegion))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsSessionCredentials.create(accessKey, secretKey, sessionToken)))
                    .build();
        } else {
            // With basic credentials
            return SesClient.builder()
                    .region(Region.of(sesRegion))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .build();
        }
    }

}
package com.epam.edp.demo.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SqsConfig {

    // Load AWS credentials and region from application properties
    @Value("${aws.dynamodb.accessKey}")
    private String accessKey;

    @Value("${aws.dynamodb.secretKey}")
    private String secretKey;

    @Value("${aws.dynamodb.sessionToken}") // Optional session token
    private String sessionToken;

    @Value("${aws.dynamodb.region}")
    private String region;

    @Bean
    public AmazonSQS amazonSQS() {
        if (accessKey == null || secretKey == null || region == null || region.isEmpty()) {
            throw new IllegalArgumentException("AWS credentials and region must be properly set.");
        }

        // Create session credentials (if sessionToken is provided)
        if (sessionToken != null && !sessionToken.isEmpty()) {
            BasicSessionCredentials sessionCredentials = new BasicSessionCredentials(accessKey, secretKey, sessionToken);
            return AmazonSQSClientBuilder.standard()
                    .withRegion(region)
                    .withCredentials(new AWSStaticCredentialsProvider(sessionCredentials))
                    .build();
        }

        // Create basic credentials
        BasicAWSCredentials basicCredentials = new BasicAWSCredentials(accessKey, secretKey);
        return AmazonSQSClientBuilder.standard()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(basicCredentials))
                .build();
    }
}
package com.epam.edp.demo.config;

import com.epam.edp.demo.model.Feedback;
import com.epam.edp.demo.model.LocationMetrics;
import com.epam.edp.demo.model.Reservation;
import com.epam.edp.demo.model.WaiterMetrics;
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

package com.epam.edp.demo.controller;

import com.epam.edp.demo.service.ReportSchedulerService;
import com.epam.edp.demo.service.ReportSenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

//@RestController
//@RequiredArgsConstructor
//public class TestController {
//
//    private final ReportSchedulerService reportSchedulerService;
//
//    @GetMapping("/")
//    public String home() {
//        return "Application is running correctly";
//    }
//
//    @PostMapping("/test")
//    public ResponseEntity<String> testEmail() {
//            reportSchedulerService.generateAndSendWeeklyReport();
//            return ResponseEntity.ok("Test email sent successfully");
//
//    }
//}

@RequiredArgsConstructor
@RestController
public class TestController {

    private final ReportSchedulerService reportSchedulerService;

    @GetMapping("/")
    public String home() {
        return "Application is running correctly";
    }
    @PostMapping("/send-reports")
    public ResponseEntity<Map<String, String>> sendWeeklyDeltaReport() {
        reportSchedulerService.generateAndSendWeeklyReport();
        return ResponseEntity.ok(Map.of("message", "Report sent successfully"));
    }
}
package com.epam.edp.demo.exception;

public class BadRequestException extends IllegalArgumentException {
    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadRequestException(Throwable cause) {
        super(cause);
    }
}

package com.epam.edp.demo.exception;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}

package com.epam.edp.demo.exception;


public class ExceptionErrorResponse {

    private String message;
    public ExceptionErrorResponse() {
    }

    public ExceptionErrorResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

package com.epam.edp.demo.exception;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}

package com.epam.edp.demo.exception;

public class InternalServerErrorException extends RuntimeException {
    public InternalServerErrorException(String message) {
        super(message);
    }
}

package com.epam.edp.demo.exception;


public class MissingParameterException extends RuntimeException{

    private final String parameterName;
    private final String parameterType;

    public MissingParameterException(String parameterName, String parameterType) {
        super(String.format("Required parameter '%s' of type '%s' is missing", parameterName, parameterType));
        this.parameterName = parameterName;
        this.parameterType = parameterType;
    }

    public String getParameterName() {
        return parameterName;
    }

    public String getParameterType() {
        return parameterType;
    }
}

package com.epam.edp.demo.exception;

public class NotFoundException extends RuntimeException{

    public NotFoundException(Throwable cause) {
        super(cause);
    }

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.epam.edp.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Optional;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ExceptionErrorResponse> handleException(BadRequestException exception) {
        ExceptionErrorResponse error = new ExceptionErrorResponse();
        error.setMessage(exception.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionErrorResponse> handleException(IllegalArgumentException exception) {
        ExceptionErrorResponse error = new ExceptionErrorResponse();
        error.setMessage(exception.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ExceptionErrorResponse> handleException(MissingParameterException exception){
        MissingParameterException customException = new MissingParameterException(
                exception.getParameterName(),
                exception.getParameterType()
        );
        ExceptionErrorResponse error = new ExceptionErrorResponse();
        error.setMessage(customException.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ExceptionErrorResponse> handleException(MethodArgumentTypeMismatchException exception) {
        // Extract relevant information from the original exception
        String parameterName = exception.getName();
        String requiredType = Optional.ofNullable(exception.getRequiredType())
                .map(Class::getSimpleName)
                .orElse("unknown");

        String providedValue = Optional.ofNullable(exception.getValue())
                .map(Object::toString)
                .orElse("null");


        TypeMismatchException customException = new TypeMismatchException(
                parameterName,
                requiredType,
                providedValue
        );
        ExceptionErrorResponse error = new ExceptionErrorResponse();
        error.setMessage(customException.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ExceptionErrorResponse> handleException(ForbiddenException exception) {
        ExceptionErrorResponse error = new ExceptionErrorResponse();
        error.setMessage(exception.getMessage());
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ExceptionErrorResponse> handleException(NotFoundException exception) {
        ExceptionErrorResponse error = new ExceptionErrorResponse();
        error.setMessage(exception.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(UnAuthorizedException.class)
    public ResponseEntity<ExceptionErrorResponse> handleException(UnAuthorizedException exception) {
        ExceptionErrorResponse error = new ExceptionErrorResponse();
        error.setMessage(exception.getMessage());
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ExceptionErrorResponse> handleException(ConflictException exception) {
        ExceptionErrorResponse error = new ExceptionErrorResponse();
        error.setMessage(exception.getMessage());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }
    @ExceptionHandler(InternalServerErrorException.class)
    public ResponseEntity<ExceptionErrorResponse> handleException(InternalServerErrorException exception) {
        ExceptionErrorResponse error = new ExceptionErrorResponse();
        error.setMessage(exception.getMessage());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionErrorResponse> handleException(Exception exception) {
        ExceptionErrorResponse error = new ExceptionErrorResponse();
        error.setMessage(exception.getMessage());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
package com.epam.edp.demo.exception;

public class TypeMismatchException extends RuntimeException {

    private final String parameterName;
    private final String requiredType;
    private final String providedValue;

    public TypeMismatchException(String parameterName, String requiredType, String providedValue) {
        super(String.format("Parameter '%s' should be of type '%s', but value '%s' was provided",
                parameterName, requiredType, providedValue));
        this.parameterName = parameterName;
        this.requiredType = requiredType;
        this.providedValue = providedValue;
    }

    public String getParameterName() {
        return parameterName;
    }

    public String getRequiredType() {
        return requiredType;
    }

    public String getProvidedValue() {
        return providedValue;
    }
}
package com.epam.edp.demo.exception;

public class UnAuthorizedException extends RuntimeException {
    public UnAuthorizedException(String message) {
        super(message);
    }

    public UnAuthorizedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnAuthorizedException(Throwable cause) {
        super(cause);
    }
}

package com.epam.edp.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class Feedback {

    private String id;
    private String locationId;
    private String rate;
    private String comment;
    private String userName;
    private String userAvatarUrl;
    private String date;
    private String type;
    private String reservationId;

}

package com.epam.edp.demo.model;

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
package com.epam.edp.demo.model;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class Reservation {

    private String reservationId;
    private String userId;
    private String locationId;
    private String tableNumber;
    private String date;
    private String timeFrom;
    private String timeTo;
    private String guestsNumber;
    private String status;
    private String preOrder;
    private String feedbackId;
    private String waiterEmail;

}
package com.epam.edp.demo.model;

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
package com.epam.edp.demo.service;

import com.epam.edp.demo.exception.BadRequestException;
import com.epam.edp.demo.model.Reservation;
import com.epam.edp.demo.model.Feedback;
import com.epam.edp.demo.model.LocationMetrics;
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
package com.epam.edp.demo.service;

import com.epam.edp.demo.model.LocationMetrics;
import com.epam.edp.demo.model.WaiterMetrics;
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
package com.epam.edp.demo.service;

import com.epam.edp.demo.model.LocationMetrics;
import com.epam.edp.demo.model.WaiterMetrics;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse;
import software.amazon.awssdk.services.ses.model.SesException;


import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

@Service
public class ReportSenderService {

    private static final Logger logger = LoggerFactory.getLogger(ReportSenderService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    @Value("${aws.ses.region}")
    private String awsRegion;

    @Value("${report.sender.email}")
    private String senderEmail;

    @Value("${manager.email}")
    private String managerEmail;

    @Autowired
    private SesClient sesClient;

    @Autowired
    private TemplateEngine templateEngine;

//    @PostConstruct
//    public void init() {
//        this.sesClient = SesClient.builder()
//                .region(Region.of(awsRegion))
//                .build();
//        logger.info("SES client initialized with region: {}", awsRegion);
//    }

    public void sendReport(List<WaiterMetrics> waiterMetrics, List<LocationMetrics> locationMetrics) {
        logger.info("Starting report generation and sending process");
        logger.info("Processing {} waiter metrics and {} location metrics",
                waiterMetrics.size(), locationMetrics.size());

        try {
            // Generate HTML email content using Thymeleaf
            String emailContent = generateEmailContent(waiterMetrics, locationMetrics);

            // Send the email with HTML content
            sendHtmlEmail(emailContent);

            logger.info("Report email sent successfully to {}", managerEmail);

        } catch (Exception e) {
            logger.error("Error during Report Generation and Sending: {}", e.getMessage(), e);
        }
    }

    private String generateEmailContent(List<WaiterMetrics> waiterMetrics, List<LocationMetrics> locationMetrics) {
        logger.debug("Generating HTML email content with Thymeleaf");

        // Create Thymeleaf context and add variables
        Context context = new Context();

        // Add report date information
        LocalDate now = LocalDate.now();
        context.setVariable("currentDate", now.format(DATE_FORMATTER));

        // Add metrics data directly
        context.setVariable("waiterMetrics", waiterMetrics);
        context.setVariable("locationMetrics", locationMetrics);

        // Process the template
        return templateEngine.process("reports/weekly-report", context);
    }

    private void sendHtmlEmail(String htmlContent) {
        logger.info("Preparing HTML email to send...");

        try {
            // Create a new MimeMessage
            Properties props = new Properties();
            Session session = Session.getInstance(props);
            MimeMessage message = new MimeMessage(session);

            // Set email headers
            message.setSubject("Weekly Restaurant Performance Report", "UTF-8");
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(managerEmail));

            // Set HTML content
            message.setContent(htmlContent, "text/html; charset=utf-8");

            // Convert to raw email format for SES
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            message.writeTo(outputStream);

            // Send via SES
            SendRawEmailRequest rawEmailRequest = SendRawEmailRequest.builder()
                    .rawMessage(RawMessage.builder()
                            .data(SdkBytes.fromByteBuffer(ByteBuffer.wrap(outputStream.toByteArray())))
                            .build())
                    .build();

            logger.debug("Sending HTML email request to SES");
            SendRawEmailResponse response = sesClient.sendRawEmail(rawEmailRequest);
            logger.info("Email sent successfully with message ID: {}", response.messageId());

        } catch (MessagingException e) {
            logger.error("Failed to create email message: {}", e.getMessage(), e);
        } catch (SesException e) {
            logger.error("AWS SES error: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error when sending email: {}", e.getMessage(), e);
        }
    }
}
package com.epam.edp.demo.service;

import com.epam.edp.demo.model.Feedback;
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
package com.epam.edp.demo.service;

import com.epam.edp.demo.model.Feedback;
import com.epam.edp.demo.model.Reservation;
import com.epam.edp.demo.model.WaiterMetrics;
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
package com.epam.edp.demo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DemoApplicationTests {

    @Test
    public void contextLoads() {
    }
}

