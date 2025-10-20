package com.restaurant.controller;

import com.restaurant.exception.ForbiddenException;
import com.restaurant.exception.UnAuthorizedException;
import com.restaurant.service.TokenContextService;
import com.restaurant.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final UserService userService;
    private final TokenContextService tokenContextService;

    @GetMapping
    public ResponseEntity<List<String>> getCustomers (@RequestParam(required = false) String name){

        String userEmail = tokenContextService.getEmailFromToken();
        if (userEmail == null || userEmail.isEmpty()) {
            throw new UnAuthorizedException("User not signed in");
        }

        // Verify user is a waiter or admin
        String userRole = userService.getUserRole(userEmail);
        if (!"WAITER".equalsIgnoreCase(userRole)) {
            throw new ForbiddenException("Only waiters can access customer list");
        }

        // Get all customers
        List<String> customers = userService.getCustomersByNamePrefix(name);
        return new ResponseEntity<>(customers, HttpStatus.OK);
    }
}
