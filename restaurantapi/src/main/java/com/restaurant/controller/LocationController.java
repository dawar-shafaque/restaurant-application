package com.restaurant.controller;

import com.restaurant.exception.BadRequestException;
import com.restaurant.exception.NotFoundException;
import com.restaurant.model.Dish;
import com.restaurant.model.Feedback;
import com.restaurant.model.Location;
import com.restaurant.dto.LocationSelectOption;
import com.restaurant.dto.PaginatedResponse;
import com.restaurant.service.LocationService;
import com.restaurant.utils.FeedbackSortOptions;
import com.restaurant.validator.FeedbackValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;
    private final FeedbackValidator feedbackValidator;

    @GetMapping
    public ResponseEntity<List<Location>> getAllLocations() {
        List<Location> locations = locationService.getAllLocations();
        return new ResponseEntity<>(locations, HttpStatus.OK); // 200 OK
    }

    @GetMapping("/select-options")
    public ResponseEntity<List<LocationSelectOption>> getLocationSelectOptions() {

        List<LocationSelectOption> selectOptions = locationService.getLocationSelectOptions();
        if (selectOptions.isEmpty()) {
            throw new NotFoundException("No valid locations found");
        }
        return new ResponseEntity<>(selectOptions,HttpStatus.OK);
    }

    //Dishes
    @GetMapping("/{id}/speciality-dishes")
    public ResponseEntity<List<Dish>> getSpecialtyDishesByLocation(@PathVariable("id") String locationId) {

        if (locationId == null) {
            throw new BadRequestException("Location ID is required");
        }

        if (!locationService.checkLocationExist(locationId)){
            throw new NotFoundException("No valid Location found");
        }

        List<Dish> dishes = locationService.getSpecialityDishesByLocationId(locationId);
        return new ResponseEntity<>(dishes,HttpStatus.OK);
    }

    //Feedbacks
    @GetMapping("/{id}/feedbacks")
    public ResponseEntity<PaginatedResponse<Feedback>> getFeedbackByLocationWithPagination(@PathVariable("id") String locationId,
                                                                                           @RequestParam(name = "type", required = true) String type,
                                                                                           @RequestParam(name = "sortBy", required = false, defaultValue = "best") String sortBy,
                                                                                           @RequestParam(name = "page", required = false, defaultValue = "0") int page,
                                                                                           @RequestParam(name = "size", required = false, defaultValue = "10") int size){

        feedbackValidator.validateLocationId(locationId);
        feedbackValidator.validateType(type);
        feedbackValidator.validateSortBy(sortBy);
        feedbackValidator.validatePageAndLimit(page,size);

        // Check if location exists
        if (!locationService.checkLocationExist(locationId)) {
            throw new NotFoundException("No such location found.");
        }

        // Normalize sortBy parameter
        String normalizedSortBy = (sortBy != null) ? sortBy.toLowerCase() : "best";
        List<String> validSortOptions = FeedbackSortOptions.getAllSortOptions();

        if (!validSortOptions.contains(normalizedSortBy)) {
            normalizedSortBy = "best"; // Default to "best" if invalid
        }

        // Get paginated feedback
        PaginatedResponse<Feedback> feedbackPage = locationService.getFeedbackByLocationWithPagination(locationId, size, page, type, normalizedSortBy);
        return new ResponseEntity<>(feedbackPage,HttpStatus.OK);

    }
}