package com.sharmila.attendance.util;

import java.time.DayOfWeek;
import java.time.LocalDate;

public final class DateUtil {
    private DateUtil() {
    }

    public static LocalDate mondayOf(LocalDate date) {
        return date.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
