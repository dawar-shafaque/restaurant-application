package com.restaurant.service;

import com.restaurant.exception.NotFoundException;
import com.restaurant.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {
    private final DynamoDbTable<User> userDynamoDbTable;

    public User getUserByEmail(String email) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        User user = userDynamoDbTable.getItem(Key.builder().partitionValue(email).build());
        if (user == null) {
            throw new NotFoundException("User not found with email: " + email);
        }
        return user;
    }

    public String getUserRole(String email){
        User user = getUserByEmail(email);
        return user.getRole();
    }

    public String getUserName(String email) {
        if (email == null || email.isEmpty()) {
            return "Anonymous";
        }

        User user = getUserByEmail(email);
        if (user != null) {
            String firstName = user.getFirstName() != null ? user.getFirstName() : "";
            String lastName = user.getLastName() != null ? user.getLastName() : "";

            if (!firstName.isEmpty() || !lastName.isEmpty()) {
                return firstName + " " + lastName;
            }
        }
        return "Anonymous";
    }

    public boolean userExists(String email){
        if (email == null || email.isEmpty()) {
            return false;
        }

        User user = userDynamoDbTable.getItem(Key.builder()
                .partitionValue(email)
                .build());

        return user != null;
    }

    public List<String> getCustomersByNamePrefix(String namePrefix) {
        // Get all customers
        List<User> customers = getAllCustomers();

        // Filter by name prefix if provided
        List<User> filteredCustomers = filterCustomersByPrefix(customers, namePrefix);

        // Format the result as "Name, Email"
        return formatCustomerList(filteredCustomers);
    }

    private List<User> getAllCustomers() {
        // Create filter expression for role = "Customer"
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":customerRole", AttributeValue.builder().s("Customer").build());

        Map<String, String> expressionNames = new HashMap<>();
        expressionNames.put("#userRole", "role");

        Expression filterExpression = Expression.builder()
                .expression("#userRole = :customerRole")
                .expressionValues(expressionValues)
                .expressionNames(expressionNames)
                .build();

        ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                .filterExpression(filterExpression)
                .build();

        // Scan the table with the filter
        List<User> customers = new ArrayList<>();
        userDynamoDbTable.scan(scanRequest).items().forEach(customers::add);
        return customers;
    }

    private List<User> filterCustomersByPrefix(List<User> customers, String namePrefix) {
        if (namePrefix == null || namePrefix.isEmpty()) {
            return customers;
        }

        String prefix = namePrefix.toLowerCase();
        return customers.stream()
                .filter(user -> matchesPrefix(user, prefix))
                .toList();
    }

    private boolean matchesPrefix(User user, String prefix) {
        String firstName = user.getFirstName() != null ? user.getFirstName().toLowerCase() : "";
        String lastName = user.getLastName() != null ? user.getLastName().toLowerCase() : "";
        String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";

        return firstName.startsWith(prefix) ||
                lastName.startsWith(prefix) ||
                email.startsWith(prefix);
    }

    private List<String> formatCustomerList(List<User> customers) {
        return customers.stream()
                .map(user -> {
                    String firstName = user.getFirstName() != null ? user.getFirstName() : "";
                    String lastName = user.getLastName() != null ? user.getLastName() : "";
                    String fullName = (firstName + " " + lastName).trim();
                    return fullName + ", " + user.getEmail();
                })
                .toList();
    }
}