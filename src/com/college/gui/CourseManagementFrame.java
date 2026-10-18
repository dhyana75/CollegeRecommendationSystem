package com.college.gui;

import com.college.model.College;
import com.college.model.Course;
import com.college.service.CollegeDataService;
import com.college.service.CourseDataService;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Course screen uses COURSES and COLLEGE_COURSES; the supplied schema has no seat-count column. */
public class CourseManagementFrame extends JFrame {
    private JComboBox<College> collegeBox;
    private JTextField courseNameField;
    private JTable courseTable;
    private DefaultTableModel tableModel;

    public CourseManagementFrame() {
        setTitle("Admin - Manage Courses"); setSize(750, 430); setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); setLayout(new BorderLayout(10, 10));
        collegeBox = new JComboBox<>();
        for (College college : CollegeDataService.getColleges()) collegeBox.addItem(college);
        collegeBox.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof College) setText(((College) value).getCollegeName());
                return this;
            }
        });
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT)); top.add(new JLabel("Select College:")); top.add(collegeBox); add(top, BorderLayout.NORTH);
        tableModel = new DefaultTableModel(new String[]{"Course ID", "Course Name", "Choice Code"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        courseTable = new JTable(tableModel); add(new JScrollPane(courseTable), BorderLayout.CENTER);
        JPanel form = new JPanel(new GridLayout(2, 2, 5, 5));
        courseNameField = new JTextField(); form.add(new JLabel("Course Name:")); form.add(new JLabel(""));
        form.add(courseNameField); form.add(new JLabel("Course IDs and choice codes are managed automatically."));
        JButton add = new JButton("Add Course"), update = new JButton("Update Selected"), delete = new JButton("Delete Selected"), clear = new JButton("Clear Fields");
        JPanel buttons = new JPanel(new GridLayout(1, 4, 5, 5)); buttons.add(add); buttons.add(update); buttons.add(delete); buttons.add(clear);
        JPanel bottom = new JPanel(new BorderLayout(5, 5)); bottom.add(form, BorderLayout.CENTER); bottom.add(buttons, BorderLayout.SOUTH); add(bottom, BorderLayout.SOUTH);
        collegeBox.addActionListener(e -> refreshTable());
        courseTable.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) fillFields(); });
        add.addActionListener(e -> addCourse()); update.addActionListener(e -> updateCourse()); delete.addActionListener(e -> deleteCourse()); clear.addActionListener(e -> clearFields());
        refreshTable();
    }

    private College getSelectedCollege() { return (College) collegeBox.getSelectedItem(); }
    private void refreshTable() {
        tableModel.setRowCount(0); College college = getSelectedCollege(); if (college == null) return;
        for (Course course : CourseDataService.getCoursesByCollege(college.getCollegeId()))
            tableModel.addRow(new Object[]{course.getCourseId(), course.getCourseName(), course.getChoiceCode()});
    }
    private void addCourse() {
        College college = getSelectedCollege(); String name = courseNameField.getText().trim();
        if (college == null) { JOptionPane.showMessageDialog(this, "Add a college first."); return; }
        if (name.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter a course name."); return; }
        Course course = new Course(CourseDataService.getNextId(), college.getCollegeId(), name, 0);
        if (CourseDataService.addCourse(course)) { refreshTable(); clearFields(); JOptionPane.showMessageDialog(this, "Course added successfully."); }
        else JOptionPane.showMessageDialog(this, "Could not add course. The course name may already exist or the database returned an error.");
    }
    private void updateCourse() {
        int row = courseTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a course to update."); return; }
        String name = courseNameField.getText().trim(); if (name.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter a course name."); return; }
        int id = Integer.parseInt(tableModel.getValueAt(row, 0).toString()); College college = getSelectedCollege();
        if (college != null && CourseDataService.updateCourse(new Course(id, college.getCollegeId(), name, 0))) {
            refreshTable(); clearFields(); JOptionPane.showMessageDialog(this, "Course updated. Course names are shared by all college associations in this schema.");
        } else JOptionPane.showMessageDialog(this, "Could not update course. Check the terminal for the database error.");
    }
    private void deleteCourse() {
        int row = courseTable.getSelectedRow(); if (row < 0) { JOptionPane.showMessageDialog(this, "Select a course to delete."); return; }
        if (JOptionPane.showConfirmDialog(this, "Delete this course from the selected college and remove its cutoff records?", "Confirm Delete", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        College college = getSelectedCollege(); int id = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
        if (college != null && CourseDataService.deleteCourse(id, college.getCollegeId())) { refreshTable(); clearFields(); JOptionPane.showMessageDialog(this, "Course association deleted successfully."); }
        else JOptionPane.showMessageDialog(this, "Could not delete course. Check the terminal for the database error.");
    }
    private void fillFields() { int row = courseTable.getSelectedRow(); if (row >= 0) courseNameField.setText(String.valueOf(tableModel.getValueAt(row, 1))); }
    private void clearFields() { courseNameField.setText(""); courseTable.clearSelection(); }
}
