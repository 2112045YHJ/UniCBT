package main.java.ui.admin;

import main.java.dto.DepartmentExamStatDto;
import main.java.dto.StudentScoreDetailDto;
import main.java.model.Department;
import main.java.model.QuestionFull;
import main.java.model.QuestionOption;
import main.java.model.QuestionStat;
import main.java.model.User;
import main.java.service.DepartmentService;
import main.java.service.DepartmentServiceImpl;
import main.java.service.ExamService;
import main.java.service.ExamServiceImpl;
import main.java.service.QuestionService;
import main.java.service.QuestionServiceImpl;
import main.java.service.ServiceException;
import main.java.ui.admin.dialog.QuestionDetailDialog;
import main.java.ui.admin.dialog.StudentAnswerSheetDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * 특정 시험에 대한 상세 결과 통계(학생별 성적, 학과별 통계, 문제별 통계 등)를 표시하는 패널입니다.
 */
public class ExamSpecificStatsPanel extends JPanel {
    private final User adminUser;
    private final int examId;
    private final String examSubject;
    private final AdminMainFrame mainFrame;
    private final ExamService examService = new ExamServiceImpl();
    private final DepartmentService departmentService = new DepartmentServiceImpl();
    private final QuestionService questionService = new QuestionServiceImpl();

    // 학생별 성적 관련 UI
    private JTable studentScoresTable;
    private DefaultTableModel studentScoresTableModel;
    private TableRowSorter<DefaultTableModel> studentScoresSorter;
    private JComboBox<DepartmentItem> departmentFilterComboBox;
    private List<StudentScoreDetailDto> allStudentScoreDetails = new ArrayList<>();

    // 학과별 통계 관련 UI
    private JTable departmentalStatsTable;
    private DefaultTableModel departmentalStatsTableModel;
    private TableRowSorter<DefaultTableModel> departmentalStatsSorter;

    // 문제별 통계 관련 UI
    private JTable questionStatsTable;
    private DefaultTableModel questionStatsTableModel;
    private TableRowSorter<DefaultTableModel> questionStatsSorter;
    private List<QuestionStat> currentQuestionStats = new ArrayList<>();

    // 숫자 포맷터
    private final DecimalFormat scoreFormat = new DecimalFormat("#.##점");
    private final DecimalFormat rankFormat = new DecimalFormat("#등");
    private final DecimalFormat rateFormat = new DecimalFormat("0.##%");
    private final DecimalFormat countFormat = new DecimalFormat("#명");


    public ExamSpecificStatsPanel(User adminUser, int examId, String examSubject, AdminMainFrame mainFrame) {
        this.adminUser = adminUser;
        this.examId = examId;
        this.examSubject = examSubject;
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initComponents();
        loadAllStatsData(); // 패널 생성 시 모든 통계 데이터 로드
    }

