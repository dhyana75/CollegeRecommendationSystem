package com.college.gui;

import com.college.model.College;
import com.college.service.BookmarkService;
import com.college.service.CollegeDataService;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Admin screen aligned with COLLEGES(institute_code, institute_name, city_id). */
public class AdminFrame extends JFrame {
    private DefaultTableModel tableModel;
    private JTable collegeTable;
    private JTextField nameField;
    private JTextField cityField;

    public AdminFrame() {
        setTitle("Admin - Manage Colleges");
        setSize(900, 520);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        add(new JLabel("ADMIN - MANAGE COLLEGES", JLabel.CENTER), BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"Institute Code", "College Name", "City"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        collegeTable = new JTable(tableModel);
        add(new JScrollPane(collegeTable), BorderLayout.CENTER);

        JPanel formPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        nameField = new JTextField();
        cityField = new JTextField();
        formPanel.add(new JLabel("College / Institute Name"));
        formPanel.add(new JLabel("City"));
        formPanel.add(nameField);
        formPanel.add(cityField);

        JButton addButton = new JButton("Add College");
        JButton updateButton = new JButton("Update Selected");
        JButton deleteButton = new JButton("Delete Selected");
        JButton clearButton = new JButton("Clear Fields");
        JPanel actions = new JPanel(new GridLayout(1, 4, 5, 5));
        actions.add(addButton); actions.add(updateButton); actions.add(deleteButton); actions.add(clearButton);

        JButton coursesButton = new JButton("Manage Courses");
        JButton cutoffsButton = new JButton("Manage Cutoffs");
        JPanel management = new JPanel(new GridLayout(1, 2, 5, 5));
        management.add(coursesButton); management.add(cutoffsButton);
        JPanel bottom = new JPanel(new GridLayout(3, 1, 5, 5));
        bottom.add(formPanel); bottom.add(actions); bottom.add(management);
        add(bottom, BorderLayout.SOUTH);

        addButton.addActionListener(e -> addCollege());
        updateButton.addActionListener(e -> updateCollege());
        deleteButton.addActionListener(e -> deleteCollege());
        clearButton.addActionListener(e -> clearFields());
        coursesButton.addActionListener(e -> new CourseManagementFrame().setVisible(true));
        cutoffsButton.addActionListener(e -> new CutoffManagementFrame().setVisible(true));
        collegeTable.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) fillFields(); });
        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (College c : CollegeDataService.getColleges())
            tableModel.addRow(new Object[]{c.getCollegeId(), c.getCollegeName(), c.getCity()});
    }

    private boolean validateFields() {
        if (nameField.getText().trim().isEmpty() || cityField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter both the college name and city.");
            return false;
        }
        return true;
    }

    private void addCollege() {
        if (!validateFields()) return;
        College college = new College(CollegeDataService.getNextId(), nameField.getText().trim(), cityField.getText().trim(), "", "");
        if (CollegeDataService.addCollege(college)) {
            refreshTable(); clearFields();
            JOptionPane.showMessageDialog(this, "College added successfully.");
        } else JOptionPane.showMessageDialog(this, "Could not add college. Check the terminal for the database error.");
    }

    private void updateCollege() {
        int row = collegeTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a college to update."); return; }
        if (!validateFields()) return;
        int id = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
        College college = new College(id, nameField.getText().trim(), cityField.getText().trim(), "", "");
        if (CollegeDataService.updateCollege(college)) {
            refreshTable(); clearFields();
            JOptionPane.showMessageDialog(this, "College updated successfully.");
        } else JOptionPane.showMessageDialog(this, "Could not update college. Check the terminal for the database error.");
    }

    private void deleteCollege() {
        int row = collegeTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a college to delete."); return; }
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete this college and its linked course/cutoff records?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;
        int id = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
        if (CollegeDataService.deleteCollege(id)) {
            BookmarkService.removeBookmark(id); refreshTable(); clearFields();
            JOptionPane.showMessageDialog(this, "College and linked records deleted successfully.");
        } else JOptionPane.showMessageDialog(this, "Could not delete college. Check the terminal for the database error.");
    }

    private void fillFields() {
        int row = collegeTable.getSelectedRow();
        if (row < 0) return;
        nameField.setText(String.valueOf(tableModel.getValueAt(row, 1)));
        cityField.setText(String.valueOf(tableModel.getValueAt(row, 2)));
    }

    private void clearFields() { nameField.setText(""); cityField.setText(""); collegeTable.clearSelection(); }
}
