package com.restaurant.service;

import com.restaurant.exception.BadRequestException;
import com.restaurant.exception.ConflictException;
import com.restaurant.exception.UnAuthorizedException;
import com.restaurant.model.User;
import com.restaurant.model.Waiter;
import com.restaurant.dto.AuthResponse;
import com.restaurant.dto.SignInRequest;
import com.restaurant.dto.SignUpRequest;
import com.restaurant.utils.JwtUtil;
import com.restaurant.utils.PasswordEncoderUtil;
import com.restaurant.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import java.time.Instant;
import static com.restaurant.utils.PasswordEncoderUtil.matchPassword;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final DynamoDbTable<User> userTable;
    private final DynamoDbTable<Waiter> waiterTable;
    private final JwtUtil jwtUtil;

    public void registerUser(SignUpRequest signUpRequest) {

        if(!UserValidator.validate(signUpRequest))
            throw new BadRequestException("Validation failed with errors");

        String email = signUpRequest.getEmail();
        String password = signUpRequest.getPassword();
        String firstName = signUpRequest.getFirstName();
        String lastName = signUpRequest.getLastName();

        User user = userTable.getItem(r -> r.key(k -> k.partitionValue(email)));

        if (user != null){
            throw new ConflictException("An user already exists with this email");
        }
        else{
            String role = "Customer";
            if(checkIfWaiter(email)){
                role = "Waiter";
            }

            // Encode the password and set user data
            String encodedPassword = PasswordEncoderUtil.encodePassword(password);
            String createdAt = Instant.now().toString();

            String userAvatarUrl = "https://team16-deployment-bucket.s3.ap-southeast-2.amazonaws.com/images/avatar/defaultProfile.jpg";

            User userForDb = new User();
            userForDb.setEmail(email);
            userForDb.setPassword(encodedPassword);
            userForDb.setFirstName(firstName);
            userForDb.setLastName(lastName);
            userForDb.setCreatedAt(createdAt);
            userForDb.setRole(role);
            userForDb.setUserAvatarUrl(userAvatarUrl);

            userTable.putItem(userForDb);
        }
    }

    public AuthResponse authenticateUser(SignInRequest signInRequest) {

        if (signInRequest.getEmail() == null || signInRequest.getEmail().isEmpty() ) {
            throw new BadRequestException("Email is required for sign-in");
        }
        if (signInRequest.getPassword() == null || signInRequest.getPassword().isEmpty() ) {
            throw new BadRequestException("Password is required for sign-in");
        }

        String email = signInRequest.getEmail();
        String password = signInRequest.getPassword();

        if(!UserValidator.isValidEmail(email))
            throw new BadRequestException("Invalid email format. Please ensure it follows the format username@domain.com.");

//        if(!UserValidator.isValidPassword(password))
//            throw new BadRequestException("Password must be 8-16 characters long and include at least one uppercase letter, one lowercase letter, one number, and one special character.");

        User user = userTable.getItem(r -> r.key(k -> k.partitionValue(email)));
        if (user == null) {
            throw new UnAuthorizedException("Invalid email or password.");
        }

        // Check password
        if (!matchPassword(password, user.getPassword())) {
            throw new UnAuthorizedException("Invalid email or password.");
        }

        //Default role is Customer.
        String role = "Customer";

        if(checkIfWaiter(email)){
            role = "Waiter";
        }
        String accessToken = jwtUtil.generateToken(email, role);
        // Create AuthResponse object to return
        AuthResponse authResponse = new AuthResponse();
        authResponse.setRole(user.getRole());
        authResponse.setAccessToken(accessToken);
        authResponse.setName(user.getFirstName() + " " + user.getLastName());
        authResponse.setMessage("Welcome, "+authResponse.getName()+"! You have successfully logged in.");
        return authResponse;
    }

    public boolean checkIfWaiter(String email){
        Key key = Key.builder().partitionValue(email).build();
        return waiterTable.getItem(key) != null;
    }
}