package com.college.service;

import com.college.dao.CollegeDAO;
import com.college.model.College;
import java.util.List;

public class CollegeDataService {
    private static final CollegeDAO collegeDAO = new CollegeDAO();
    public static List<College> getColleges() { return collegeDAO.getAllColleges(); }
    public static boolean addCollege(College college) { return collegeDAO.addCollege(college); }
    public static boolean updateCollege(College college) { return collegeDAO.updateCollege(college); }
    public static boolean deleteCollege(int collegeId) { return collegeDAO.deleteCollege(collegeId); }
    public static int getNextId() {
        int maxId = 0;
        for (College college : getColleges()) if (college.getCollegeId() > maxId) maxId = college.getCollegeId();
        return maxId + 1;
    }
}
