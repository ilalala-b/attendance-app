package com.sharmila.attendance.model;

public class Student {
    private int id;
    private String studentId;
    private String studentName;
    private int courseId;

    public Student(int id, String studentId, String studentName, int courseId) {
        this.id = id;
        this.studentId = studentId;
        this.studentName = studentName;
        this.courseId = courseId;
    }

    public Student(String studentId, String studentName, int courseId) {
        this(0, studentId, studentName, courseId);
    }

    public int getId() {
        return id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public int getCourseId() {
        return courseId;
    }
}
