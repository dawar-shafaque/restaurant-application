package com.restaurant.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum TimeSlot {

    SLOT_10_30("10:30 - 12:00"),
    SLOT_12_15("12:15 - 13:45"),
    SLOT_14_00("14:00 - 15:30"),
    SLOT_15_45("15:45 - 17:15"),
    SLOT_17_30("17:30 - 19:00"),
    SLOT_19_15("19:15 - 20:45"),
    SLOT_21_00("21:00 - 22:30");

    private final String timeRange;

    public String getStartTime() {
        return timeRange.split(" - ")[0];
    }

    public static List<String> getAllTimeSlots() {
        return Arrays.asList(
                SLOT_10_30.getTimeRange(),
                SLOT_12_15.getTimeRange(),
                SLOT_14_00.getTimeRange(),
                SLOT_15_45.getTimeRange(),
                SLOT_17_30.getTimeRange(),
                SLOT_19_15.getTimeRange(),
                SLOT_21_00.getTimeRange()
        );
    }

    public static List<String> getTimeSlotsAtOrAfter(String time) {
        if (time == null || time.isEmpty() || time.equals("00:00")) {
            return getAllTimeSlots();
        }

        return Arrays.stream(TimeSlot.values())
                .filter(slot -> {
                    String slotStartTime = slot.getStartTime();
                    // Compare hours first, then minutes
                    String[] requestedParts = time.split(":");
                    String[] slotParts = slotStartTime.split(":");

                    int requestedHour = Integer.parseInt(requestedParts[0]);
                    int requestedMinute = Integer.parseInt(requestedParts[1]);
                    int slotHour = Integer.parseInt(slotParts[0]);
                    int slotMinute = Integer.parseInt(slotParts[1]);

                    if (slotHour > requestedHour) {
                        return true;
                    } else if (slotHour == requestedHour) {
                        return slotMinute >= requestedMinute;
                    }
                    return false;
                })
                .map(TimeSlot::getTimeRange)
                .collect(Collectors.toList());
    }

}