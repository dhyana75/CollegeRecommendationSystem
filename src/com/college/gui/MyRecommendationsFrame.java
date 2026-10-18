
package com.college.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import com.college.model.College;
import com.college.service.BookmarkService;

public class MyRecommendationsFrame extends JFrame {

    private DefaultListModel<College> listModel;
    private JList<College> collegeList;

    public MyRecommendationsFrame() {
        setTitle("My Bookmarked Colleges");
        setSize(650, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JLabel heading = new JLabel(
                "My Bookmarked Colleges",
                SwingConstants.CENTER
        );
        add(heading, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        collegeList = new JList<>(listModel);

        refreshBookmarks();

        add(new JScrollPane(collegeList), BorderLayout.CENTER);

        JButton removeButton = new JButton("Remove Bookmark");
        JButton closeButton = new JButton("Close");

        JPanel buttonPanel = new JPanel(
                new FlowLayout(FlowLayout.CENTER)
        );
        buttonPanel.add(removeButton);
        buttonPanel.add(closeButton);

        add(buttonPanel, BorderLayout.SOUTH);

        removeButton.addActionListener(e -> removeBookmark());
        closeButton.addActionListener(e -> dispose());
    }

    private void refreshBookmarks() {
        listModel.clear();

        List<College> colleges =
                BookmarkService.getBookmarkedColleges();

        for (College college : colleges) {
            listModel.addElement(college);
        }

        if (colleges.isEmpty()) {
            listModel.addElement(
                    new College(
                            -1,
                            "No colleges bookmarked yet.",
                            "",
                            "",
                            ""
                    )
            );
        }
    }

    private void removeBookmark() {
        College selected = collegeList.getSelectedValue();

        if (selected == null || selected.getCollegeId() == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a bookmarked college."
            );
            return;
        }

        BookmarkService.removeBookmark(selected.getCollegeId());

        refreshBookmarks();

        JOptionPane.showMessageDialog(
                this,
                "Bookmark removed successfully."
        );
    }
}
