package main.java.ui.admin;

import main.java.context.ExamCreationContext;
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
    private List<Exam> allExams = new ArrayList<>();

    public ExamMgmtPanel() {
        setLayout(new BorderLayout());

        JLabel header = new JLabel("시험 관리");
        header.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(header, BorderLayout.NORTH);

        JButton addExamBtn = new JButton("시험 추가");
        addExamBtn.addActionListener(e -> {
            ExamCreationContext context = new ExamCreationContext();
            JFrame frame = new JFrame("시험 등록");

            ExamEditorPanel editorPanel = new ExamEditorPanel(
                    context,
                    () -> frame.dispose(),  // onBack: 닫기
                    frame                   // parentFrame 전달
            );

            frame.setContentPane(editorPanel);
            frame.setSize(600, 400);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.add(addExamBtn);
        add(topPanel, BorderLayout.SOUTH);

        JTabbedPane tabbedPane = new JTabbedPane();

        try {
            allExams = examService.getAllExams();
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
                return column >= 5;
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

    // 렌더러 공통
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

    // 에디터 공통
    class ButtonEditor extends DefaultCellEditor {
        private final JButton button = new JButton();
        private String label;
        private boolean clicked;
        private final String actionType;
        private int row;

        public ButtonEditor(JCheckBox checkBox, String actionType) {
            super(checkBox);
            this.actionType = actionType;
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int col) {
            this.label = (value == null) ? "" : value.toString();
            this.row = row;
            button.setText(label);
            clicked = true;
            return button;
        }

        public Object getCellEditorValue() {
            if (clicked) {
                Exam exam = allExams.get(row);
                switch (actionType) {
                    case "수정" -> {
                        LocalDateTime now = LocalDateTime.now();
                        if (!now.isBefore(exam.getStartDate()) && !now.isAfter(exam.getEndDate())) {
                            JOptionPane.showMessageDialog(button, "진행 중인 시험은 수정할 수 없습니다.", "수정 불가", JOptionPane.WARNING_MESSAGE);
                            break;
                        }
                        if (now.isAfter(exam.getEndDate())) {
                            JOptionPane.showMessageDialog(button, "종료된 시험은 수정할 수 없습니다.", "수정 불가", JOptionPane.WARNING_MESSAGE);
                            break;
                        }

                        ExamCreationContext context = new ExamCreationContext();
                        context.setExam(exam);

                        JFrame frame = new JFrame("시험 수정");
                        ExamEditorPanel editorPanel = new ExamEditorPanel(
                                context,
                                () -> frame.dispose(),
                                frame
                        );
                        frame.setContentPane(editorPanel);
                        frame.setSize(600, 400);
                        frame.setLocationRelativeTo(button);
                        frame.setVisible(true);
                    }
                    case "비활성화" -> {
                        if (!exam.isActive()) {
                            JOptionPane.showMessageDialog(button, "이미 비활성화된 시험입니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
                            break;
                        }

                        int confirm = JOptionPane.showConfirmDialog(
                                button,
                                "시험을 강제 비활성화 하시겠습니까?",
                                "비활성화 확인",
                                JOptionPane.YES_NO_OPTION
                        );

                        if (confirm == JOptionPane.YES_OPTION) {
                            try {
                                examService.disableExam(exam.getExamId()); // 실제 DB 상태 변경
                                JOptionPane.showMessageDialog(button, "시험이 비활성화되었습니다.");
                            } catch (ServiceException ex) {
                                JOptionPane.showMessageDialog(button, "비활성화 중 오류 발생: " + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                    case "응시 대상" -> {
                        try {
                            List<String> targets = examService.getAssignedDepartmentsAndGrades(exam.getExamId());
                            if (targets.isEmpty()) {
                                JOptionPane.showMessageDialog(button, "응시 대상이 지정되지 않았습니다.");
                            } else {
                                String msg = String.join("\n", targets);
                                JOptionPane.showMessageDialog(button, msg, "응시 대상", JOptionPane.INFORMATION_MESSAGE);
                            }
                        } catch (ServiceException e) {
                            JOptionPane.showMessageDialog(button, "응시 대상 조회 실패:\n" + e.getMessage());
                        }
                    }
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
