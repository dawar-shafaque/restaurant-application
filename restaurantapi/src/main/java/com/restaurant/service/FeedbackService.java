package com.restaurant.service;

import com.epam.edai.run8.team16.exception.*;
import com.restaurant.exception.*;
import com.restaurant.model.Feedback;
import com.restaurant.dto.FeedbackRequest;
import com.restaurant.model.Reservation;
import com.restaurant.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {

    private final DynamoDbTable<Feedback> feedbackTable;
    private final DynamoDbTable<Reservation> reservationTable;
    private final UserService userService;
    private final ReportService reportService;
    private final TokenContextService tokenContextService;

    public void saveFeedback(Feedback feedback) {
        try {
            feedbackTable.putItem(feedback);
        } catch (DynamoDbException e) {
            log.error("Failed to save feedback: {}", e.getMessage());
            throw new InternalServerErrorException("Failed to save feedback");
        }
    }

    public Feedback getFeedbackByReservationIdAndType(String reservationId, String type) {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("Reservation ID cannot be null or empty");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Feedback type cannot be null or empty");
        }

        try {
            PageIterable<Feedback> results = feedbackTable.scan();

            return results.items().stream()
                    .filter(fb -> reservationId.equals(fb.getReservationId()) && type.equalsIgnoreCase(fb.getType()))
                    .findFirst()
                    .orElse(null);

        } catch (DynamoDbException e) {
            log.error("Failed to retrieve feedback by reservation ID and type: {}", e.getMessage());
            throw new InternalServerErrorException("Failed to retrieve feedback");
        }
    }

    public void updateFeedback(Feedback feedback) {
        if (feedback == null) {
            throw new IllegalArgumentException("Feedback object cannot be null.");
        }
        if (feedback.getId() == null || feedback.getId().isEmpty()) {
            throw new IllegalArgumentException("Feedback ID cannot be null or empty.");
        }

        try {
            feedbackTable.updateItem(feedback);
            log.info("Feedback updated successfully: {}", feedback.getId());
        } catch (DynamoDbException e) {
            log.error("Failed to update feedback: {}", e.getMessage());
            throw new InternalServerErrorException("Failed to update feedback");
        }
    }

    public String createFeedback(FeedbackRequest request) {
        try {
            if (request.getReservationId() == null || request.getReservationId().isBlank()) {
                throw new IllegalArgumentException("Reservation ID is required");
            }

            if ((request.getCuisineRating() == null || request.getCuisineRating().toString().isEmpty()) || (request.getServiceRating() == null || request.getServiceRating().toString().isEmpty())) {
                throw new IllegalArgumentException("Rating to be given for both cuisine and service.");
            }

            // Validate cuisine rating (if provided)
            if (request.getCuisineRating() != null && !request.getCuisineRating().toString().isEmpty()) {
                try {
                    int rating = request.getCuisineRating();
                    if (rating < 1 || rating > 5) {
                        throw new IllegalArgumentException("Cuisine rating must be between 1 and 5");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Cuisine rating must be a valid number between 1 and 5");
                }
            }

            // Validate service rating (if provided)
            if (request.getServiceRating() != null && !request.getServiceRating().toString().isEmpty()) {
                try {
                    int rating = request.getServiceRating();
                    if (rating < 1 || rating > 5) {
                        throw new IllegalArgumentException("Service rating must be between 1 and 5");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Service rating must be a valid number between 1 and 5");
                }
            }

            // Check if the reservation exists
            Reservation reservation = reservationTable.getItem(Key.builder().partitionValue(request.getReservationId()).build());

            if (reservation == null) {
                throw new NotFoundException("Reservation not found");
            }
            // Validate user authentication
            String userEmail = tokenContextService.getEmailFromToken();
            if (userEmail == null || userEmail.isEmpty()) {
                throw new UnAuthorizedException("Unauthorized: User not signed in");
            }
            // Validate user authorization - check if the signed-in user is the one who made the reservation
            if (!Objects.equals(reservation.getUserId(), userEmail)) {
                throw new NotFoundException("Forbidden: User is not authorized to submit feedback for this reservation");
            }
            // Validate reservation status
            if (!("IN_PROGRESS".equalsIgnoreCase(reservation.getStatus()) ||
                    "FINISHED".equalsIgnoreCase(reservation.getStatus()))) {
                throw new BadRequestException("Feedback can only be submitted for reservations that are 'in progress' or 'finished'");
            }

            // Initialize feedback IDs
            String cuisineFeedbackId = null;
            String serviceFeedbackId = null;
            String currentDate = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

            // Process cuisine feedback
            if (request.getCuisineRating() != null && !request.getCuisineRating().toString().isEmpty()) {
                cuisineFeedbackId = processFeedback(
                        request.getReservationId(),
                        reservation.getLocationId(),
                        "cuisine",
                        request.getCuisineRating().toString(),
                        request.getCuisineComment(),
                        currentDate,
                        request
                );
            }

            // Process service feedback
            if (request.getServiceRating() != null && !request.getServiceRating().toString().isEmpty()) {
                serviceFeedbackId = processFeedback(
                        request.getReservationId(),
                        reservation.getLocationId(),
                        "service",
                        request.getServiceRating().toString(),
                        request.getServiceComment(),
                        currentDate,
                        request
                );
            }

            // Update reservation with feedback IDs if needed
            if (cuisineFeedbackId != null || serviceFeedbackId != null) {
                if (cuisineFeedbackId != null) {
                    reservation.setFeedbackId(cuisineFeedbackId);
                }
                reservationTable.putItem(reservation);
            }
            return "Feedback has been submitted successfully";
        } catch (DynamoDbException e) {
            log.info("Error in CreateFeedbackHandler: ");
            throw new InternalServerErrorException("Internal Server Error" + e.getMessage());
        }
    }

    private String processFeedback(String reservationId, String locationId, String type,
                                   String rating, String comment, String date, FeedbackRequest request) {

        Feedback existingFeedback = getFeedbackByReservationIdAndType(reservationId, type);

        if (existingFeedback != null) {
            // Update existing feedback
            existingFeedback.setRate(rating);
            existingFeedback.setComment(comment == null ? "" : comment);
            existingFeedback.setDate(date);

            updateFeedback(existingFeedback);
            log.info(type + " feedback updated with ID: " + existingFeedback.getId());
            return existingFeedback.getId();
        }
        else {
            Reservation reservation = reservationTable.getItem(Key.builder().partitionValue(request.getReservationId()).build());
            User user =userService.getUserByEmail(reservation.getUserId());
            // Create new feedback
            Feedback newFeedback = new Feedback();
            newFeedback.setId(UUID.randomUUID().toString().substring(0, 8));
            newFeedback.setLocationId(locationId);
            newFeedback.setRate(rating);
            newFeedback.setComment(comment == null ? "" : comment);
            newFeedback.setType(type);
            newFeedback.setReservationId(reservationId);
            newFeedback.setDate(date);
            newFeedback.setUserName(user.getFirstName()+ " "+ user.getLastName());
            newFeedback.setUserAvatarUrl(user.getUserAvatarUrl());

            saveFeedback(newFeedback);
            log.info(type + " feedback created with ID: " + newFeedback.getId());

            reportService.sendReservationToSQS(newFeedback);
            return newFeedback.getId();
        }
    }

    public FeedbackRequest getFeedbackById(String id) {

        // Extract reservationId from the path parameters
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Reservation ID is required");
        }

        // Fetch the reservation
        Reservation reservation = reservationTable.getItem(Key.builder().partitionValue(id).build());

        if (reservation == null) {
            throw new NotFoundException("Reservation not found");
        }

        // Extract and validate user's email from headers or authentication token
        // Validate user authentication
        String userEmail = tokenContextService.getEmailFromToken();
        if (userEmail == null || userEmail.isEmpty()) {
            throw new UnAuthorizedException("Unauthorized: User not signed in");
        }

        if (!Objects.equals(reservation.getUserId(), userEmail)) {
            throw new ForbiddenException("Forbidden: You are not authorized to view feedback for this reservation");
        }

        // Adjust logic to fetch feedback for an existing reservation (no arbitrary restriction based on status).
        if (!reservation.getStatus().equalsIgnoreCase("FINISHED")) {
            log.info("Reservation status is '" + reservation.getStatus() + "'. Feedback retrieval allowed for all statuses.");
            throw new BadRequestException("Feedback can only be viewed for reservations that are finished'");

        }

        // Check if feedback exists
        Feedback cuisineFeedback = getFeedbackByReservationIdAndType(id, "cuisine");
        Feedback serviceFeedback = getFeedbackByReservationIdAndType(id, "service");

        if (cuisineFeedback == null && serviceFeedback == null) {
            throw new NotFoundException("Feedback not found for the given reservation ID");
        }

        FeedbackRequest request = new FeedbackRequest();
        request.setReservationId(id);
        request.setCuisineRating(cuisineFeedback != null ? Integer.parseInt(cuisineFeedback.getRate()) : 0);
        request.setCuisineComment(cuisineFeedback != null ? cuisineFeedback.getComment() : "");
        request.setServiceRating(serviceFeedback != null ? Integer.parseInt(serviceFeedback.getRate()) : 0);
        request.setServiceComment(serviceFeedback != null ? serviceFeedback.getComment() : "");
        // Log operation details
        log.info("Feedback successfully retrieved for reservation ID: " + id);

        return request;

    }
}