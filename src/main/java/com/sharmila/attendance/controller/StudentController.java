package com.sharmila.attendance.controller;

import com.sharmila.attendance.model.Student;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.util.Optional;

public class StudentController {
    public Optional<Student> showStudentDialog(Window owner, Student existing, int courseId) {
        Dialog<Student> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Student" : "Edit Student");
        dialog.setHeaderText(existing == null ? "Add a student to the selected class" : "Edit student details");
        dialog.initOwner(owner);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField idField = new TextField(existing == null ? "" : existing.getStudentId());
        TextField nameField = new TextField(existing == null ? "" : existing.getStudentName());
        idField.setPromptText("Student ID");
        nameField.setPromptText("Student name");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.add(new Label("Student ID:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("Student name:"), 0, 1);
        grid.add(nameField, 1, 1);
        dialog.getDialogPane().setContent(grid);

        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(idField.getText().isBlank() || nameField.getText().isBlank());
        Runnable validate = () -> okButton.setDisable(idField.getText().isBlank() || nameField.getText().isBlank());
        idField.textProperty().addListener((obs, old, value) -> validate.run());
        nameField.textProperty().addListener((obs, old, value) -> validate.run());

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                if (existing == null) {
                    return new Student(idField.getText().trim(), nameField.getText().trim(), courseId);
                }
                existing.setStudentId(idField.getText().trim());
                existing.setStudentName(nameField.getText().trim());
                return existing;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    public boolean confirmDelete(Window owner, Student student) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete " + student.getStudentName() + " (" + student.getStudentId() + ")?",
                ButtonType.CANCEL, ButtonType.OK);
        alert.setTitle("Delete Student");
        alert.setHeaderText("Delete student");
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
