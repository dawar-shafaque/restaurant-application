package com.restaurant.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FeedbackRequest {
    private String reservationId;
    private String cuisineComment;
    private Integer cuisineRating;
    private String serviceComment;
    private Integer serviceRating;
}