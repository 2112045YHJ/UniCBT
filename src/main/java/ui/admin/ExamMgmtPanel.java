package main.java.ui.admin;

import main.java.context.ExamCreationContext;
import main.java.model.Exam;
import main.java.model.QuestionFull;
import main.java.service.ExamService;
import main.java.service.ExamServiceImpl;
import main.java.service.ServiceException;
// ExamAssignmentDao 또는 관련 서비스 메서드를 사용하기 위한 import (필요시 ExamService에 추가 가정)
import main.java.dao.ExamAssignmentDao;
import main.java.dao.ExamAssignmentDaoImpl;
import main.java.dao.DaoException;


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
    // 진행 중 시험 수정 시, 이전 시험의 학생 배정 정보를 삭제하기 위해 필요
    private final ExamAssignmentDao examAssignmentDao = new ExamAssignmentDaoImpl();
    private List<Exam> allExams = new ArrayList<>();
    private JTabbedPane tabbedPane;

    public ExamMgmtPanel() {
        setLayout(new BorderLayout());
        refreshExamList();
    }

    public void refreshExamList() {
        removeAll();

        JLabel header = new JLabel("시험 관리");
        header.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(header, BorderLayout.NORTH);

        JButton addExamBtn = new JButton("시험 추가");
        addExamBtn.addActionListener(e -> {
            ExamCreationContext context = new ExamCreationContext();
            context.setUpdateMode(false); // 새 시험 추가 모드
            JFrame frame = new JFrame("시험 등록");

            ExamEditorPanel editorPanel = new ExamEditorPanel(
                    context,
                    () -> frame.dispose(), // 이전 (취소)
                    () -> { // 저장 성공 후 콜백 (TargetSelectionDialog에서 호출됨)
                        frame.dispose();
                        this.refreshExamList(); // 시험 목록 새로고침
                    },
                    frame
            );

            frame.setContentPane(editorPanel);
            frame.setSize(800, 600); // 프레임 크기 조정
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.add(addExamBtn);
        add(topPanel, BorderLayout.SOUTH); // 버튼을 SOUTH로 이동 (일관성)

        tabbedPane = new JTabbedPane();

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

        revalidate();
        repaint();
    }

    private JPanel createExamTablePanel(List<Exam> exams) {
        JPanel panel = new JPanel(new BorderLayout());

        String[] cols = {"과목", "시작일", "마감일", "제한시간", "상태", "수정", "비활성화", "응시 대상"};
        DefaultTableModel model = new DefaultTableModel(null, cols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column >= 5; // 수정, 비활성화, 응시 대상 버튼 컬럼만 편집 가능
            }
        };
        JTable table = new JTable(model);
        table.setRowHeight(28);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"); // 시간까지 표시
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

        ButtonRenderer buttonRenderer = new ButtonRenderer();
        modifyCol.setCellRenderer(buttonRenderer);
        disableCol.setCellRenderer(buttonRenderer);
        targetCol.setCellRenderer(buttonRenderer);

        // exams 리스트를 ButtonEditor 생성자에 전달
        modifyCol.setCellEditor(new ButtonEditor(new JCheckBox(), "수정", exams, this));
        disableCol.setCellEditor(new ButtonEditor(new JCheckBox(), "비활성화", exams, this));
        targetCol.setCellEditor(new ButtonEditor(new JCheckBox(), "응시 대상", exams, this));


        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

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

    class ButtonEditor extends DefaultCellEditor {
        private final JButton button = new JButton();
        private String label;
        private boolean clicked;
        private final String actionType;
        private int row;
        private final List<Exam> examList; // 현재 탭의 시험 목록
        private final ExamMgmtPanel parentPanel; // refreshExamList 호출용
        private int originalExamIdToClearAssignments = 0; // 진행 중 시험 수정 시 이전 시험 ID 저장

        public ButtonEditor(JCheckBox checkBox, String actionType, List<Exam> examList, ExamMgmtPanel parentPanel) {
            super(checkBox);
            this.actionType = actionType;
            this.examList = examList;
            this.parentPanel = parentPanel;
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int col) {
            this.label = (value == null) ? "" : value.toString();
            this.row = row; // 선택된 행 인덱스 저장
            button.setText(label);
            clicked = true;
            return button;
        }

        public Object getCellEditorValue() {
            if (clicked) {
                // 선택된 행에서 Exam 객체 가져오기
                // examList는 해당 탭에 표시된 시험 목록이므로, this.row로 정확한 Exam 객체를 가져올 수 있음
                Exam selectedExam = examList.get(this.row);

                switch (actionType) {
                    case "수정":
                        handleModifyAction(selectedExam);
                        break;
                    case "비활성화":
                        try {
                            handleDisableAction(selectedExam);
                        } catch (ServiceException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    case "응시 대상":
                        handleShowTargetsAction(selectedExam);
                        break;
                }
            }
            clicked = false;
            return label;
        }

        private void handleShowTargetsAction(Exam exam) {
            try {
                List<String> targets = examService.getAssignedDepartmentsAndGrades(exam.getExamId());
                if (targets.isEmpty()) {
                    JOptionPane.showMessageDialog(button, "응시 대상이 지정되지 않았습니다.");
                } else {
                    String msg = String.join("\n", targets);
                    JOptionPane.showMessageDialog(button, msg, "응시 대상: " + exam.getSubject(), JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (ServiceException e) {
                JOptionPane.showMessageDialog(button, "응시 대상 조회 실패:\n" + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void handleDisableAction(Exam exam) throws ServiceException {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(exam.getEndDate()) && !exam.isActive()) { // 이미 종료된 시험 (isActive는 DB상태가 아닌 계산된 상태일 수 있으므로 end_date로 재확인)
                // exam.isActive()는 getEndDate()가 과거면 false를 반환할 수 있음.
                // 정확히는 DB에 저장된 end_date가 현재보다 이전인지 확인 필요.
                // Exam 모델에 DB에서 가져온 end_date가 있으므로 그것을 사용.
                if(examService.getExamById(exam.getExamId()).getEndDate().isBefore(now)) {
                    JOptionPane.showMessageDialog(button, "이미 종료된 시험입니다. 비활성화할 수 없습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
            }


            int confirm = JOptionPane.showConfirmDialog(
                    button,
                    "시험을 비활성화 하시겠습니까?\n비활성화하면 시험의 마감일이 현재로 설정되어 더 이상 응시할 수 없게 됩니다.",
                    "비활성화 확인",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    examService.disableExam(exam.getExamId());
                    JOptionPane.showMessageDialog(button, "시험이 비활성화되었습니다.");
                    parentPanel.refreshExamList();
                } catch (ServiceException ex) {
                    JOptionPane.showMessageDialog(button, "비활성화 중 오류 발생: " + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        private void handleModifyAction(Exam examToModify) {
            LocalDateTime now = LocalDateTime.now();
            Exam currentExamState; // DB에서 최신 상태를 가져옴
            try {
                currentExamState = examService.getExamById(examToModify.getExamId());
                if (currentExamState == null) {
                    JOptionPane.showMessageDialog(button, "시험 정보를 찾을 수 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (ServiceException e) {
                JOptionPane.showMessageDialog(button, "시험 정보 조회 중 오류: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Case 1: 시험 시작 전 (now < start_date)
            if (now.isBefore(currentExamState.getStartDate())) {
                launchExamEditorForUpdate(currentExamState);
            }
            // Case 2: 시험 기간 종료 후 (now > end_date)
            // currentExamState.getEndDate()는 DB에서 가져온 실제 마감일입니다.
            else if (now.isAfter(currentExamState.getEndDate())) {
                JOptionPane.showMessageDialog(button, "종료된 시험은 수정할 수 없습니다.", "수정 불가", JOptionPane.WARNING_MESSAGE);
            }
            // Case 3: 시험 진행 중 (start_date <= now <= end_date)
            else {
                int confirm = JOptionPane.showConfirmDialog(button,
                        "현재 진행 중인 시험입니다. 수정하시겠습니까?\n" +
                                "수정을 진행하면 현재 시험은 제목에 '(수정전 버전)'이 추가되며 즉시 비활성화 처리되고,\n" +
                                "수정된 내용은 새로운 시험으로 생성됩니다.\n" +
                                "(기존 응시 결과는 보존되며, 학생들은 새 시험에 대한 응시 자격을 갖게 됩니다.)",
                        "시험 수정 확인", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        // 1. 현재 시험(E1)의 제목을 변경하고 비활성화
                        String originalSubject = currentExamState.getSubject();
                        String archivedSubject = originalSubject + " (수정전 버전)";
                        examService.deactivateAndRenameExam(currentExamState.getExamId(), archivedSubject);

                        // 2. 새 시험(E2) 생성을 위한 컨텍스트 준비 (E1 정보 템플릿으로 사용)
                        ExamCreationContext newExamContext = new ExamCreationContext();
                        newExamContext.setUpdateMode(false); // 새 시험 생성 모드
                        newExamContext.setOriginalExamIdToClearAssignments(currentExamState.getExamId()); // 이전 시험 ID 컨텍스트에 설정

                        Exam templateExam = new Exam(); // ID가 없는 새 Exam 객체
                        templateExam.setSubject(originalSubject); // 새 시험(E2)은 원래 제목으로 시작
                        templateExam.setDurationMinutes(currentExamState.getDurationMinutes());

                        // templateExam의 startDate와 endDate를 null이 아닌 값으로 초기화
                        if (currentExamState.getStartDate() != null) {
                            templateExam.setStartDate(currentExamState.getStartDate());
                        } else {
                            templateExam.setStartDate(LocalDateTime.now());
                        }
                        // 새 시험의 종료일은 기존 시험의 종료일을 따르거나 새로 설정할 수 있도록 유도
                        // 여기서는 기존 시험의 종료일을 기본값으로 설정
                        if (currentExamState.getEndDate() != null && currentExamState.getEndDate().isAfter(LocalDateTime.now())) {
                            templateExam.setEndDate(currentExamState.getEndDate());
                        } else {
                            templateExam.setEndDate(LocalDateTime.now().plusDays(7)); // 예: 7일 후로 기본 설정
                        }

                        newExamContext.setExam(templateExam);

                        // E1의 문제들 로드
                        List<QuestionFull> questions = examService.getQuestionsByExamId(currentExamState.getExamId());
                        newExamContext.setQuestions(new ArrayList<>(questions));

                        // E1의 응시 대상(학과/학년 ID) 로드
                        List<int[]> deptAndGradeIds = examService.getAssignedDepartmentAndGradeIds(currentExamState.getExamId());
                        List<Integer> dpmtIds = new ArrayList<>();
                        List<Integer> grades = new ArrayList<>();
                        for (int[] pair : deptAndGradeIds) {
                            dpmtIds.add(pair[0]);
                            grades.add(pair[1]);
                        }
                        newExamContext.setTargetDepartments(dpmtIds);
                        newExamContext.setTargetGrades(grades);

                        // 3. 새 시험(E2) 편집/생성 UI 실행
                        JFrame frame = new JFrame("새 시험으로 수정 (원본: " + originalSubject + ")");
                        ExamEditorPanel editorPanel = new ExamEditorPanel(
                                newExamContext,
                                () -> frame.dispose(),
                                () -> {
                                    frame.dispose();
                                    parentPanel.refreshExamList(); // ExamServiceImpl에서 E1의 배정 삭제 후 목록 새로고침
                                    JOptionPane.showMessageDialog(parentPanel, "시험이 새 버전으로 수정 및 등록되었으며, 이전 버전은 비활성화 및 이름 변경 처리되었습니다.", "수정 완료", JOptionPane.INFORMATION_MESSAGE);
                                },
                                frame
                        );
                        frame.setContentPane(editorPanel);
                        frame.setSize(800, 600);
                        frame.setLocationRelativeTo(button);
                        frame.setVisible(true);

                    } catch (ServiceException ex) {
                        JOptionPane.showMessageDialog(button, "시험 처리 중 오류 발생:\n" + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }

        private void launchExamEditorForUpdate(Exam examToUpdate) {
            ExamCreationContext context = new ExamCreationContext();
            context.setUpdateMode(true); // 업데이트 모드
            context.setExam(examToUpdate); // 기존 시험 객체 (ID 포함)

            try {
                // 기존 문제 불러오기
                List<QuestionFull> questions = examService.getQuestionsByExamId(examToUpdate.getExamId());
                context.setQuestions(questions);

                // 기존 응시 대상(학과 및 학년 ID) 불러오기
                List<int[]> deptAndGradeIds = examService.getAssignedDepartmentAndGradeIds(examToUpdate.getExamId());
                List<Integer> dpmtIds = new ArrayList<>();
                List<Integer> grades = new ArrayList<>();
                for (int[] pair : deptAndGradeIds) {
                    dpmtIds.add(pair[0]);
                    grades.add(pair[1]);
                }
                context.setTargetDepartments(dpmtIds);
                context.setTargetGrades(grades);

            } catch (ServiceException ex) {
                JOptionPane.showMessageDialog(button, "기존 시험 데이터 불러오기 실패:\n" + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
                return; // 편집기 열기 중단
            }

            JFrame frame = new JFrame("시험 수정: " + examToUpdate.getSubject());
            ExamEditorPanel editorPanel = new ExamEditorPanel(
                    context,
                    () -> frame.dispose(), // 이전 (취소)
                    () -> { // 저장 성공 후 콜백 (TargetSelectionDialog에서 호출됨)
                        // 업데이트 로직은 ExamServiceImpl.saveOrUpdateExamWithDetails 에서 처리 (context.isUpdateMode() 확인)
                        frame.dispose();
                        parentPanel.refreshExamList();
                        JOptionPane.showMessageDialog(parentPanel, "시험 정보가 성공적으로 업데이트되었습니다.", "업데이트 완료", JOptionPane.INFORMATION_MESSAGE);
                    },
                    frame
            );
            frame.setContentPane(editorPanel);
            frame.setSize(800, 600); // 프레임 크기 조정
            frame.setLocationRelativeTo(button);
            frame.setVisible(true);
        }

        @Override
        public boolean stopCellEditing() {
            clicked = false;
            return super.stopCellEditing();
        }
    }
}