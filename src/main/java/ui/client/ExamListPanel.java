package main.java.ui.client;

import main.java.model.Exam;
import main.java.model.ExamResult; // ExamResult 모델 임포트
import main.java.model.User;
import main.java.service.ExamService;
import main.java.service.ExamServiceImpl;
import main.java.service.ServiceException;
import main.java.dao.DaoException; // DaoException 임포트 (getExamResultsByUser 호출 시 필요할 수 있음)

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList; // ArrayList 임포트
import java.util.HashMap; // HashMap 임포트
import java.util.List;
import java.util.Map; // Map 임포트

/**
 * 클라이언트용 시험 목록 패널입니다.
 * 할당된 시험 중 특정 기간 내의 시험들을 표시하고, 상태에 따라 응시/결과확인 등의 기능을 제공합니다.
 */
public class ExamListPanel extends JPanel {
    private final User user; // 현재 로그인한 사용자 정보
    private final ExamService examService = new ExamServiceImpl(); // 시험 관련 서비스
    private JTable table; // 시험 목록을 표시할 테이블
    // private List<Exam> examList; // 이 필드는 displayedExamList로 대체되거나 ButtonEditor 생성 시점에 전달

    public ExamListPanel(User user) {
        this.user = user;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10)); // 패널 레이아웃 설정

        // 헤더 레이블 설정
        JLabel header = new JLabel("시험 목록");
        header.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(header, BorderLayout.NORTH);

        List<Exam> allAssignedExams; // 사용자에게 할당된 모든 시험 리스트
        List<Exam> examsToDisplay = new ArrayList<>(); // 화면에 표시될 필터링된 시험 리스트

        try {
            // 사용자에게 할당된 모든 시험 정보 로드
            allAssignedExams = examService.getAssignedExams(user.getUserId());

            // 필터링 로직: 현재 날짜가 각 시험의 (시작일-1주) ~ (종료일+1주) 범위 내에 있는 시험만 선택
            LocalDateTime now = LocalDateTime.now();
            for (Exam exam : allAssignedExams) {
                LocalDateTime periodStart = exam.getStartDate().minusWeeks(1); // 표시 기간 시작 = 시험 시작일 - 1주
                LocalDateTime periodEnd = exam.getEndDate().plusWeeks(1);     // 표시 기간 끝 = 시험 종료일 + 1주

                // 현재 날짜가 계산된 표시 기간 내에 있는지 확인
                if (!now.isBefore(periodStart) && !now.isAfter(periodEnd)) {
                    examsToDisplay.add(exam);
                }
            }
        } catch (ServiceException e) {
            JOptionPane.showMessageDialog(this, "시험 목록을 불러오는 중 오류가 발생했습니다: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            // 오류 발생 시 빈 리스트로 진행
        }

        // 사용자 시험 결과 정보 로드
        Map<Integer, ExamResult> userResults = new HashMap<>();
        try {
            userResults = examService.getExamResultsByUser(user.getUserId());
        } catch (ServiceException | DaoException e) { // DaoException도 처리
            JOptionPane.showMessageDialog(this, "사용자 시험 결과 정보를 가져오는 중 오류가 발생했습니다: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }

        // 테이블 모델 및 컬럼명 정의
        String[] columns = {"시험 ID", "과목명", "시험 시작일", "시험 종료일", "제한 시간(분)", "상태/응시"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // "상태/응시" 버튼 컬럼만 편집 가능하도록 설정
            }
        };

        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"); // 날짜 표시 형식
        LocalDateTime nowForStatus = LocalDateTime.now(); // 버튼 상태 결정을 위한 현재 시간

        // 필터링된 시험 목록(examsToDisplay)을 사용하여 테이블 데이터 채우기
        for (Exam exam : examsToDisplay) {
            String actionButtonText; // 버튼에 표시될 텍스트
            boolean alreadyTaken = userResults.containsKey(exam.getExamId()); // 해당 시험 응시 여부

            if (alreadyTaken) {
                actionButtonText = "결과 확인";
            } else if (nowForStatus.isBefore(exam.getStartDate())) {
                actionButtonText = "응시 불가 (예정)";
            } else if (nowForStatus.isAfter(exam.getEndDate())) {
                actionButtonText = "기간 종료";
            } else {
                actionButtonText = "응시 하기";
            }

            model.addRow(new Object[]{
                    exam.getExamId(),
                    exam.getSubject(),
                    exam.getStartDate().format(df),
                    exam.getEndDate().format(df),
                    exam.getDurationMinutes(),
                    actionButtonText // "상태/응시" 컬럼에 동적 텍스트 설정
            });
        }

        table = new JTable(model); // 테이블 생성
        table.setRowHeight(30); // 행 높이 설정
        table.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 14)); // 헤더 폰트
        table.setFont(new Font("맑은 고딕", Font.PLAIN, 13)); // 셀 폰트

        // 버튼 컬럼 렌더러 및 에디터 설정
        TableColumn btnColumn = table.getColumn("상태/응시");
        btnColumn.setCellRenderer(new ButtonRenderer());
        // ButtonEditor에는 필터링된 examsToDisplay 목록과 userResults를 전달
        btnColumn.setCellEditor(new ButtonEditor(new JCheckBox(), examsToDisplay, userResults));

        add(new JScrollPane(table), BorderLayout.CENTER); // 테이블을 스크롤 패널에 추가하여 패널 중앙에 배치
    }

    /**
     * 테이블 셀에 버튼을 그리기 위한 렌더러입니다.
     */
    private static class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true); // 버튼 배경 표시 설정
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString()); // 버튼 텍스트 설정
            return this;
        }
    }

    /**
     * 테이블 셀의 버튼 클릭 이벤트를 처리하기 위한 에디터입니다.
     */
    private class ButtonEditor extends DefaultCellEditor {
        private final JButton button; // 실제 버튼 컴포넌트
        private String currentButtonText; // 현재 버튼에 표시된 텍스트
        private boolean isPushed; // 버튼 클릭 상태 플래그
        private int selectedRow; // 현재 선택된 행
        private final List<Exam> displayedExamList; // 현재 테이블에 표시된 시험 목록
        private final Map<Integer, ExamResult> currentUserResults; // 현재 사용자의 시험 결과

        public ButtonEditor(JCheckBox checkBox, List<Exam> displayedExamList, Map<Integer, ExamResult> userResults) {
            super(checkBox);
            this.displayedExamList = displayedExamList;
            this.currentUserResults = userResults;
            this.button = new JButton();
            this.button.setOpaque(true);
            this.button.addActionListener(e -> {
                // 버튼 클릭 시 JTable 셀 편집을 중단하고 getCellEditorValue()가 호출되도록 함
                fireEditingStopped();
            });
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected, int row, int column) {
            this.currentButtonText = (value == null) ? "" : value.toString(); // 모델로부터 버튼 텍스트 가져오기
            this.selectedRow = row;

            Exam exam = displayedExamList.get(selectedRow); // 현재 행의 시험 객체
            LocalDateTime now = LocalDateTime.now();
            boolean buttonShouldBeEnabled = false; // 버튼 활성화 여부

            // 시험 상태에 따라 버튼 활성화 결정
            if (currentUserResults.containsKey(exam.getExamId())) { // 이미 응시한 경우
                buttonShouldBeEnabled = true; // "결과 확인" 버튼은 활성화
            } else if (now.isBefore(exam.getStartDate())) { // 시험 시작 전 (예정)
                buttonShouldBeEnabled = false;
            } else if (now.isAfter(exam.getEndDate())) { // 시험 기간 종료
                buttonShouldBeEnabled = false;
            } else { // 현재 응시 가능 기간
                buttonShouldBeEnabled = true;
            }

            button.setText(this.currentButtonText);
            button.setEnabled(buttonShouldBeEnabled); // 계산된 상태에 따라 버튼 활성화/비활성화
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                Exam exam = displayedExamList.get(selectedRow);

                if (button.isEnabled()) {
                    if ("응시 하기".equals(currentButtonText)) {
                        if (currentUserResults.containsKey(exam.getExamId())) {
                            JOptionPane.showMessageDialog(button, // 수정: ExamListPanel.this.button -> button
                                    "이미 응시한 시험입니다. 재응시할 수 없습니다.",
                                    "알림", JOptionPane.INFORMATION_MESSAGE);
                            return currentButtonText;
                        }

                        String subject = exam.getSubject();
                        int examId = exam.getExamId();
                        LocalDateTime endTime = LocalDateTime.now().plusMinutes(exam.getDurationMinutes());
                        try {
                            ExamTakingPanel takingPanel = new ExamTakingPanel(user, examId, subject, endTime);
                            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(button); // 수정: ExamListPanel.this.button -> button (JOptionPane과 동일한 이유)

                            if (frame instanceof ClientMainFrame) {
                                frame.setContentPane(takingPanel);
                                frame.revalidate();
                                frame.repaint();
                            } else {
                                JFrame examFrame = new JFrame("시험 응시: " + subject);
                                examFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                                examFrame.setContentPane(takingPanel);
                                examFrame.setSize(800, 600);
                                examFrame.setLocationRelativeTo(null);
                                examFrame.setVisible(true);
                            }
                        } catch (ServiceException ex) {
                            JOptionPane.showMessageDialog(button, // 수정: ExamListPanel.this.button -> button
                                    "시험 데이터 로딩 중 오류가 발생했습니다:\n" + ex.getMessage(),
                                    "오류", JOptionPane.ERROR_MESSAGE);
                        }
                    } else if ("결과 확인".equals(currentButtonText)) {
                        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(button); // 수정: ExamListPanel.this.button -> button
                        if (frame instanceof ClientMainFrame) {
                            ((ClientMainFrame) frame).switchToResultsPanel(); // 이 부분은 ClientMainFrame에 메서드가 있어야 함
                        } else {
                            ExamResult result = currentUserResults.get(exam.getExamId());
                            if (result != null) {
                                JOptionPane.showMessageDialog(button, // 수정: ExamListPanel.this.button -> button
                                        "과목: " + exam.getSubject() + "\n점수: " + result.getScore() + "/100" +
                                                "\n응시일: " + (result.getCompletedAt() != null ? result.getCompletedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "-"),
                                        "시험 결과", JOptionPane.INFORMATION_MESSAGE);
                            }
                        }
                    }
                }
            }
            isPushed = false;
            return currentButtonText;
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false; // 셀 편집 중단 시 플래그 리셋
            return super.stopCellEditing();
        }

        // fireEditingStopped()는 DefaultCellEditor의 것을 사용하므로 별도 오버라이드 필요 없음
    }
}