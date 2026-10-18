package com.college.model;

public class Course {
    private int courseId;
    private int collegeId;
    private String courseName;
    private int totalSeats;
    private String choiceCode;

    public Course(int courseId, int collegeId, String courseName, int totalSeats) {
        this(courseId, collegeId, courseName, totalSeats, "");
    }

    public Course(int courseId, int collegeId, String courseName, int totalSeats, String choiceCode) {
        this.courseId = courseId;
        this.collegeId = collegeId;
        this.courseName = courseName;
        this.totalSeats = totalSeats;
        this.choiceCode = choiceCode == null ? "" : choiceCode;
    }

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }
    public int getCollegeId() { return collegeId; }
    public void setCollegeId(int collegeId) { this.collegeId = collegeId; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }
    public String getChoiceCode() { return choiceCode; }
    public void setChoiceCode(String choiceCode) { this.choiceCode = choiceCode == null ? "" : choiceCode; }

    @Override public String toString() { return courseName; }
}
