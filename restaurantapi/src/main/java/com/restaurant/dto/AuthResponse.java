package com.restaurant.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AuthResponse {
    private String role;           // The user's role (e.g., "USER", "ADMIN")
    private String accessToken;    // The JWT access token
    private String name;           // The user's full name (constructed as "FirstName LastName")
    private String message;
}