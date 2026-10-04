package com.sharmila.attendance.dao;

import com.sharmila.attendance.database.Database;
import com.sharmila.attendance.model.Student;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO {
    public List<Student> findByCourseId(int courseId) {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT id, student_id, student_name, course_id FROM students WHERE course_id = ? ORDER BY student_name";

        try (var connection = Database.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, courseId);
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    students.add(new Student(
                            result.getInt("id"),
                            result.getString("student_id"),
                            result.getString("student_name"),
                            result.getInt("course_id")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load students.", e);
        }
        return students;
    }

    public void insert(Student student) {
        String sql = "INSERT INTO students(student_id, student_name, course_id) VALUES(?, ?, ?)";
        try (var connection = Database.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, student.getStudentId());
            statement.setString(2, student.getStudentName());
            statement.setInt(3, student.getCourseId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not add student. Student ID may already exist.", e);
        }
    }

    public void update(Student student) {
        String sql = "UPDATE students SET student_id = ?, student_name = ? WHERE id = ?";
        try (var connection = Database.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, student.getStudentId());
            statement.setString(2, student.getStudentName());
            statement.setInt(3, student.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not update student. Student ID may already exist.", e);
        }
    }

    public void delete(int studentId) {
        try (var connection = Database.getConnection();
             var statement = connection.prepareStatement("DELETE FROM students WHERE id = ?")) {
            statement.setInt(1, studentId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete student.", e);
        }
    }
}
