// 파일: main/java/ui/admin/QuestionRowPanel.java
package main.java.ui.admin;

import main.java.model.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionRowPanel extends JPanel {
    private final QuestionType type;
    private final QuestionEditorPanel parentPanel;

    private JLabel numberLabel;
    private JTextField questionField;
    private List<JTextField> optionsFields; // 객관식 보기
    private JComboBox<String> correctBox;

    public QuestionRowPanel(QuestionType type, QuestionEditorPanel parent) {
        this.type = type;
        this.parentPanel = parent;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // 좌측 번호 및 삭제 버튼
        JPanel leftPanel = new JPanel(new BorderLayout());
        numberLabel = new JLabel("Q.");
        numberLabel.setPreferredSize(new Dimension(30, 30));
        leftPanel.add(numberLabel, BorderLayout.WEST);

        JButton deleteBtn = new JButton("삭제");
        deleteBtn.addActionListener(e -> parentPanel.removeQuestionRow(this));
        leftPanel.add(deleteBtn, BorderLayout.EAST);
        add(leftPanel, BorderLayout.WEST);

        // 중앙 질문 및 보기/정답
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        questionField = new JTextField();
        questionField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        questionField.setBorder(BorderFactory.createTitledBorder("문제 내용"));
        contentPanel.add(questionField);

        if (type == QuestionType.MCQ) {
            optionsFields = new ArrayList<>();
            JPanel optionsPanel = new JPanel(new GridLayout(5, 2, 5, 5));
            for (int i = 1; i <= 5; i++) {
                JTextField opt = new JTextField();
                opt.setBorder(BorderFactory.createTitledBorder(i + "번 보기"));
                optionsFields.add(opt);
                optionsPanel.add(opt);
            }
            contentPanel.add(optionsPanel);

            correctBox = new JComboBox<>(new String[]{"1", "2", "3", "4", "5"});
            correctBox.setBorder(BorderFactory.createTitledBorder("정답"));
            contentPanel.add(correctBox);

        } else if (type == QuestionType.OX) {
            correctBox = new JComboBox<>(new String[]{"O", "X"});
            correctBox.setBorder(BorderFactory.createTitledBorder("정답"));
            contentPanel.add(correctBox);
        }

        add(contentPanel, BorderLayout.CENTER);
    }

    public void setQuestionNumber(int number) {
        numberLabel.setText("Q" + number);
    }

    public QuestionType getType() {
        return type;
    }

    public String getQuestionText() {
        return questionField.getText();
    }

    public List<String> getOptions() {
        if (type != QuestionType.MCQ) return null;
        List<String> list = new ArrayList<>();
        for (JTextField f : optionsFields) {
            list.add(f.getText());
        }
        return list;
    }

    public String getCorrectAnswer() {
        return (String) correctBox.getSelectedItem();
    }
    public boolean validateInputs() {
        String question = questionField.getText().trim();
        if (question.isEmpty()) {
            JOptionPane.showMessageDialog(this, "문제 내용을 입력해주세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        if (type == QuestionType.MCQ) {
            for (int i = 0; i < optionsFields.size(); i++) {
                String opt = optionsFields.get(i).getText().trim();
                if (opt.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            (i + 1) + "번 보기를 입력해주세요.",
                            "입력 오류", JOptionPane.WARNING_MESSAGE);
                    return false;
                }
            }
        }

        if (correctBox.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "정답을 선택해주세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        return true;
    }

    public QuestionFull toQuestionFull() {
        QuestionFull qf = new QuestionFull();

        QuestionBank qb = new QuestionBank();
        qb.setQuestionText(questionField.getText().trim());
        qb.setType(type.name());
        qf.setQuestionBank(qb);

        qf.setType(type);
        qf.setQuestionText(questionField.getText().trim());

        AnswerKey ak = new AnswerKey();

        if (type == QuestionType.MCQ) {
            List<QuestionOption> options = new ArrayList<>();
            for (int i = 0; i < optionsFields.size(); i++) {
                QuestionOption opt = new QuestionOption();
                opt.setOptionLabel((char) ('1' + i));
                opt.setContent(optionsFields.get(i).getText().trim());
                options.add(opt);
            }
            qf.setOptions(options);

            String selected = (String) correctBox.getSelectedItem();
            qf.setCorrectLabel(selected);
            ak.setCorrectLabel(selected.charAt(0));  // ← AnswerKey용
        } else if (type == QuestionType.OX) {
            String selected = (String) correctBox.getSelectedItem();
            qf.setCorrectText(selected);
            ak.setCorrectText(selected);  // ← AnswerKey용
        }

        qf.setAnswerKey(ak); // 🛠️ 이걸 안 해줘서 오류 났던 거예요!

        return qf;
    }

}