    /**
     * 패널의 주요 UI 컴포넌트를 초기화하고 배치합니다.
     */
    private void initComponents() {
        JLabel headerLabel = new JLabel("시험 상세 통계: " + this.examSubject, SwingConstants.CENTER);
        headerLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        add(headerLabel, BorderLayout.NORTH);

        JTabbedPane statsTabbedPane = new JTabbedPane();
        statsTabbedPane.setFont(new Font("맑은 고딕", Font.PLAIN, 13));

        statsTabbedPane.addTab("학생별 성적", createStudentScoresPanel());
        statsTabbedPane.addTab("학과별 통계", createDepartmentalStatsPanel()); // 학과별 통계 탭 추가
        statsTabbedPane.addTab("문제별 통계", createQuestionStatsPanel());

        add(statsTabbedPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton backButton = new JButton("전체 통계 목록으로");
        backButton.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        backButton.addActionListener(e -> {
            if (mainFrame != null) {
                mainFrame.cardLayout.show(mainFrame.contentPanel, "Stats"); // "Stats"는 ExamStatsPanel의 키
            }
        });
        bottomPanel.add(backButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    // --- 학생별 성적 탭 관련 메서드들 ---
    // (createStudentScoresPanel, setNumericSortersForStudentTable, populateDepartmentFilter, filterAndDisplayStudentScores 는 이전과 동일)
    private JPanel createStudentScoresPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        departmentFilterComboBox = new JComboBox<>();
        departmentFilterComboBox.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        departmentFilterComboBox.addItem(new DepartmentItem(0, "전체 학과"));

        departmentFilterComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                filterAndDisplayStudentScores();
            }
        });

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("학과 필터: "));
        filterPanel.add(departmentFilterComboBox);
        panel.add(filterPanel, BorderLayout.NORTH);


        String[] studentColumns = {"석차", "학번", "이름", "학과", "점수", "답안지 확인"};
        studentScoresTableModel = new DefaultTableModel(studentColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5;
            }
        };
        studentScoresTable = new JTable(studentScoresTableModel);
        studentScoresTable.setRowHeight(28);
        studentScoresTable.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 14));
        studentScoresTable.setFont(new Font("맑은 고딕", Font.PLAIN, 13));

        studentScoresSorter = new TableRowSorter<>(studentScoresTableModel);
        studentScoresTable.setRowSorter(studentScoresSorter);
        setNumericSortersForStudentTable();


        TableColumn answerSheetButtonColumn = studentScoresTable.getColumn("답안지 확인");
        answerSheetButtonColumn.setCellRenderer(new StatsTableButtonRenderer());
        answerSheetButtonColumn.setCellEditor(new StudentAnswerSheetButtonEditor(new JCheckBox()));
        answerSheetButtonColumn.setPreferredWidth(120);
        studentScoresTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        studentScoresTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        studentScoresTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        studentScoresTable.getColumnModel().getColumn(4).setPreferredWidth(80);


        panel.add(new JScrollPane(studentScoresTable), BorderLayout.CENTER);
        return panel;
    }

    private void setNumericSortersForStudentTable() {
        studentScoresSorter.setComparator(0, (Object o1, Object o2) -> {
            try {
                Integer rank1 = Integer.parseInt(o1.toString().replace("등", ""));
                Integer rank2 = Integer.parseInt(o2.toString().replace("등", ""));
                return rank1.compareTo(rank2);
            } catch (NumberFormatException e) {
                return o1.toString().compareTo(o2.toString());
            }
        });

        studentScoresSorter.setComparator(4, (Object o1, Object o2) -> {
            try {
                Double score1 = Double.parseDouble(o1.toString().replace("점", ""));
                Double score2 = Double.parseDouble(o2.toString().replace("점", ""));
                return score1.compareTo(score2);
            } catch (NumberFormatException e) {
                return o1.toString().compareTo(o2.toString());
            }
        });
    }


    private void populateDepartmentFilter() {
        departmentFilterComboBox.removeAllItems();
        departmentFilterComboBox.addItem(new DepartmentItem(0, "전체 학과"));

        try {
            List<Department> departments = departmentService.getAllDepartments();
            // 응시자가 있는 학과만 필터에 추가하려면 allStudentScoreDetails를 사용
            // 여기서는 모든 학과를 추가
            departments.sort(Comparator.comparing(Department::getDpmtName));
            for (Department dept : departments) {
                departmentFilterComboBox.addItem(new DepartmentItem(dept.getDpmtId(), dept.getDpmtName()));
            }
        } catch (ServiceException e) {
            System.err.println("학과 필터 목록 로드 중 오류: " + e.getMessage());
        }
    }

    private void filterAndDisplayStudentScores() {
        studentScoresTableModel.setRowCount(0);
        DepartmentItem selectedDeptItem = (DepartmentItem) departmentFilterComboBox.getSelectedItem();

        List<StudentScoreDetailDto> listToDisplay;

        if (selectedDeptItem == null || selectedDeptItem.getId() == 0) { // "전체 학과" 선택 시
            listToDisplay = new ArrayList<>(allStudentScoreDetails);
            // 전체 석차는 allStudentScoreDetails에 이미 계산되어 있다고 가정 (loadStudentScoreDetails에서 처리)
            // 여기서는 표시만 함
            for(StudentScoreDetailDto dto : listToDisplay){ // 원본 석차 사용
                studentScoresTableModel.addRow(new Object[]{
                        rankFormat.format(dto.getRank()),
                        dto.getStudentNumber(),
                        dto.getStudentName(),
                        dto.getDepartmentName(),
                        scoreFormat.format(dto.getScore()),
                        "답안 확인"
                });
            }

        } else {
            final String selectedDeptName = selectedDeptItem.getName();
            listToDisplay = allStudentScoreDetails.stream()
                    .filter(dto -> selectedDeptName.equals(dto.getDepartmentName()))
                    .collect(Collectors.toList());

            // 선택된 학과 내에서 석차 재계산
            listToDisplay.sort(Comparator.comparingInt(StudentScoreDetailDto::getScore).reversed()
                    .thenComparing(StudentScoreDetailDto::getStudentNumber));

            int rank = 0;
            int lastScore = -1;
            int sameRankCount = 0;
            for(StudentScoreDetailDto dto : listToDisplay){
                if (dto.getScore() != lastScore) {
                    rank += (sameRankCount + 1);
                    sameRankCount = 0;
                    lastScore = dto.getScore();
                } else {
                    sameRankCount++;
                }
                // 이 석차는 필터링된 목록 내에서의 석차임
                studentScoresTableModel.addRow(new Object[]{
                        rankFormat.format(rank),
                        dto.getStudentNumber(),
                        dto.getStudentName(),
                        dto.getDepartmentName(),
                        scoreFormat.format(dto.getScore()),
                        "답안 확인"
                });
            }
        }
    }


    // --- 학과별 통계 탭 생성 메서드 ---
    /**
     * 학과별 통계 정보를 표시할 패널을 생성합니다.
     */
    private JPanel createDepartmentalStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        String[] deptColumns = {"학과명", "응시자 수", "평균 점수", "최고 점수", "최저 점수"};
        departmentalStatsTableModel = new DefaultTableModel(deptColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 모든 셀 편집 불가
            }
        };
        departmentalStatsTable = new JTable(departmentalStatsTableModel);
        departmentalStatsTable.setRowHeight(28);
        departmentalStatsTable.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 14));
        departmentalStatsTable.setFont(new Font("맑은 고딕", Font.PLAIN, 13));

        departmentalStatsSorter = new TableRowSorter<>(departmentalStatsTableModel);
        departmentalStatsTable.setRowSorter(departmentalStatsSorter);
        setNumericSortersForDepartmentalTable(); // 숫자 컬럼 정렬자 설정

        // 컬럼 너비 설정
        departmentalStatsTable.getColumnModel().getColumn(0).setPreferredWidth(250); // 학과명
        departmentalStatsTable.getColumnModel().getColumn(1).setPreferredWidth(100); // 응시자 수
        departmentalStatsTable.getColumnModel().getColumn(2).setPreferredWidth(120); // 평균 점수
        departmentalStatsTable.getColumnModel().getColumn(3).setPreferredWidth(100); // 최고 점수
        departmentalStatsTable.getColumnModel().getColumn(4).setPreferredWidth(100); // 최저 점수

        panel.add(new JScrollPane(departmentalStatsTable), BorderLayout.CENTER);
        return panel;
    }

    /**
     * 학과별 통계 테이블의 숫자 컬럼에 대한 정렬자를 설정합니다.
     */
    private void setNumericSortersForDepartmentalTable() {
        // 응시자 수 정렬
        departmentalStatsSorter.setComparator(1, Comparator.comparingInt(o -> Integer.parseInt(o.toString().replace("명", "").trim())));
        // 평균 점수 정렬
        departmentalStatsSorter.setComparator(2, Comparator.comparingDouble(o -> Double.parseDouble(o.toString().replace("점", "").trim())));
        // 최고 점수 정렬
        departmentalStatsSorter.setComparator(3, Comparator.comparingInt(o -> Integer.parseInt(o.toString().replace("점", "").trim())));
        // 최저 점수 정렬
        departmentalStatsSorter.setComparator(4, Comparator.comparingInt(o -> Integer.parseInt(o.toString().replace("점", "").trim())));
    }


    // --- 문제별 통계 탭 관련 메서드들 ---
    // (createQuestionStatsPanel, setNumericSortersForQuestionTable - 필요시, loadQuestionStats는 이전과 동일)
    private JPanel createQuestionStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5,5));
        String[] questionColumns = {"문제ID(순번)", "문제 유형", "정답률", "문제 확인"};
        questionStatsTableModel = new DefaultTableModel(questionColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3;
            }
        };
        questionStatsTable = new JTable(questionStatsTableModel);
        questionStatsTable.setRowHeight(28);
        questionStatsTable.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 14));
        questionStatsTable.setFont(new Font("맑은 고딕", Font.PLAIN, 13));

        questionStatsSorter = new TableRowSorter<>(questionStatsTableModel);
        questionStatsTable.setRowSorter(questionStatsSorter);

        questionStatsSorter.setComparator(2, (Object o1, Object o2) -> { // 정답률 컬럼 정렬
            try {
                // "%" 문자 제거 후 Float으로 변환하여 비교
                Float rate1 = Float.parseFloat(o1.toString().replaceAll("[^\\d.]", ""));
                Float rate2 = Float.parseFloat(o2.toString().replaceAll("[^\\d.]", ""));
                return rate1.compareTo(rate2);
            } catch (NumberFormatException e) {
                return o1.toString().compareTo(o2.toString()); // 변환 실패 시 문자열 비교
            }
        });

        TableColumn questionDetailButtonColumn = questionStatsTable.getColumn("문제 확인");
        questionDetailButtonColumn.setCellRenderer(new StatsTableButtonRenderer());
        questionDetailButtonColumn.setCellEditor(new QuestionDetailButtonEditor(new JCheckBox()));
        questionDetailButtonColumn.setPreferredWidth(120);
        questionStatsTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        questionStatsTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        questionStatsTable.getColumnModel().getColumn(2).setPreferredWidth(100);

        panel.add(new JScrollPane(questionStatsTable), BorderLayout.CENTER);
        return panel;
    }

    /**
     * 패널 로드 시 모든 관련 통계 데이터를 가져옵니다.
     */
    private void loadAllStatsData() {
        loadStudentScoreDetails(); // 학생별 성적 먼저 로드 (학과 필터 데이터 소스)
        populateDepartmentFilter();  // 학생 성적 로드 후 학과 필터 채우기
        loadDepartmentalStats();   // 학과별 통계 데이터 로드
        loadQuestionStats();       // 문제별 통계 데이터 로드
    }

    /**
     * 학생별 성적 상세 정보를 서비스로부터 가져와 allStudentScoreDetails에 저장하고, 테이블에 표시합니다.
     */
    private void loadStudentScoreDetails() {
        allStudentScoreDetails.clear(); // 기존 데이터 초기화
        try {
            // 서비스 호출 시에는 학과 필터 ID 0 (전체)으로 호출하여 모든 학생 데이터 (전체 석차 포함) 가져옴
            allStudentScoreDetails.addAll(examService.getStudentScoreDetailsForExam(this.examId, 0));
            // 초기 표시는 전체 학과 기준으로, 전체 석차를 사용
            filterAndDisplayStudentScores();
        } catch (ServiceException e) {
            JOptionPane.showMessageDialog(this, "학생 성적 상세 정보 조회 중 오류: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 학과별 통계 정보를 서비스로부터 가져와 테이블에 표시합니다.
     */
    private void loadDepartmentalStats() {
        departmentalStatsTableModel.setRowCount(0);
        try {
            List<DepartmentExamStatDto> deptStats = examService.getDepartmentalExamStats(this.examId);
            for (DepartmentExamStatDto dto : deptStats) {
                departmentalStatsTableModel.addRow(new Object[]{
                        dto.getDepartmentName() + " (ID:" + dto.getDepartmentId() + ")", // 학과명과 ID 함께 표시
                        countFormat.format(dto.getParticipantCount()), // 응시자 수 포맷 적용
                        scoreFormat.format(dto.getAverageScore()),
                        scoreFormat.format(dto.getHighestScore()),
                        scoreFormat.format(dto.getLowestScore())
                });
            }
        } catch (ServiceException e) {
            JOptionPane.showMessageDialog(this, "학과별 통계 정보 조회 중 오류: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 문제별 통계 정보를 서비스로부터 가져와 테이블에 표시합니다.
     */
    private void loadQuestionStats() {
        questionStatsTableModel.setRowCount(0);
        this.currentQuestionStats.clear();
        try {
            List<QuestionStat> stats = examService.getQuestionStatsForExam(this.examId);
            this.currentQuestionStats.addAll(stats);

            // 문제 순서는 QuestionStat 객체에 순번 정보가 있거나, 여기서 ID 기준으로 정렬 후 매겨야 함
            // 여기서는 DB에서 가져온 순서대로 가정하고, UI에서 보기 좋게 순번을 추가
            this.currentQuestionStats.sort(Comparator.comparingInt(QuestionStat::getQuestionId)); // ID 기준 정렬 (선택적)

            for (int i = 0; i < currentQuestionStats.size(); i++) {
                QuestionStat stat = currentQuestionStats.get(i);
                questionStatsTableModel.addRow(new Object[]{
                        "Q." + (i + 1) + " (ID:" + stat.getQuestionId() + ")", // 화면 표시용 순번
                        stat.getQuestionType(),
                        rateFormat.format(stat.getCorrectRate() / 100.0), // 0-1 사이 값으로 변환 후 % 포맷
                        "문제 확인"
                });
            }
        } catch (ServiceException e) {
            JOptionPane.showMessageDialog(this, "문제별 통계 정보 조회 중 오류: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- 공통 버튼 렌더러 및 각 테이블 버튼 에디터들 ---
    // (StatsTableButtonRenderer, StudentAnswerSheetButtonEditor, QuestionDetailButtonEditor 는 이전과 동일)
    private static class StatsTableButtonRenderer extends JButton implements TableCellRenderer {
        public StatsTableButtonRenderer() { setOpaque(true); }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    private class StudentAnswerSheetButtonEditor extends DefaultCellEditor {
        // ... (이전 코드와 동일) ...
        private JButton button;
        private String label;
        private boolean isPushed;
        private int viewRow;

        public StudentAnswerSheetButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.label = (value == null) ? "" : value.toString();
            this.viewRow = row;
            button.setText(label);
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                int modelRow = studentScoresTable.convertRowIndexToModel(viewRow);
                String studentNumber = (String) studentScoresTableModel.getValueAt(modelRow, 1);

                StudentScoreDetailDto selectedDto = allStudentScoreDetails.stream()
                        .filter(dto -> dto.getStudentNumber().equals(studentNumber))
                        .findFirst().orElse(null);

                if (selectedDto != null) {
                    StudentAnswerSheetDialog dialog = new StudentAnswerSheetDialog(
                            mainFrame,
                            examId,
                            selectedDto.getUserId(),
                            selectedDto.getStudentName() + " (" + selectedDto.getStudentNumber() + ")"
                    );
                    dialog.setVisible(true);
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

    private class QuestionDetailButtonEditor extends DefaultCellEditor {
        // ... (이전 코드와 동일) ...
        private JButton button;
        private String label;
        private boolean isPushed;
        private int viewRow;

        public QuestionDetailButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.label = (value == null) ? "" : value.toString();
            this.viewRow = row;
            button.setText(label);
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                int modelRow = questionStatsTable.convertRowIndexToModel(viewRow);
                if (modelRow >= 0 && modelRow < currentQuestionStats.size()) {
                    QuestionStat stat = currentQuestionStats.get(modelRow);
                    int questionId = stat.getQuestionId();

                    QuestionDetailDialog dialog = new QuestionDetailDialog(
                            mainFrame,
                            questionId,
                            examId
                    );
                    dialog.setVisible(true);
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


    /**
     * 학과 필터 콤보박스에 사용될 아이템 클래스입니다.
     */
    private static class DepartmentItem {
        private int id;
        private String name;

        public DepartmentItem(int id, String name) {
            this.id = id;
            this.name = name;
        }
        public int getId() { return id; }
        public String getName() { return name; }
        @Override public String toString() { return name; }
    }
}