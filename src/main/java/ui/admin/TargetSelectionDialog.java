package main.java.ui.admin;

import main.java.context.ExamCreationContext;
import main.java.model.Department;
import main.java.service.*;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class TargetSelectionDialog extends JDialog {

    private final Set<Integer> selectedGrades = new HashSet<>();
    private final Set<Integer> selectedDpmtIds = new HashSet<>();
    private final Runnable onSuccess;

    public TargetSelectionDialog(JFrame parent, ExamCreationContext context, Runnable onSuccess) {
        super(parent, "응시 대상 선택", true);
        this.onSuccess = onSuccess;
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new GridLayout(1, 2));
        mainPanel.add(createGradePanel());
        mainPanel.add(createDepartmentPanel());

        JPanel buttonPanel = new JPanel();
        JButton confirmBtn = new JButton("확인");
        JButton cancelBtn = new JButton("취소");

        confirmBtn.addActionListener(e -> {
            if (selectedGrades.isEmpty() || selectedDpmtIds.isEmpty()) {
                JOptionPane.showMessageDialog(this, "학년과 학과를 하나 이상 선택해주세요.");
                return;
            }

            context.setTargetGrades(new ArrayList<>(selectedGrades));
            context.setTargetDepartments(new ArrayList<>(selectedDpmtIds));

            try {
                ExamService examService = new ExamServiceImpl();
                examService.saveExamWithDetails(context);
                JOptionPane.showMessageDialog(this, "시험이 성공적으로 저장되었습니다.");
                dispose();
                onSuccess.run();
            } catch (ServiceException ex) {
                JOptionPane.showMessageDialog(this, "시험 저장 중 오류 발생: " + ex.getMessage(),
                        "오류", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelBtn.addActionListener(e -> dispose());

        buttonPanel.add(confirmBtn);
        buttonPanel.add(cancelBtn);

        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
    }

    private JPanel createGradePanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.setBorder(BorderFactory.createTitledBorder("학년 선택"));

        for (int grade = 1; grade <= 3; grade++) {
            JCheckBox cb = new JCheckBox(grade + "학년");
            int g = grade;
            cb.addActionListener(e -> {
                if (cb.isSelected()) selectedGrades.add(g);
                else selectedGrades.remove(g);
            });
            panel.add(cb);
        }

        return panel;
    }

    private JPanel createDepartmentPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.setBorder(BorderFactory.createTitledBorder("학과 선택"));

        DepartmentService dpmtService = new DepartmentServiceImpl();
        try {
            List<Department> departments = dpmtService.getAllDepartments();
            for (Department d : departments) {
                JCheckBox cb = new JCheckBox(d.getDpmtName());
                int dpmtId = d.getDpmtId();
                cb.addActionListener(e -> {
                    if (cb.isSelected()) selectedDpmtIds.add(dpmtId);
                    else selectedDpmtIds.remove(dpmtId);
                });
                panel.add(cb);
            }
        } catch (Exception e) {
            panel.add(new JLabel("학과 정보를 불러올 수 없습니다."));
        }

        return panel;
    }
}