package com.sharmila.attendance.model;

import java.time.LocalDate;

public class Attendance {
    private int id;
    private int studentId;
    private int courseId;
    private LocalDate attendanceDate;
    private int period;
    private String status;

    public Attendance(int id, int studentId, int courseId, LocalDate attendanceDate, int period, String status) {
        this.id = id;
        this.studentId = studentId;
        this.courseId = courseId;
        this.attendanceDate = attendanceDate;
        this.period = period;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public int getStudentId() {
        return studentId;
    }

    public int getCourseId() {
        return courseId;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public int getPeriod() {
        return period;
    }

    public String getStatus() {
        return status;
    }
}
