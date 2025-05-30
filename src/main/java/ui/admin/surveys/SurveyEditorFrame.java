package main.java.ui.admin.surveys;

import main.java.dao.DaoException;
import main.java.dto.SurveyFullDto;
import main.java.model.Survey;
import main.java.model.SurveyQuestion;
import main.java.model.SurveyQuestionOption;
import main.java.service.SurveyService;
import main.java.service.ServiceException; // ServiceException 임포트

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 설문조사 생성 및 수정을 위한 프레임입니다.
 * 설문 기본 정보와 함께 여러 질문(객관식/주관식) 및 선택지를 편집할 수 있습니다.
 */
public class SurveyEditorFrame extends JFrame {
    private final SurveyService surveyService;
    private final Survey surveyToEdit;
    private final Runnable onSaveSuccessCallback;

    private JTextField titleField;
    private JSpinner startDateSpinner;
    private JSpinner endDateSpinner;

    private JPanel questionsContainerPanel; // SurveyQuestionEditRowPanel들이 추가될 패널
    private List<SurveyQuestionEditRowPanel> questionRowPanels = new ArrayList<>();
    private JScrollPane questionsScrollPane; // questionsContainerPanel을 담을 스크롤 패인

    private int nextQuestionNumber = 1; // 다음에 추가될 질문의 번호 (UI 표시용)

    public SurveyEditorFrame(Frame owner, SurveyService surveyService, Survey surveyToEdit, Runnable onSaveSuccessCallback) {
        super(surveyToEdit == null ? "새 설문조사 등록" : "설문조사 수정: " + surveyToEdit.getTitle());
        this.surveyService = surveyService;
        this.surveyToEdit = surveyToEdit;
        this.onSaveSuccessCallback = onSaveSuccessCallback;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(850, 700);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initComponents();

        if (surveyToEdit != null) {
            populateFieldsForEdit();
        } else {
            addNewQuestionRow(false); // 새 설문조사 시 기본으로 주관식 질문 하나 추가
        }
    }

