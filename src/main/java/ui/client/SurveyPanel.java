package main.java.ui.client;

import main.java.dto.SurveyFullDto;
import main.java.dto.SurveyFullDto.SurveyQuestionDto; // 중첩 DTO 임포트
import main.java.model.Survey;
import main.java.model.SurveyQuestion;
import main.java.model.SurveyQuestionOption;
import main.java.model.User;
import main.java.service.SurveyService;
import main.java.service.SurveyServiceImpl;
import main.java.service.ServiceException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.ArrayList; // ArrayList 임포트
import java.util.Map;
import java.util.HashMap; // HashMap 임포트
import javax.swing.SwingConstants; // SwingConstants 임포트

public class SurveyPanel extends JPanel {
    // ... (기존 필드 선언: user, surveyService, mainFrame, currentActiveSurvey 등) ...
    private final User user;
    private final SurveyService surveyService = new SurveyServiceImpl();
    private ClientMainFrame mainFrame;

    private SurveyFullDto currentActiveSurvey;
    private List<SurveyQuestionDto> surveyQuestions;
    private int currentQuestionIndex = 0;
    private Map<Integer, String> userAnswers;

    private CardLayout cardLayout;
    private JPanel mainSurveyContentPanel;

    private JLabel surveyTitleLabel;
    private JLabel questionNavigationLabel;
    private JPanel questionDisplayArea;
    private JButton prevButton;
    private JButton nextOrSubmitButton;
    private JLabel noSurveyMessageLabel;
    private JLabel completionMessageLabel;
    private JLabel alreadyCompletedMessageLabel; // 이미 참여한 경우 표시될 메시지 레이블


    public SurveyPanel(User user, ClientMainFrame mainFrame) {
        this.user = user;
        this.mainFrame = mainFrame;
        this.userAnswers = new HashMap<>();

        // cardLayout은 패널 자체에 설정
        cardLayout = new CardLayout();
        setLayout(cardLayout);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        initBaseUI();
        loadActiveSurvey();
    }

    private void initBaseUI() {
        // mainSurveyContentPanel, noSurveyPanel, completionPanel 초기화는 이전과 동일

        // 1. 설문 진행 패널 (mainSurveyContentPanel)
        mainSurveyContentPanel = new JPanel(new BorderLayout(10,10));
        // ... (surveyTitleLabel, questionDisplayArea, navigationPanel 구성은 이전과 동일) ...
        surveyTitleLabel = new JLabel("", SwingConstants.CENTER);
        surveyTitleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        surveyTitleLabel.setBorder(new EmptyBorder(0,0,10,0));
        mainSurveyContentPanel.add(surveyTitleLabel, BorderLayout.NORTH);

        questionDisplayArea = new JPanel();
        questionDisplayArea.setLayout(new BorderLayout());
        mainSurveyContentPanel.add(questionDisplayArea, BorderLayout.CENTER);

        JPanel navigationPanel = new JPanel(new BorderLayout());
        questionNavigationLabel = new JLabel("", SwingConstants.CENTER);
        questionNavigationLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        navigationPanel.add(questionNavigationLabel, BorderLayout.CENTER);

        prevButton = new JButton("이전");
        prevButton.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        prevButton.addActionListener(e -> showPreviousQuestion());
        JPanel prevButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        prevButtonPanel.add(prevButton);
        navigationPanel.add(prevButtonPanel, BorderLayout.WEST);

        nextOrSubmitButton = new JButton("다음");
        nextOrSubmitButton.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        nextOrSubmitButton.addActionListener(e -> processNextOrSubmit());
        JPanel nextButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        nextButtonPanel.add(nextOrSubmitButton);
        navigationPanel.add(nextButtonPanel, BorderLayout.EAST);

        mainSurveyContentPanel.add(navigationPanel, BorderLayout.SOUTH);
        add(mainSurveyContentPanel, "SURVEY_CONTENT");


        // 2. 진행 중인 설문 없음 메시지 패널
        JPanel noSurveyPanel = new JPanel(new GridBagLayout());
        noSurveyMessageLabel = new JLabel("현재 진행 중인 설문 조사가 없습니다.");
        noSurveyMessageLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        noSurveyPanel.add(noSurveyMessageLabel);
        add(noSurveyPanel, "NO_SURVEY");

        // 3. 설문 완료 메시지 패널
        JPanel completionPanel = new JPanel(new BorderLayout());
        completionMessageLabel = new JLabel("설문 조사가 완료되었습니다. 참여해주셔서 감사합니다!", SwingConstants.CENTER);
        completionMessageLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        completionPanel.add(completionMessageLabel, BorderLayout.CENTER);

        JButton goToMainButtonFromCompletion = new JButton("메인 화면으로"); // 버튼 이름 변경하여 구분
        goToMainButtonFromCompletion.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        goToMainButtonFromCompletion.addActionListener(e -> {
            if (mainFrame != null) {
                mainFrame.cardLayout.show(mainFrame.contentPanel, "Notice"); // 예: 공지사항으로 이동
            }
        });
        JPanel bottomButtonPanelCompletion = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomButtonPanelCompletion.setBorder(new EmptyBorder(20,0,0,0));
        bottomButtonPanelCompletion.add(goToMainButtonFromCompletion);
        completionPanel.add(bottomButtonPanelCompletion, BorderLayout.SOUTH);
        add(completionPanel, "SURVEY_COMPLETED");

        // 4. 이미 참여한 설문 메시지 패널 (새로 추가)
        JPanel alreadyCompletedPanel = new JPanel(new BorderLayout());
        alreadyCompletedMessageLabel = new JLabel("이미 이 설문조사에 참여하셨습니다. 감사합니다.", SwingConstants.CENTER);
        alreadyCompletedMessageLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        alreadyCompletedPanel.add(alreadyCompletedMessageLabel, BorderLayout.CENTER);

        JButton goToMainButtonFromAlreadyCompleted = new JButton("메인 화면으로");
        goToMainButtonFromAlreadyCompleted.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        goToMainButtonFromAlreadyCompleted.addActionListener(e -> {
            if (mainFrame != null) {
                mainFrame.cardLayout.show(mainFrame.contentPanel, "Notice"); // 예: 공지사항으로 이동
            }
        });
        JPanel bottomButtonPanelAlreadyCompleted = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomButtonPanelAlreadyCompleted.setBorder(new EmptyBorder(20,0,0,0));
        bottomButtonPanelAlreadyCompleted.add(goToMainButtonFromAlreadyCompleted);
        alreadyCompletedPanel.add(bottomButtonPanelAlreadyCompleted, BorderLayout.SOUTH);
        add(alreadyCompletedPanel, "SURVEY_ALREADY_COMPLETED"); // 새 카드 추가
    }

