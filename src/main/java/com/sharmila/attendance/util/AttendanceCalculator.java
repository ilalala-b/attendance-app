package com.sharmila.attendance.util;

public final class AttendanceCalculator {
    private AttendanceCalculator() {
    }

    public static String formatPercentage(double value) {
        return String.format("%.0f%%", value);
    }
}
