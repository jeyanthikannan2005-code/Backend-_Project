package com.example.smartinvent.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

/** Shared, predictable date-period rules for every table in the application. */
public final class DateFilterUtil {
    private DateFilterUtil() {}

    public static boolean matches(LocalDate value, String period) {
        if (value == null || period == null || period.isBlank() || "ALL".equalsIgnoreCase(period)) {
            return true;
        }
        LocalDate today = LocalDate.now();
        String normalized = period.trim().toUpperCase();
        return switch (normalized) {
            case "TODAY" -> value.equals(today);
            case "THIS_WEEK", "WEEK" -> !value.isBefore(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
                    && !value.isAfter(today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
            case "THIS_MONTH", "MONTH" -> value.getYear() == today.getYear() && value.getMonth() == today.getMonth();
            case "THIS_YEAR", "YEAR" -> value.getYear() == today.getYear();
            default -> true;
        };
    }

    public static boolean matches(LocalDateTime value, String period) {
        return value == null || matches(value.toLocalDate(), period);
    }
}
