package com.restaurant.validator;

import com.restaurant.exception.BadRequestException;
import com.restaurant.utils.FeedbackSortOptions;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class FeedbackValidator {

    public void validateLocationId(String locationId) {
        if (locationId == null || locationId.isBlank()) {
            throw new BadRequestException("Missing path parameter 'id'");
        }
    }

    public void validateType(String type) {
        if (type == null || type.isBlank()) {
            throw new BadRequestException("Missing or invalid 'type' parameter");
        }

        List<String> validTypes = List.of("cuisine", "service");
        if (!validTypes.contains(type.toLowerCase())) {
            throw new BadRequestException("Invalid feedback type. Allowed values: cuisine, service");
        }
    }

    public void validateSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return; // Default will be applied
        }

        List<String> validSortOptions = FeedbackSortOptions.getAllSortOptions();
        if (!validSortOptions.contains(sortBy.toLowerCase())) {
            throw new BadRequestException("Invalid sortBy parameter. Valid options are: best, worst, newest, oldest");
        }
    }

    public void validatePageAndLimit(int page, int limit){
        if(page<0)
            throw new BadRequestException("Page number must be non-negative");
        if (limit <= 0 || limit > 100)
            throw new BadRequestException("Limit must be between 1 and 100");
    }
}