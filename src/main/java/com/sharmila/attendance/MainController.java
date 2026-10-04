package com.sharmila.attendance;

import com.sharmila.attendance.controller.AttendanceController;
import com.sharmila.attendance.controller.CourseController;
import com.sharmila.attendance.controller.StudentController;
import com.sharmila.attendance.dao.AttendanceDAO;
import com.sharmila.attendance.dao.CourseDAO;
import com.sharmila.attendance.dao.StudentDAO;
import com.sharmila.attendance.model.Course;
import com.sharmila.attendance.model.Student;
import com.sharmila.attendance.util.AttendanceCalculator;
import com.sharmila.attendance.util.DateUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MainController {
    @FXML private ListView<Course> courseList;
    @FXML private Label courseTitle;
    @FXML private Label teacherLabel;
    @FXML private Label statusLabel;
    @FXML private DatePicker sessionDatePicker;
    @FXML private ChoiceBox<Integer> periodChoice;
    @FXML private TableView<Student> attendanceTable;
    @FXML private Button editCourseButton;
    @FXML private Button deleteCourseButton;
    @FXML private Button editStudentButton;
    @FXML private Button deleteStudentButton;

    private final CourseDAO courseDAO = new CourseDAO();
    private final StudentDAO studentDAO = new StudentDAO();
    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final CourseController courseController = new CourseController();
    private final StudentController studentController = new StudentController();
    private final AttendanceController attendanceController = new AttendanceController();

    private final ObservableList<Course> courses = FXCollections.observableArrayList();
    private final ObservableList<Student> students = FXCollections.observableArrayList();
    private final Map<String, String> statusCache = new HashMap<>();
    private final DateTimeFormatter headerFormatter = DateTimeFormatter.ofPattern("dd MMM");

    @FXML
    private void initialize() {
        periodChoice.setItems(FXCollections.observableArrayList(1, 2, 3));
        periodChoice.setValue(1);
        sessionDatePicker.setValue(LocalDate.now());

        courses.setAll(courseDAO.findAll());
        courseList.setItems(courses);
        courseList.getSelectionModel().selectedItemProperty().addListener((obs, oldCourse, newCourse) -> onCourseSelected(newCourse));
        sessionDatePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null) {
                rebuildAttendanceColumns();
                loadAttendanceGrid();
            }
        });

        if (!courses.isEmpty()) {
            courseList.getSelectionModel().selectFirst();
        } else {
            onCourseSelected(null);
        }
        setStatus("Ready. Click an attendance cell to cycle: blank → Present → Absent → blank.");
    }

    @FXML
    private void addCourse() {
        Window window = courseList.getScene().getWindow();
        try {
            Optional<Course> result = courseController.showCourseDialog(window, null);
            result.ifPresent(course -> {
                int id = courseDAO.insert(course);
                courseList.getItems().setAll(courseDAO.findAll());
                courseList.getItems().stream().filter(c -> c.getId() == id).findFirst().ifPresent(c -> courseList.getSelectionModel().select(c));
                setStatus("Class added successfully.");
            });
        } catch (RuntimeException ex) {
            courseController.showError(window, ex.getMessage());
        }
    }

    @FXML
    private void editCourse() {
        Course course = courseList.getSelectionModel().getSelectedItem();
        if (course == null) return;
        Window window = courseList.getScene().getWindow();
        try {
            courseController.showCourseDialog(window, course).ifPresent(updated -> {
                courseDAO.update(updated);
                courseList.refresh();
                courseTitle.setText(updated.getCourseName());
                teacherLabel.setText(updated.getTeacherName());
                setStatus("Class updated successfully.");
            });
        } catch (RuntimeException ex) {
            courseController.showError(window, ex.getMessage());
        }
    }

    @FXML
    private void deleteCourse() {
        Course course = courseList.getSelectionModel().getSelectedItem();
        if (course == null) return;
        Window window = courseList.getScene().getWindow();
        if (courseController.confirmDelete(window, course)) {
            try {
                courseDAO.delete(course.getId());
                courses.setAll(courseDAO.findAll());
                if (!courses.isEmpty()) {
                    courseList.getSelectionModel().selectFirst();
                } else {
                    onCourseSelected(null);
                }
                setStatus("Class deleted successfully.");
            } catch (RuntimeException ex) {
                courseController.showError(window, ex.getMessage());
            }
        }
    }

    @FXML
    private void addStudent() {
        Course course = selectedCourse();
        if (course == null) return;
        Window window = courseList.getScene().getWindow();
        try {
            studentController.showStudentDialog(window, null, course.getId()).ifPresent(student -> {
                studentDAO.insert(student);
                loadStudents();
                setStatus("Student added successfully.");
            });
        } catch (RuntimeException ex) {
            studentController.showError(window, ex.getMessage());
        }
    }

    @FXML
    private void editStudent() {
        Course course = selectedCourse();
        Student student = attendanceTable.getSelectionModel().getSelectedItem();
        if (course == null || student == null) return;
        Window window = courseList.getScene().getWindow();
        try {
            studentController.showStudentDialog(window, student, course.getId()).ifPresent(updated -> {
                studentDAO.update(updated);
                loadStudents();
                setStatus("Student updated successfully.");
            });
        } catch (RuntimeException ex) {
            studentController.showError(window, ex.getMessage());
        }
    }

    @FXML
    private void deleteStudent() {
        Student student = attendanceTable.getSelectionModel().getSelectedItem();
        if (student == null) return;
        Window window = courseList.getScene().getWindow();
        if (studentController.confirmDelete(window, student)) {
            try {
                studentDAO.delete(student.getId());
                loadStudents();
                setStatus("Student deleted successfully.");
            } catch (RuntimeException ex) {
                studentController.showError(window, ex.getMessage());
            }
        }
    }

    @FXML
    private void markAllPresent() {
        Course course = selectedCourse();
        LocalDate date = sessionDatePicker.getValue();
        Integer period = periodChoice.getValue();
        if (course == null || date == null || period == null) return;
        Window window = courseList.getScene().getWindow();
        attendanceController.markAllPresent(window, students, course.getId(), date, period);
        loadAttendanceGrid();
        setStatus("Attendance updated.");
    }

    @FXML
    private void refresh() {
        Course current = selectedCourse();
        int currentId = current == null ? -1 : current.getId();
        courses.setAll(courseDAO.findAll());
        if (currentId != -1) {
            courses.stream().filter(c -> c.getId() == currentId).findFirst()
                    .ifPresentOrElse(
                            c -> courseList.getSelectionModel().select(c),
                            () -> { if (!courses.isEmpty()) courseList.getSelectionModel().selectFirst(); }
                    );
        } else if (!courses.isEmpty()) {
            courseList.getSelectionModel().selectFirst();
        } else {
            onCourseSelected(null);
        }
        loadStudents();
        rebuildAttendanceColumns();
        loadAttendanceGrid();
        setStatus("Data refreshed.");
    }

    private void onCourseSelected(Course course) {
        boolean enabled = course != null;
        editCourseButton.setDisable(!enabled);
        deleteCourseButton.setDisable(!enabled);
        editStudentButton.setDisable(!enabled);
        deleteStudentButton.setDisable(!enabled);

        if (!enabled) {
            courseTitle.setText("Select a class");
            teacherLabel.setText("No class selected");
            students.clear();
            attendanceTable.getColumns().clear();
            return;
        }

        courseTitle.setText(course.getCourseName());
        teacherLabel.setText("Teacher: " + course.getTeacherName());
        loadStudents();
        rebuildAttendanceColumns();
        loadAttendanceGrid();
    }

    private void loadStudents() {
        Course course = selectedCourse();
        if (course == null) {
            students.clear();
            return;
        }
        students.setAll(studentDAO.findByCourseId(course.getId()));
        attendanceTable.setItems(students);
    }

    private void rebuildAttendanceColumns() {
        Course course = selectedCourse();
        attendanceTable.getColumns().clear();

        TableColumn<Student, String> idColumn = new TableColumn<>("Student ID");
        idColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getStudentId()));
        idColumn.setPrefWidth(120);

        TableColumn<Student, String> nameColumn = new TableColumn<>("Student Name");
        nameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getStudentName()));
        nameColumn.setPrefWidth(190);

        TableColumn<Student, String> percentageColumn = new TableColumn<>("Attendance %");
        percentageColumn.setCellValueFactory(data -> {
            double percentage = course == null ? 0 : attendanceDAO.calculatePercentage(data.getValue().getId(), course.getId());
            return new ReadOnlyStringWrapper(AttendanceCalculator.formatPercentage(percentage));
        });
        percentageColumn.setPrefWidth(100);

        attendanceTable.getColumns().addAll(idColumn, nameColumn, percentageColumn);

        if (sessionDatePicker.getValue() == null) return;
        LocalDate start = DateUtil.mondayOf(sessionDatePicker.getValue());

        for (int week = 0; week < 4; week++) {
            for (int period = 1; period <= 3; period++) {
                final LocalDate cellDate = start.plusDays(week * 7L);
                final int cellPeriod = period;
                final Course cellCourse = course;
                String title = "W" + (week + 1) + " P" + period + "\n" + headerFormatter.format(cellDate);
                TableColumn<Student, String> column = new TableColumn<>(title);
                column.setPrefWidth(82);
                column.setMinWidth(82);
                column.setCellValueFactory(data -> new ReadOnlyStringWrapper(""));
                column.setCellFactory(tc -> createAttendanceCell(cellDate, cellPeriod, cellCourse));
                attendanceTable.getColumns().add(column);
            }
        }
    }

    private TableCell<Student, String> createAttendanceCell(LocalDate date, int period, Course course) {
        return new TableCell<>() {
            private final Button button = new Button();

            {
                button.getStyleClass().add("attendance-button");
                button.setMaxWidth(Double.MAX_VALUE);
                button.setPrefHeight(30);
                button.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                    Student student = getTableView().getItems().get(getIndex());
                    if (course == null || student == null) return;
                    String status = attendanceController.toggle(student.getId(), course.getId(), date, period);
                    statusCache.put(AttendanceDAO.key(student.getId(), date, period), status);
                    getTableView().refresh();
                    setStatus("Attendance updated for " + student.getStudentName() + ".");
                    event.consume();
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                Student student = getTableView().getItems().get(getIndex());
                String status = statusCache.getOrDefault(AttendanceDAO.key(student.getId(), date, period), "");
                button.setText(status.equals("PRESENT") ? "✓" : status.equals("ABSENT") ? "✕" : "—");
                button.getStyleClass().removeAll("present", "absent", "empty");
                button.getStyleClass().add(status.equals("PRESENT") ? "present" : status.equals("ABSENT") ? "absent" : "empty");
                setGraphic(button);
            }
        };
    }

    private void loadAttendanceGrid() {
        Course course = selectedCourse();
        if (course == null || sessionDatePicker.getValue() == null) {
            statusCache.clear();
            attendanceTable.refresh();
            return;
        }
        LocalDate start = DateUtil.mondayOf(sessionDatePicker.getValue());
        LocalDate end = start.plusDays(27);
        statusCache.clear();
        statusCache.putAll(attendanceDAO.loadStatuses(course.getId(), start, end));
        attendanceTable.refresh();
    }

    private Course selectedCourse() {
        return courseList.getSelectionModel().getSelectedItem();
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }
}
