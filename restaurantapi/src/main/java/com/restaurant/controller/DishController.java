package com.restaurant.controller;

import com.restaurant.dto.DishDetails;
import com.restaurant.dto.DishSummary;
import com.restaurant.service.DishService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/dishes")
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;

    @GetMapping
    public ResponseEntity<List<DishSummary>> getAllDishes(@RequestParam(required = true) String dishType,
                                                          @RequestParam(required = false, defaultValue = "popularity,desc") String sort) {
        List<DishSummary> dishes = dishService.getDishes(dishType, sort);
        return new ResponseEntity<>(dishes, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DishDetails> getDishById(@PathVariable int id) {
        DishDetails dish = dishService.getDishById(id);
        return new ResponseEntity<>(dish, HttpStatus.OK);
    }
}