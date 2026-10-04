package com.sharmila.attendance.dao;

import com.sharmila.attendance.database.Database;
import com.sharmila.attendance.model.Course;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CourseDAO {
    public List<Course> findAll() {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT id, course_name, teacher_name FROM courses ORDER BY course_name";

        try (var connection = Database.getConnection();
             var statement = connection.prepareStatement(sql);
             var result = statement.executeQuery()) {
            while (result.next()) {
                courses.add(new Course(
                        result.getInt("id"),
                        result.getString("course_name"),
                        result.getString("teacher_name")
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load courses.", e);
        }
        return courses;
    }

    public int insert(Course course) {
        String sql = "INSERT INTO courses(course_name, teacher_name) VALUES(?, ?)";
        try (var connection = Database.getConnection();
             var statement = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, course.getCourseName());
            statement.setString(2, course.getTeacherName());
            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not add course.", e);
        }
    }

    public void update(Course course) {
        String sql = "UPDATE courses SET course_name = ?, teacher_name = ? WHERE id = ?";
        try (var connection = Database.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, course.getCourseName());
            statement.setString(2, course.getTeacherName());
            statement.setInt(3, course.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not update course.", e);
        }
    }

    public void delete(int courseId) {
        try (var connection = Database.getConnection();
             var statement = connection.prepareStatement("DELETE FROM courses WHERE id = ?")) {
            statement.setInt(1, courseId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete course.", e);
        }
    }
}
