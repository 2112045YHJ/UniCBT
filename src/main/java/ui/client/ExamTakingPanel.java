package main.java.ui.client;

import main.java.dao.DaoException;
import main.java.model.QuestionFull;
import main.java.model.QuestionOption;
import main.java.model.User;
import main.java.service.*;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import javax.swing.Timer;

/**
 * 클라이언트용 시험 응시 패널
 * - 상단: 과목명 + 남은 시간 표시
 * - 좌측: 문제 본문 + 선택지
 * - 우측: 문제 번호 네비게이션 + [<<], [>>]
 * - 하단: 최종 제출 버튼
 */
public class ExamTakingPanel extends JPanel {
    private static final Color CURRENT_COLOR  = new Color(255, 215, 0);   // Gold
    private static final Color ANSWERED_COLOR = new Color(135, 206, 250); // SkyBlue

    private final User user;
    private final int examId;
    private final QuestionService      questionService   = new QuestionServiceImpl();
    private final SubmissionService    submissionService = new SubmissionServiceImpl();

    private CardLayout    cardLayout;
    private JPanel        questionsContainer;
    private List<QuestionFull> questions;

    private JLabel        subjectLabel;
    private JLabel        timerLabel;
    private LocalDateTime endTime;
    private Timer         countdownTimer;

    // 선택·이동 상태 관리
    private final Map<Integer, String>      selectedAnswers    = new HashMap<>();
    private final Map<Integer, ButtonGroup> questionGroups     = new HashMap<>();
    private final Map<Integer, JButton>     navButtons         = new HashMap<>();
    private final Map<Integer, Integer>     questionIndexMap   = new HashMap<>();
    private final Set<Integer>              answeredIndices    = new HashSet<>();
    private int                             currentIndex       = 0;
    private Color                           defaultNavColor;

    public ExamTakingPanel(User user, int examId, String subject, LocalDateTime examEndTime) throws ServiceException {
        this.user    = user;
        this.examId  = examId;
        this.endTime = examEndTime;

        initComponents();
        subjectLabel.setText("과목: " + subject);
        loadQuestions();
        startTimer();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // 상단: 과목명 + 남은 시간
        JPanel topPanel = new JPanel(new BorderLayout());
        subjectLabel = new JLabel();
        subjectLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        topPanel.add(subjectLabel, BorderLayout.WEST);
        timerLabel = new JLabel();
        timerLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        topPanel.add(timerLabel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 중앙: CardLayout 문제 페이지 전환
        cardLayout = new CardLayout();
        questionsContainer = new JPanel(cardLayout);
        add(questionsContainer, BorderLayout.CENTER);

        // 우측: 네비게이션 패널
        JPanel navPanel = new JPanel(new BorderLayout(5, 5));
        JPanel btnGrid = new JPanel(new GridLayout(0, 5, 5, 5));
        navPanel.add(new JScrollPane(btnGrid), BorderLayout.CENTER);

        JPanel arrowPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        JButton prev = new JButton("<<");
        JButton next = new JButton(">>");
        prev.addActionListener(e -> {
            if (currentIndex > 0) {
                currentIndex--;
                cardLayout.show(questionsContainer, String.valueOf(currentIndex));
                updateNavHighlight();
            }
        });
        next.addActionListener(e -> {
            if (currentIndex < questions.size() - 1) {
                currentIndex++;
                cardLayout.show(questionsContainer, String.valueOf(currentIndex));
                updateNavHighlight();
            }
        });
        arrowPanel.add(prev);
        arrowPanel.add(next);
        navPanel.add(arrowPanel, BorderLayout.SOUTH);

        add(navPanel, BorderLayout.EAST);

        // 하단: 최종 제출 버튼
        JButton finalSubmitBtn = new JButton("최종 제출");
        finalSubmitBtn.setBackground(Color.RED);
        finalSubmitBtn.setForeground(Color.BLACK); // 검은색 텍스트
        finalSubmitBtn.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        finalSubmitBtn.addActionListener(e -> handleFinalSubmit());
        add(finalSubmitBtn, BorderLayout.SOUTH);
    }

    private void loadQuestions() throws ServiceException {
        questions = questionService.getQuestionsByExam(examId);
        if (questions == null || questions.isEmpty()) {
            add(new JLabel("출제된 문제가 없습니다."), BorderLayout.CENTER);
            return;
        }

        JScrollPane sp = (JScrollPane)((JPanel)getComponent(2)).getComponent(0);
        JPanel btnGrid = (JPanel)sp.getViewport().getView();

        for (int i = 0; i < questions.size(); i++) {
            QuestionFull qf = questions.get(i);
            questionIndexMap.put(qf.getQuestionId(), i);

            // 문제 페이지 생성
            JPanel page = createQuestionPage(qf, i);
            questionsContainer.add(page, String.valueOf(i));

            // 네비 버튼 생성
            JButton numBtn = new JButton(String.valueOf(i + 1));
            numBtn.setOpaque(true);
            numBtn.setContentAreaFilled(true);
            numBtn.setBorderPainted(false);
            numBtn.setFocusPainted(false);
            if (defaultNavColor == null) {
                defaultNavColor = numBtn.getBackground();
            }
            final int idx = i;
            numBtn.addActionListener(e -> {
                currentIndex = idx;
                cardLayout.show(questionsContainer, String.valueOf(idx));
                updateNavHighlight();
            });
            if (selectedAnswers.containsKey(qf.getQuestionId())) {
                answeredIndices.add(i);
            }
            navButtons.put(i, numBtn);
            btnGrid.add(numBtn);
        }

        cardLayout.show(questionsContainer, "0");
        updateNavHighlight();
        revalidate();
        repaint();
    }

    private JPanel createQuestionPage(QuestionFull qf, int idx) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // 문제 텍스트
        JTextArea text = new JTextArea(qf.getQuestionText());
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setEditable(false);
        text.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        panel.add(new JScrollPane(text), BorderLayout.NORTH);

        // 선택지
        JPanel optionsPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        ButtonGroup group = new ButtonGroup();
        if ("MCQ".equals(qf.getType().name())) {
            for (QuestionOption opt : qf.getOptions()) {
                String label = String.valueOf(opt.getOptionLabel());
                JRadioButton rb = new JRadioButton(label + ". " + opt.getContent());
                rb.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
                group.add(rb);
                optionsPanel.add(rb);
                rb.addActionListener(e -> {
                    selectedAnswers.put(qf.getQuestionId(), label);
                    answeredIndices.add(idx);
                    updateNavHighlight();
                });
            }
        } else {
            JRadioButton rbO = new JRadioButton("O");
            JRadioButton rbX = new JRadioButton("X");
            rbO.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
            rbX.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
            group.add(rbO);
            group.add(rbX);
            optionsPanel.add(rbO);
            optionsPanel.add(rbX);
            rbO.addActionListener(e -> {
                selectedAnswers.put(qf.getQuestionId(), "O");
                answeredIndices.add(idx);
                updateNavHighlight();
            });
            rbX.addActionListener(e -> {
                selectedAnswers.put(qf.getQuestionId(), "X");
                answeredIndices.add(idx);
                updateNavHighlight();
            });
        }
        panel.add(optionsPanel, BorderLayout.CENTER);
        questionGroups.put(qf.getQuestionId(), group);
        return panel;
    }

