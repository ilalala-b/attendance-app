package com.sharmila.attendance.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
    private static final String URL = "jdbc:sqlite:attendance.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(URL);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public static void initialize() {
        String courses = """
                CREATE TABLE IF NOT EXISTS courses (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    course_name TEXT NOT NULL,
                    teacher_name TEXT NOT NULL
                )
                """;

        String students = """
                CREATE TABLE IF NOT EXISTS students (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    student_id TEXT NOT NULL UNIQUE,
                    student_name TEXT NOT NULL,
                    course_id INTEGER NOT NULL,
                    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
                )
                """;

        String attendance = """
                CREATE TABLE IF NOT EXISTS attendance (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    student_id INTEGER NOT NULL,
                    course_id INTEGER NOT NULL,
                    attendance_date TEXT NOT NULL,
                    period INTEGER NOT NULL CHECK(period BETWEEN 1 AND 3),
                    status TEXT NOT NULL CHECK(status IN ('PRESENT', 'ABSENT')),
                    UNIQUE(student_id, course_id, attendance_date, period),
                    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
                    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
                )
                """;

        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(courses);
            statement.execute(students);
            statement.execute(attendance);
            seedIfEmpty(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize SQLite database.", e);
        }
    }

    private static void seedIfEmpty(Connection connection) throws SQLException {
        try (var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT COUNT(*) FROM courses")) {
            if (result.next() && result.getInt(1) > 0) {
                return;
            }
        }

        try (var insertCourse = connection.prepareStatement(
                "INSERT INTO courses(course_name, teacher_name) VALUES(?, ?)",
                Statement.RETURN_GENERATED_KEYS);
             var insertStudent = connection.prepareStatement(
                "INSERT INTO students(student_id, student_name, course_id) VALUES(?, ?, ?)")) {

            String[][] seedCourses = {
                    {"Data Structures", "Dr. Umair"},
                    {"Probability Theory and Mathematical Statistics", "Justice Odoom, Ph.D"},
                    {"Principles of Computer Organization", "Dr. Umair"}
            };

            String[][] seedStudents = {
                    {"S001", "Amina Rahman"},
                    {"S002", "Nusrat Jahan"},
                    {"S003", "Mim Akter"},
                    {"S004", "Sadia Islam"},
                    {"S005", "Tanvir Hasan"}
            };

            for (String[] course : seedCourses) {
                insertCourse.setString(1, course[0]);
                insertCourse.setString(2, course[1]);
                insertCourse.executeUpdate();

                int courseId;
                try (var keys = insertCourse.getGeneratedKeys()) {
                    keys.next();
                    courseId = keys.getInt(1);
                }

                for (String[] student : seedStudents) {
                    insertStudent.setString(1, student[0] + "-" + courseId);
                    insertStudent.setString(2, student[1]);
                    insertStudent.setInt(3, courseId);
                    insertStudent.executeUpdate();
                }
            }
        }
    }
}
