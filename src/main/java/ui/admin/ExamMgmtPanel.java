package main.java.ui.admin;

import main.java.model.Exam;
import main.java.service.ExamService;
import main.java.service.ExamServiceImpl;
import main.java.service.ServiceException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class ExamMgmtPanel extends JPanel {
    private final ExamService examService = new ExamServiceImpl();

    public ExamMgmtPanel() {
        setLayout(new BorderLayout());

        JLabel header = new JLabel("시험 관리");
        header.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(header, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();

        try {
            List<Exam> allExams = examService.getAllExams();
            Map<Integer, List<Exam>> grouped = new TreeMap<>(Comparator.reverseOrder());
            for (Exam exam : allExams) {
                int year = exam.getStartDate().getYear();
                grouped.computeIfAbsent(year, k -> new ArrayList<>()).add(exam);
            }

            for (Map.Entry<Integer, List<Exam>> entry : grouped.entrySet()) {
                int year = entry.getKey();
                List<Exam> exams = entry.getValue();
                tabbedPane.addTab(String.valueOf(year), createExamTablePanel(exams));
            }
        } catch (ServiceException e) {
            tabbedPane.addTab("오류", new JLabel("시험 목록 조회 오류: " + e.getMessage()));
        }

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createExamTablePanel(List<Exam> exams) {
        JPanel panel = new JPanel(new BorderLayout());

        String[] cols = {"과목", "시작일", "마감일", "제한시간", "상태", "수정", "비활성화", "응시 대상"};
        DefaultTableModel model = new DefaultTableModel(null, cols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setRowHeight(28);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (Exam exam : exams) {
            String subject = exam.getSubject();
            String start = exam.getStartDate().format(dtf);
            String end = exam.getEndDate().format(dtf);
            String duration = exam.getDurationMinutes() + "분";

            String status;
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(exam.getStartDate())) status = "예정";
            else if (now.isAfter(exam.getEndDate())) status = "완료";
            else status = "진행중";

            model.addRow(new Object[]{subject, start, end, duration, status, "수정", "비활성화", "응시 대상"});
        }

        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }
}