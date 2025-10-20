package com.restaurant.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
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

    @Bean
    public AmazonSQS amazonSQS() {
        // Validate required inputs
        if (accessKey == null || secretKey == null || region == null || region.isEmpty()) {
            throw new IllegalArgumentException("AWS credentials and region must be properly set.");
        }

        // Use session credentials from temporary tokens
        BasicSessionCredentials sessionCredentials = new BasicSessionCredentials(
                accessKey,
                secretKey,
                sessionToken // Optional if sessionToken is null or not needed
        );

        // Amazon SQS client builder with explicit credentials and region
        return AmazonSQSClientBuilder.standard()
                .withRegion(region) // Default AWS region
                .withCredentials(new AWSStaticCredentialsProvider(sessionCredentials)) // Provide credentials
                .build();
    }

    @Bean
    public S3Client s3Client() {
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
                .refreshRequest(r -> r.roleArn(roleArn).roleSessionName("assume-role-session-s3"))
                .stsClient(stsClient)
                .build();

        // Step 4: Create S3 client using assumed credentials
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(assumeRoleCredentialsProvider)
                .build();
    }
}