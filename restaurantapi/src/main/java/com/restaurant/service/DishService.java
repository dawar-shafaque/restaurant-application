package com.restaurant.service;

import com.restaurant.dto.DishDetails;
import com.restaurant.dto.DishSummary;
import com.restaurant.exception.BadRequestException;
import com.restaurant.exception.NotFoundException;
import com.restaurant.model.Dish;
import com.restaurant.model.Waiter;
import com.restaurant.utils.DishType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DishService {

    @Value("${dynamodb.target.bucket}")
    private String bucketName;
    private final DynamoDbTable<Dish> dishTable;
    private final DynamoDbTable<Waiter> waiterTable;
    private final UserService userService;
    private final TokenContextService tokenContextService;

    public List<Dish> getPopularDishes() {
        List<Dish> dishes = popularDishes();

        if (dishes.isEmpty()) {
            return new ArrayList<>();
        }
        return dishes;
    }

    public List<Dish> popularDishes() {
        return dishTable.scan().items().stream()
                .filter(Dish::isPopular)
                .toList();
    }

    public List<DishSummary> getDishes(String dishType, String sort) {

        // Validate required parameters
        if (dishType == null || dishType.isBlank()) {
            throw new BadRequestException("dishType is a required parameter");
        }

        String userEmail = null;
        try {
            userEmail = tokenContextService.getEmailFromToken();
        }
        catch(Exception e){
            log.debug("No valid authentication token found, proceeding as unauthenticated user");
        }
        String userRole = null;

        if (userEmail != null) {
            userRole = userService.getUserRole(userEmail);
        }

        List<DishSummary> dishes;

        if (userEmail == null || !"WAITER".equalsIgnoreCase(userRole)) {
            dishes = getAllDishes(dishType, sort);
        } else {
            dishes = getAllDishes(dishType, sort, userRole, userEmail);
        }

        return dishes;
    }

    public List<DishSummary> getAllDishes(String dishType, String sort, String role, String userEmail) {
        List<Dish> dishes = dishTable.scan().items().stream()
                .collect(Collectors.toList());

        if ("waiter".equalsIgnoreCase(role)) {
            String locationId = getWaiterLocation(userEmail);
            dishes = dishes.stream()
                    .filter(d -> locationId.equals(d.getLocationId()))
                    .collect(Collectors.toList());
        }

        Set<String> dishTypes = DishType.getAllDishTypes();

        if (!dishTypes.contains(dishType)) {
            throw new IllegalArgumentException("Invalid dish type. Allowed values: Appetizers, Main Courses, Desserts");
        }

        dishes = dishes.stream()
                .filter(d -> d.getDishType().toLowerCase().equalsIgnoreCase(dishType))
                .collect(Collectors.toList());

        if (sort != null && !sort.isBlank()) {
            switch (sort.toLowerCase()) {
                case "price,asc" -> dishes.sort(Comparator.comparing(d -> parsePrice(d.getPrice())));
                case "price,desc" -> dishes.sort(Comparator.comparing((Dish d) -> parsePrice(d.getPrice())).reversed());
                case "popularity,asc" -> dishes.sort(Comparator.comparing(Dish::isPopular));
                case "popularity,desc" -> dishes.sort(Comparator.comparing(Dish::isPopular).reversed());
                default -> throw new BadRequestException("Invalid sort parameter");
            }
        }

        return dishes.stream().map(this::toSummaryDto).toList();
    }

    private double parsePrice(String price) {
        if (price == null || price.isBlank()) return 0;
        return Double.parseDouble(price.replace("$", "").trim());
    }

    public List<DishSummary> getAllDishes(String dishType, String sort) {


        List<Dish> dishes = dishTable.scan().items().stream()
                .collect(Collectors.toList());

        Set<String> dishTypes = DishType.getAllDishTypes();

        if (!dishTypes.contains(dishType)) {
            throw new IllegalArgumentException("Invalid dish type. Allowed values: Appetizers, Main Courses, Desserts");
        }

        dishes = dishes.stream()
                .filter(d -> d.getDishType().toLowerCase().equalsIgnoreCase(dishType))
                .collect(Collectors.toList());

        dishes = dishes.stream()
                .filter(d -> d.getDishType().toLowerCase().equalsIgnoreCase(dishType))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(Dish::getName, d -> d, (d1, d2) -> d1),
                        map -> new ArrayList<>(map.values())
                ));

        if (sort != null && !sort.isBlank()) {
            switch (sort.toLowerCase()) {
                case "price,asc" -> dishes.sort(Comparator.comparing(d -> parsePrice(d.getPrice())));
                case "price,desc" -> dishes.sort(Comparator.comparing((Dish d) -> parsePrice(d.getPrice())).reversed());
                case "popularity,asc" -> dishes.sort(Comparator.comparing(Dish::isPopular));
                case "popularity,desc" -> dishes.sort(Comparator.comparing(Dish::isPopular).reversed());
                default -> throw new BadRequestException("Invalid sort parameter");
            }
        }
        return dishes.stream().map(this::toSummaryDto).toList();
    }

    private DishSummary toSummaryDto(Dish dish) {
        DishSummary dto = new DishSummary();
        dto.setId(dish.getId());
        dto.setName(dish.getName());
        dto.setPrice(dish.getPrice());
        dto.setWeight(dish.getWeight());
        dto.setPreviewImageUrl(dish.getImageUrl());
        dto.setAvailable(dish.isAvailable());
        return dto;
    }

    private String getWaiterLocation(String waiterEmail) {
        Waiter waiter = waiterTable.scan().items().stream()
                .filter(waiter1 -> waiter1.getEmail().equalsIgnoreCase(waiterEmail))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Waiter not found"));


        return waiter.getLocationId();
    }

    public DishDetails getDishById(int id) {

        DishDetails dish = getDish(id);

        if (dish == null) {
            throw new NotFoundException("Dish not found");
        }
        return dish;
    }

    public DishDetails getDish(int id) {
        Dish dish = dishTable.getItem(Key.builder().partitionValue(id).build());
        if (dish == null || dish.toString().isEmpty()) {
            return null;
        }

        DishDetails dto = new DishDetails();
        dto.setId(dish.getId());
        dto.setName(dish.getName());
        dto.setDescription(dish.getDescription());
        dto.setImageUrl(dish.getImageUrl());
        dto.setPrice(dish.getPrice());
        dto.setWeight(dish.getWeight());
        dto.setDishType(dish.getDishType());
        dto.setCalories(dish.getCalories());
        dto.setFats(dish.getFats());
        dto.setCarbohydrates(dish.getCarbohydrates());
        dto.setProteins(dish.getProteins());
        dto.setVitamins(dish.getVitamins());
        dto.setState(dish.isAvailable());
        dto.setPopular(dish.isPopular());
        dto.setLocationId(dish.getLocationId());

        return dto;
    }
}