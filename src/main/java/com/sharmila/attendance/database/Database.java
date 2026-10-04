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
                    student_id TEXT NOT NULL,
                    student_name TEXT NOT NULL,
                    course_id INTEGER NOT NULL,
                    UNIQUE(student_id, course_id),
                    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
                )
                """;

        String attendance = """
                CREATE TABLE IF NOT EXISTS attendance (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    student_id INTEGER NOT NULL,
                    course_id INTEGER NOT NULL,
                    attendance_date TEXT NOT NULL,
                    period INTEGER NOT NULL CHECK(period BETWEEN 1 AND 4),
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
            migrateStudentTableIfNeeded(connection);
            migrateAttendanceTableIfNeeded(connection);
            seedIfEmpty(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize SQLite database.", e);
        }
    }

    private static void migrateAttendanceTableIfNeeded(Connection connection) throws SQLException {
        String definition;
        try (var statement = connection.createStatement();
             var result = statement.executeQuery(
                     "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'attendance'")) {
            if (!result.next()) {
                return;
            }
            definition = result.getString("sql");
        }

        if (definition != null && definition.contains("period BETWEEN 1 AND 4")) {
            return;
        }

        connection.setAutoCommit(false);
        try (var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE attendance RENAME TO attendance_old");
            statement.execute("""
                    CREATE TABLE attendance (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        student_id INTEGER NOT NULL,
                        course_id INTEGER NOT NULL,
                        attendance_date TEXT NOT NULL,
                        period INTEGER NOT NULL CHECK(period BETWEEN 1 AND 4),
                        status TEXT NOT NULL CHECK(status IN ('PRESENT', 'ABSENT')),
                        UNIQUE(student_id, course_id, attendance_date, period),
                        FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
                        FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
                    )
                    """);
            statement.execute("""
                    INSERT INTO attendance(id, student_id, course_id, attendance_date, period, status)
                    SELECT id, student_id, course_id, attendance_date, period, status
                    FROM attendance_old
                    """);
            statement.execute("DROP TABLE attendance_old");
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private static void migrateStudentTableIfNeeded(Connection connection) throws SQLException {
        try (var statement = connection.createStatement();
             var result = statement.executeQuery(
                     "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'students'")) {
            if (!result.next()) {
                return;
            }

            String definition = result.getString("sql");
            if (definition != null && definition.contains("UNIQUE(student_id, course_id)")) {
                return;
            }

            connection.setAutoCommit(false);
            try {
                statement.execute("ALTER TABLE students RENAME TO students_old");
                statement.execute("""
                        CREATE TABLE students (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            student_id TEXT NOT NULL,
                            student_name TEXT NOT NULL,
                            course_id INTEGER NOT NULL,
                            UNIQUE(student_id, course_id),
                            FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
                        )
                        """);
                statement.execute("""
                        INSERT INTO students(id, student_id, student_name, course_id)
                        SELECT id, student_id, student_name, course_id FROM students_old
                        """);
                statement.execute("DROP TABLE students_old");
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private static void seedIfEmpty(Connection connection) throws SQLException {
        try (var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT COUNT(*) FROM courses")) {
            if (result.next() && result.getInt(1) > 0) {
                seedMissingStudents(connection);
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
                    {"Principles of Computer Organization", "Dr. Umair"},
                    {"Compressive Design of Data Structure", "Lord"}
            };

            String[][] seedStudents = {
                    {"4420250902", "WEMEGHA STEPHANIE SEFAK"},
                    {"4420250903", "BIADI MOHAMED"},
                    {"4420250914", "BOUPPHADUVONG ANOUSITH"},
                    {"4420250944", "DURANTO THOMAS GOMES"},
                    {"4420250945", "JOY JOSEPH COSTA"},
                    {"4420250969", "ABBASI MEMOONA ZAINAB"},
                    {"4420250970", "ISLAM MD ASHIKUL"},
                    {"4420250973", "MARYAM NAVEED MIR"},
                    {"4420250980", "BAI LACHMI"},
                    {"4420250981", "JABOUR FATIMA ZAHRA"},
                    {"4420250983", "ARMAAN ARIFUL ISLAM"},
                    {"4420250984", "CHAKMA BIBISHWAR"},
                    {"4420250985", "CHAKMA SHELI"},
                    {"4420250986", "PRCTHOM MD TOFAEL SARKAR"},
                    {"4420250989", "NUPUR TASHNOVA ISLAM"},
                    {"4420250990", "SEEAM SAJIM MAHMUD"},
                    {"4420250995", "BISWAS SHARMILA"},
                    {"4420250996", "QURBONALIZODA AMINJON"}
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
                    insertStudent.setString(1, student[0]);
                    insertStudent.setString(2, student[1]);
                    insertStudent.setInt(3, courseId);
                    insertStudent.executeUpdate();
                }
            }
        }
    }

    private static void seedMissingStudents(Connection connection) throws SQLException {
        String[][] seedCourses = {
                {"Data Structures", "Dr. Umair"},
                {"Probability Theory and Mathematical Statistics", "Justice Odoom, Ph.D"},
                {"Principles of Computer Organization", "Dr. Umair"},
                {"Compressive Design of Data Structure", "Lord"}
        };

        String[][] seedStudents = {
                {"4420250902", "WEMEGHA STEPHANIE SEFAK"},
                {"4420250903", "BIADI MOHAMED"},
                {"4420250914", "BOUPPHADUVONG ANOUSITH"},
                {"4420250944", "DURANTO THOMAS GOMES"},
                {"4420250945", "JOY JOSEPH COSTA"},
                {"4420250969", "ABBASI MEMOONA ZAINAB"},
                {"4420250970", "ISLAM MD ASHIKUL"},
                {"4420250973", "MARYAM NAVEED MIR"},
                {"4420250980", "BAI LACHMI"},
                {"4420250981", "JABOUR FATIMA ZAHRA"},
                {"4420250983", "ARMAAN ARIFUL ISLAM"},
                {"4420250984", "CHAKMA BIBISHWAR"},
                {"4420250985", "CHAKMA SHELI"},
                {"4420250986", "PRCTHOM MD TOFAEL SARKAR"},
                {"4420250989", "NUPUR TASHNOVA ISLAM"},
                {"4420250990", "SEEAM SAJIM MAHMUD"},
                {"4420250995", "BISWAS SHARMILA"},
                {"4420250996", "QURBONALIZODA AMINJON"}
        };

        try (var selectCourses = connection.createStatement();
             var selectStudents = connection.prepareStatement("SELECT id, course_name FROM courses");
             var insertStudent = connection.prepareStatement(
                     "INSERT INTO students(student_id, student_name, course_id) VALUES(?, ?, ?)")) {
            try (var courseRows = selectStudents.executeQuery()) {
                while (courseRows.next()) {
                    int courseId = courseRows.getInt("id");
                    String courseName = courseRows.getString("course_name");
                    boolean exists = false;
                    for (String[] course : seedCourses) {
                        if (course[0].equals(courseName)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        continue;
                    }

                    try (var countStmt = connection.prepareStatement(
                            "SELECT COUNT(*) FROM students WHERE course_id = ?")) {
                        countStmt.setInt(1, courseId);
                        try (var countRs = countStmt.executeQuery()) {
                            if (countRs.next() && countRs.getInt(1) >= seedStudents.length) {
                                continue;
                            }
                        }
                    }

                    try (var deleteStmt = connection.prepareStatement(
                            "DELETE FROM students WHERE course_id = ?")) {
                        deleteStmt.setInt(1, courseId);
                        deleteStmt.executeUpdate();
                    }

                    for (String[] student : seedStudents) {
                        insertStudent.setString(1, student[0]);
                        insertStudent.setString(2, student[1]);
                        insertStudent.setInt(3, courseId);
                        insertStudent.executeUpdate();
                    }
                }
            }
        }
    }
}
