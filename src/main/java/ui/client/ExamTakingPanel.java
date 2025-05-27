package main.java.ui.client;

import main.java.model.QuestionFull;
import main.java.model.QuestionOption;
import main.java.model.User;
import main.java.service.ExamService;
import main.java.service.ExamServiceImpl;
import main.java.service.QuestionService;
import main.java.service.QuestionServiceImpl;
import main.java.service.ServiceException;

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
    private final User user;
    private final int examId;
    private final String subject;
    private final LocalDateTime endTime;
    private final QuestionService questionService = new QuestionServiceImpl();
    private final ExamService examService = new ExamServiceImpl();

    private CardLayout cardLayout;
    private JPanel questionsContainer;
    private List<QuestionFull> questions;

    private JLabel subjectLabel;
    private JLabel timerLabel;
    private Timer countdownTimer;

    // <문제ID, 선택한 답>
    private final Map<Integer, String> selectedAnswers = new HashMap<>();
    // <문제ID, ButtonGroup> (답 선택 UI와 동기화)
    private final Map<Integer, ButtonGroup> questionGroups = new HashMap<>();

    public ExamTakingPanel(User user, int examId, String subject, LocalDateTime examEndTime) throws ServiceException {
        this.user = user;
        this.examId = examId;
        this.subject = subject;
        this.endTime = examEndTime;
        initComponents();
        loadQuestions();
        startTimer();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        // 상단: 과목명 + 남은 시간
        JPanel topPanel = new JPanel(new BorderLayout());
        subjectLabel = new JLabel("과목: " + subject);
        subjectLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        topPanel.add(subjectLabel, BorderLayout.WEST);

        timerLabel = new JLabel();
        timerLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        topPanel.add(timerLabel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 중앙: CardLayout 문제 페이지
        cardLayout = new CardLayout();
        questionsContainer = new JPanel(cardLayout);
        add(questionsContainer, BorderLayout.CENTER);

        // 우측: 문제 번호 네비 + <<, >> 버튼
        JPanel navPanel = new JPanel(new BorderLayout(5, 5));
        JPanel btnGrid = new JPanel(new GridLayout(5, 2, 5, 5));
        navPanel.add(new JScrollPane(btnGrid), BorderLayout.CENTER);

        JPanel arrowPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        JButton prev = new JButton("<<");
        JButton next = new JButton(">>");
        prev.addActionListener(e -> cardLayout.previous(questionsContainer));
        next.addActionListener(e -> cardLayout.next(questionsContainer));
        arrowPanel.add(prev);
        arrowPanel.add(next);
        navPanel.add(arrowPanel, BorderLayout.SOUTH);

        add(navPanel, BorderLayout.EAST);

        // 하단: 최종 제출 버튼
        JButton finalSubmitBtn = new JButton("최종 제출");
        finalSubmitBtn.setBackground(Color.RED);
        finalSubmitBtn.setForeground(Color.WHITE);
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

        JScrollPane sp = (JScrollPane) ((JPanel) getComponent(2)).getComponent(0);
        JPanel btnGrid = (JPanel) sp.getViewport().getView();

        for (int i = 0; i < questions.size(); i++) {
            QuestionFull qf = questions.get(i);
            JPanel page = createQuestionPage(qf);
            questionsContainer.add(page, String.valueOf(i));
            // 번호 네비 버튼
            JButton numBtn = new JButton(String.valueOf(i + 1));
            final int idx = i;
            numBtn.addActionListener(e -> cardLayout.show(questionsContainer, String.valueOf(idx)));
            btnGrid.add(numBtn);
        }

        cardLayout.show(questionsContainer, "0");
        revalidate();
        repaint();
    }

    private JPanel createQuestionPage(QuestionFull qf) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        // 문제 텍스트
        JTextArea text = new JTextArea(qf.getQuestionText());
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setEditable(false);
        text.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        panel.add(new JScrollPane(text), BorderLayout.NORTH);

        // 선택지 라디오 버튼
        JPanel optionsPanel = new JPanel(new GridLayout(0, 1, 5, 5));
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

        // 기존 선택 불러오기(뒤로가거나, 자동 제출시 활용)
        if (selectedAnswers.containsKey(qf.getQuestionId())) {
            String sel = selectedAnswers.get(qf.getQuestionId());
            for (Enumeration<AbstractButton> en = group.getElements(); en.hasMoreElements(); ) {
                AbstractButton btn = en.nextElement();
                String txt = btn.getText();
                String val = txt.contains(".") ? txt.split("\\.")[0] : txt;
                if (val.equals(sel)) btn.setSelected(true);
            }
        }
        // ButtonGroup 맵에 등록
        questionGroups.put(qf.getQuestionId(), group);

        return panel;
    }

    private void handleFinalSubmit() {
        // 1. 마지막으로 선택 상태 동기화
        for (Map.Entry<Integer, ButtonGroup> entry : questionGroups.entrySet()) {
            Integer qid = entry.getKey();
            ButtonGroup group = entry.getValue();
            String selected = null;
            for (Enumeration<AbstractButton> en = group.getElements(); en.hasMoreElements(); ) {
                AbstractButton btn = en.nextElement();
                if (btn.isSelected()) {
                    String txt = btn.getText();
                    selected = txt.contains(".") ? txt.split("\\.")[0] : txt;
                    break;
                }
            }
            if (selected != null) selectedAnswers.put(qid, selected);
        }

        // 2. 미답안 경고
        if (selectedAnswers.size() < questions.size()) {
            int c = JOptionPane.showConfirmDialog(this, "답안이 선택되지 않은 문제가 있습니다.\n그래도 제출하시겠습니까?", "미답안 경고", JOptionPane.YES_NO_OPTION);
            if (c != JOptionPane.YES_OPTION) return;
        }

        // 3. 실제 제출, 점수 저장, 안내
        try {
            examService.submitAllAnswers(user.getUserId(), examId, selectedAnswers);
            int score = calculateScore();
            examService.saveExamResult(user.getUserId(), examId, score);
            JOptionPane.showMessageDialog(this, "시험이 완료되었습니다. 점수: " + score, "완료", JOptionPane.INFORMATION_MESSAGE);
            countdownTimer.stop();
            disableExamUI(); // 입력 비활성화(옵션)
            SwingUtilities.invokeLater(() -> {
                JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
                if (frame instanceof ClientMainFrame) {
                    // 현재 프레임 종료
                    frame.dispose();
                    // 새 MainFrame을 새로 생성해서 항상 최신 화면으로 복귀
                    new ClientMainFrame(user).setVisible(true);
                }
            });
        } catch (ServiceException ex) {
            JOptionPane.showMessageDialog(this, "제출 중 오류가 발생했습니다:\n" + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void disableExamUI() {
        setEnabled(false);
        // 또는 문제별 panel/버튼/입력 필드에 setEnabled(false) 호출
    }

    private int calculateScore() throws ServiceException {
        int score = 0;
        for (QuestionFull qf : questions) {
            String correct = questionService.getCorrectAnswer(qf.getQuestionId());
            String answer = selectedAnswers.get(qf.getQuestionId());
            if (correct != null && correct.equals(answer)) {
                score += (100 / questions.size());
            }
        }
        return score;
    }

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