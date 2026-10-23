package com.sharmila.attendance.ui;

import com.sharmila.attendance.model.Student;
import java.time.LocalDate;

@FunctionalInterface
public interface AttendanceCellListener {
    void onToggle(Student student, LocalDate date, int period);
}
