package main.java.ui.admin;

import main.java.model.Survey;
import main.java.model.User;
import main.java.service.SurveyService;
import main.java.service.SurveyServiceImpl; // 실제 구현체
import main.java.service.ServiceException;
import main.java.ui.admin.surveys.SurveyEditorFrame; // 설문조사 편집기 프레임 (다음 단계에서 구체화)
// import main.java.ui.admin.surveys.SurveyResultsViewerPanel; // 설문 결과 뷰어 패널 (다음 단계에서 구체화)

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 관리자용 설문조사 관리 패널입니다.
 * 설문조사 목록 조회, 생성, 수정, 삭제 및 결과 확인 기능을 제공합니다.
 * 시나리오 문서 Page 33에 해당합니다.
 */
public class SurveyMgmtPanel extends JPanel {

    private final SurveyService surveyService; // SurveyService 사용
    private final User adminUser;
    private final AdminMainFrame mainFrame; // 화면 전환 또는 다이얼로그 부모 프레임

    private JTable surveyTable;
    private DefaultTableModel surveyTableModel;
    private List<Survey> currentSurveyList = new ArrayList<>(); // 현재 테이블에 표시된 설문조사 목록

    // 날짜 포맷터
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public SurveyMgmtPanel(User adminUser, AdminMainFrame mainFrame) {
        this.adminUser = adminUser;
        this.mainFrame = mainFrame;
        // SurveyService 인스턴스화 (실제로는 의존성 주입 고려)
        this.surveyService = new SurveyServiceImpl();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // 패널 여백
        initComponents();
        loadSurveys(); // 초기 데이터 로드
    }

    /**
     * UI 컴포넌트를 초기화하고 배치합니다.
     */
    private void initComponents() {
        JLabel headerLabel = new JLabel("설문조사 관리");
        headerLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        add(headerLabel, BorderLayout.NORTH);

        // 컬럼명 정의 (시나리오 Page 33 참조 + 설문 제목 추가)
        String[] columnNames = {"ID", "제목", "등록일", "시작일", "마감일", "상태", "응답 현황", "결과", "수정", "삭제"};
        surveyTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column >= 7; // 결과, 수정, 삭제 버튼 컬럼
            }

