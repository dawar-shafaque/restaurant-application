package com.restaurant.service;

import com.restaurant.dto.LocationSelectOption;
import com.restaurant.dto.PaginatedResponse;
import com.restaurant.exception.BadRequestException;
import com.restaurant.exception.InternalServerErrorException;
import com.restaurant.exception.NotFoundException;
import com.restaurant.model.Dish;
import com.restaurant.model.Feedback;
import com.restaurant.model.Location;
import com.restaurant.utils.FeedbackSortOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    private final DynamoDbTable<Location> locationDynamoDbTable;
    private final DynamoDbTable<Dish> dishesDynamoDbTable;
    private final DynamoDbTable<Feedback> feedbackDynamoDbTable;

    public List<Location> getAllLocations() {
        List<Location> locations = new ArrayList<>();
        locationDynamoDbTable.scan().items().forEach(locations::add);

        if (locations.isEmpty()) {
            throw new NotFoundException("No Location Found"); // 404 Not Found
        }
        return locations;
    }

    public List<Dish> getSpecialityDishesByLocationId(String locationId) {
        if (!checkLocationExist(locationId)) {
            throw new NotFoundException("Location not found.");
        }
        List<Dish> resultDish = dishesDynamoDbTable.scan()
                .items()
                .stream()
                .filter(dish -> dish.getLocationId().equals(locationId))
                .collect(Collectors.toList());

        if (resultDish.isEmpty()) {
            throw new NotFoundException("No specialty dishes found for this location.");
        }
        return resultDish;
    }

    public List<LocationSelectOption> getLocationSelectOptions() {
        List<LocationSelectOption> selectOptions = new ArrayList<>();

        locationDynamoDbTable.scan().items().forEach(location ->
        {
            if (location.getId() != null && location.getAddress() != null) {
                selectOptions.add(new LocationSelectOption(location.getId(), location.getAddress()));
            }
        });

        if (selectOptions.isEmpty()) {
            throw new NotFoundException("No Selected Location Found");
        }
        return selectOptions;
    }

    public boolean checkLocationExist(String locationId) {
        return locationDynamoDbTable.getItem(r -> r.key(k -> k.partitionValue(locationId))) != null;
    }

    public String getLocationName(String locationId) {
        if (locationId == null || locationId.isEmpty()) {
            return "Unknown Location";
        }

        try {
            Location location = locationDynamoDbTable.getItem(Key.builder().partitionValue(locationId).build());

            if (location != null && location.getAddress() != null) {
                return location.getAddress();
            }
            return "Unknown Location";
        } catch (Exception e) {
            return "Unknown Location";
        }
    }

    public PaginatedResponse<Feedback> getFeedbackByLocationWithPagination(String locationId, int limit, int page, String type, String sortBy) {
        if (locationId == null || locationId.isBlank()) {
            throw new BadRequestException("Location ID cannot be null or empty");
        }

        if (type == null || type.isBlank()) {
            throw new BadRequestException("Feedback type cannot be null or empty");
        }

        String normalizedSortBy = (sortBy != null) ? sortBy.toLowerCase() : "best";
        List<String> sortOptions = FeedbackSortOptions.getAllSortOptions();
        if (!sortOptions.contains(normalizedSortBy)) {
            normalizedSortBy = "best";
            log.warn("Invalid sortBy value '{}' provided, defaulting to 'best'", sortBy);
        }

        try {
            List<Feedback> feedbacks = feedbackDynamoDbTable.scan().items().stream().toList();

            List<Feedback> filteredFeedbacks = feedbacks.stream()
                    .filter(fb -> locationId.equals(fb.getLocationId()))
                    .filter(fb -> type.equalsIgnoreCase(fb.getType()))
                    .collect(Collectors.toList());

            sortFeedbacks(filteredFeedbacks, normalizedSortBy);

            int totalElements = filteredFeedbacks.size();
            int totalPages = (int) Math.ceil((double) totalElements / limit);

            if (page < 0) {
                throw new BadRequestException("Page number must be non-negative");
            }

            page = Math.min(page, totalPages > 0 ? totalPages - 1 : 0);
            int offset = page * limit;

            List<Feedback> pagedContent = filteredFeedbacks.stream()
                    .skip(offset)
                    .limit(limit)
                    .collect(Collectors.toList());

            return buildPaginatedResponse(pagedContent, page, limit, totalElements, totalPages, offset, normalizedSortBy);
        } catch (DynamoDbException e) {
            log.error("DynamoDB error in getFeedbackByLocationWithPagination: {}", e.getMessage(), e);
            throw new InternalServerErrorException("Database error while retrieving feedback");
        }
    }

    private void sortFeedbacks(List<Feedback> feedbacks, String sortCriteria) {
        feedbacks.sort((f1, f2) -> {
            if ("worst".equals(sortCriteria) || "best".equals(sortCriteria)) {
                try {
                    double rate1 = parseRate(f1.getRate());
                    double rate2 = parseRate(f2.getRate());
                    return "worst".equals(sortCriteria)
                            ? Double.compare(rate1, rate2)
                            : Double.compare(rate2, rate1);
                } catch (NumberFormatException e) {
                    log.warn("Invalid rate format encountered during sorting: {}", e.getMessage());
                    return 0;
                }
            } else {
                if (f1.getDate() == null && f2.getDate() == null) return 0;
                if (f1.getDate() == null) return "oldest".equals(sortCriteria) ? -1 : 1;
                if (f2.getDate() == null) return "oldest".equals(sortCriteria) ? 1 : -1;
                return "oldest".equals(sortCriteria)
                        ? f1.getDate().compareTo(f2.getDate())
                        : f2.getDate().compareTo(f1.getDate());
            }
        });
    }

    private double parseRate(String rate) {
        if (rate == null || rate.trim().isEmpty()) {
            throw new IllegalArgumentException("Rate cannot be null or empty");
        }
        return Double.parseDouble(rate);
    }

    private PaginatedResponse<Feedback> buildPaginatedResponse(
            List<Feedback> content, int page, int limit, int totalElements,
            int totalPages, int offset, String sortCriteria) {

        PaginatedResponse<Feedback> response = new PaginatedResponse<>();
        response.setTotalElements(totalElements);
        response.setTotalPages(totalPages);
        response.setSize(limit);
        response.setNumber(page);
        response.setNumberOfElements(content.size());
        response.setFirst(page == 0);
        response.setLast(page >= totalPages - 1);
        response.setEmpty(content.isEmpty());
        response.setContent(content);

        PaginatedResponse.Pageable pageable = new PaginatedResponse.Pageable();
        pageable.setOffset(offset);
        pageable.setPageNumber(page);
        pageable.setPageSize(limit);
        pageable.setPaged(true);
        pageable.setUnPaged(false);

        PaginatedResponse.Sort sort = new PaginatedResponse.Sort();
        sort.setDirection("worst".equals(sortCriteria) || "oldest".equals(sortCriteria) ? "ASC" : "DESC");
        sort.setProperty("best".equals(sortCriteria) || "worst".equals(sortCriteria) ? "rate" : "date");
        sort.setAscending("oldest".equals(sortCriteria) || "worst".equals(sortCriteria));
        sort.setIgnoreCase(false);
        sort.setNullHandling("NATIVE");

        pageable.setSort(List.of(sort));
        response.setPageable(pageable);
        response.setSort(List.of(sort));

        return response;
    }
}