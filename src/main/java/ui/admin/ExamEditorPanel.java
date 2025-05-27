package main.java.ui.admin;

import main.java.context.ExamCreationContext;
import main.java.model.Exam;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.function.Consumer;

public class ExamEditorPanel extends JPanel {

    private final JTextField subjectField = new JTextField();
    private final JSpinner durationSpinner = new JSpinner(new SpinnerNumberModel(60, 10, 180, 5));
    private final JSpinner startDateSpinner = new JSpinner(new SpinnerDateModel());
    private final JSpinner endDateSpinner = new JSpinner(new SpinnerDateModel());

    private final JButton backButton = new JButton("이전");
    private final JButton nextButton = new JButton("다음");

    private final ExamCreationContext context;
    private final Runnable onBack;
    private final JFrame parentFrame;

    public ExamEditorPanel(ExamCreationContext context, Runnable onBack, JFrame parentFrame) {
        this.context = context;
        this.onBack = onBack;
        this.parentFrame = parentFrame;

        setLayout(new BorderLayout());
        add(createHeader(), BorderLayout.NORTH);
        add(createForm(), BorderLayout.CENTER);
        add(createButtons(), BorderLayout.SOUTH);

        loadExamIfEditing();
    }

    private JComponent createHeader() {
        JLabel header = new JLabel("시험 정보 입력", SwingConstants.CENTER);
        header.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return header;
    }

    private JPanel createForm() {
        JPanel form = new JPanel(new GridLayout(4, 2, 10, 10));
        form.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));

        form.add(new JLabel("과목명"));
        form.add(subjectField);

        form.add(new JLabel("제한시간 (분)"));
        form.add(durationSpinner);

        form.add(new JLabel("시작일"));
        startDateSpinner.setEditor(new JSpinner.DateEditor(startDateSpinner, "yyyy-MM-dd HH:mm"));
        form.add(startDateSpinner);

        form.add(new JLabel("마감일"));
        endDateSpinner.setEditor(new JSpinner.DateEditor(endDateSpinner, "yyyy-MM-dd HH:mm"));
        form.add(endDateSpinner);

        return form;
    }

    private JPanel createButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        backButton.addActionListener(e -> onBack.run());

        nextButton.addActionListener(e -> {
            if (validateInput()) {
                Exam exam = context.getExam();
                if (exam == null) exam = new Exam();

                exam.setSubject(subjectField.getText().trim());
                exam.setDurationMinutes((int) durationSpinner.getValue());
                exam.setStartDate(convert((Date) startDateSpinner.getValue()));
                exam.setEndDate(convert((Date) endDateSpinner.getValue()));

                context.setExam(exam);

                // 다음 화면: 문제 입력 패널로 교체
                parentFrame.setContentPane(new QuestionEditorPanel(context,
                        () -> parentFrame.setContentPane(new ExamEditorPanel(context, onBack, parentFrame)),
                        () -> System.out.println("다음 단계로 이동 예정")));
                parentFrame.revalidate();
            }
        });

        panel.add(backButton);
        panel.add(nextButton);
        return panel;
    }

    private void loadExamIfEditing() {
        Exam exam = context.getExam();
        if (exam != null) {
            subjectField.setText(exam.getSubject());
            durationSpinner.setValue(exam.getDurationMinutes());
            startDateSpinner.setValue(java.sql.Timestamp.valueOf(exam.getStartDate()));
            endDateSpinner.setValue(java.sql.Timestamp.valueOf(exam.getEndDate()));
        }
    }

    private boolean validateInput() {
        String subject = subjectField.getText().trim();
        if (subject.isEmpty()) {
            showError("과목명을 입력해주세요.");
            return false;
        }
        Date start = (Date) startDateSpinner.getValue();
        Date end = (Date) endDateSpinner.getValue();
        Date now = new Date();

        if (!end.after(now)) {
            showError("마감일은 현재 시간보다 이후로 설정해야 합니다.");
            return false;
        }
        if (!end.after(start)) {
            showError("마감일은 시작일보다 늦어야 합니다.");
            return false;
        }

        return true;
    }

    private LocalDateTime convert(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "입력 오류", JOptionPane.WARNING_MESSAGE);
    }
}
