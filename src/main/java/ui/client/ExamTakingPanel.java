package main.java.ui.client;

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
    private static final Color ANSWERED_COLOR = new Color(135, 206, 250); // SkyBlue

    private final User user;
    private final int examId;
    private final QuestionService questionService = new QuestionServiceImpl();
    private final SubmissionService submissionService = new SubmissionServiceImpl();

    private CardLayout cardLayout;
    private JPanel questionsContainer;
    private List<QuestionFull> questions;

    private JLabel subjectLabel;
    private JLabel timerLabel;
    private LocalDateTime endTime;
    private Timer countdownTimer;

    // <문제ID, 선택한 답>
    private final Map<Integer, String> selectedAnswers = new HashMap<>();
    // <문제ID, ButtonGroup>
    private final Map<Integer, ButtonGroup> questionGroups = new HashMap<>();
    // index -> navigation button
    private final Map<Integer, JButton> navButtons = new HashMap<>();
    // questionId -> index map
    private final Map<Integer, Integer> questionIndexMap = new HashMap<>();

    public ExamTakingPanel(User user, int examId, String subject, LocalDateTime examEndTime) throws ServiceException {
        this.user = user;
        this.examId = examId;
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

        // 중앙: CardLayout 으로 문제 페이지 전환
        cardLayout = new CardLayout();
        questionsContainer = new JPanel(cardLayout);
        add(questionsContainer, BorderLayout.CENTER);

        // 우측: 네비게이션 패널
        JPanel navPanel = new JPanel(new BorderLayout(5,5));
        JPanel btnGrid = new JPanel(new GridLayout(0, 5, 5, 5));
        navPanel.add(new JScrollPane(btnGrid), BorderLayout.CENTER);
        JPanel arrowPanel = new JPanel(new GridLayout(1,2,5,5));
        JButton prev = new JButton("<<");
        JButton next = new JButton(">>");
        prev.addActionListener(e -> cardLayout.previous(questionsContainer));
        next.addActionListener(e -> cardLayout.next(questionsContainer));
        arrowPanel.add(prev);
        arrowPanel.add(next);
        navPanel.add(arrowPanel, BorderLayout.SOUTH);
        add(navPanel, BorderLayout.EAST);
    }

    private void loadQuestions() throws ServiceException {
        questions = questionService.getQuestionsByExam(examId);
        if (questions == null || questions.isEmpty()) {
            add(new JLabel("출제된 문제가 없습니다."), BorderLayout.CENTER);
            return;
        }
        // 네비 버튼 그리드 참조
        JScrollPane sp = (JScrollPane)((JPanel)getComponent(2)).getComponent(0);
        JPanel btnGrid = (JPanel)sp.getViewport().getView();

        for (int i = 0; i < questions.size(); i++) {
            QuestionFull qf = questions.get(i);
            questionIndexMap.put(qf.getQuestionId(), i);
            JPanel page = createQuestionPage(qf, i);
            questionsContainer.add(page, String.valueOf(i));

            JButton numBtn = new JButton(String.valueOf(i + 1));
            navButtons.put(i, numBtn);
            int idx = i;
            numBtn.addActionListener(e -> cardLayout.show(questionsContainer, String.valueOf(idx)));
            // 이미 풀었던 문제인 경우 색상 변경
            if (selectedAnswers.containsKey(qf.getQuestionId())) {
                numBtn.setBackground(ANSWERED_COLOR);
            }
            btnGrid.add(numBtn);
        }
        cardLayout.show(questionsContainer, "0");
        revalidate();
        repaint();
    }

    private JPanel createQuestionPage(QuestionFull qf, int idx) {
        JPanel panel = new JPanel(new BorderLayout(10,10));
        JTextArea text = new JTextArea(qf.getQuestionText());
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setEditable(false);
        text.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        panel.add(new JScrollPane(text), BorderLayout.NORTH);

        JPanel optionsPanel = new JPanel(new GridLayout(0, 1, 5,5));
        ButtonGroup group = new ButtonGroup();
        if (qf.getType().name().equals("MCQ")) {
            for (QuestionOption opt : qf.getOptions()) {
                String label = String.valueOf(opt.getOptionLabel());
                JRadioButton rb = new JRadioButton(label + ". " + opt.getContent());
                rb.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
                group.add(rb);
                optionsPanel.add(rb);
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
        }
        panel.add(optionsPanel, BorderLayout.CENTER);

        // 기존 선택 로드
        if (selectedAnswers.containsKey(qf.getQuestionId())) {
            String sel = selectedAnswers.get(qf.getQuestionId());
            for (AbstractButton btn : Collections.list(group.getElements())) {
                String txt = btn.getText();
                String val = txt.contains(".") ? txt.split("\\.")[0] : txt;
                if (val.equals(sel)) btn.setSelected(true);
            }
        }
        questionGroups.put(qf.getQuestionId(), group);

        JPanel submitPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton submitBtn = new JButton("제출");
        submitBtn.addActionListener(e -> handleSubmit(qf, group, idx));
        submitPanel.add(submitBtn);
        panel.add(submitPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void handleSubmit(QuestionFull qf, ButtonGroup group, int idx) {
        String selected = null;
        for (AbstractButton btn : Collections.list(group.getElements())) {
            if (btn.isSelected()) {
                String txt = btn.getText();
                selected = txt.contains(".") ? txt.split("\\.")[0] : txt;
                break;
            }
        }
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "답안을 선택해주세요.", "제출 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            submissionService.submitAnswer(user.getUserId(), examId, qf.getQuestionId(), selected);
            selectedAnswers.put(qf.getQuestionId(), selected);
            markAnswered(idx);
            JOptionPane.showMessageDialog(this, "답안이 저장되었습니다.");
        } catch (ServiceException ex) {
            JOptionPane.showMessageDialog(this,
                    "답안 저장 중 오류가 발생했습니다:\n" + ex.getMessage(),
                    "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void markAnswered(int idx) {
        JButton btn = navButtons.get(idx);
        if (btn != null) {
            btn.setBackground(ANSWERED_COLOR);
        }
    }

    private void startTimer() {
        countdownTimer = new Timer(1000, e -> {
            Duration remaining = Duration.between(LocalDateTime.now(), endTime);
            if (remaining.isNegative() || remaining.isZero()) {
                timerLabel.setText("남은 시간: 00:00");
                countdownTimer.stop();
                // TODO: 자동 제출 또는 종료 처리
            } else {
                long mins = remaining.toMinutes();
                long secs = remaining.minusMinutes(mins).getSeconds();
                timerLabel.setText(String.format("남은 시간: %02d:%02d", mins, secs));
            }
        });
        countdownTimer.start();
    }
}