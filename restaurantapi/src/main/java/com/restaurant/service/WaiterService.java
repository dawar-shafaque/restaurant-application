package com.restaurant.service;

import com.restaurant.exception.BadRequestException;
import com.restaurant.exception.InternalServerErrorException;
import com.restaurant.exception.NotFoundException;
import com.restaurant.utils.DateSlot;
import com.restaurant.model.Waiter;
import com.restaurant.utils.TimeSlot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaiterService {

    private final DynamoDbTable<Waiter> waiterDynamoDbTable;

    public List<Waiter> getWaitersByLocation(String locationId) {

        // Scan all waiters and filter by locationId
        List<Waiter> allWaiters = new ArrayList<>();
        waiterDynamoDbTable.scan().items().forEach(allWaiters::add);

        return allWaiters.stream()
                .filter(waiter -> waiter.getLocationId().equals(locationId))
                .toList();
    }

    public String findLeastBusyWaiter(String locationId, String date, String timeSlot) {
        try {
            List<Waiter> waiters = getWaitersByLocation(locationId);
            validateWaitersExist(waiters, locationId);

            String fullTimeSlot = findFullTimeSlot(timeSlot);
            List<String> availableWaiters = findAvailableWaitersForTimeSlot(waiters, date, fullTimeSlot);

            if (availableWaiters.isEmpty()) {
                throw new NotFoundException("No available waiters for the requested time slot.");
            }

            if (availableWaiters.size() == 1) {
                return availableWaiters.get(0);
            }

            return selectOptimalWaiter(availableWaiters, date, timeSlot);
        } catch (DynamoDbException e) {
            log.error("Error finding least busy waiter: " + e.getMessage());
            throw new InternalServerErrorException("Failed to find least busy waiter: " + e.getMessage());
        }
    }

    private void validateWaitersExist(List<Waiter> waiters, String locationId) {
        if (waiters.isEmpty()) {
            throw new BadRequestException("No waiters found for location " + locationId);
        }
    }

    private List<String> findAvailableWaitersForTimeSlot(List<Waiter> waiters, String date, String fullTimeSlot) {
        List<String> availableWaiters = new ArrayList<>();

        for (Waiter waiter : waiters) {
            if (isWaiterAvailableForTimeSlot(waiter, date, fullTimeSlot)) {
                availableWaiters.add(waiter.getEmail());
            }
        }

        return availableWaiters;
    }

    private boolean isWaiterAvailableForTimeSlot(Waiter waiter, String date, String fullTimeSlot) {
        try {
            DateSlot[] availableSlots = getWaiterAvailableSlots(waiter);

            for (DateSlot dateSlot : availableSlots) {
                if (!isValidDateSlot(dateSlot, date)) {
                    continue;
                }

                for (String slot : dateSlot.getAvailableTimeSlots()) {
                    if (slot != null && slot.equals(fullTimeSlot)) {
                        return true;
                    }
                }
            }

            return false;
        } catch (DynamoDbException e) {
            log.error("Error checking availability for waiter {}: {}", waiter.getEmail(), e.getMessage());
            return false;
        }
    }

    private DateSlot[] getWaiterAvailableSlots(Waiter waiter) {
        DateSlot[] availableSlots = waiter.getAvailableSlots();
        return availableSlots != null ? availableSlots : new DateSlot[0];
    }

    private boolean isValidDateSlot(DateSlot dateSlot, String date) {
        return dateSlot != null &&
                dateSlot.getDate() != null &&
                dateSlot.getDate().equals(date) &&
                dateSlot.getAvailableTimeSlots() != null;
    }

    private String selectOptimalWaiter(List<String> availableWaiters, String date, String timeSlot) {
        // Get availability counts for each waiter
        Map<String, Integer> waiterAvailabilityCount = getWaiterAvailabilityCounts(availableWaiters, date);

        // Find waiters with maximum availability (least busy)
        List<String> leastBusyWaiters = findLeastBusyWaiters(waiterAvailabilityCount);

        if (leastBusyWaiters.size() == 1) {
            return leastBusyWaiters.get(0);
        }

        // For multiple least busy waiters, find the one with optimal time slot distribution
        return findWaiterWithOptimalDistribution(leastBusyWaiters, date, timeSlot);
    }

    private Map<String, Integer> getWaiterAvailabilityCounts(List<String> waiterEmails, String date) {
        Map<String, Integer> waiterAvailabilityCount = new HashMap<>();

        for (String email : waiterEmails) {
            int count = countAvailableSlotsForDate(email, date);
            waiterAvailabilityCount.put(email, count);
        }

        return waiterAvailabilityCount;
    }

    private int countAvailableSlotsForDate(String waiterEmail, String date) {
        try {
            Waiter waiter = getWaiterByEmail(waiterEmail);
            DateSlot[] availableSlots = getWaiterAvailableSlots(waiter);

            for (DateSlot dateSlot : availableSlots) {
                if (isValidDateSlot(dateSlot, date)) {
                    return dateSlot.getAvailableTimeSlots().length;
                }
            }

            return 0;
        } catch (Exception e) {
            log.error("Error counting slots for waiter {}: {}", waiterEmail, e.getMessage());
            return 0;
        }
    }

    private List<String> findLeastBusyWaiters(Map<String, Integer> waiterAvailabilityCount) {
        int maxAvailableSlots = Collections.max(waiterAvailabilityCount.values());
        List<String> leastBusyWaiters = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : waiterAvailabilityCount.entrySet()) {
            if (entry.getValue() == maxAvailableSlots) {
                leastBusyWaiters.add(entry.getKey());
            }
        }

        return leastBusyWaiters;
    }

    private String findWaiterWithOptimalDistribution(List<String> waiterEmails, String date, String timeSlot) {
        int timeSlotIndex = findTimeSlotIndex(timeSlot);

        if (timeSlotIndex < 0) {
            return waiterEmails.get(0);
        }

        String selectedWaiter = null;
        int maxDistance = -1;

        for (String email : waiterEmails) {
            int[] binaryArray = createWaiterAvailabilityArray(email, date);
            int distance = calculateFarthestDistance(binaryArray, timeSlotIndex);

            if (distance > maxDistance) {
                maxDistance = distance;
                selectedWaiter = email;
            }
        }

        return selectedWaiter != null ? selectedWaiter : waiterEmails.get(0);
    }

    private int findTimeSlotIndex(String timeSlot) {
        List<String> allTimeSlots = TimeSlot.getAllTimeSlots();

        for (int i = 0; i < allTimeSlots.size(); i++) {
            if (allTimeSlots.get(i).startsWith(timeSlot)) {
                return i;
            }
        }

        return -1;
    }

    private int[] createWaiterAvailabilityArray(String waiterEmail, String date) {
        try {
            Waiter waiter = getWaiterByEmail(waiterEmail);
            DateSlot[] availableSlots = getWaiterAvailableSlots(waiter);
            return initializeBinaryArray(availableSlots, date);
        } catch (Exception e) {
            log.error("Error creating availability array for waiter {}: {}", waiterEmail, e.getMessage());
            return new int[TimeSlot.getAllTimeSlots().size()];
        }
    }

    private Waiter getWaiterByEmail(String email) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Waiter email cannot be null or empty");
        }

        try {
            Key key = Key.builder().partitionValue(email).build();
            Waiter waiter = waiterDynamoDbTable.getItem(key);

            if (waiter == null) {
                throw new NotFoundException("Waiter not found with email: " + email);
            }

            return waiter;
        } catch (DynamoDbException e) {
            log.error("Error retrieving waiter with email {}: {}", email, e.getMessage());
            throw new InternalServerErrorException("Failed to retrieve waiter: " + e.getMessage());
        }
    }

    // Helper method to find the full time slot
    private String findFullTimeSlot(String timeSlot) {
        List<String> allTimeSlots = TimeSlot.getAllTimeSlots();
        for (String slot : allTimeSlots) {
            if (slot.startsWith(timeSlot)) {
                return slot;
            }
        }
        return null;
    }

    private int[] initializeBinaryArray(DateSlot[] availableSlots, String date) {
        List<String> allTimeSlots = TimeSlot.getAllTimeSlots();
        int[] binaryArray = new int[allTimeSlots.size()];

        for (DateSlot dateSlot : availableSlots) {
            if (dateSlot.getDate().equals(date)) {
                String[] availableTimeSlots = dateSlot.getAvailableTimeSlots();
                for (String slot : availableTimeSlots) {
                    int index = allTimeSlots.indexOf(slot);
                    if (index >= 0) {
                        binaryArray[index] = 1; // Mark slot as available
                    }
                }
            }
        }

        return binaryArray;
    }

    private int calculateFarthestDistance(int[] binaryArray, int index) {
        int n = binaryArray.length;

        // Check left side
        int leftDistance = Integer.MAX_VALUE;
        for (int i = index - 1; i >= 0; i--) {
            if (binaryArray[i] == 1) {
                leftDistance = index - i;
                break;
            }
        }

        // Check right side
        int rightDistance = Integer.MAX_VALUE;
        for (int i = index + 1; i < n; i++) {
            if (binaryArray[i] == 1) {
                rightDistance = i - index;
                break;
            }
        }

        return Math.max(leftDistance, rightDistance) == Integer.MAX_VALUE ? n : Math.max(leftDistance, rightDistance);
    }

    public void updateWaiterAvailability(String email, String date, String timeSlot) {
        Key key = Key.builder().partitionValue(email).build();
        Waiter waiter = waiterDynamoDbTable.getItem(key);

        if (waiter == null) {
            throw new BadRequestException("Waiter " + email + " does not exist.");
        }

        DateSlot[] slots = waiter.getAvailableSlots();
        if (slots == null || slots.length == 0) {
            throw new BadRequestException("Waiter " + email + " has no available time slots.");
        }

        List<String> allTimeSlots = TimeSlot.getAllTimeSlots();
        String fullTimeSlot = null;
        for (String slot : allTimeSlots) {
            if (slot.startsWith(timeSlot)) {
                fullTimeSlot = slot;
                break;
            }
        }

        if (fullTimeSlot == null) {
            throw new BadRequestException("Invalid time slot: " + timeSlot);
        }

        for (DateSlot slot : slots) {
            if (slot.getDate().equals(date)) {
                List<String> updatedSlots = new ArrayList<>(Arrays.asList(slot.getAvailableTimeSlots()));
                updatedSlots.remove(fullTimeSlot);
                slot.setAvailableTimeSlots(updatedSlots.toArray(new String[0]));
                break;
            }
        }

        waiter.setAvailableSlots(slots);
        waiterDynamoDbTable.putItem(waiter);
    }

    public String getWaiterLocation(String waiterEmail) {
        if (waiterEmail == null || waiterEmail.isEmpty()) {
            throw new BadRequestException("Waiter email is required");
        }

        try {
            Waiter waiter = waiterDynamoDbTable.getItem(Key.builder().partitionValue(waiterEmail).build());

            if (waiter == null) {
                return null;
            }
            return waiter.getLocationId();
        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to get waiter location: " + e.getMessage());
        }
    }

    public List<String> getWaiterAssignedTables(String waiterEmail) {
        if (waiterEmail == null || waiterEmail.isEmpty()) {
            throw new BadRequestException("Waiter email is required");
        }

        try {
            return List.of("Any Table", "T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8", "T9", "T10");
        } catch (Exception e) {
            return List.of("Any Table"); // Default fallback
        }
    }

}