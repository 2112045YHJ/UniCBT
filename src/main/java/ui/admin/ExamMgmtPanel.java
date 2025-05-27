package main.java.ui.admin;

import main.java.model.Exam;
import main.java.service.ExamService;
import main.java.service.ExamServiceImpl;
import main.java.service.ServiceException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
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

        // 상단 시험 추가 버튼
        JButton addExamBtn = new JButton("시험 추가");
        addExamBtn.addActionListener(e -> {
            // TODO: ExamEditorPanel 또는 새로운 프레임 호출
            JOptionPane.showMessageDialog(this, "시험 추가 버튼 클릭됨!");
        });
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.add(addExamBtn);
        add(topPanel, BorderLayout.SOUTH);

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
                return column >= 5; // 버튼만 클릭 가능
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

            model.addRow(new Object[]{
                    subject, start, end, duration, status, "수정", "비활성화", "응시 대상"
            });
        }

        TableColumn modifyCol = table.getColumn("수정");
        TableColumn disableCol = table.getColumn("비활성화");
        TableColumn targetCol = table.getColumn("응시 대상");

        modifyCol.setCellRenderer(new ButtonRenderer());
        disableCol.setCellRenderer(new ButtonRenderer());
        targetCol.setCellRenderer(new ButtonRenderer());

        modifyCol.setCellEditor(new ButtonEditor(new JCheckBox(), "수정"));
        disableCol.setCellEditor(new ButtonEditor(new JCheckBox(), "비활성화"));
        targetCol.setCellEditor(new ButtonEditor(new JCheckBox(), "응시 대상"));

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    // -------------------------------
    // 내부 버튼 렌더러 및 에디터 정의
    // -------------------------------
    static class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    static class ButtonEditor extends DefaultCellEditor {
        private final JButton button = new JButton();
        private String label;
        private boolean clicked;
        private final String actionType;

        public ButtonEditor(JCheckBox checkBox, String actionType) {
            super(checkBox);
            this.actionType = actionType;
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int col) {
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            clicked = true;
            return button;
        }

        public Object getCellEditorValue() {
            if (clicked) {
                switch (actionType) {
                    case "수정" -> JOptionPane.showMessageDialog(button, "시험 수정 기능 연결 예정");
                    case "비활성화" -> {
                        int result = JOptionPane.showConfirmDialog(button,
                                "시험을 강제 비활성화 하시겠습니까?",
                                "시험 비활성화 확인",
                                JOptionPane.YES_NO_OPTION);
                        if (result == JOptionPane.YES_OPTION) {
                            JOptionPane.showMessageDialog(button, "비활성화 처리 완료 (예시)");
                        }
                    }
                    case "응시 대상" -> JOptionPane.showMessageDialog(button, "응시 대상 확인 기능 연결 예정");
                }
            }
            clicked = false;
            return label;
        }

        public boolean stopCellEditing() {
            clicked = false;
            return super.stopCellEditing();
        }
    }
}
