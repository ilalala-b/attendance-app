package com.sharmila.attendance.controller;

import com.sharmila.attendance.model.Course;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.util.Optional;

public class CourseController {
    public Optional<Course> showCourseDialog(Window owner, Course existing) {
        Dialog<Course> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Class" : "Edit Class");
        dialog.setHeaderText(existing == null ? "Create a new class/course" : "Edit class/course details");
        dialog.initOwner(owner);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField courseField = new TextField(existing == null ? "" : existing.getCourseName());
        TextField teacherField = new TextField(existing == null ? "" : existing.getTeacherName());
        courseField.setPromptText("e.g. Data Structures");
        teacherField.setPromptText("e.g. Dr. Umair");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.add(new Label("Class name:"), 0, 0);
        grid.add(courseField, 1, 0);
        grid.add(new Label("Teacher:"), 0, 1);
        grid.add(teacherField, 1, 1);
        dialog.getDialogPane().setContent(grid);

        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(courseField.getText().isBlank() || teacherField.getText().isBlank());
        Runnable validate = () -> okButton.setDisable(courseField.getText().isBlank() || teacherField.getText().isBlank());
        courseField.textProperty().addListener((obs, old, value) -> validate.run());
        teacherField.textProperty().addListener((obs, old, value) -> validate.run());

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                if (existing == null) {
                    return new Course(courseField.getText().trim(), teacherField.getText().trim());
                }
                existing.setCourseName(courseField.getText().trim());
                existing.setTeacherName(teacherField.getText().trim());
                return existing;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    public boolean confirmDelete(Window owner, Course course) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, 
                "Delete '" + course.getCourseName() + "'? All students and attendance in this class will also be deleted.",
                ButtonType.CANCEL, ButtonType.OK);
        alert.setTitle("Delete Class");
        alert.setHeaderText("Delete class");
        alert.initOwner(owner);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    public void showError(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Error");
        alert.setHeaderText("Could not complete the operation");
        alert.initOwner(owner);
        alert.showAndWait();
    }
}
