package com.sharmila.attendance.dao;

import com.sharmila.attendance.database.Database;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class AttendanceDAO {
    public String getStatus(int studentId, int courseId, LocalDate date, int period) {
        String sql = "SELECT status FROM attendance WHERE student_id = ? AND course_id = ? AND attendance_date = ? AND period = ?";
        try (var connection = Database.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            statement.setInt(2, courseId);
            statement.setString(3, date.toString());
            statement.setInt(4, period);
            try (var result = statement.executeQuery()) {
                return result.next() ? result.getString("status") : "";
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read attendance status.", e);
        }
    }

    public void setStatus(int studentId, int courseId, LocalDate date, int period, String status) {
        if (status == null || status.isBlank()) {
            deleteStatus(studentId, courseId, date, period);
            return;
        }

        String sql = """
                INSERT INTO attendance(student_id, course_id, attendance_date, period, status)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(student_id, course_id, attendance_date, period)
                DO UPDATE SET status = excluded.status
                """;

        try (var connection = Database.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            statement.setInt(2, courseId);
            statement.setString(3, date.toString());
            statement.setInt(4, period);
            statement.setString(5, status);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save attendance.", e);
        }
    }

    public void deleteStatus(int studentId, int courseId, LocalDate date, int period) {
        try (var connection = Database.getConnection();
             var statement = connection.prepareStatement(
                     "DELETE FROM attendance WHERE student_id = ? AND course_id = ? AND attendance_date = ? AND period = ?")) {
            statement.setInt(1, studentId);
            statement.setInt(2, courseId);
            statement.setString(3, date.toString());
            statement.setInt(4, period);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not clear attendance.", e);
        }
    }

    public Map<String, String> loadStatuses(int courseId, LocalDate startDate, LocalDate endDate) {
        Map<String, String> map = new HashMap<>();
        String sql = """
                SELECT student_id, attendance_date, period, status
                FROM attendance
                WHERE course_id = ? AND attendance_date BETWEEN ? AND ?
                """;

        try (var connection = Database.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, courseId);
            statement.setString(2, startDate.toString());
            statement.setString(3, endDate.toString());
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    String key = key(
                            result.getInt("student_id"),
                            LocalDate.parse(result.getString("attendance_date")),
                            result.getInt("period")
                    );
                    map.put(key, result.getString("status"));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load attendance grid.", e);
        }
        return map;
    }

    public double calculatePercentage(int studentId, int courseId) {
        String sql = """
                SELECT COUNT(*) AS total,
                       COALESCE(SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END), 0) AS present
                FROM attendance
                WHERE student_id = ? AND course_id = ?
                """;

        try (var connection = Database.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            statement.setInt(2, courseId);
            try (var result = statement.executeQuery()) {
                if (result.next()) {
                    int total = result.getInt("total");
                    int present = result.getInt("present");
                    return total == 0 ? 0 : (present * 100.0) / total;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not calculate attendance percentage.", e);
        }
        return 0;
    }

    public static String key(int studentId, LocalDate date, int period) {
        return studentId + "|" + date + "|" + period;
    }
}
