package com.restaurant.controller;

import com.restaurant.dto.PasswordUpdateRequest;
import com.restaurant.dto.UserProfileRequest;
import com.restaurant.dto.UserProfileResponse;
import com.restaurant.exception.NotFoundException;
import com.restaurant.exception.UnAuthorizedException;
import com.restaurant.service.ProfileService;
import com.restaurant.service.TokenContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final TokenContextService tokenContextService;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(){

        String userEmail = tokenContextService.getEmailFromToken();

        if (userEmail == null || userEmail.isEmpty()) {
            throw new UnAuthorizedException("Please log in to make a Changes");
        }

        UserProfileResponse response = profileService.getUserProfile(userEmail);
        if (response == null) {
            throw new NotFoundException("User Data Not Found");
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/profile")
    public ResponseEntity<String> updateUserProfile(@RequestBody UserProfileRequest userProfileRequest){

        String userEmail = tokenContextService.getEmailFromToken();
        if (userEmail == null || userEmail.isEmpty()) {
            throw new UnAuthorizedException("User not authenticated: Please log in to make a Changes");
        }

        String result = profileService.updateUserProfile(userEmail , userProfileRequest);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PutMapping("/profile/password")
    public ResponseEntity<String> updatePassword(@RequestBody PasswordUpdateRequest passwordUpdateRequest){

        String userEmail = tokenContextService.getEmailFromToken();
        if (userEmail == null || userEmail.isEmpty()) {
            throw new UnAuthorizedException("User not authenticated: Please log in to make a Changes");
        }
        String result = profileService.updatePassword(userEmail , passwordUpdateRequest);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
