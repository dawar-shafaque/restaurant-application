package com.restaurant.service;

import software.amazon.awssdk.services.s3.model.S3Exception;
import com.restaurant.exception.BadRequestException;
import com.restaurant.exception.InternalServerErrorException;
import com.restaurant.exception.NotFoundException;
import com.restaurant.exception.UnAuthorizedException;
import com.restaurant.model.User;
import com.restaurant.dto.PasswordUpdateRequest;
import com.restaurant.dto.UserProfileRequest;
import com.restaurant.dto.UserProfileResponse;
import com.restaurant.utils.PasswordEncoderUtil;
import com.restaurant.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    @Value("${dynamodb.target.bucket}")
    private String bucketName;
    @Value("${aws.dynamodb.region}")
    private String region;
    private final DynamoDbTable<User> userTable;
    private final S3Client s3Client;

    public UserProfileResponse getUserProfile(String email) {

        User user = userTable.getItem(r -> r.key(k -> k.partitionValue(email)));
        if (user == null) {
            throw new NotFoundException("User with the provided email address does not exist.");
        }

        return new UserProfileResponse(
                user.getFirstName(),
                user.getLastName(),
                user.getUserAvatarUrl() != null ? user.getUserAvatarUrl() : ""
        );
    }

    public String updateUserProfile(String email , UserProfileRequest request) {

        if(request==null)
            throw new BadRequestException("Request body is required");

        User user = userTable.getItem(r -> r.key(k -> k.partitionValue(email)));

        if (user == null) {
            throw new NotFoundException("User not found for email: " + email);
        }

        if (request.getFirstName() != null && !request.getFirstName().isEmpty()) {
            if (!UserValidator.isValidName(request.getFirstName())) {
                throw new BadRequestException("Invalid first name provided. Please provide a valid name.");
            }
            user.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null && !request.getLastName().isEmpty()) {
            if (!UserValidator.isValidName(request.getLastName())) {
                throw new BadRequestException("Invalid last name provided. Please provide a valid name.");
            }
            user.setLastName(request.getLastName());
        }

        if (request.getUserAvatarUrl() != null && !request.getUserAvatarUrl().isEmpty()) {
            try {
                // Generate unique filename for the avatar
                String filename = UUID.randomUUID() + ".jpg";

                // Save the Base64 image to S3 and retrieve the image URL
                String imageUrl = saveImageToS3(request.getUserAvatarUrl(), filename);
                user.setUserAvatarUrl(imageUrl);
                userTable.updateItem(user);

            } catch (S3Exception e) {
                // Handle unexpected issues during S3 upload
                throw new InternalServerErrorException("Failed to upload avatar image: " + e.getMessage());
            }
        } else {
            throw new InternalServerErrorException("Failed to update user in DynamoDB: Base64-encoded image is required but was not provided.");
        }
        return "User profile updated successfully.";
    }
    private String saveImageToS3(String base64Image, String filename) {
        try {
            String base64Data = base64Image;
            if (base64Image.contains(",")) {
                base64Data = base64Image.split(",")[1]; // Remove any prefix (e.g., "data:image/jpeg;base64,")
            }

            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            String key = "images/avatar/" + filename; // Path in the bucket

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("image/jpeg")
                    .build();

            RequestBody requestBody = RequestBody.fromBytes(imageBytes);
            s3Client.putObject(putObjectRequest, requestBody);

            return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;

        } catch (S3Exception e) {
            throw new InternalServerErrorException("Failed to upload image to S3: " + e.getMessage());
        }
    }

    public String updatePassword(String email, PasswordUpdateRequest request) {
        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();

        if (oldPassword == null || oldPassword.isEmpty()) {
            throw new BadRequestException("Old password cannot be empty.");
        }

        if (newPassword == null || newPassword.isEmpty()) {
            throw new BadRequestException("New password cannot be empty.");
        }

        if (oldPassword.equals(newPassword)) {
            throw new BadRequestException("New password cannot be the same as the old password.");
        }

        if (!UserValidator.isValidPassword(newPassword)) {
            throw new BadRequestException("New Password must be 8-16 characters long and include at least one uppercase letter, one lowercase letter, one number, and one special character.");
        }

        try {
            boolean passwordUpdated = changePassword(email, oldPassword, newPassword);

            if (!passwordUpdated) {
                throw new UnAuthorizedException("Password update failed.");
            }

            return "Password updated successfully."; // 200 OK
        } catch (DynamoDbException e) {
            throw new InternalServerErrorException("An error occurred while updating the password: " + e.getMessage());
        }
    }

    private boolean changePassword(String email, String oldPassword, String newPassword){
        try {
            // 1. Fetch the user from the database
            User user = userTable.getItem(r -> r.key(k -> k.partitionValue(email)));
            if (user == null) {
                throw new NotFoundException("User not found.");
            }

            // 2. Validate the old password
            if (!PasswordEncoderUtil.matchPassword(oldPassword, user.getPassword())) {
                throw new BadRequestException("Current password is incorrect.");
            }

            // 3. Validate the new password against policy
            if (!UserValidator.isValidPassword(newPassword)) {
                throw new BadRequestException("New password does not meet the policy requirements.");
            }

            // 4. Encode and update the new password
            String encodedNewPassword = PasswordEncoderUtil.encodePassword(newPassword);
            user.setPassword(encodedNewPassword);

            user.setEmail(email);
            userTable.updateItem(user); // Save the updated user
            return true;

        } catch (DynamoDbException e) {
            return false;
        }
    }
}