package com.restaurant.config;

import com.restaurant.utils.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        return http
                // Enable CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                //Disables CSRF protection
                .csrf(AbstractHttpConfigurer::disable)
                //auth rule for incoming request
                .authorizeHttpRequests(auth -> auth
                        //allow all requests under  auth without authentication
                        .requestMatchers(
                                "/auth/**",
                                "/dish/popular",
                                "/dishes/{id}",
                                "/dishes",
                                "/locations/select-options",
                                "/locations",
                                "/locations/{id}/speciality-dishes",
                                "/locations/{id}/feedbacks",
                                "/bookings/tables",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                                ).permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement( sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                //Register the custom JWT filter before the default user-password auth
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}