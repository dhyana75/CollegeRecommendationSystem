package com.college.service;

import com.college.db.DBConnection;
import com.college.model.Cutoff;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** JDBC service for CUTOFFS and its JEE/MHT-CET subtype tables. */
public class CutoffDataService {
    private static final String CUTOFF_SELECT =
            "SELECT c.cutoff_id, c.course_id, c.category_code, c.percentile, c.choice_code, " +
            "c.year, c.cap_round, COALESCE(c.merit_rank, -1) AS merit_rank, " +
            "CASE WHEN j.cutoff_id IS NOT NULL THEN 'JEE Main' " +
            "WHEN m.cutoff_id IS NOT NULL THEN 'MHT-CET' ELSE 'Unknown' END AS exam_name " +
            "FROM CUTOFFS c LEFT JOIN jee_cutoff j ON j.cutoff_id = c.cutoff_id " +
            "LEFT JOIN mht_cet_cutoff m ON m.cutoff_id = c.cutoff_id ";

    public static List<Cutoff> getCutoffs() { return query(null, null); }
    public static List<Cutoff> getCutoffsByCourse(int courseId) { return query(courseId, null); }
    public static List<Cutoff> getCutoffsByChoiceCode(String choiceCode) { return query(null, choiceCode); }

    private static List<Cutoff> query(Integer courseId, String choiceCode) {
        List<Cutoff> result = new ArrayList<>();
        String sql = CUTOFF_SELECT;
        if (courseId != null) sql += " WHERE c.course_id = ?";
        else if (choiceCode != null) sql += " WHERE c.choice_code = ?";
        sql += " ORDER BY c.year DESC, c.cutoff_id";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (courseId != null) ps.setInt(1, courseId);
            else if (choiceCode != null) ps.setString(1, choiceCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(new Cutoff(rs.getInt("cutoff_id"), rs.getInt("course_id"),
                        rs.getString("category_code"), rs.getDouble("percentile"), rs.getString("exam_name"),
                        rs.getString("choice_code"), rs.getInt("year"), rs.getInt("cap_round"), rs.getInt("merit_rank")));
            }
        } catch (SQLException e) { System.err.println("Could not load cutoffs: " + e.getMessage()); }
        return result;
    }

    public static List<String> getCategoryCodes() {
        List<String> values = new ArrayList<>();
        try (Connection con = DBConnection.getConnection(); Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT category_code FROM CATEGORY ORDER BY category_code")) {
            while (rs.next()) values.add(rs.getString(1));
        } catch (SQLException e) { System.err.println("Could not load categories: " + e.getMessage()); }
        if (values.isEmpty()) {
            values.add("AI"); values.add("OPEN"); values.add("OBC"); values.add("SC"); values.add("ST"); values.add("EWS");
        }
        return values;
    }

    public static boolean addCutoff(Cutoff cutoff) {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                String choiceCode = cutoff.getChoiceCode();
                if (choiceCode == null || choiceCode.trim().isEmpty()) choiceCode = findChoiceCode(con, cutoff.getCourseId());
                if (choiceCode == null) throw new SQLException("No college-course association exists for this course.");
                ensureCategory(con, cutoff.getCategory());
                int year = cutoff.getYear() > 0 ? cutoff.getYear() : latestYear(con);
                long cutoffId;
                String sql = "INSERT INTO CUTOFFS(choice_code, course_id, year, cap_round, category_code, merit_rank, percentile) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, choiceCode); ps.setInt(2, cutoff.getCourseId()); ps.setInt(3, year);
                    ps.setInt(4, Math.max(1, cutoff.getCapRound())); ps.setString(5, cutoff.getCategory());
                    if (cutoff.getMeritRank() < 0) ps.setNull(6, Types.INTEGER); else ps.setInt(6, cutoff.getMeritRank());
                    ps.setDouble(7, cutoff.getCutoffPercentile()); ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) { if (!keys.next()) throw new SQLException("No cutoff ID generated."); cutoffId = keys.getLong(1); }
                }
                insertExamType(con, cutoffId, cutoff.getExamName());
                con.commit(); return true;
            } catch (SQLException e) { con.rollback(); System.err.println("Could not add cutoff: " + e.getMessage()); return false; }
            finally { con.setAutoCommit(true); }
        } catch (SQLException e) { System.err.println("Could not add cutoff: " + e.getMessage()); return false; }
    }

    public static boolean updateCutoff(Cutoff cutoff) {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                ensureCategory(con, cutoff.getCategory());
                try (PreparedStatement ps = con.prepareStatement("UPDATE CUTOFFS SET category_code = ?, percentile = ? WHERE cutoff_id = ?")) {
                    ps.setString(1, cutoff.getCategory()); ps.setDouble(2, cutoff.getCutoffPercentile()); ps.setInt(3, cutoff.getCutoffId());
                    if (ps.executeUpdate() == 0) throw new SQLException("Cutoff record was not found.");
                }
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM jee_cutoff WHERE cutoff_id = ?")) { ps.setInt(1, cutoff.getCutoffId()); ps.executeUpdate(); }
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM mht_cet_cutoff WHERE cutoff_id = ?")) { ps.setInt(1, cutoff.getCutoffId()); ps.executeUpdate(); }
                insertExamType(con, cutoff.getCutoffId(), cutoff.getExamName());
                con.commit(); return true;
            } catch (SQLException e) { con.rollback(); System.err.println("Could not update cutoff: " + e.getMessage()); return false; }
            finally { con.setAutoCommit(true); }
        } catch (SQLException e) { System.err.println("Could not update cutoff: " + e.getMessage()); return false; }
    }

    public static boolean deleteCutoff(int cutoffId) {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM jee_cutoff WHERE cutoff_id = ?")) { ps.setInt(1, cutoffId); ps.executeUpdate(); }
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM mht_cet_cutoff WHERE cutoff_id = ?")) { ps.setInt(1, cutoffId); ps.executeUpdate(); }
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM CUTOFFS WHERE cutoff_id = ?")) { ps.setInt(1, cutoffId); ps.executeUpdate(); }
                con.commit(); return true;
            } catch (SQLException e) { con.rollback(); System.err.println("Could not delete cutoff: " + e.getMessage()); return false; }
            finally { con.setAutoCommit(true); }
        } catch (SQLException e) { System.err.println("Could not delete cutoff: " + e.getMessage()); return false; }
    }

    public static int getNextId() {
        try (Connection con = DBConnection.getConnection(); Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(cutoff_id), 0) + 1 FROM CUTOFFS")) {
            return rs.next() ? rs.getInt(1) : 1;
        } catch (SQLException e) { System.err.println("Could not generate cutoff ID: " + e.getMessage()); return 1; }
    }

    public static int getLatestYear() {
        try (Connection con = DBConnection.getConnection()) { return latestYear(con); }
        catch (SQLException e) { return java.time.Year.now().getValue(); }
    }

    private static int latestYear(Connection con) throws SQLException {
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(year), YEAR(CURDATE())) FROM CUTOFFS")) {
            return rs.next() ? rs.getInt(1) : java.time.Year.now().getValue();
        }
    }
    private static String findChoiceCode(Connection con, int courseId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT choice_code FROM COLLEGE_COURSES WHERE course_id = ? ORDER BY choice_code LIMIT 1")) {
            ps.setInt(1, courseId); try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
        }
    }
    private static void ensureCategory(Connection con, String category) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("INSERT IGNORE INTO CATEGORY(category_code, category_name) VALUES (?, ?)")) {
            ps.setString(1, category); ps.setString(2, category); ps.executeUpdate();
        }
    }
    private static void insertExamType(Connection con, long cutoffId, String examName) throws SQLException {
        boolean jee = examName != null && examName.toLowerCase().contains("jee");
        String sql = jee ? "INSERT INTO jee_cutoff(cutoff_id, exam_type) VALUES (?, ?)" :
                "INSERT INTO mht_cet_cutoff(cutoff_id, exam_type) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, cutoffId); ps.setString(2, jee ? "JEE(Main)" : "MHT-CET"); ps.executeUpdate();
        }
    }
}
