package com.sharmila.attendance.controller;

import com.sharmila.attendance.dao.AttendanceDAO;
import com.sharmila.attendance.model.Student;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

import java.time.LocalDate;
import java.util.List;

public class AttendanceController {
    private final AttendanceDAO dao = new AttendanceDAO();

    public String toggle(int studentId, int courseId, LocalDate date, int period) {
        String current = dao.getStatus(studentId, courseId, date, period);
        String next = switch (current) {
            case "PRESENT" -> "ABSENT";
            case "ABSENT" -> "";
            default -> "PRESENT";
        };
        dao.setStatus(studentId, courseId, date, period, next);
        return next;
    }

    public void markAllPresent(Window owner, List<Student> students, int courseId, LocalDate date, int period) {
        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Mark all " + students.size() + " students as Present for " + date + ", Period " + period + "?",
                ButtonType.CANCEL,
                ButtonType.OK
        );
        confirm.setTitle("Mark All Present");
        confirm.setHeaderText("Confirm attendance");
        confirm.initOwner(owner);

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        for (Student student : students) {
            dao.setStatus(student.getId(), courseId, date, period, "PRESENT");
        }
    }

    public AttendanceDAO getDao() {
        return dao;
    }
}
