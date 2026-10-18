package com.college.dao;

import com.college.db.DBConnection;
import com.college.model.College;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** JDBC access for the COLLEGES and CITIES tables in collegedb. */
public class CollegeDAO {

    public List<College> getAllColleges() {
        List<College> colleges = new ArrayList<>();
        String sql = "SELECT CAST(c.institute_code AS UNSIGNED) AS college_id, " +
                "c.institute_name AS college_name, ci.city_name AS city " +
                "FROM COLLEGES c LEFT JOIN CITIES ci ON ci.city_id = c.city_id " +
                "ORDER BY c.institute_name";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                colleges.add(new College(rs.getInt("college_id"),
                        rs.getString("college_name"), rs.getString("city"), "", ""));
            }
        } catch (SQLException e) {
            System.err.println("Could not load colleges: " + e.getMessage());
        }
        return colleges;
    }

    public boolean addCollege(College college) {
        String citySql = "INSERT INTO CITIES(city_name) VALUES (?) " +
                "ON DUPLICATE KEY UPDATE city_id = LAST_INSERT_ID(city_id)";
        String cityIdSql = "SELECT city_id FROM CITIES WHERE city_name = ?";
        String collegeSql = "INSERT INTO COLLEGES(institute_code, institute_name, city_id) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(citySql)) {
                    ps.setString(1, college.getCity().trim());
                    ps.executeUpdate();
                }
                int cityId;
                try (PreparedStatement ps = con.prepareStatement(cityIdSql)) {
                    ps.setString(1, college.getCity().trim());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Could not resolve city ID.");
                        cityId = rs.getInt(1);
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(collegeSql)) {
                    ps.setString(1, String.valueOf(college.getCollegeId()));
                    ps.setString(2, college.getCollegeName().trim());
                    ps.setInt(3, cityId);
                    ps.executeUpdate();
                }
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                System.err.println("Could not add college: " + e.getMessage());
                return false;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Could not add college: " + e.getMessage());
            return false;
        }
    }

    public boolean updateCollege(College college) {
        String citySql = "INSERT INTO CITIES(city_name) VALUES (?) " +
                "ON DUPLICATE KEY UPDATE city_id = LAST_INSERT_ID(city_id)";
        String cityIdSql = "SELECT city_id FROM CITIES WHERE city_name = ?";
        String updateSql = "UPDATE COLLEGES SET institute_name = ?, city_id = ? WHERE institute_code = ?";
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(citySql)) {
                    ps.setString(1, college.getCity().trim());
                    ps.executeUpdate();
                }
                int cityId;
                try (PreparedStatement ps = con.prepareStatement(cityIdSql)) {
                    ps.setString(1, college.getCity().trim());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Could not resolve city ID.");
                        cityId = rs.getInt(1);
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                    ps.setString(1, college.getCollegeName().trim());
                    ps.setInt(2, cityId);
                    ps.setString(3, String.valueOf(college.getCollegeId()));
                    int changed = ps.executeUpdate();
                    con.commit();
                    return changed > 0;
                }
            } catch (SQLException e) {
                con.rollback();
                System.err.println("Could not update college: " + e.getMessage());
                return false;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Could not update college: " + e.getMessage());
            return false;
        }
    }

    /** Deletes dependent cutoff/association/recommendation rows before the college. */
    public boolean deleteCollege(int collegeId) {
        String code = String.valueOf(collegeId);
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                deleteRelatedToCollege(con, code);
                try (PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM COLLEGES WHERE institute_code = ?")) {
                    ps.setString(1, code);
                    int changed = ps.executeUpdate();
                    con.commit();
                    return changed > 0;
                }
            } catch (SQLException e) {
                con.rollback();
                System.err.println("Could not delete college: " + e.getMessage());
                return false;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Could not delete college: " + e.getMessage());
            return false;
        }
    }

    private void deleteRelatedToCollege(Connection con, String instituteCode) throws SQLException {
        String choices = "SELECT choice_code FROM COLLEGE_COURSES WHERE institute_code = ?";
        List<String> codes = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(choices)) {
            ps.setString(1, instituteCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) codes.add(rs.getString(1));
            }
        }
        for (String choiceCode : codes) deleteChoiceDependents(con, choiceCode);
        try (PreparedStatement ps = con.prepareStatement(
                "DELETE FROM COLLEGE_COURSES WHERE institute_code = ?")) {
            ps.setString(1, instituteCode);
            ps.executeUpdate();
        }
    }

    private void deleteChoiceDependents(Connection con, String choiceCode) throws SQLException {
        String idsSql = "SELECT cutoff_id FROM CUTOFFS WHERE choice_code = ?";
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(idsSql)) {
            ps.setString(1, choiceCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getLong(1));
            }
        }
        for (long id : ids) {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM jee_cutoff WHERE cutoff_id = ?")) {
                ps.setLong(1, id); ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM mht_cet_cutoff WHERE cutoff_id = ?")) {
                ps.setLong(1, id); ps.executeUpdate();
            }
        }
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM CUTOFFS WHERE choice_code = ?")) {
            ps.setString(1, choiceCode); ps.executeUpdate();
        }
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM RECOMMENDED_COLLEGES WHERE choice_code = ?")) {
            ps.setString(1, choiceCode); ps.executeUpdate();
        }
    }
}
