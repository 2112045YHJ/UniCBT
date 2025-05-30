package main.java.ui.admin;

import main.java.dto.ExamProgressSummaryDto;
import main.java.model.Exam; // Exam 모델 임포트
import main.java.model.User; // User 모델 임포트 (AdminMainFrame에서 User 객체를 받을 수 있음)
import main.java.service.ExamService;
import main.java.service.ExamServiceImpl;
import main.java.service.ServiceException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.time.format.DateTimeFormatter; // 날짜 포맷팅
import java.util.ArrayList;
import java.util.Comparator; // 정렬
import java.util.List;
import java.util.Map;
import java.util.TreeMap;    // 연도별 정렬을 위해 TreeMap 사용

/**
 * 관리자용 시험 진행 관리 패널입니다.
 * 연도별로 시험 목록과 응시 현황, 시험 상태 등을 표시하고 상세 보기 기능을 제공합니다.
 */
public class ExamProgressPanel extends JPanel {

    private final ExamService examService = new ExamServiceImpl();
    private final User adminUser; // 현재 로그인한 관리자 정보 (필요시 AdminMainFrame에서 전달받음)
    private final AdminMainFrame mainFrame; // 화면 전환을 위해 AdminMainFrame 참조

    public ExamProgressPanel(User adminUser, AdminMainFrame mainFrame) {
        this.adminUser = adminUser;
        this.mainFrame = mainFrame; // AdminMainFrame 참조 저장
        setLayout(new BorderLayout(10, 10));
        initComponents();
    }

    private void initComponents() {
        // 헤더 레이블 설정
        JLabel headerLabel = new JLabel("시험 진행 현황");
        headerLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(headerLabel, BorderLayout.NORTH);

        JTabbedPane yearTabbedPane = new JTabbedPane(); // 연도별 탭

        try {
            List<ExamProgressSummaryDto> summaries = examService.getAllExamProgressSummaries();

            // 연도별로 DTO 그룹핑
            Map<Integer, List<ExamProgressSummaryDto>> summariesByYear = new TreeMap<>(Comparator.reverseOrder()); // 최신 연도부터
            for (ExamProgressSummaryDto summary : summaries) {
                int year = summary.getExam().getStartDate().getYear();
                summariesByYear.computeIfAbsent(year, k -> new ArrayList<>()).add(summary);
            }

            if (summariesByYear.isEmpty()) {
                JPanel emptyPanel = new JPanel(new GridBagLayout());
                emptyPanel.add(new JLabel("조회할 시험 진행 현황이 없습니다."));
                yearTabbedPane.addTab("정보 없음", emptyPanel);
            } else {
                for (Map.Entry<Integer, List<ExamProgressSummaryDto>> entry : summariesByYear.entrySet()) {
                    int year = entry.getKey();
                    List<ExamProgressSummaryDto> yearSummaries = entry.getValue();
                    // 각 연도별 시험 목록을 표시할 테이블 패널 생성
                    yearTabbedPane.addTab(year + "년", createYearlyExamTablePanel(yearSummaries));
                }
            }
        } catch (ServiceException e) {
            JPanel errorPanel = new JPanel(new GridBagLayout());
            errorPanel.add(new JLabel("시험 진행 현황 조회 중 오류가 발생했습니다: " + e.getMessage()));
            yearTabbedPane.addTab("오류", errorPanel);
            e.printStackTrace(); // 개발 중에는 스택 트레이스 출력
        }

        add(yearTabbedPane, BorderLayout.CENTER);
    }

    /**
     * 특정 연도의 시험 진행 상황 요약 목록을 받아 테이블 패널을 생성합니다.
     * @param summaries 해당 연도의 시험 진행 상황 요약 DTO 목록
     * @return 시험 목록 테이블이 포함된 JPanel
     */
    private JPanel createYearlyExamTablePanel(List<ExamProgressSummaryDto> summaries) {
        JPanel tablePanel = new JPanel(new BorderLayout());
        String[] columnNames = {"과목명", "시험 기간", "응시 현황", "시험 상태", "상세보기"};

        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 4; // "상세보기" 버튼 컬럼만 편집 가능
            }
        };

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd HH:mm");

        for (ExamProgressSummaryDto summary : summaries) {
            Exam exam = summary.getExam();
            String examPeriod = exam.getStartDate().format(dtf) + " ~ " + exam.getEndDate().format(dtf);
            String progressStatus = summary.getCompletedStudents() + " / " + summary.getTotalAssignedStudents() + " 명";

            tableModel.addRow(new Object[]{
                    exam.getSubject(),
                    examPeriod,
                    progressStatus,
                    summary.getCurrentExamStatus(),
                    "자세히" // 버튼 텍스트
            });
        }

        JTable examTable = new JTable(tableModel);
        examTable.setRowHeight(28);
        examTable.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 14));
        examTable.setFont(new Font("맑은 고딕", Font.PLAIN, 13));

        // "상세보기" 버튼 컬럼 설정
        TableColumn detailButtonColumn = examTable.getColumn("상세보기");
        detailButtonColumn.setCellRenderer(new ButtonRenderer());
        // ButtonEditor에 summary 리스트를 전달하여 클릭 시 해당 Exam 객체에 접근 가능하도록 함
        detailButtonColumn.setCellEditor(new DetailButtonEditor(new JCheckBox(), summaries));

        tablePanel.add(new JScrollPane(examTable), BorderLayout.CENTER);
        return tablePanel;
    }

    /**
     * 테이블 셀에 버튼을 그리기 위한 렌더러입니다.
     */
    private static class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    /**
     * "상세보기" 버튼 클릭 이벤트를 처리하는 에디터입니다.
     */
    private class DetailButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private String label;
        private boolean isPushed;
        private int selectedRow;
        private final List<ExamProgressSummaryDto> summaryList; // 현재 탭에 표시된 DTO 목록

        public DetailButtonEditor(JCheckBox checkBox, List<ExamProgressSummaryDto> summaryList) {
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
                ExamProgressSummaryDto selectedSummary = summaryList.get(selectedRow);
                Exam selectedExam = selectedSummary.getExam();

                // AdminMainFrame의 화면 전환 메서드 호출
                if (mainFrame != null) {
                    // 상세 패널로 전환하기 전에, 해당 패널이 AdminMainFrame에 등록되어 있어야 함
                    // 예: mainFrame.addScreen("ExamProgressDetail_" + selectedExam.getExamId(), new ExamProgressDetailPanel(adminUser, selectedExam, mainFrame));
                    // 위와 같이 동적으로 추가하거나, 미리 키를 정해두고 해당 키로 상세 패널을 보여줌
                    mainFrame.showExamProgressDetailScreen(selectedExam.getExamId(), selectedExam.getSubject());
                } else {
                    JOptionPane.showMessageDialog(button, "상세보기 화면으로 이동할 수 없습니다. (mainFrame 참조 오류)");
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