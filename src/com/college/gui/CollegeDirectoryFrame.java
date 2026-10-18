
package com.college.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

import com.college.model.College;
import com.college.service.CollegeDataService;
import com.college.service.RecommendationService;

public class CollegeDirectoryFrame extends JFrame {

    private List<College> colleges;
    private JList<College> collegeList;

    public CollegeDirectoryFrame() {
        colleges = CollegeDataService.getColleges();

        setTitle("College Directory");
        setSize(650, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JLabel heading = new JLabel("Available Engineering Colleges");
        heading.setHorizontalAlignment(JLabel.CENTER);
        add(heading, BorderLayout.NORTH);

        collegeList = new JList<>(colleges.toArray(new College[0]));
        collegeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(collegeList), BorderLayout.CENTER);

        JButton viewButton = new JButton("View College Details");
        JButton closeButton = new JButton("Close");

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        buttonPanel.add(viewButton);
        buttonPanel.add(closeButton);

        add(buttonPanel, BorderLayout.SOUTH);

        viewButton.addActionListener(e -> viewCollegeDetails());
        closeButton.addActionListener(e -> dispose());
    }

    private void viewCollegeDetails() {
        College selectedCollege = collegeList.getSelectedValue();

        if (selectedCollege == null) {
            JOptionPane.showMessageDialog(
                    this, "Please select a college first."
            );
            return;
        }

        RecommendationService service = new RecommendationService();

        new CollegeDetailsFrame(
                selectedCollege,
                service.getCourses()
        ).setVisible(true);
    }
}
