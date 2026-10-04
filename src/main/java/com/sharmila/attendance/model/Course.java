package com.sharmila.attendance.model;

public class Course {
    private int id;
    private String courseName;
    private String teacherName;

    public Course(int id, String courseName, String teacherName) {
        this.id = id;
        this.courseName = courseName;
        this.teacherName = teacherName;
    }

    public Course(String courseName, String teacherName) {
        this(0, courseName, teacherName);
    }

    public int getId() {
        return id;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    @Override
    public String toString() {
        return courseName;
    }
}
