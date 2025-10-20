package com.restaurant.controller;

import com.restaurant.dto.FeedbackRequest;
import com.restaurant.dto.FeedbackResponse;
import com.restaurant.exception.NotFoundException;
import com.restaurant.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping("/{id}")
    public ResponseEntity<FeedbackRequest> getFeedbackById(@PathVariable String id) {
        FeedbackRequest feedback = feedbackService.getFeedbackById(id);
        if(feedback == null)
            throw new NotFoundException("No feedback was found.");
        return new ResponseEntity<>(feedback,HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<FeedbackResponse> createFeedback(@RequestBody FeedbackRequest feedback) {
        String response = feedbackService.createFeedback(feedback);
        FeedbackResponse feedbackResponse = new FeedbackResponse();
        feedbackResponse.setMessage(response);
        return new ResponseEntity<>(feedbackResponse, HttpStatus.CREATED);
    }
}