package com.college.gui;

import com.college.model.College;
import com.college.model.Course;
import com.college.model.Cutoff;
import com.college.service.CollegeDataService;
import com.college.service.CourseDataService;
import com.college.service.CutoffDataService;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Cutoff maintenance for CUTOFFS plus jee_cutoff/mht_cet_cutoff. */
public class CutoffManagementFrame extends JFrame {
    private JComboBox<College> collegeBox;
    private JComboBox<Course> courseBox;
    private JComboBox<String> categoryBox;
    private JComboBox<String> examBox;
    private JTextField percentileField;
    private JTable cutoffTable;
    private DefaultTableModel tableModel;

    public CutoffManagementFrame() {
        setTitle("Admin - Manage Cutoffs"); setSize(900, 500); setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); setLayout(new BorderLayout(10, 10));
        collegeBox = new JComboBox<>(); for (College c : CollegeDataService.getColleges()) collegeBox.addItem(c);
        collegeBox.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof College) setText(((College) value).getCollegeName()); return this;
            }
        });
        courseBox = new JComboBox<>();
        categoryBox = new JComboBox<>(CutoffDataService.getCategoryCodes().toArray(new String[0]));
        examBox = new JComboBox<>(new String[]{"JEE Main", "MHT-CET"}); percentileField = new JTextField();
        JPanel selection = new JPanel(new GridLayout(2, 4, 5, 5));
        selection.add(new JLabel("College:")); selection.add(new JLabel("Course:")); selection.add(new JLabel("Category Code:")); selection.add(new JLabel("Exam:"));
        selection.add(collegeBox); selection.add(courseBox); selection.add(categoryBox); selection.add(examBox); add(selection, BorderLayout.NORTH);
        tableModel = new DefaultTableModel(new String[]{"Cutoff ID", "Course", "Category", "Percentile", "Exam", "Year"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        cutoffTable = new JTable(tableModel); add(new JScrollPane(cutoffTable), BorderLayout.CENTER);
        JPanel form = new JPanel(new GridLayout(1, 2, 5, 5)); form.add(new JLabel("Cutoff Percentile (0-100):")); form.add(percentileField);
        JButton add = new JButton("Add Cutoff"), update = new JButton("Update Selected"), delete = new JButton("Delete Selected"), clear = new JButton("Clear Fields");
        JPanel buttons = new JPanel(new GridLayout(1, 4, 5, 5)); buttons.add(add); buttons.add(update); buttons.add(delete); buttons.add(clear);
        JPanel bottom = new JPanel(new BorderLayout(5, 5)); bottom.add(form, BorderLayout.CENTER); bottom.add(buttons, BorderLayout.SOUTH); add(bottom, BorderLayout.SOUTH);
        collegeBox.addActionListener(e -> refreshCourses()); courseBox.addActionListener(e -> refreshTable());
        cutoffTable.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) fillFields(); });
        add.addActionListener(e -> addCutoff()); update.addActionListener(e -> updateCutoff()); delete.addActionListener(e -> deleteCutoff()); clear.addActionListener(e -> clearFields());
        refreshCourses();
    }

    private College getSelectedCollege() { return (College) collegeBox.getSelectedItem(); }
    private Course getSelectedCourse() { return (Course) courseBox.getSelectedItem(); }
    private void refreshCourses() {
        courseBox.removeAllItems(); College c = getSelectedCollege();
        if (c != null) for (Course course : CourseDataService.getCoursesByCollege(c.getCollegeId())) courseBox.addItem(course);
        refreshTable();
    }
    private void refreshTable() {
        tableModel.setRowCount(0); Course course = getSelectedCourse(); if (course == null) return;
        for (Cutoff cutoff : CutoffDataService.getCutoffsByChoiceCode(course.getChoiceCode()))
            tableModel.addRow(new Object[]{cutoff.getCutoffId(), course.getCourseName(), cutoff.getCategory(), cutoff.getCutoffPercentile(), cutoff.getExamName(), cutoff.getYear()});
    }
    private Double readPercentile() {
        try {
            double value = Double.parseDouble(percentileField.getText().trim());
            if (value < 0 || value > 100) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Enter a valid percentile between 0 and 100."); return null; }
    }
    private void addCutoff() {
        Course course = getSelectedCourse(); if (course == null) { JOptionPane.showMessageDialog(this, "Select a course first."); return; }
        Double value = readPercentile(); if (value == null) return;
        String category = (String) categoryBox.getSelectedItem(), exam = (String) examBox.getSelectedItem();
        for (Cutoff existing : CutoffDataService.getCutoffsByChoiceCode(course.getChoiceCode())) {
            if (existing.getCategory().equals(category) && existing.getExamName().equals(exam) && existing.getYear() == CutoffDataService.getLatestYear()) {
                JOptionPane.showMessageDialog(this, "A cutoff already exists for this course, category, exam, and year. Update it instead."); return;
            }
        }
        Cutoff cutoff = new Cutoff(CutoffDataService.getNextId(), course.getCourseId(), category, value, exam,
                course.getChoiceCode(), CutoffDataService.getLatestYear(), 1, -1);
        if (CutoffDataService.addCutoff(cutoff)) { refreshTable(); clearFields(); JOptionPane.showMessageDialog(this, "Cutoff added successfully."); }
        else JOptionPane.showMessageDialog(this, "Could not add cutoff. Check the terminal for the database error.");
    }
    private void updateCutoff() {
        int row = cutoffTable.getSelectedRow(); if (row < 0) { JOptionPane.showMessageDialog(this, "Select a cutoff to update."); return; }
        Double value = readPercentile(); if (value == null) return; Course course = getSelectedCourse(); if (course == null) return;
        int id = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
        Cutoff cutoff = new Cutoff(id, course.getCourseId(), (String) categoryBox.getSelectedItem(), value,
                (String) examBox.getSelectedItem(), course.getChoiceCode(), CutoffDataService.getLatestYear(), 1, -1);
        if (CutoffDataService.updateCutoff(cutoff)) { refreshTable(); clearFields(); JOptionPane.showMessageDialog(this, "Cutoff updated successfully."); }
        else JOptionPane.showMessageDialog(this, "Could not update cutoff. Check the terminal for the database error.");
    }
    private void deleteCutoff() {
        int row = cutoffTable.getSelectedRow(); if (row < 0) { JOptionPane.showMessageDialog(this, "Select a cutoff to delete."); return; }
        if (JOptionPane.showConfirmDialog(this, "Delete this cutoff?", "Confirm Delete", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        int id = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
        if (CutoffDataService.deleteCutoff(id)) { refreshTable(); clearFields(); JOptionPane.showMessageDialog(this, "Cutoff deleted successfully."); }
        else JOptionPane.showMessageDialog(this, "Could not delete cutoff. Check the terminal for the database error.");
    }
    private void fillFields() {
        int row = cutoffTable.getSelectedRow(); if (row < 0) return;
        percentileField.setText(String.valueOf(tableModel.getValueAt(row, 3)));
        categoryBox.setSelectedItem(String.valueOf(tableModel.getValueAt(row, 2)));
        examBox.setSelectedItem(String.valueOf(tableModel.getValueAt(row, 4)));
    }
    private void clearFields() { percentileField.setText(""); cutoffTable.clearSelection(); }
}
