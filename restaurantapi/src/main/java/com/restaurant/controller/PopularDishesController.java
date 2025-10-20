package com.restaurant.controller;

import com.restaurant.model.Dish;
import com.restaurant.service.DishService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/dish")
@RequiredArgsConstructor
public class PopularDishesController {

    private final DishService dishService;

    @GetMapping("/popular")
    public ResponseEntity<List<Dish>> getPopularDishes() {
        List<Dish> dishes = dishService.getPopularDishes();
        return new ResponseEntity<>(dishes, HttpStatus.OK);
    }
}
