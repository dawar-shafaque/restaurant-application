package com.restaurant.controller;

import com.restaurant.dto.AuthResponse;
import com.restaurant.dto.Response;
import com.restaurant.dto.SignInRequest;
import com.restaurant.dto.SignUpRequest;
import com.restaurant.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signUp")
    public ResponseEntity<Response> registerUser(@RequestBody SignUpRequest signUpRequest) {
        authService.registerUser(signUpRequest);
        Response response = new Response();
        response.setMessage("User registered successfully.");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/signIn")
    public ResponseEntity<AuthResponse> authenticateUser(@RequestBody SignInRequest signInRequest) {
        AuthResponse authResponse = authService.authenticateUser(signInRequest);
        return new ResponseEntity<>(authResponse, HttpStatus.OK);
    }
}