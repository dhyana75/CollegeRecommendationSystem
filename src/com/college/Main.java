
package com.college;

import com.college.gui.AdminFrame;
import com.college.gui.SearchFrame;
import com.college.gui.CollegeDirectoryFrame;
import com.college.gui.MyRecommendationsFrame;

import javax.swing.*;
import java.awt.*;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("College Recommendation System");

            frame.setSize(750, 520);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setLayout(new BorderLayout(10, 10));

            JLabel title = new JLabel(
                    "COLLEGE RECOMMENDATION SYSTEM",
                    SwingConstants.CENTER
            );
            title.setFont(new Font("Arial", Font.BOLD, 22));
            title.setBorder(
                    BorderFactory.createEmptyBorder(20, 10, 10, 10)
            );

            JPanel panel = new JPanel(new GridLayout(5, 1, 12, 12));
            panel.setBorder(
                    BorderFactory.createEmptyBorder(15, 100, 25, 100)
            );

            JButton findButton = new JButton("Find Colleges");
            JButton searchButton = new JButton("Search Colleges");
            JButton recommendationButton =
                    new JButton("My Recommendations");
            JButton adminButton = new JButton("Admin");
            JButton exitButton = new JButton("Exit");

            panel.add(findButton);
            panel.add(searchButton);
            panel.add(recommendationButton);
            panel.add(adminButton);
            panel.add(exitButton);

            findButton.addActionListener(e ->
                    new SearchFrame().setVisible(true)
            );

            searchButton.addActionListener(e ->
                    new CollegeDirectoryFrame().setVisible(true)
            );

            recommendationButton.addActionListener(e ->
                    new MyRecommendationsFrame().setVisible(true)
            );

            adminButton.addActionListener(e ->
                    new AdminFrame().setVisible(true)
            );

            exitButton.addActionListener(e -> frame.dispose());

            frame.add(title, BorderLayout.NORTH);
            frame.add(panel, BorderLayout.CENTER);

            frame.setVisible(true);
        });
    }
}
