package com.restaurant.validator;

import com.restaurant.dto.SignUpRequest;
import com.restaurant.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

@Component
@Slf4j
public class UserValidator {

    private static final String NAME_PATTERN = "^[A-Za-z'\\-]{1,50}$";
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9](?:[A-Za-z0-9._%+-]{0,63})@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$";
    private static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>])[A-Za-z\\d!@#$%^&*(),.?\":{}|<>]{8,16}$";

    private UserValidator(){}
    public static boolean validate(SignUpRequest request) {

        try {
            if (request == null) {
                log.warn("Validation failed: SignUpRequest is null");
                throw new BadRequestException("Invalid registration request: Request cannot be null");
            }

            if (request.getFirstName() == null) {
                throw new BadRequestException("Invalid registration request: First name is required");
            } else if (!isValidName(request.getFirstName())) {
                log.debug("Invalid first name: {}", request.getFirstName());
                throw new BadRequestException("Invalid registration request: First name must be up to 50 characters. Only Latin letters, hyphens, and apostrophes are allowed.");
            }

            if (request.getLastName() == null) {
                throw new BadRequestException("Invalid registration request: Last name is required");
            } else if (!isValidName(request.getLastName())) {
                log.debug("Invalid last name: {}", request.getLastName());
                throw new BadRequestException("Invalid registration request: Last name must be up to 50 characters. Only Latin letters, hyphens, and apostrophes are allowed.");
            }

            if (request.getEmail() == null) {
                throw new BadRequestException("Invalid registration request: Email is required");
            } else if (!isValidEmail(request.getEmail())) {
                log.debug("Invalid email: {}", request.getEmail());
                throw new BadRequestException("Invalid registration request: Invalid email format. Please ensure it follows the format username@domain.com.");
            }

            if (request.getPassword() == null) {
                throw new BadRequestException("Invalid registration request: Password is required");
            } else if (!isValidPassword(request.getPassword())) {
                log.debug("Password validation failed");
                throw new BadRequestException("Invalid registration request: Password must be 8-16 characters long and include at least one uppercase letter, one lowercase letter, one number, and one special character.");
            }

            return true;

        }catch (DynamoDbException e){
            log.warn("Validation failed with errors");
            return false;
        }
    }

    public static boolean isValidName(String name) {
        try {
            return name != null && name.matches(NAME_PATTERN);
        } catch (Exception e) {
            log.error("Error validating name: {}", e.getMessage());
            return false;
        }
    }

    public static boolean isValidEmail(String email) {
        try {
            return email != null && email.matches(EMAIL_PATTERN);
        } catch (Exception e) {
            log.error("Error validating email: {}", e.getMessage());
            return false;
        }
    }

    public static boolean isValidPassword(String password) {
        try {
            return password != null && password.matches(PASSWORD_PATTERN);
        } catch (Exception e) {
            log.error("Error validating password: {}", e.getMessage());
            return false;
        }
    }

}