    private void initComponents() {
        // 상단: 설문 기본 정보 입력 패널
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("설문 기본 정보"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        infoPanel.add(new JLabel("설문 제목:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        titleField = new JTextField(30);
        titleField.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        infoPanel.add(titleField, gbc);
        gbc.weightx = 0;

        gbc.gridx = 0; gbc.gridy = 1;
        infoPanel.add(new JLabel("시작일:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        startDateSpinner = new JSpinner(new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH));
        startDateSpinner.setEditor(new JSpinner.DateEditor(startDateSpinner, "yyyy-MM-dd"));
        infoPanel.add(startDateSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        infoPanel.add(new JLabel("마감일:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        Date defaultEndDate = new Date(((Date)startDateSpinner.getValue()).getTime() + 7L * 24 * 60 * 60 * 1000);
        endDateSpinner = new JSpinner(new SpinnerDateModel(defaultEndDate, null, null, java.util.Calendar.DAY_OF_MONTH));
        endDateSpinner.setEditor(new JSpinner.DateEditor(endDateSpinner, "yyyy-MM-dd"));
        infoPanel.add(endDateSpinner, gbc);

        add(infoPanel, BorderLayout.NORTH);

        // 중앙: 질문 관리 패널
        JPanel questionsOuterPanel = new JPanel(new BorderLayout(0,5));
        questionsOuterPanel.setBorder(BorderFactory.createTitledBorder("질문 목록"));

        questionsContainerPanel = new JPanel();
        questionsContainerPanel.setLayout(new BoxLayout(questionsContainerPanel, BoxLayout.Y_AXIS));
        questionsScrollPane = new JScrollPane(questionsContainerPanel); // 필드에 할당
        questionsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        questionsScrollPane.setPreferredSize(new Dimension(780, 380));

        questionsOuterPanel.add(questionsScrollPane, BorderLayout.CENTER);

        JPanel addQuestionButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addQuestionButtonsPanel.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
        JButton addMcqButton = new JButton("객관식 질문 추가");
        addMcqButton.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        addMcqButton.addActionListener(e -> addNewQuestionRow(true));
        JButton addTextButton = new JButton("주관식 질문 추가");
        addTextButton.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        addTextButton.addActionListener(e -> addNewQuestionRow(false));
        addQuestionButtonsPanel.add(addMcqButton);
        addQuestionButtonsPanel.add(addTextButton);
        questionsOuterPanel.add(addQuestionButtonsPanel, BorderLayout.SOUTH);

        add(questionsOuterPanel, BorderLayout.CENTER);

        // 하단: 저장/취소 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("저장");
        saveButton.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        JButton cancelButton = new JButton("취소");
        cancelButton.setFont(new Font("맑은 고딕", Font.PLAIN, 13));

        saveButton.addActionListener(e -> saveSurvey());
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void addNewQuestionRow(boolean isMCQ) {
        SurveyQuestionEditRowPanel rowPanel = new SurveyQuestionEditRowPanel(isMCQ, this::removeQuestionRow);
        rowPanel.setQuestionNumber(questionRowPanels.size() + 1); // 현재 리스트 크기 + 1로 번호 설정
        questionRowPanels.add(rowPanel);
        questionsContainerPanel.add(rowPanel);
        questionsContainerPanel.add(Box.createVerticalStrut(5));

        refreshQuestionNumbers(); // 질문 번호 전체 재정렬
        updateQuestionsPanel();
    }

    private void removeQuestionRow(SurveyQuestionEditRowPanel rowPanelToRemove) {
        int removedIndex = questionRowPanels.indexOf(rowPanelToRemove);
        if (removedIndex != -1) {
            // questionsContainerPanel에서 해당 패널과 그 아래의 Strut 함께 제거
            // 컴포넌트 인덱스를 정확히 찾아서 제거
            int componentIndexInContainer = -1;
            for (int i = 0; i < questionsContainerPanel.getComponentCount(); i++) {
                if (questionsContainerPanel.getComponent(i) == rowPanelToRemove) {
                    componentIndexInContainer = i;
                    break;
                }
            }

            if (componentIndexInContainer != -1) {
                questionsContainerPanel.remove(componentIndexInContainer); // SurveyQuestionEditRowPanel 제거
                // 바로 다음 컴포넌트가 Strut인지 확인하고 제거 (더 안전한 방식)
                if (componentIndexInContainer < questionsContainerPanel.getComponentCount() &&
                        questionsContainerPanel.getComponent(componentIndexInContainer) instanceof Box.Filler) {
                    questionsContainerPanel.remove(componentIndexInContainer);
                }
            }
            questionRowPanels.remove(rowPanelToRemove); // 리스트에서도 제거
            refreshQuestionNumbers(); // 질문 번호 전체 재정렬
            updateQuestionsPanel();
        }
    }

    /**
     * questionRowPanels 리스트에 있는 모든 질문 패널의 번호를 순서대로 업데이트합니다.
     */
    private void refreshQuestionNumbers() {
        for (int i = 0; i < questionRowPanels.size(); i++) {
            questionRowPanels.get(i).setQuestionNumber(i + 1);
        }
    }

    /**
     * questionsContainerPanel을 새로고침하고 스크롤을 조정합니다.
     */
    private void updateQuestionsPanel() {
        questionsContainerPanel.revalidate();
        questionsContainerPanel.repaint();
        // 스크롤 조정 (예: 맨 아래로 이동 또는 특정 패널 보이도록)
        SwingUtilities.invokeLater(() -> {
            JScrollBar verticalScrollBar = questionsScrollPane.getVerticalScrollBar();
            verticalScrollBar.setValue(verticalScrollBar.getMaximum());
        });
    }


    private void populateFieldsForEdit() {
        if (surveyToEdit == null) return;

        titleField.setText(surveyToEdit.getTitle());
        if (surveyToEdit.getStartDate() != null) {
            startDateSpinner.setValue(Date.from(surveyToEdit.getStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }
        if (surveyToEdit.getEndDate() != null) {
            endDateSpinner.setValue(Date.from(surveyToEdit.getEndDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }

        try {
            SurveyFullDto surveyFull = surveyService.getSurveyFullById(surveyToEdit.getSurveyId());
            if (surveyFull != null && surveyFull.getQuestions() != null) {
                questionsContainerPanel.removeAll(); // 기존 UI 요소 모두 제거
                questionRowPanels.clear();
                // nextQuestionNumber = 1; // refreshQuestionNumbers가 처리

                for (SurveyFullDto.SurveyQuestionDto qDto : surveyFull.getQuestions()) {
                    SurveyQuestion question = qDto.getQuestion();
                    boolean isMCQ = "MCQ".equalsIgnoreCase(question.getQuestionType());

                    SurveyQuestionEditRowPanel rowPanel = new SurveyQuestionEditRowPanel(isMCQ, this::removeQuestionRow);
                    // rowPanel.setQuestionNumber(nextQuestionNumber++); // refreshQuestionNumbers가 처리
                    rowPanel.setData(question, qDto.getOptions());

                    questionRowPanels.add(rowPanel);
                    questionsContainerPanel.add(rowPanel);
                    questionsContainerPanel.add(Box.createVerticalStrut(5));
                }
                refreshQuestionNumbers(); // 로드 후 번호 재정렬
                updateQuestionsPanel();
            }
        } catch (ServiceException e) {
            JOptionPane.showMessageDialog(this, "기존 설문 질문을 불러오는 중 오류가 발생했습니다: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveSurvey() {
        String title = titleField.getText().trim();
        Date startDateUtil = (Date) startDateSpinner.getValue();
        Date endDateUtil = (Date) endDateSpinner.getValue();

        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "설문 제목을 입력해주세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
            titleField.requestFocus();
            return;
        }
        if (startDateUtil == null || endDateUtil == null) {
            JOptionPane.showMessageDialog(this, "시작일과 마감일을 모두 선택해주세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }
        LocalDate startDate = startDateUtil.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate endDate = endDateUtil.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        if (endDate.isBefore(startDate)) {
            JOptionPane.showMessageDialog(this, "마감일은 시작일보다 이전일 수 없습니다.", "날짜 오류", JOptionPane.WARNING_MESSAGE);
            endDateSpinner.requestFocus();
            return;
        }

        // 새 설문이거나, 수정 대상 설문이 아직 시작 전일 경우에만 마감일이 과거인지 체크
        boolean isNewSurveyOrBeforeStart = (surveyToEdit == null) ||
                (surveyToEdit.getStartDate() != null && LocalDate.now().isBefore(surveyToEdit.getStartDate()));
        if (isNewSurveyOrBeforeStart && endDate.isBefore(LocalDate.now())) {
            JOptionPane.showMessageDialog(this, "새 설문 또는 시작 전 설문의 마감일은 오늘(" + LocalDate.now() + ")보다 이전일 수 없습니다.", "날짜 오류", JOptionPane.WARNING_MESSAGE);
            endDateSpinner.requestFocus();
            return;
        }

        List<SurveyFullDto.SurveyQuestionDto> questionDtos = new ArrayList<>();
        for (int i = 0; i < questionRowPanels.size(); i++) {
            SurveyQuestionEditRowPanel rowPanel = questionRowPanels.get(i);
            if (!rowPanel.validateInputs()) { // 각 질문 행의 유효성 검사
                // validateInputs 내부에서 이미 JOptionPane으로 메시지 표시됨
                // 해당 패널에 포커스를 주거나 할 수 있음
                return;
            }
            SurveyFullDto.SurveyQuestionDto qDto = rowPanel.getQuestionData();
            // SurveyQuestion 객체에 순서 정보가 있다면 설정
            // qDto.getQuestion().setQuestionOrder(i + 1);
            questionDtos.add(qDto);
        }

        if (questionDtos.isEmpty()) {
            JOptionPane.showMessageDialog(this, "최소 하나 이상의 질문을 추가해주세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }

        SurveyFullDto surveyFullDto = new SurveyFullDto();
        Survey surveyData;

        if (surveyToEdit == null) { // 새 설문조사 생성
            surveyData = new Survey();
            // surveyData.setCreateDate(LocalDate.now()); // 서비스에서 처리하도록 변경 (save 시)
        } else { // 기존 설문조사 수정
            surveyData = new Survey();
            surveyData.setSurveyId(surveyToEdit.getSurveyId()); // 기존 ID 설정
            surveyData.setCreateDate(surveyToEdit.getCreateDate()); // 기존 생성일 유지
        }

        surveyData.setTitle(title);
        surveyData.setStartDate(startDate);
        surveyData.setEndDate(endDate);
        // surveyData.setActive(true); // is_active 설정은 서비스 계층에서 최종적으로 결정

        surveyFullDto.setSurvey(surveyData);
        surveyFullDto.setQuestions(questionDtos);

        try {
            if (surveyToEdit == null) {
                surveyService.createSurvey(surveyFullDto);
                JOptionPane.showMessageDialog(this, "새 설문조사가 성공적으로 등록되었습니다.", "등록 완료", JOptionPane.INFORMATION_MESSAGE);
            } else {
                surveyService.updateSurvey(surveyFullDto); // 서비스에서 수정 정책(시작 전/진행 중)에 따라 처리
                JOptionPane.showMessageDialog(this, "설문조사가 성공적으로 수정되었습니다.", "수정 완료", JOptionPane.INFORMATION_MESSAGE);
            }

            if (onSaveSuccessCallback != null) {
                onSaveSuccessCallback.run();
            }
            dispose();

        } catch (ServiceException ex) {
            JOptionPane.showMessageDialog(this, "설문조사 저장 중 오류 발생:\n" + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        } catch (DaoException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}