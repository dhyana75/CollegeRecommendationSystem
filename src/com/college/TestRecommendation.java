
package com.college;

import com.college.service.RecommendationService;
import java.util.List;
import java.util.Scanner;

public class TestRecommendation {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        RecommendationService service = new RecommendationService();

        System.out.print("Enter JEE Main percentile: ");
        double percentile = scanner.nextDouble();

        System.out.print("Enter category (OPEN): ");
        String category = scanner.next();

        List<String> results =
            service.recommendColleges(percentile, category);

        System.out.println("\nRecommended colleges and courses:");

        if (results.isEmpty()) {
            System.out.println("No matching colleges found.");
        } else {
            for (String result : results) {
                System.out.println(result);
            }
        }

        scanner.close();
    }
}