    private void loadActiveSurvey() {
        try {
            currentActiveSurvey = surveyService.getActiveSurveyForClient();
            if (currentActiveSurvey != null && currentActiveSurvey.getSurvey() != null) {
                Survey survey = currentActiveSurvey.getSurvey();
                // 사용자가 이미 이 설문에 응답했는지 확인
                boolean alreadyCompleted = surveyService.hasUserCompletedSurvey(user.getUserId(), survey.getSurveyId());

                if (alreadyCompleted) {
                    alreadyCompletedMessageLabel.setText("'" + survey.getTitle() + "' 설문조사에 이미 참여하셨습니다. 감사합니다.");
                    cardLayout.show(this, "SURVEY_ALREADY_COMPLETED");
                } else if (currentActiveSurvey.getQuestions() != null && !currentActiveSurvey.getQuestions().isEmpty()) {
                    surveyTitleLabel.setText("설문조사: " + survey.getTitle());
                    surveyQuestions = currentActiveSurvey.getQuestions();
                    currentQuestionIndex = 0;
                    userAnswers.clear();
                    displayCurrentQuestion();
                    cardLayout.show(this, "SURVEY_CONTENT");
                } else { // 설문은 있으나 질문이 없는 경우
                    noSurveyMessageLabel.setText("'" + survey.getTitle() + "' 설문조사에 질문이 없습니다. 관리자에게 문의하세요.");
                    cardLayout.show(this, "NO_SURVEY");
                }
            } else {
                noSurveyMessageLabel.setText("현재 진행 중인 설문 조사가 없습니다."); // 메시지 명확화
                cardLayout.show(this, "NO_SURVEY");
            }
        } catch (ServiceException e) {
            JOptionPane.showMessageDialog(this, "설문조사 정보를 가져오는 중 오류가 발생했습니다: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            cardLayout.show(this, "NO_SURVEY");
        }
    }

    // ... (displayCurrentQuestion, saveCurrentAnswer, showPreviousQuestion, processNextOrSubmit 메서드는 이전과 동일) ...
    private void displayCurrentQuestion() {
        if (surveyQuestions == null || currentQuestionIndex < 0 || currentQuestionIndex >= surveyQuestions.size()) {
            return;
        }

        SurveyQuestionDto currentQDto = surveyQuestions.get(currentQuestionIndex);
        SurveyQuestion currentQuestion = currentQDto.getQuestion();

        questionNavigationLabel.setText("질문 " + (currentQuestionIndex + 1) + " / " + surveyQuestions.size());
        questionDisplayArea.removeAll();

        JTextArea questionTextDisplay = new JTextArea(currentQuestion.getQuestionText());
        questionTextDisplay.setWrapStyleWord(true);
        questionTextDisplay.setLineWrap(true);
        questionTextDisplay.setEditable(false);
        questionTextDisplay.setFont(new Font("맑은 고딕", Font.BOLD, 15));
        questionTextDisplay.setMargin(new Insets(5,5,10,5));
        questionTextDisplay.setOpaque(false);
        questionDisplayArea.add(new JScrollPane(questionTextDisplay), BorderLayout.NORTH);

        JPanel answerInputPanel = new JPanel();
        String savedAnswer = userAnswers.get(currentQuestion.getQuestionId());

        if ("MCQ".equalsIgnoreCase(currentQuestion.getQuestionType())) {
            answerInputPanel.setLayout(new BoxLayout(answerInputPanel, BoxLayout.Y_AXIS));
            ButtonGroup mcqGroup = new ButtonGroup();
            List<SurveyQuestionOption> options = currentQDto.getOptions();
            if (options != null) {
                for (SurveyQuestionOption option : options) {
                    JRadioButton radioButton = new JRadioButton("<html><body style='width: 400px;'>" + option.getOptionText() + "</body></html>"); // 자동 줄바꿈을 위해 HTML 사용
                    radioButton.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
                    radioButton.setActionCommand(String.valueOf(option.getOptionId()));
                    if (savedAnswer != null && savedAnswer.equals(String.valueOf(option.getOptionId()))) {
                        radioButton.setSelected(true);
                    }
                    mcqGroup.add(radioButton);
                    // 왼쪽 정렬 및 여백을 위해 각 라디오 버튼을 작은 패널에 추가
                    JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                    radioPanel.add(radioButton);
                    answerInputPanel.add(radioPanel);
                }
            }
        } else { // TEXT (주관식) 유형
            answerInputPanel.setLayout(new BorderLayout());
            JTextArea textAreaAnswer = new JTextArea(5, 30);
            textAreaAnswer.setLineWrap(true);
            textAreaAnswer.setWrapStyleWord(true);
            textAreaAnswer.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
            if (savedAnswer != null) {
                textAreaAnswer.setText(savedAnswer);
            }
            textAreaAnswer.setName("textAnswerArea_" + currentQuestion.getQuestionId());
            JScrollPane textAnswerScrollPane = new JScrollPane(textAreaAnswer);
            answerInputPanel.add(textAnswerScrollPane, BorderLayout.CENTER);
        }

        questionDisplayArea.add(answerInputPanel, BorderLayout.CENTER);

        prevButton.setEnabled(currentQuestionIndex > 0);
        if (currentQuestionIndex == surveyQuestions.size() - 1) {
            nextOrSubmitButton.setText("제출");
        } else {
            nextOrSubmitButton.setText("다음");
        }

        questionDisplayArea.revalidate();
        questionDisplayArea.repaint();
    }

    private void saveCurrentAnswer() {
        // ... (이전과 동일)
        if (surveyQuestions == null || currentQuestionIndex < 0 || currentQuestionIndex >= surveyQuestions.size()) {
            return;
        }
        SurveyQuestionDto currentQDto = surveyQuestions.get(currentQuestionIndex);
        SurveyQuestion currentQuestion = currentQDto.getQuestion();
        int currentQId = currentQuestion.getQuestionId();

        Component answerComponentContainer = questionDisplayArea.getComponent(1);

        if ("MCQ".equalsIgnoreCase(currentQuestion.getQuestionType())) {
            if (answerComponentContainer instanceof JPanel) {
                JPanel mcqPanel = (JPanel) answerComponentContainer;
                for (Component compOuter : mcqPanel.getComponents()) { // radioPanel 순회
                    if (compOuter instanceof JPanel) {
                        JPanel radioPanel = (JPanel) compOuter;
                        if (radioPanel.getComponentCount() > 0 && radioPanel.getComponent(0) instanceof JRadioButton) {
                            JRadioButton rb = (JRadioButton) radioPanel.getComponent(0);
                            if (rb.isSelected()) {
                                userAnswers.put(currentQId, rb.getActionCommand());
                                return;
                            }
                        }
                    }
                }
                userAnswers.remove(currentQId);
            }
        } else {
            if (answerComponentContainer instanceof JPanel) {
                JPanel textPanel = (JPanel) answerComponentContainer;
                if (textPanel.getComponent(0) instanceof JScrollPane) {
                    JScrollPane textAnswerScrollPane = (JScrollPane) textPanel.getComponent(0);
                    JViewport viewport = textAnswerScrollPane.getViewport();
                    if (viewport.getView() instanceof JTextArea) {
                        JTextArea textAreaAnswer = (JTextArea) viewport.getView();
                        if (textAreaAnswer != null) {
                            String answer = textAreaAnswer.getText().trim();
                            if (!answer.isEmpty()) {
                                userAnswers.put(currentQId, answer);
                            } else {
                                userAnswers.remove(currentQId);
                            }
                        }
                    }
                }
            }
        }
    }

    private void showPreviousQuestion() {
        saveCurrentAnswer();
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--;
            displayCurrentQuestion();
        }
    }

    private void processNextOrSubmit() {
        saveCurrentAnswer();

        if (currentQuestionIndex < surveyQuestions.size() - 1) {
            currentQuestionIndex++;
            displayCurrentQuestion();
        } else {
            int confirm = JOptionPane.showConfirmDialog(this, "설문조사를 제출하시겠습니까?", "제출 확인", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    surveyService.submitSurveyResponses(user.getUserId(), currentActiveSurvey.getSurvey().getSurveyId(), userAnswers);
                    cardLayout.show(this, "SURVEY_COMPLETED");
                } catch (ServiceException e) {
                    JOptionPane.showMessageDialog(this, "설문조사 제출 중 오류가 발생했습니다: " + e.getMessage(), "제출 오류", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}