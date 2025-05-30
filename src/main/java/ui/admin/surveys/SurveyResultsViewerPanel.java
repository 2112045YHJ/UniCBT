package main.java.ui.admin.surveys; // admin 패널 하위 또는 surveys 패키지

import main.java.dto.SurveyFullDto;
import main.java.dto.SurveyQuestionResultDto;
import main.java.dto.SurveyQuestionOptionResultDto;
import main.java.model.SurveyQuestion;
import main.java.model.User; // adminUser 타입
import main.java.service.SurveyService;
import main.java.service.SurveyServiceImpl;
import main.java.service.ServiceException;
import main.java.ui.admin.AdminMainFrame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.text.DecimalFormat; // 비율 표시용
import java.util.List;
import java.util.ArrayList; // 추가

/**
 * 특정 설문조사의 질문별 응답 결과를 상세히 보여주는 패널입니다.
 * 시나리오 문서 Page 36, 37에 해당합니다.
 */
public class SurveyResultsViewerPanel extends JPanel {
    private final User adminUser;
    private final int surveyId;
    private String surveyTitle; // 생성자에서 설정
    private final AdminMainFrame mainFrame; // "목록으로" 또는 "나가기" 버튼용
    private final SurveyService surveyService = new SurveyServiceImpl();

    private List<SurveyQuestion> questionsInSurvey = new ArrayList<>(); // 현재 설문조사의 질문 목록
    private int currentQuestionIndex = 0; // 현재 보여주는 질문의 인덱스

    // UI 컴포넌트
    private JLabel surveyTitleLabel;
    private JLabel questionNavigationLabel; // 예: "질문 1 / 5"
    private JTextPane questionDisplayPane;  // 질문 내용 및 결과 표시
    private JButton prevQuestionButton;
    private JButton nextQuestionButton;
    private JButton backToListButton; // "목록으로" 또는 "나가기"

    private final DecimalFormat rateFormatter = new DecimalFormat("0.##%"); // 선택률 표시 포맷

    public SurveyResultsViewerPanel(User adminUser, int surveyId, String surveyTitle, AdminMainFrame mainFrame) {
        this.adminUser = adminUser;
        this.surveyId = surveyId;
        this.surveyTitle = surveyTitle;
        this.mainFrame = mainFrame;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initComponents();
        loadSurveyQuestions(); // 설문 질문 목록 로드
    }

    private void initComponents() {
        // 상단: 설문 제목 및 질문 네비게이션
        JPanel topPanel = new JPanel(new BorderLayout(10, 5));
        surveyTitleLabel = new JLabel("설문 결과: " + surveyTitle, SwingConstants.CENTER);
        surveyTitleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        topPanel.add(surveyTitleLabel, BorderLayout.NORTH);

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        prevQuestionButton = new JButton("<< 이전 질문");
        questionNavigationLabel = new JLabel("질문 - / -");
        questionNavigationLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        nextQuestionButton = new JButton("다음 질문 >>");

        prevQuestionButton.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        nextQuestionButton.setFont(new Font("맑은 고딕", Font.PLAIN, 12));

        prevQuestionButton.addActionListener(e -> showPreviousQuestion());
        nextQuestionButton.addActionListener(e -> showNextQuestion());

        navPanel.add(prevQuestionButton);
        navPanel.add(questionNavigationLabel);
        navPanel.add(nextQuestionButton);
        topPanel.add(navPanel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // 중앙: 질문 내용 및 결과 표시
        questionDisplayPane = new JTextPane();
        questionDisplayPane.setEditable(false);
        questionDisplayPane.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        questionDisplayPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                new EmptyBorder(10,10,10,10)
        ));
        add(new JScrollPane(questionDisplayPane), BorderLayout.CENTER);


