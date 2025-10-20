package com.restaurant.config;

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