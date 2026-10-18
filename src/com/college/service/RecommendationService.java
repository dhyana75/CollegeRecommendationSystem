package com.college.service;

import com.college.db.DBConnection;
import com.college.model.College;
import com.college.model.Course;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Database-backed percentile recommendations using the final collegedb schema. */
public class RecommendationService {
    public RecommendationService() { }

    public List<String> recommendColleges(double percentile, String category) {
        return recommendColleges(percentile, category, "", "All Branches");
    }

    public List<String> recommendColleges(double percentile, String category, String preferredCity, String preferredBranch) {
        List<String> recommendations = new ArrayList<>();
        String city = preferredCity == null || preferredCity.trim().isEmpty() ? null : preferredCity.trim();
        String branch = preferredBranch == null || preferredBranch.equalsIgnoreCase("All Branches") || preferredBranch.trim().isEmpty()
                ? null : preferredBranch.trim();
        String sql = "SELECT cc.choice_code, CAST(c.institute_code AS UNSIGNED) AS college_id, " +
                "c.institute_name, ci.city_name, co.course_name, MAX(cu.percentile) AS cutoff_percentile, " +
                "MIN(cu.percentile) AS lowest_cutoff " +
                "FROM CUTOFFS cu JOIN COLLEGE_COURSES cc ON cc.choice_code = cu.choice_code " +
                "JOIN COLLEGES c ON c.institute_code = cc.institute_code " +
                "JOIN CITIES ci ON ci.city_id = c.city_id JOIN COURSES co ON co.course_id = cc.course_id " +
                "JOIN jee_cutoff jc ON jc.cutoff_id = cu.cutoff_id " +
                "WHERE cu.category_code = ? AND cu.year = (SELECT MAX(year) FROM CUTOFFS) " +
                "AND (? IS NULL OR LOWER(ci.city_name) LIKE LOWER(CONCAT('%', ?, '%'))) " +
                "AND (? IS NULL OR LOWER(co.course_name) LIKE LOWER(CONCAT('%', ?, '%'))) " +
                "GROUP BY cc.choice_code, c.institute_code, c.institute_name, ci.city_name, co.course_name " +
                "HAVING MAX(cu.percentile) <= ? ORDER BY MAX(cu.percentile) DESC LIMIT 3";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, category);
            ps.setString(2, city); ps.setString(3, city);
            ps.setString(4, branch); ps.setString(5, branch);
            ps.setDouble(6, percentile);
            List<RecommendationRow> rows = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RecommendationRow row = new RecommendationRow();
                    row.choiceCode = rs.getString("choice_code");
                    row.collegeId = rs.getInt("college_id");
                    row.collegeName = rs.getString("institute_name");
                    row.city = rs.getString("city_name");
                    row.course = rs.getString("course_name");
                    row.lowest = rs.getDouble("lowest_cutoff");
                    row.highest = rs.getDouble("cutoff_percentile");
                    rows.add(row);
                    recommendations.add(row.collegeName + " | " + row.city + " | " + row.course + " | Cutoff: " + row.highest);
                }
            }
            saveRecommendationSession(con, percentile, category, city, branch, rows);
        } catch (SQLException e) {
            System.err.println("Could not generate recommendations: " + e.getMessage());
        }
        return recommendations;
    }

    public List<College> getColleges() { return CollegeDataService.getColleges(); }
    public List<Course> getCourses() { return CourseDataService.getCourses(); }

    private void saveRecommendationSession(Connection con, double percentile, String category, String city,
                                           String branch, List<RecommendationRow> rows) throws SQLException {
        if (rows.isEmpty()) return;
        boolean autoCommit = con.getAutoCommit();
        con.setAutoCommit(false);
        try {
            int year;
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT MAX(year) FROM CUTOFFS")) {
                year = rs.next() ? rs.getInt(1) : 0;
            }
            long sessionId;
            String insertSession = "INSERT INTO RECOMMENDATION_SESSIONS(student_percentile, preferred_city, preferred_branch, " +
                    "category_code, search_metric, cutoff_year) VALUES (?, ?, ?, ?, 'percentile', ?)";
            try (PreparedStatement ps = con.prepareStatement(insertSession, Statement.RETURN_GENERATED_KEYS)) {
                ps.setDouble(1, percentile); ps.setString(2, city); ps.setString(3, branch);
                ps.setString(4, category); ps.setInt(5, year); ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) { if (!keys.next()) throw new SQLException("Could not save recommendation session."); sessionId = keys.getLong(1); }
            }
            String insertRecommendation = "INSERT INTO RECOMMENDED_COLLEGES(session_id, choice_code, institute_name, course_name, city_name, category_code, lowest_cutoff, highest_cutoff) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(insertRecommendation)) {
                for (RecommendationRow row : rows) {
                    ps.setLong(1, sessionId); ps.setString(2, row.choiceCode); ps.setString(3, row.collegeName);
                    ps.setString(4, row.course); ps.setString(5, row.city); ps.setString(6, category);
                    ps.setDouble(7, row.lowest); ps.setDouble(8, row.highest); ps.addBatch();
                }
                ps.executeBatch();
            }
            con.commit();
        } catch (SQLException e) { con.rollback(); throw e; }
        finally { con.setAutoCommit(autoCommit); }
    }

    private static class RecommendationRow {
        String choiceCode, collegeName, city, course;
        int collegeId;
        double lowest, highest;
    }
}
