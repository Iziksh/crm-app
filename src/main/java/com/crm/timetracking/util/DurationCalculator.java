package com.crm.timetracking.util;

import java.time.Duration;
import java.time.LocalTime;

public final class DurationCalculator {

    private DurationCalculator() {}

    /**
     * Computes the duration in whole minutes between two LocalTime values.
     * Adds 1440 when the raw Duration is negative to handle midnight-crossing shifts.
     * Example: entry=22:00, exit=00:00 → 120 min.
     */
    public static int computeMinutes(LocalTime entry, LocalTime exit) {
        int minutes = (int) Duration.between(entry, exit).toMinutes();
        if (minutes < 0) {
            minutes += 24 * 60;
        }
        return minutes;
    }

    /** Formats total minutes as "H:mm". */
    public static String formatMinutes(int totalMinutes) {
        return String.format("%d:%02d", totalMinutes / 60, totalMinutes % 60);
    }
}