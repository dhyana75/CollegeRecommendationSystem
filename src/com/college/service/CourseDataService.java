package com.college.service;

import com.college.db.DBConnection;
import com.college.model.Course;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** JDBC service for COURSES and COLLEGE_COURSES. Seat counts are not in this schema. */
public class CourseDataService {
    public static List<Course> getCourses() { return queryCourses(null); }
    public static List<Course> getCoursesByCollege(int collegeId) { return queryCourses(collegeId); }

    private static List<Course> queryCourses(Integer collegeId) {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT co.course_id, CAST(cc.institute_code AS UNSIGNED) AS college_id, " +
                "co.course_name, cc.choice_code FROM COURSES co " +
                "JOIN COLLEGE_COURSES cc ON cc.course_id = co.course_id " +
                (collegeId == null ? "" : "WHERE cc.institute_code = ? ") +
                "ORDER BY co.course_name, cc.choice_code";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (collegeId != null) ps.setString(1, String.valueOf(collegeId));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) courses.add(new Course(rs.getInt("course_id"),
                        rs.getInt("college_id"), rs.getString("course_name"), 0, rs.getString("choice_code")));
            }
        } catch (SQLException e) {
            System.err.println("Could not load courses: " + e.getMessage());
        }
        return courses;
    }

    public static boolean addCourse(Course course) {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int courseId = findCourseIdByName(con, course.getCourseName());
                if (courseId == -1) {
                    courseId = course.getCourseId();
                    try (PreparedStatement ps = con.prepareStatement("INSERT INTO COURSES(course_id, course_name) VALUES (?, ?)")) {
                        ps.setInt(1, courseId); ps.setString(2, course.getCourseName().trim()); ps.executeUpdate();
                    }
                }
                String choiceCode = "ADM" + course.getCollegeId() + "-" + courseId;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT IGNORE INTO COLLEGE_COURSES(choice_code, institute_code, course_id) VALUES (?, ?, ?)")) {
                    ps.setString(1, choiceCode); ps.setString(2, String.valueOf(course.getCollegeId())); ps.setInt(3, courseId); ps.executeUpdate();
                }
                con.commit(); return true;
            } catch (SQLException e) {
                con.rollback(); System.err.println("Could not add course: " + e.getMessage()); return false;
            } finally { con.setAutoCommit(true); }
        } catch (SQLException e) { System.err.println("Could not add course: " + e.getMessage()); return false; }
    }

    public static boolean updateCourse(Course course) {
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(
                "UPDATE COURSES SET course_name = ? WHERE course_id = ?")) {
            ps.setString(1, course.getCourseName().trim()); ps.setInt(2, course.getCourseId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("Could not update course: " + e.getMessage()); return false; }
    }

    /** Deletes only the selected college-course association and its cutoff records. */
    public static boolean deleteCourse(int courseId, int collegeId) {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                String choiceCode = null;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT choice_code FROM COLLEGE_COURSES WHERE course_id = ? AND institute_code = ?")) {
                    ps.setInt(1, courseId); ps.setString(2, String.valueOf(collegeId));
                    try (ResultSet rs = ps.executeQuery()) { if (rs.next()) choiceCode = rs.getString(1); }
                }
                if (choiceCode != null) deleteChoiceDependents(con, choiceCode);
                try (PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM COLLEGE_COURSES WHERE course_id = ? AND institute_code = ?")) {
                    ps.setInt(1, courseId); ps.setString(2, String.valueOf(collegeId)); ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM COURSES WHERE course_id = ? AND NOT EXISTS " +
                        "(SELECT 1 FROM COLLEGE_COURSES WHERE course_id = ?)")) {
                    ps.setInt(1, courseId); ps.setInt(2, courseId); ps.executeUpdate();
                }
                con.commit(); return true;
            } catch (SQLException e) { con.rollback(); System.err.println("Could not delete course: " + e.getMessage()); return false; }
            finally { con.setAutoCommit(true); }
        } catch (SQLException e) { System.err.println("Could not delete course: " + e.getMessage()); return false; }
    }

    /** Compatibility method: removes all college associations for the supplied course. */
    public static void deleteCourse(int courseId) {
        List<Course> matches = new ArrayList<>();
        for (Course c : getCourses()) if (c.getCourseId() == courseId) matches.add(c);
        for (Course c : matches) deleteCourse(courseId, c.getCollegeId());
    }

    public static int getNextId() {
        try (Connection con = DBConnection.getConnection(); Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(course_id), 0) + 1 FROM COURSES")) {
            return rs.next() ? rs.getInt(1) : 1;
        } catch (SQLException e) { System.err.println("Could not generate course ID: " + e.getMessage()); return 1; }
    }

    private static int findCourseIdByName(Connection con, String name) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT course_id FROM COURSES WHERE course_name = ?")) {
            ps.setString(1, name.trim()); try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : -1; }
        }
    }

    private static void deleteChoiceDependents(Connection con, String choiceCode) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM RECOMMENDED_COLLEGES WHERE choice_code = ?")) {
            ps.setString(1, choiceCode); ps.executeUpdate();
        }
        try (PreparedStatement ps = con.prepareStatement("DELETE j FROM jee_cutoff j JOIN CUTOFFS c ON c.cutoff_id=j.cutoff_id WHERE c.choice_code = ?")) {
            ps.setString(1, choiceCode); ps.executeUpdate();
        }
        try (PreparedStatement ps = con.prepareStatement("DELETE m FROM mht_cet_cutoff m JOIN CUTOFFS c ON c.cutoff_id=m.cutoff_id WHERE c.choice_code = ?")) {
            ps.setString(1, choiceCode); ps.executeUpdate();
        }
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM CUTOFFS WHERE choice_code = ?")) {
            ps.setString(1, choiceCode); ps.executeUpdate();
        }
    }
}
