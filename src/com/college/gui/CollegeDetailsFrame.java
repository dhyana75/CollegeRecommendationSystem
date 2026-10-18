package com.college.gui;

import javax.swing.*;
import java.awt.*;
import com.college.model.College;
import com.college.model.Course;
import java.util.List;

public class CollegeDetailsFrame extends JFrame {
    public CollegeDetailsFrame(College college, List<Course> courses) {
        setTitle("College Details"); setSize(500, 380); setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); setLocationRelativeTo(null);
        JPanel main = new JPanel(new BorderLayout(10, 10)); main.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JPanel details = new JPanel(new GridLayout(0, 1, 5, 5)); details.setBorder(BorderFactory.createTitledBorder("College Information"));
        details.add(new JLabel("College Name: " + college.getCollegeName()));
        details.add(new JLabel("City: " + (college.getCity() == null ? "" : college.getCity())));
        details.add(new JLabel("Institute Code: " + college.getCollegeId()));
        JPanel coursePanel = new JPanel(new BorderLayout(5, 5)); coursePanel.setBorder(BorderFactory.createTitledBorder("Available Courses"));
        DefaultListModel<String> model = new DefaultListModel<>();
        for (Course course : courses) if (course.getCollegeId() == college.getCollegeId()) model.addElement(course.getCourseName());
        if (model.isEmpty()) model.addElement("No courses listed for this college.");
        coursePanel.add(new JScrollPane(new JList<>(model)), BorderLayout.CENTER);
        JButton close = new JButton("Close"); close.addActionListener(e -> dispose());
        main.add(details, BorderLayout.NORTH); main.add(coursePanel, BorderLayout.CENTER);
        JPanel bottom = new JPanel(); bottom.add(close); main.add(bottom, BorderLayout.SOUTH); add(main);
    }
}