            @Override
            public void setValueAt(Object aValue, int row, int column) {
                // 버튼 컬럼에 대해서는 모델에 값을 저장하지 않음
                if (column >= 7) {
                    // System.out.println("setValueAt called for button column, ignoring.");
                    return;
                }
                super.setValueAt(aValue, row, column);
            }
        };
        surveyTable = new JTable(surveyTableModel);
        surveyTable.setRowHeight(28);
        surveyTable.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 14));
        surveyTable.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        surveyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // 단일 행 선택

        // 각 컬럼 너비 설정
        surveyTable.getColumnModel().getColumn(0).setPreferredWidth(40);   // ID
        surveyTable.getColumnModel().getColumn(1).setPreferredWidth(250);  // 제목
        surveyTable.getColumnModel().getColumn(2).setPreferredWidth(100);  // 등록일
        surveyTable.getColumnModel().getColumn(3).setPreferredWidth(100);  // 시작일
        surveyTable.getColumnModel().getColumn(4).setPreferredWidth(100);  // 마감일
        surveyTable.getColumnModel().getColumn(5).setPreferredWidth(80);   // 상태
        surveyTable.getColumnModel().getColumn(6).setPreferredWidth(100);  // 응답 현황

        // 버튼 컬럼들 렌더러 및 에디터 설정
        SurveyListButtonRenderer buttonRenderer = new SurveyListButtonRenderer();
        TableColumn resultColumn = surveyTable.getColumn("결과");
        resultColumn.setCellRenderer(buttonRenderer);
        resultColumn.setCellEditor(new SurveyListButtonEditor(new JCheckBox(), "결과 확인"));
        resultColumn.setPreferredWidth(90);

        TableColumn modifyColumn = surveyTable.getColumn("수정");
        modifyColumn.setCellRenderer(buttonRenderer);
        modifyColumn.setCellEditor(new SurveyListButtonEditor(new JCheckBox(), "수정"));
        modifyColumn.setPreferredWidth(80);

        TableColumn deleteColumn = surveyTable.getColumn("삭제");
        deleteColumn.setCellRenderer(buttonRenderer);
        deleteColumn.setCellEditor(new SurveyListButtonEditor(new JCheckBox(), "삭제"));
        deleteColumn.setPreferredWidth(80);

        JScrollPane scrollPane = new JScrollPane(surveyTable);
        add(scrollPane, BorderLayout.CENTER);

        // 하단 "설문조사 추가" 버튼
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addSurveyButton = new JButton("설문조사 추가");
        addSurveyButton.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        addSurveyButton.addActionListener(e -> openSurveyEditor(null)); // null 전달 시 새 설문
        bottomPanel.add(addSurveyButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * 설문조사 목록을 서비스로부터 가져와 테이블에 표시합니다.
     */
    public void loadSurveys() {
        surveyTableModel.setRowCount(0); // 기존 데이터 모두 삭제
        try {
            currentSurveyList = surveyService.getAllSurveys(); // 서비스 호출
            if (currentSurveyList == null) { // 혹시 모를 null 방지
                currentSurveyList = new ArrayList<>();
            }

            for (Survey survey : currentSurveyList) {
                String statusText = determineSurveyStatus(survey);
                String responseCountText = ""; // 기본값
                try {
                    int count = surveyService.getResponseCount(survey.getSurveyId());
                    responseCountText = count + "명 응답";
                } catch (ServiceException e) {
                    responseCountText = "확인 불가";
                    System.err.println("설문 ID " + survey.getSurveyId() + "의 응답 수 로드 실패: " + e.getMessage());
                }


                surveyTableModel.addRow(new Object[]{
                        survey.getSurveyId(),
                        survey.getTitle(),
                        survey.getCreateDate() != null ? survey.getCreateDate().format(dateFormatter) : "-",
                        survey.getStartDate() != null ? survey.getStartDate().format(dateFormatter) : "-",
                        survey.getEndDate() != null ? survey.getEndDate().format(dateFormatter) : "-",
                        statusText,
                        responseCountText,
                        "결과",
                        "수정",
                        "삭제"
                });
            }
        } catch (ServiceException e) {
            JOptionPane.showMessageDialog(this, "설문조사 목록을 불러오는 중 오류가 발생했습니다: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 설문조사의 현재 상태(예정, 진행중, 종료, 비활성)를 결정합니다.
     * @param survey 상태를 확인할 Survey 객체
     * @return 상태 문자열
     */
    private String determineSurveyStatus(Survey survey) {
        if (!survey.isActive()) { // 수동 비활성화 또는 (수정됨) 처리된 경우
            return "비활성";
        }
        LocalDate today = LocalDate.now();
        if (survey.getStartDate() != null && today.isBefore(survey.getStartDate())) {
            return "예정";
        } else if (survey.getEndDate() != null && today.isAfter(survey.getEndDate())) {
            return "종료";
        } else if (survey.getStartDate() != null && survey.getEndDate() != null &&
                !today.isBefore(survey.getStartDate()) && !today.isAfter(survey.getEndDate())) {
            return "진행중";
        }
        return "상태 알수없음"; // 날짜 정보가 불완전한 경우 등
    }


    /**
     * 설문조사 편집기(새 창 또는 다이얼로그)를 엽니다.
     * @param surveyToEdit 수정할 Survey 객체 (새로 생성 시 null)
     */
    private void openSurveyEditor(Survey surveyToEdit) {
        // SurveyEditorFrame은 다음 단계에서 상세 구현될 클래스입니다.
        SurveyEditorFrame editorFrame = new SurveyEditorFrame(
                mainFrame,      // 부모 프레임 (AdminMainFrame)
                surveyService,  // 서비스 객체 전달
                surveyToEdit,   // 수정할 설문 객체 (신규 시 null)
                this::loadSurveys // 저장 성공 후 호출될 콜백 (목록 새로고침)
        );
        editorFrame.setVisible(true);
    }

    /**
     * 설문조사 결과 상세 보기 화면으로 이동합니다.
     * @param surveyId 결과를 볼 설문조사 ID
     * @param surveyTitle 설문조사 제목
     */
    private void showSurveyResultsDetail(int surveyId, String surveyTitle) {
        // AdminMainFrame에 화면 전환을 요청하거나, 직접 새 프레임/다이얼로그를 엽니다.
        // 예시: mainFrame.navigateToSurveyResults(surveyId, surveyTitle);
        // SurveyResultsViewerPanel viewerPanel = new SurveyResultsViewerPanel(this.adminUser, surveyId, surveyTitle, this.mainFrame);
        // this.mainFrame.addAndShowScreen("SurveyResults_" + surveyId, viewerPanel); // AdminMainFrame에 메서드 필요
        JOptionPane.showMessageDialog(this, "설문 결과 보기: " + surveyTitle + " (ID: " + surveyId + ")\n(결과 상세 화면은 다음 단계에서 구현합니다.)");
    }


    /**
     * 테이블 셀 버튼 렌더러입니다.
     */
    private static class SurveyListButtonRenderer extends JButton implements TableCellRenderer {
        public SurveyListButtonRenderer() {
            setOpaque(true);
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            // TODO: 버튼 상태(활성화/비활성화)를 여기서도 반영할 수 있음 (예: 수정 불가능한 설문의 수정 버튼)
            return this;
        }
    }

    /**
     * 설문조사 목록 테이블의 버튼(결과, 수정, 삭제)에 대한 액션 에디터입니다.
     */
    private class SurveyListButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String currentActionCommand; // 버튼에 표시된 텍스트이자 액션 구분자
        private boolean isPushed;
        private int selectedRowInView; // JTable 뷰에서의 행 인덱스

        public SurveyListButtonEditor(JCheckBox checkBox, String actionCommandPlaceholder) {
            super(checkBox);
            // actionCommandPlaceholder는 실제 버튼 텍스트는 getCellEditorComponent에서 설정하므로 여기서는 사용 안함
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.currentActionCommand = (value == null) ? "" : value.toString();
            this.selectedRowInView = row;
            button.setText(currentActionCommand); // "결과", "수정", "삭제"
            isPushed = true;

            // 버튼 활성화/비활성화 로직 (예: 종료된 설문은 수정/삭제 불가 등)
            // Survey selectedSurvey = currentSurveyList.get(table.convertRowIndexToModel(selectedRowInView));
            // if ("수정".equals(currentActionCommand) && "종료".equals(determineSurveyStatus(selectedSurvey))) {
            //    button.setEnabled(false);
            // } else {
            //    button.setEnabled(true);
            // }

            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                int modelRow = surveyTable.convertRowIndexToModel(selectedRowInView); // 실제 모델 인덱스
                if (modelRow < 0 || modelRow >= currentSurveyList.size()) {
                    // 유효하지 않은 행 선택 (필터링/정렬 중 발생 가능성)
                    isPushed = false;
                    return currentActionCommand;
                }
                Survey selectedSurvey = currentSurveyList.get(modelRow);

                if ("결과".equals(currentActionCommand)) {
                    mainFrame.showSurveyResultsViewerScreen(selectedSurvey.getSurveyId(), selectedSurvey.getTitle());
                } else if ("수정".equals(currentActionCommand)) {
                    // 수정 정책에 따라 버튼 활성화 여부가 getTableCellEditorComponent에서 결정되었어야 함.
                    // 여기서는 일단 호출. 서비스단에서 실제 수정 가능 여부 판단.
                    LocalDate today = LocalDate.now();
                    if (selectedSurvey.getEndDate() != null && today.isAfter(selectedSurvey.getEndDate()) && selectedSurvey.isActive()) {
                        // isActive가 true이지만 기간이 지난 경우 (예: 수정됨 태그 없는 원본)
                        JOptionPane.showMessageDialog(SurveyMgmtPanel.this, "기간이 종료된 설문조사는 수정할 수 없습니다.", "수정 불가", JOptionPane.WARNING_MESSAGE);
                    } else if (!selectedSurvey.isActive() && selectedSurvey.getTitle().contains("(수정됨)")) {
                        // (수정됨) 태그가 있고 비활성인 경우 (원본이 수정되어 비활성화된 버전)
                        JOptionPane.showMessageDialog(SurveyMgmtPanel.this, "이미 수정 처리된 이전 버전의 설문조사입니다.\n새로 생성된 버전을 확인하거나, 이 버전의 결과를 확인하세요.", "수정 불가", JOptionPane.INFORMATION_MESSAGE);
                    }
                    else {
                        openSurveyEditor(selectedSurvey);
                    }
                } else if ("삭제".equals(currentActionCommand)) {
                    int confirm = JOptionPane.showConfirmDialog(SurveyMgmtPanel.this,
                            "설문조사 '" + selectedSurvey.getTitle() + "'을(를) 삭제하시겠습니까?\n이 작업은 되돌릴 수 없으며, 관련된 모든 질문과 응답도 함께 삭제됩니다.",
                            "설문조사 삭제 확인", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (confirm == JOptionPane.YES_OPTION) {
                        try {
                            surveyService.deleteSurvey(selectedSurvey.getSurveyId());
                            JOptionPane.showMessageDialog(SurveyMgmtPanel.this, "설문조사가 성공적으로 삭제되었습니다.");
                            loadSurveys(); // 목록 새로고침
                        } catch (ServiceException ex) {
                            JOptionPane.showMessageDialog(SurveyMgmtPanel.this, "설문조사 삭제 중 오류 발생: " + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
            isPushed = false;
            return currentActionCommand;
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }
}