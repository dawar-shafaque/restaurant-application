package com.restaurant.config;

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