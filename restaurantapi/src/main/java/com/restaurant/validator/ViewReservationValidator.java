package com.restaurant.validator;

import com.restaurant.exception.UnAuthorizedException;
import org.springframework.stereotype.Component;

@Component
public class ViewReservationValidator {

    public void validateEmail(String email) {
        if (email == null || email.isEmpty()) {
            throw new UnAuthorizedException("Unauthorized: User not signed in");
        }
    }

    public boolean validateWaiterParameters(String email, String userRole) {
        validateEmail(email);

        return userRole.equalsIgnoreCase("WAITER");
    }

}
