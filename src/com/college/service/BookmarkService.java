
package com.college.service;

import com.college.model.College;
import java.util.ArrayList;
import java.util.List;

public class BookmarkService {

    private static final List<College> bookmarkedColleges =
            new ArrayList<>();

    public static void bookmarkCollege(College college) {
        for (College bookmarked : bookmarkedColleges) {
            if (bookmarked.getCollegeId() == college.getCollegeId()) {
                return;
            }
        }

        bookmarkedColleges.add(college);
    }

    public static void removeBookmark(int collegeId) {
        bookmarkedColleges.removeIf(
                college -> college.getCollegeId() == collegeId
        );
    }

    public static List<College> getBookmarkedColleges() {
        return new ArrayList<>(bookmarkedColleges);
    }

    public static boolean isBookmarked(int collegeId) {
        for (College college : bookmarkedColleges) {
            if (college.getCollegeId() == collegeId) {
                return true;
            }
        }

        return false;
    }
}
