package com.restaurant.config;

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