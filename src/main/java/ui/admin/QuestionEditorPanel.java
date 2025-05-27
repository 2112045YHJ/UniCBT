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

    public QuestionEditorPanel(ExamCreationContext context, Runnable onBack, Runnable onNext) {
        this.context = context;
        this.onBack = onBack;
        this.onNext = onNext;

        setLayout(new BorderLayout());

        questionListPanel.setLayout(new BoxLayout(questionListPanel, BoxLayout.Y_AXIS));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
        add(createTopPanel(), BorderLayout.NORTH);
        add(createBottomPanel(), BorderLayout.SOUTH);
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
        prevBtn.addActionListener(e -> onBack.run());
        nextBtn.addActionListener(e -> {
            List<QuestionFull> questionList = collectQuestions();
            if (questionList == null || questionList.isEmpty()) {
                JOptionPane.showMessageDialog(this, "문제를 한 개 이상 등록해주세요.", "오류", JOptionPane.WARNING_MESSAGE);
                return;
            }
            context.setQuestions(questionList);

            // ✅ TargetSelectionDialog 호출
            TargetSelectionDialog dialog = new TargetSelectionDialog(
                    (JFrame) SwingUtilities.getWindowAncestor(this),
                    context,
                    () -> {
                        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
                        frame.setContentPane(new ExamMgmtPanel());
                        frame.revalidate();
                        frame.repaint();
                    }
            );
            dialog.setVisible(true);

            // 응시 대상이 정상적으로 설정된 경우에만 다음 단계로
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
}
