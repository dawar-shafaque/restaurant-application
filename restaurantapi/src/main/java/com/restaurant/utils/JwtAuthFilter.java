package com.restaurant.utils;

import com.restaurant.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final DynamoDbTable<User> userTable;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Check if this is a public endpoint that doesn't need authentication
        String path = request.getRequestURI();
        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract the authorization header from the request
        final String authHeader = request.getHeader("Authorization");

        // If the header is empty or doesn't start with "Bearer", return 401
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorizedResponse(response);
            return;
        }

        // Extract the actual token from the header
        String token = authHeader.substring(7);

        try {
            // Validate the token and extract the claims
            Claims claims = jwtUtil.validateToken(token);
            String email = claims.getSubject();
            String role = claims.get("role", String.class);

            // If Email and role are present in the token, then:
            if (email != null && role != null) {
                // Look up the user in the DynamoDB
                User user = userTable.getItem(r -> r.key(k -> k.partitionValue(email)));

                // User exists and Roles match
                if (user != null && user.getRole().equalsIgnoreCase(role)) {
                    // Downstream access to logged-in User's identity
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(email, null, List.of());
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // Allow the Request to proceed - let parameter validation happen naturally
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            // If email and role mismatch, return 401
            sendUnauthorizedResponse(response);

        } catch (JwtException e) {
            // If token is invalid, expired, malformed, etc., return 401
            sendUnauthorizedResponse(response);
        }
    }

    private void sendUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", "Please sign in to continue");

        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/auth/") ||
                path.startsWith("/dish/popular") ||
                path.startsWith("/dishes") ||
                path.startsWith("/locations") ||
                path.startsWith("/bookings/tables") ||
                path.startsWith("/swagger-ui/") ||
                path.equals("/swagger-ui.html") ||
                path.startsWith("/v3/api-docs");
    }
}