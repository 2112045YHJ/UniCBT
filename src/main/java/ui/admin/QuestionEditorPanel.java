package main.java.ui.admin;

import main.java.context.ExamCreationContext;
import main.java.model.QuestionFull;
import main.java.model.QuestionType;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionEditorPanel extends JPanel {
    private final List<QuestionRowPanel> questionRows = new ArrayList<>();
    private final JPanel questionListPanel = new JPanel();
    private final JScrollPane scrollPane = new JScrollPane(questionListPanel);

    private final JButton prevBtn = new JButton("이전");
    private final JButton nextBtn = new JButton("다음");

    private final ExamCreationContext context;
    private final Runnable onBack;
    private final Runnable onNext;
    private final JFrame parentFrame;

    public QuestionEditorPanel(ExamCreationContext context, Runnable onBack, Runnable onNext, JFrame parentFrame) {
        this.context = context;
        this.onBack = onBack;
        this.onNext = onNext;
        this.parentFrame = parentFrame;

        setLayout(new BorderLayout());

        questionListPanel.setLayout(new BoxLayout(questionListPanel, BoxLayout.Y_AXIS));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
        add(createTopPanel(), BorderLayout.NORTH);
        add(createBottomPanel(), BorderLayout.SOUTH);

        // ✅ 기존 문제 복원
        if (context.getQuestions() != null) {
            for (QuestionFull q : context.getQuestions()) {
                addQuestionRow(q);  // 빈 값 포함하여 방어적으로 추가
            }
        }
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addMCQ = new JButton("객관식 문제 추가");
        JButton addOX = new JButton("OX 문제 추가");

        addMCQ.addActionListener(e -> addQuestionRow(QuestionType.MCQ));
        addOX.addActionListener(e -> addQuestionRow(QuestionType.OX));

        panel.add(addMCQ);
        panel.add(addOX);
        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        prevBtn.addActionListener(e -> {
            // 유효성 검사 없이 현재 상태 저장
            List<QuestionFull> savedList = collectQuestionsWithoutValidation();
            context.setQuestions(savedList);

            parentFrame.setContentPane(new ExamEditorPanel(context, onBack, onNext, parentFrame));
            parentFrame.revalidate();
            parentFrame.repaint();
        });

        nextBtn.addActionListener(e -> {
            List<QuestionFull> questionList = collectQuestions();  // 유효성 검사 포함
            if (questionList == null || questionList.isEmpty()) {
                JOptionPane.showMessageDialog(this, "문제를 한 개 이상 등록해주세요.", "오류", JOptionPane.WARNING_MESSAGE);
                return;
            }
            context.setQuestions(questionList);

            TargetSelectionDialog dialog = new TargetSelectionDialog(
                    parentFrame,
                    context,
                    () -> {
                        parentFrame.dispose();
                        onNext.run();
                    },
                    parentFrame
            );
            dialog.setVisible(true);

            if (context.getTargetGrades() != null && !context.getTargetGrades().isEmpty()
                    && context.getTargetDepartments() != null && !context.getTargetDepartments().isEmpty()) {
                onNext.run();
            }
        });

        panel.add(prevBtn);
        panel.add(nextBtn);
        return panel;
    }

    private void addQuestionRow(QuestionType type) {
        QuestionRowPanel row = new QuestionRowPanel(type, this);
        questionRows.add(row);
        questionListPanel.add(row);
        renumberQuestions();
        revalidate();
        repaint();
    }

    private void addQuestionRow(QuestionFull question) {
        QuestionRowPanel row = new QuestionRowPanel(question, this);
        questionRows.add(row);
        questionListPanel.add(row);
        renumberQuestions();
        revalidate();
        repaint();
    }

    public void removeQuestionRow(QuestionRowPanel row) {
        questionRows.remove(row);
        questionListPanel.remove(row);
        renumberQuestions();
        revalidate();
        repaint();
    }

    private void renumberQuestions() {
        for (int i = 0; i < questionRows.size(); i++) {
            questionRows.get(i).setQuestionNumber(i + 1);
        }
    }

    private List<QuestionFull> collectQuestions() {
        List<QuestionFull> result = new ArrayList<>();
        for (QuestionRowPanel row : questionRows) {
            if (!row.validateInputs()) return null;
            result.add(row.toQuestionFull());
        }
        return result;
    }

    // ✅ 유효성 검사 없이 저장
    private List<QuestionFull> collectQuestionsWithoutValidation() {
        List<QuestionFull> result = new ArrayList<>();
        for (QuestionRowPanel row : questionRows) {
            result.add(row.toQuestionFull());
        }
        return result;
    }
}
