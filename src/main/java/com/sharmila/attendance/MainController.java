package com.sharmila.attendance;

import com.sharmila.attendance.controller.AttendanceController;
import com.sharmila.attendance.controller.CourseController;
import com.sharmila.attendance.controller.StudentController;
import com.sharmila.attendance.dao.AttendanceDAO;
import com.sharmila.attendance.dao.CourseDAO;
import com.sharmila.attendance.dao.StudentDAO;
import com.sharmila.attendance.model.Course;
import com.sharmila.attendance.model.Student;
import com.sharmila.attendance.ui.AttendanceGridBuilder;
import com.sharmila.attendance.util.DateUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Window;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MainController {

    // --- UI COMPONENTS ---
    @FXML private ListView<Course> courseList;
    @FXML private Label courseTitle;
    @FXML private Label teacherLabel;
    @FXML private Label statusLabel;

    @FXML private ChoiceBox<Integer> periodChoice;
    @FXML private TableView<Student> attendanceTable;
    @FXML private Button editCourseButton;
    @FXML private Button deleteCourseButton;
    @FXML private Button editStudentButton;
    @FXML private Button deleteStudentButton;

    // --- NEW UI COMPONENTS FOR DYNAMIC WEEKS ---
    @FXML private DatePicker weekStartDatePicker;
    @FXML private Spinner<Integer> weekCountSpinner;
    @FXML private ChoiceBox<Integer> weekChoice;

    private final CourseDAO courseDAO = new CourseDAO();
    private final StudentDAO studentDAO = new StudentDAO();
    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final CourseController courseController = new CourseController();
    private final StudentController studentController = new StudentController();
    private final AttendanceController attendanceController = new AttendanceController();

    private final ObservableList<Course> courses = FXCollections.observableArrayList();
    private final ObservableList<Student> students = FXCollections.observableArrayList();
    private final Map<String, String> statusCache = new HashMap<>();

    @FXML
    private void initialize() {
        periodChoice.setItems(FXCollections.observableArrayList(1, 2, 3, 4));
        periodChoice.setValue(1);

        // --- Setup dynamic weeks ---
        weekStartDatePicker.setValue(DateUtil.mondayOf(LocalDate.now()));
        weekCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 52, 8));
        updateWeekChoices(8);

        weekCountSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateWeekChoices(newVal);
            rebuildAttendanceColumns();
            loadAttendanceGrid();
        });

        weekStartDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                rebuildAttendanceColumns();
                loadAttendanceGrid();
            }
        });
        // ------------------------------------

        courses.setAll(courseDAO.findAll());
        courseList.setItems(courses);
        courseList.getSelectionModel().selectedItemProperty().addListener((obs, oldCourse, newCourse) -> onCourseSelected(newCourse));

        if (!courses.isEmpty()) {
            courseList.getSelectionModel().selectFirst();
        } else {
            onCourseSelected(null);
        }
        setStatus("Ready. Click an attendance cell to cycle: blank → Present → Absent → blank.");
    }

    private void updateWeekChoices(int maxWeeks) {
        ObservableList<Integer> weeks = FXCollections.observableArrayList();
        for (int i = 1; i <= maxWeeks; i++) {
            weeks.add(i);
        }
        weekChoice.setItems(weeks);
        weekChoice.setValue(1);
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
        LocalDate startDate = weekStartDatePicker.getValue();
        Integer week = weekChoice.getValue();
        Integer period = periodChoice.getValue();

        if (course == null || startDate == null || week == null || period == null) return;
        Window window = courseList.getScene().getWindow();

        LocalDate targetDate = startDate.plusWeeks(week - 1);

        attendanceController.markAllPresent(window, students, course.getId(), targetDate, period);
        loadAttendanceGrid();
        setStatus("Attendance marked for Week " + week + ", Period " + period + ".");
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
        LocalDate weekStart = weekStartDatePicker.getValue();
        int weeks = weekCountSpinner.getValue();

        new AttendanceGridBuilder(
                attendanceTable,
                course,
                weekStart,
                weeks,
                statusCache,
                student -> course == null ? 0 : attendanceDAO.calculatePercentage(student.getId(), course.getId()),
                (Student student, LocalDate date, int period) -> {
                    String status = attendanceController.toggle(student.getId(), course.getId(), date, period);
                    statusCache.put(AttendanceDAO.key(student.getId(), date, period), status);
                    setStatus("Attendance updated for " + student.getStudentName() + ".");
                }
        ).build();
    }

    private void loadAttendanceGrid() {
        Course course = selectedCourse();
        if (course == null || weekStartDatePicker.getValue() == null) {
            statusCache.clear();
            attendanceTable.refresh();
            return;
        }

        LocalDate start = weekStartDatePicker.getValue();
        LocalDate end = start.plusWeeks(weekCountSpinner.getValue()).minusDays(1);

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