    /** 네비 버튼들 색상 업데이트 */
    private void updateNavHighlight() {
        for (Map.Entry<Integer, JButton> entry : navButtons.entrySet()) {
            int idx = entry.getKey();
            JButton btn = entry.getValue();
            btn.setOpaque(true);
            btn.setContentAreaFilled(true);
            if (idx == currentIndex) {
                btn.setBackground(CURRENT_COLOR);
            } else if (answeredIndices.contains(idx)) {
                btn.setBackground(ANSWERED_COLOR);
            } else {
                btn.setBackground(defaultNavColor);
            }
        }
    }

    /** 최종 제출 처리 */
    private void handleFinalSubmit() {
        // 선택 상태 동기화
        for (Map.Entry<Integer, ButtonGroup> entry : questionGroups.entrySet()) {
            int qid = entry.getKey();
            ButtonGroup group = entry.getValue();
            for (Enumeration<AbstractButton> en = group.getElements(); en.hasMoreElements(); ) {
                AbstractButton btn = en.nextElement();
                if (btn.isSelected()) {
                    String val = btn.getText().split("\\.")[0];
                    selectedAnswers.put(qid, val);
                    break;
                }
            }
        }

        // 미답안 경고
        if (selectedAnswers.size() < questions.size()) {
            int c = JOptionPane.showConfirmDialog(this,
                    "답안이 선택되지 않은 문제가 있습니다.\n그래도 제출하시겠습니까?",
                    "미답안 경고", JOptionPane.YES_NO_OPTION);
            if (c != JOptionPane.YES_OPTION) return;
        }

        try {
            submissionService.submitAnswerBatch(user.getUserId(), examId, selectedAnswers);
            JOptionPane.showMessageDialog(this, "시험이 완료되었습니다.");
        } catch (ServiceException | DaoException ex) {
            JOptionPane.showMessageDialog(this,
                    "제출 중 오류가 발생했습니다:\n" + ex.getMessage(),
                    "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** 타이머 시작 */
    private void startTimer() {
        countdownTimer = new Timer(1000, e -> {
            Duration remaining = Duration.between(LocalDateTime.now(), endTime);
            if (remaining.isNegative() || remaining.isZero()) {
                timerLabel.setText("남은 시간: 00:00");
                countdownTimer.stop();
                handleFinalSubmit();
            } else {
                long mins = remaining.toMinutes();
                long secs = remaining.minusMinutes(mins).getSeconds();
                timerLabel.setText(String.format("남은 시간: %02d:%02d", mins, secs));
            }
        });
        countdownTimer.start();
    }
}