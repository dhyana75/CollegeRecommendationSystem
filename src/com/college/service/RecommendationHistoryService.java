
package com.college.service;

import java.util.ArrayList;
import java.util.List;

public class RecommendationHistoryService {

    private static final List<String> savedRecommendations =
            new ArrayList<>();

    public static void saveRecommendations(List<String> recommendations) {
        savedRecommendations.clear();
        savedRecommendations.addAll(recommendations);
    }

    public static List<String> getSavedRecommendations() {
        return new ArrayList<>(savedRecommendations);
    }

    public static boolean hasRecommendations() {
        return !savedRecommendations.isEmpty();
    }
}