        // 하단: 나가기 버튼
        backToListButton = new JButton("설문 목록으로"); // 시나리오의 "나가기" 역할
        backToListButton.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        backToListButton.addActionListener(e -> {
            if (mainFrame != null) {
                // "SurveyMgmt"는 SurveyMgmtPanel을 AdminMainFrame에 등록할 때 사용한 CardLayout 키
                mainFrame.cardLayout.show(mainFrame.contentPanel, "SurveyMgmt");
            }
        });
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10,0,0,0));
        bottomPanel.add(backToListButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * 현재 설문조사의 모든 질문 목록을 서비스로부터 로드합니다.
     */
    private void loadSurveyQuestions() {
        try {
            SurveyFullDto surveyFullDto = surveyService.getSurveyFullById(surveyId);
            if (surveyFullDto != null && surveyFullDto.getQuestions() != null) {
                this.questionsInSurvey.clear();
                for(SurveyFullDto.SurveyQuestionDto qDto : surveyFullDto.getQuestions()){
                    this.questionsInSurvey.add(qDto.getQuestion()); // SurveyQuestion 객체만 저장
                }
            }
            if (!this.questionsInSurvey.isEmpty()) {
                currentQuestionIndex = 0; // 첫 번째 질문부터 시작
                displayCurrentQuestionResults();
            } else {
                questionNavigationLabel.setText("표시할 질문 없음");
                questionDisplayPane.setText("이 설문조사에 질문이 없습니다.");
                prevQuestionButton.setEnabled(false);
                nextQuestionButton.setEnabled(false);
            }
        } catch (ServiceException e) {
            JOptionPane.showMessageDialog(this, "설문 질문 목록 로드 중 오류: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            questionDisplayPane.setText("질문 정보를 불러오는 중 오류가 발생했습니다.");
        }
        updateNavigationButtons();
    }

    /**
     * 현재 선택된 질문(currentQuestionIndex)의 응답 결과를 가져와 화면에 표시합니다.
     */
    private void displayCurrentQuestionResults() {
        if (questionsInSurvey.isEmpty() || currentQuestionIndex < 0 || currentQuestionIndex >= questionsInSurvey.size()) {
            return;
        }

        SurveyQuestion currentQuestion = questionsInSurvey.get(currentQuestionIndex);
        questionNavigationLabel.setText("질문 " + (currentQuestionIndex + 1) + " / " + questionsInSurvey.size());

        StyledDocument doc = questionDisplayPane.getStyledDocument();
        clearStyledDocument(doc); // 이전 내용 지우기
        addStylesToDocument(doc); // 스타일 정의

        try {
            // 서비스로부터 현재 질문의 상세 결과 DTO 가져오기
            SurveyQuestionResultDto questionResult = surveyService.getQuestionResults(surveyId, currentQuestion.getQuestionId());

            doc.insertString(doc.getLength(), "Q" + (currentQuestionIndex + 1) + ". ", doc.getStyle("questionHeader"));
            doc.insertString(doc.getLength(), currentQuestion.getQuestionText() + "\n\n", doc.getStyle("questionText"));

            if ("MCQ".equalsIgnoreCase(questionResult.getQuestionType())) {
                doc.insertString(doc.getLength(), "객관식 응답 분포 (총 " + questionResult.getTotalResponsesForThisQuestion() + "명 응답):\n", doc.getStyle("subHeader"));
                if (questionResult.getMcqOptionResults().isEmpty()){
                    doc.insertString(doc.getLength(), "  - 해당 질문에 대한 응답이 없습니다.\n", doc.getStyle("regular"));
                } else {
                    for (SurveyQuestionOptionResultDto optResult : questionResult.getMcqOptionResults()) {
                        String optionText = optResult.getOption().getOptionText();
                        int count = optResult.getResponseCount();
                        double rate = optResult.getSelectionRate();
                        doc.insertString(doc.getLength(), "  - " + optionText + ": ", doc.getStyle("regular"));
                        doc.insertString(doc.getLength(), count + "명 (" + rateFormatter.format(rate / 100.0) + ")\n", doc.getStyle("bold"));
                    }
                }
            } else if ("TEXT".equalsIgnoreCase(questionResult.getQuestionType())) {
                doc.insertString(doc.getLength(), "주관식 답변 목록 (총 " + questionResult.getTotalResponsesForThisQuestion() + "개 답변):\n", doc.getStyle("subHeader"));
                if (questionResult.getTextResponses().isEmpty()) {
                    doc.insertString(doc.getLength(), "  - 해당 질문에 대한 답변이 없습니다.\n", doc.getStyle("regular"));
                } else {
                    int answerNum = 1;
                    for (String textResponse : questionResult.getTextResponses()) {
                        doc.insertString(doc.getLength(), "  " + (answerNum++) + ". " + textResponse + "\n", doc.getStyle("regular"));
                    }
                }
            }
            questionDisplayPane.setCaretPosition(0); // 스크롤을 맨 위로

        } catch (ServiceException | BadLocationException e) {
            questionDisplayPane.setText("질문 결과를 불러오는 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
        updateNavigationButtons();
    }

    private void clearStyledDocument(StyledDocument doc){
        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException e) {
            // 무시하거나 로깅
        }
    }

    private void addStylesToDocument(StyledDocument doc) {
        Style regular = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        doc.addStyle("regular", regular);
        StyleConstants.setFontFamily(regular, "맑은 고딕");
        StyleConstants.setFontSize(regular, 14);

        Style questionHeader = doc.addStyle("questionHeader", regular);
        StyleConstants.setBold(questionHeader, true);
        StyleConstants.setFontSize(questionHeader, 16);
        StyleConstants.setForeground(questionHeader, new Color(0,0,128)); // Navy

        Style questionText = doc.addStyle("questionText", regular);
        StyleConstants.setFontSize(questionText, 15);

        Style subHeader = doc.addStyle("subHeader", regular);
        StyleConstants.setBold(subHeader, true);
        StyleConstants.setItalic(subHeader, true);
        StyleConstants.setFontSize(subHeader, 13);
        StyleConstants.setForeground(subHeader, Color.DARK_GRAY);

        Style bold = doc.addStyle("bold", regular);
        StyleConstants.setBold(bold, true);
    }


    private void showPreviousQuestion() {
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--;
            displayCurrentQuestionResults();
        }
    }

    private void showNextQuestion() {
        if (currentQuestionIndex < questionsInSurvey.size() - 1) {
            currentQuestionIndex++;
            displayCurrentQuestionResults();
        }
    }

    /**
     * 질문 이동 버튼의 활성화 상태를 업데이트합니다.
     */
    private void updateNavigationButtons() {
        prevQuestionButton.setEnabled(currentQuestionIndex > 0);
        nextQuestionButton.setEnabled(currentQuestionIndex < questionsInSurvey.size() - 1);
    }
}