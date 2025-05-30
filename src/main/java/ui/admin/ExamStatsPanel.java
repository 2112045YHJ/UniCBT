package main.java.ui.admin;

import main.java.dto.ExamOverallStatsDto; // DTO 임포트
import main.java.model.Exam; // Exam 모델 임포트
import main.java.model.User; // User 모델 임포트
import main.java.service.ExamService;
import main.java.service.ExamServiceImpl;
import main.java.service.ServiceException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.time.format.DateTimeFormatter; // 날짜/시간 포맷
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.text.DecimalFormat; // 숫자 포맷 (평균 점수)

/**
 * 관리자용 시험 결과 통계 패널입니다.
 * 연도별로 시험 목록과 응시자 수, 평균 점수 등을 표시하고 상세 통계 보기 기능을 제공합니다.
 */
public class ExamStatsPanel extends JPanel {

    private final ExamService examService = new ExamServiceImpl();
    private final User adminUser; // 현재 로그인한 관리자 정보
    private final AdminMainFrame mainFrame; // 화면 전환을 위해 AdminMainFrame 참조

    public ExamStatsPanel(User adminUser, AdminMainFrame mainFrame) {
        this.adminUser = adminUser;
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(10, 10));
        initComponents();
    }

    private void initComponents() {
        // 헤더 레이블 설정
        JLabel headerLabel = new JLabel("시험 결과 통계");
        headerLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(headerLabel, BorderLayout.NORTH);

        JTabbedPane yearTabbedPane = new JTabbedPane(); // 연도별 탭

        try {
            // 서비스로부터 모든 시험의 전체 통계 요약 정보 가져오기
            List<ExamOverallStatsDto> allStatsSummaries = examService.getAllExamOverallStats();

            // 연도별로 DTO 그룹핑
            Map<Integer, List<ExamOverallStatsDto>> summariesByYear = new TreeMap<>(Comparator.reverseOrder()); // 최신 연도부터
            for (ExamOverallStatsDto summary : allStatsSummaries) {
                int year = summary.getExam().getStartDate().getYear();
                summariesByYear.computeIfAbsent(year, k -> new ArrayList<>()).add(summary);
            }

            if (summariesByYear.isEmpty()) {
                JPanel emptyPanel = new JPanel(new GridBagLayout());
                emptyPanel.add(new JLabel("조회할 시험 결과 통계가 없습니다."));
                yearTabbedPane.addTab("정보 없음", emptyPanel);
            } else {
                for (Map.Entry<Integer, List<ExamOverallStatsDto>> entry : summariesByYear.entrySet()) {
                    int year = entry.getKey();
                    List<ExamOverallStatsDto> yearSummaries = entry.getValue();
                    // 각 연도별 시험 통계 목록을 표시할 테이블 패널 생성
                    yearTabbedPane.addTab(year + "년", createYearlyStatsTablePanel(yearSummaries));
                }
            }
        } catch (ServiceException e) {
            JPanel errorPanel = new JPanel(new GridBagLayout());
            errorPanel.add(new JLabel("시험 결과 통계 조회 중 오류가 발생했습니다: " + e.getMessage()));
            yearTabbedPane.addTab("오류", errorPanel);
            // e.printStackTrace(); // 개발 중 스택 트레이스 확인
        }

        add(yearTabbedPane, BorderLayout.CENTER);
    }

    /**
     * 특정 연도의 시험 통계 요약 목록을 받아 테이블 패널을 생성합니다.
     * @param summaries 해당 연도의 시험 통계 요약 DTO 목록
     * @return 시험 통계 목록 테이블이 포함된 JPanel
     */
    private JPanel createYearlyStatsTablePanel(List<ExamOverallStatsDto> summaries) {
        JPanel tablePanel = new JPanel(new BorderLayout());
        String[] columnNames = {"과목명", "시험 기간", "응시 현황", "평균 점수", "시험 상태", "상세 통계"};

        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // "상세 통계" 버튼 컬럼만 편집 가능
            }
        };

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd HH:mm"); // 날짜/시간 포맷
        DecimalFormat scoreDf = new DecimalFormat("#.##"); // 평균 점수 포맷 (소수점 둘째 자리)

        for (ExamOverallStatsDto summary : summaries) {
            Exam exam = summary.getExam();
            String examPeriod = exam.getStartDate().format(dtf) + " ~ " + exam.getEndDate().format(dtf);
            // 응시 현황: 실제 응시자 수 / 총 배정자 수 (또는 응시 가능자 수)
            String participationStatus = summary.getTotalParticipants() + " / " + summary.getTotalAssignedOrEligible() + " 명";
            String averageScoreFormatted = scoreDf.format(summary.getAverageScore());

            tableModel.addRow(new Object[]{
                    exam.getSubject(),
                    examPeriod,
                    participationStatus,
                    averageScoreFormatted + " 점",
                    summary.getCurrentExamStatus(),
                    "자세히" // 버튼 텍스트
            });
        }

        JTable statsTable = new JTable(tableModel);
        statsTable.setRowHeight(28);
        statsTable.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 14));
        statsTable.setFont(new Font("맑은 고딕", Font.PLAIN, 13));

        // "상세 통계" 버튼 컬럼 설정
        TableColumn detailButtonColumn = statsTable.getColumn("상세 통계");
        detailButtonColumn.setCellRenderer(new StatsButtonRenderer());
        // ButtonEditor에 summary 리스트를 전달
        detailButtonColumn.setCellEditor(new StatsDetailButtonEditor(new JCheckBox(), summaries));

        tablePanel.add(new JScrollPane(statsTable), BorderLayout.CENTER);
        return tablePanel;
    }

    /**
     * 테이블 셀에 버튼을 그리기 위한 렌더러입니다.
     */
    private static class StatsButtonRenderer extends JButton implements TableCellRenderer {
        public StatsButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    /**
     * "상세 통계" 버튼 클릭 이벤트를 처리하는 에디터입니다.
     */
    private class StatsDetailButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private String label;
        private boolean isPushed;
        private int selectedRow;
        private final List<ExamOverallStatsDto> summaryList; // 현재 탭에 표시된 DTO 목록

        public StatsDetailButtonEditor(JCheckBox checkBox, List<ExamOverallStatsDto> summaryList) {
            super(checkBox);
            this.summaryList = summaryList;
            this.button = new JButton();
            this.button.setOpaque(true);
            this.button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.label = (value == null) ? "" : value.toString();
            this.selectedRow = row;
            button.setText(label);
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                ExamOverallStatsDto selectedSummary = summaryList.get(selectedRow);
                Exam selectedExam = selectedSummary.getExam();

                // AdminMainFrame의 화면 전환 메서드 호출
                if (mainFrame != null) {
                    // 상세 통계 패널로 전환
                    mainFrame.showExamSpecificStatsScreen(selectedExam.getExamId(), selectedExam.getSubject());
                } else {
                    JOptionPane.showMessageDialog(button, "상세 통계 화면으로 이동할 수 없습니다. (mainFrame 참조 오류)");
                }
            }
            isPushed = false;
            return label;
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }
}