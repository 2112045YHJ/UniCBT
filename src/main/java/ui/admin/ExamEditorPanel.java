package main.java.ui.admin;

import main.java.model.Exam;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.function.Consumer;

/**
 * 시험 등록/수정 화면 (슬라이드 23번 기반)
 * - 과목명, 제한 시간, 시작일, 마감일 입력
 * - 기존 Exam 객체가 주어지면 수정 모드로 동작
 * - 콜백으로 다음/이전 화면 이동 처리
 */
public class ExamEditorPanel extends JPanel {

    private final JTextField subjectField = new JTextField();
    private final JSpinner durationSpinner = new JSpinner(new SpinnerNumberModel(60, 10, 180, 5));
    private final JSpinner startDateSpinner = new JSpinner(new SpinnerDateModel());
    private final JSpinner endDateSpinner = new JSpinner(new SpinnerDateModel());

    private final JButton backButton = new JButton("이전");
    private final JButton nextButton = new JButton("다음");

    private final Consumer<Exam> onNext;
    private final Runnable onBack;

    private Exam editingExam;

    public ExamEditorPanel(Exam exam, Consumer<Exam> onNext, Runnable onBack) {
        this.editingExam = exam;
        this.onNext = onNext;
        this.onBack = onBack;

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
        panel.add(backButton);
        panel.add(nextButton);

        backButton.addActionListener(e -> onBack.run());
        nextButton.addActionListener(e -> {
            if (validateInput()) {
                if (editingExam == null) editingExam = new Exam();
                editingExam.setSubject(subjectField.getText().trim());
                editingExam.setDurationMinutes((int) durationSpinner.getValue());
                editingExam.setStartDate(convert((Date) startDateSpinner.getValue()));
                editingExam.setEndDate(convert((Date) endDateSpinner.getValue()));
                onNext.accept(editingExam);
            }
        });

        return panel;
    }

    private void loadExamIfEditing() {
        if (editingExam != null) {
            subjectField.setText(editingExam.getSubject());
            durationSpinner.setValue(editingExam.getDurationMinutes());
            startDateSpinner.setValue(java.sql.Timestamp.valueOf(editingExam.getStartDate()));
            endDateSpinner.setValue(java.sql.Timestamp.valueOf(editingExam.getEndDate()));
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
