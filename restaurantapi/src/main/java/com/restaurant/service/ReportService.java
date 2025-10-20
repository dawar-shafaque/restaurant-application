package com.restaurant.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.restaurant.exception.InternalServerErrorException;
import com.restaurant.model.Feedback;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {
    private final AmazonSQS amazonSQS;
    private final ObjectMapper objectMapper;

    public void sendReservationToSQS(Feedback feedback) {
        String waiterSQSQueueUrl = "https://sqs.ap-southeast-2.amazonaws.com/089718700404/tm16-waiter-report-dev6";

        try {
            log.info("Attempting to send feedback to SQS queue: {}", waiterSQSQueueUrl);

            // Convert feedback to JSON
            String messageBody = objectMapper.writeValueAsString(feedback);
            log.debug("Message body: {}", messageBody);

            // Create and configure the send message request
            SendMessageRequest sendMessageRequest = new SendMessageRequest()
                    .withQueueUrl(waiterSQSQueueUrl)
                    .withMessageBody(messageBody);

            // Send the message and get the result
            SendMessageResult result = amazonSQS.sendMessage(sendMessageRequest);

            // Log success
            log.info("Successfully sent message to SQS. MessageId: {}", result.getMessageId());

        } catch (Exception e) {
            log.error("Failed to send feedback to SQS queue: {}", e.getMessage(), e);
            throw new InternalServerErrorException("Failed to send feedback to SQS queue: " + e.getMessage());
        }
    }
}