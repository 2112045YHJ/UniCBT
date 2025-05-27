package main.java.ui.client;

import main.java.model.Exam;
import main.java.model.User;
import main.java.service.ExamService;
import main.java.service.ExamServiceImpl;
import main.java.service.ServiceException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 클라이언트용 시험 목록 패널
 * - 응시 가능한 시험을 테이블로 보여주고, 각 행의 "응시" 버튼으로 시험 응시를 시작합니다.
 */
public class ExamListPanel extends JPanel {
    private final User user;
    private final ExamService examService = new ExamServiceImpl();
    private JTable table;
    private List<Exam> examList;

    public ExamListPanel(User user) {
        this.user = user;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // 헤더
        JLabel header = new JLabel("응시 가능한 시험 목록");
        header.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(header, BorderLayout.NORTH);

        // 데이터 로드
        try {
            examList = examService.getOpenExams(user.getDpmtId(), user.getGrade());
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }

        // 테이블 모델 생성
        String[] columns = {"시험 ID", "과목명", "시작일", "종료일", "제한 시간(분)", "응시"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5;
            }
        };

        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (Exam exam : examList) {
            model.addRow(new Object[]{
                    exam.getExamId(),
                    exam.getSubject(),
                    exam.getStartDate().format(df),
                    exam.getEndDate().format(df),
                    exam.getDurationMinutes(),
                    get응시()
            });
        }

        // 테이블 생성
        table = new JTable(model);
        table.setRowHeight(30);

        // 버튼 컬럼 렌더러/에디터 설정
        TableColumn btnColumn = table.getColumn("응시");
        btnColumn.setCellRenderer(new ButtonRenderer());
        btnColumn.setCellEditor(new ButtonEditor(new JCheckBox()));

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private static String get응시() {
        return "응시";
    }

    // 버튼 렌더러
    private static class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value == null ? "" : value.toString());
            return this;
        }
    }

    // 버튼 에디터
    private class ButtonEditor extends DefaultCellEditor {
        private final JButton button = new JButton();
        private String label;
        private boolean isPushed;
        private int selectedRow;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button.setOpaque(true);
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fireEditingStopped();
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected, int row, int column) {
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            isPushed = true;
            selectedRow = row;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            try {
                if (isPushed) {
                    Exam exam = examList.get(selectedRow);
                    try {
                        // "이미 응시했는지" 검사
                        if (examService.hasUserTakenExam(user.getUserId(), exam.getExamId())) {
                            JOptionPane.showMessageDialog(button,
                                    "이미 응시한 시험입니다. 재응시할 수 없습니다.",
                                    "알림", JOptionPane.INFORMATION_MESSAGE);
                            return label; // 바로 리턴!
                        }

                        // 아래는 응시 가능한 경우에만 실행됨!
                        String subject = exam.getSubject();
                        int examId = exam.getExamId();
                        LocalDateTime endTime = LocalDateTime.now().plusMinutes(exam.getDurationMinutes());
                        ExamTakingPanel takingPanel = new ExamTakingPanel(user, examId, subject, endTime);

                        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(button);
                        frame.setContentPane(takingPanel);
                        frame.revalidate();
                        frame.repaint();

                    } catch (ServiceException ex) {
                        JOptionPane.showMessageDialog(button,
                                "시험 데이터 로딩 중 오류가 발생했습니다:\n" + ex.getMessage(),
                                "오류", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } finally {
                isPushed = false; // 반드시 finally에서 false 처리
            }
            return label;
        }


        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }

        @Override
        protected void fireEditingStopped() {
            super.fireEditingStopped();
        }
    }
}