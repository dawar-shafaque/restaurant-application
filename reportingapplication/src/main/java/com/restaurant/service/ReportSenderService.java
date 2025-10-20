package com.restaurant.service;

import com.restaurant.model.LocationMetrics;
import com.restaurant.model.WaiterMetrics;
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