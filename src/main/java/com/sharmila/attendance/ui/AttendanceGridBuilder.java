package com.sharmila.attendance.ui;

import com.sharmila.attendance.dao.AttendanceDAO;
import com.sharmila.attendance.model.Course;
import com.sharmila.attendance.model.Student;
import com.sharmila.attendance.util.AttendanceCalculator;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;

import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;

public final class AttendanceGridBuilder {

    public static final int PERIODS_PER_WEEK = 4;

    private final TableView<Student> table;
    private final Course course;
    private final LocalDate weekStart;
    private final int weekCount; // NEW: Dynamic week count
    private final Map<String, String> statusCache;
    private final Function<Student, Double> percentageProvider;
    private final AttendanceCellListener cellListener;

    public AttendanceGridBuilder(TableView<Student> table,
                                 Course course,
                                 LocalDate weekStart,
                                 int weekCount, // NEW: Added to constructor
                                 Map<String, String> statusCache,
                                 Function<Student, Double> percentageProvider,
                                 AttendanceCellListener cellListener) {
        this.table = table;
        this.course = course;
        this.weekStart = weekStart;
        this.weekCount = weekCount; // NEW: Assign value
        this.statusCache = statusCache;
        this.percentageProvider = percentageProvider;
        this.cellListener = cellListener;
    }

    public void build() {
        table.getColumns().clear();
        table.getColumns().add(createIdColumn());
        table.getColumns().add(createNameColumn());
        table.getColumns().add(createPercentageColumn());

        if (weekStart == null) {
            return;
        }

        // NEW: Loop uses weekCount variable instead of WEEK_COUNT constant
        for (int week = 0; week < weekCount; week++) {
            LocalDate weekDate = weekStart.plusDays(week * 7L);

            TableColumn<Student, String> weekColumn = new TableColumn<>("W" + (week + 1));
            weekColumn.setPrefWidth(PERIODS_PER_WEEK * 62);
            weekColumn.setMinWidth(PERIODS_PER_WEEK * 54);

            for (int period = 1; period <= PERIODS_PER_WEEK; period++) {
                weekColumn.getColumns().add(createPeriodColumn(weekDate, period));
            }
            table.getColumns().add(weekColumn);
        }
    }

    private TableColumn<Student, String> createIdColumn() {
        TableColumn<Student, String> column = new TableColumn<>("Student ID");
        column.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getStudentId()));
        column.setPrefWidth(120);
        return column;
    }

    private TableColumn<Student, String> createNameColumn() {
        TableColumn<Student, String> column = new TableColumn<>("Student Name");
        column.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getStudentName()));
        column.setPrefWidth(190);
        return column;
    }

    private TableColumn<Student, String> createPercentageColumn() {
        TableColumn<Student, String> column = new TableColumn<>("Attendance %");
        column.setCellValueFactory(d -> {
            double percentage = percentageProvider.apply(d.getValue());
            return new ReadOnlyStringWrapper(AttendanceCalculator.formatPercentage(percentage));
        });
        column.setPrefWidth(100);
        return column;
    }

    private TableColumn<Student, String> createPeriodColumn(LocalDate date, int period) {
        TableColumn<Student, String> column = new TableColumn<>("P" + period);
        column.setPrefWidth(62);
        column.setMinWidth(54);
        column.setCellValueFactory(d -> new ReadOnlyStringWrapper(""));
        column.setCellFactory(tc -> createAttendanceCell(date, period));
        return column;
    }

    private TableCell<Student, String> createAttendanceCell(LocalDate date, int period) {
        return new TableCell<>() {
            private final Button button = new Button();

            {
                button.getStyleClass().add("attendance-button");
                button.setMaxWidth(Double.MAX_VALUE);
                button.setPrefHeight(30);
                button.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                    Student student = getTableView().getItems().get(getIndex());
                    if (course == null || student == null) return;
                    cellListener.onToggle(student, date, period);
                    getTableView().refresh();
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
                String status = statusCache.getOrDefault(
                        AttendanceDAO.key(student.getId(), date, period), "");

                button.setText(status.equals("PRESENT") ? "✓"
                        : status.equals("ABSENT") ? "✕" : "—");
                button.getStyleClass().removeAll("present", "absent", "empty");
                button.getStyleClass().add(
                        status.equals("PRESENT") ? "present"
                                : status.equals("ABSENT") ? "absent" : "empty");
                setGraphic(button);
            }
        };
    }
}