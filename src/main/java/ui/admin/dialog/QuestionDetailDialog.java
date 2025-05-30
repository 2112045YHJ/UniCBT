package main.java.ui.admin.dialog; // 다이얼로그를 위한 새 패키지 또는 기존 admin 패키지 사용

import main.java.model.QuestionFull;
import main.java.model.QuestionOption;
import main.java.service.QuestionService;
import main.java.service.QuestionServiceImpl;
import main.java.service.ServiceException;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.util.List;


/**
 * 문제의 상세 내용을 보여주는 다이얼로그입니다.
 */
public class QuestionDetailDialog extends JDialog {
    private final int questionId;
    private final int examId; // 특정 시험 내의 문제를 식별하기 위해 필요
    private final QuestionService questionService = new QuestionServiceImpl();

    public QuestionDetailDialog(Frame owner, int questionId, int examId) {
        super(owner, "문제 내용 상세 (문제 ID: " + questionId + ", 시험 ID: " + examId + ")", true);
        this.questionId = questionId;
        this.examId = examId;

        setSize(650, 450); // 다이얼로그 크기
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        StyledDocument doc = textPane.getStyledDocument();
        addStylesToDocument(doc); // 스타일 정의 (위 StudentAnswerSheetDialog의 것과 유사하게 정의)


        try {
            // QuestionService를 통해 해당 시험의 모든 문제를 가져온 후, 특정 questionId로 필터링
            List<QuestionFull> questionsInExam = questionService.getQuestionsByExam(examId);
            QuestionFull qf = null;
            for (QuestionFull q : questionsInExam) {
                if (q.getQuestionId() == questionId) {
                    qf = q;
                    break;
                }
            }

            if (qf != null) {
                doc.insertString(doc.getLength(), "문제 ID: " + qf.getQuestionId() + "\n", doc.getStyle("bold"));
                doc.insertString(doc.getLength(), "시험 ID: " + qf.getExamId() + "\n", doc.getStyle("bold"));
                doc.insertString(doc.getLength(), "유형: " + qf.getType().name() + "\n\n", doc.getStyle("bold"));

                doc.insertString(doc.getLength(), "문제 내용:\n", doc.getStyle("italicHeader"));
                doc.insertString(doc.getLength(), qf.getQuestionText() + "\n\n", doc.getStyle("regular"));

                if (qf.getType() == main.java.model.QuestionType.MCQ && qf.getOptions() != null && !qf.getOptions().isEmpty()) {
                    doc.insertString(doc.getLength(), "보기:\n", doc.getStyle("italicHeader"));
                    for (QuestionOption opt : qf.getOptions()) {
                        doc.insertString(doc.getLength(), "  " + opt.getOptionLabel() + ". " + opt.getContent() + "\n", doc.getStyle("regular"));
                    }
                    doc.insertString(doc.getLength(), "\n", doc.getStyle("regular"));
                }

                doc.insertString(doc.getLength(), "정답: ", doc.getStyle("italicHeader"));
                if (qf.getAnswerKey() != null) {
                    if (qf.getType() == main.java.model.QuestionType.MCQ && qf.getAnswerKey().getCorrectLabel() != null) {
                        doc.insertString(doc.getLength(), String.valueOf(qf.getAnswerKey().getCorrectLabel()), doc.getStyle("bold"));
                    } else if (qf.getType() == main.java.model.QuestionType.OX && qf.getAnswerKey().getCorrectText() != null) {
                        doc.insertString(doc.getLength(), qf.getAnswerKey().getCorrectText(), doc.getStyle("bold"));
                    } else {
                        doc.insertString(doc.getLength(), "정보 없음", doc.getStyle("regular"));
                    }
                } else {
                    doc.insertString(doc.getLength(), "정보 없음", doc.getStyle("regular"));
                }
                doc.insertString(doc.getLength(), "\n", doc.getStyle("regular"));

            } else {
                doc.insertString(doc.getLength(), "해당 문제 정보를 찾을 수 없습니다. (문제 ID: " + questionId + ", 시험 ID: " + examId + ")", doc.getStyle("error"));
            }
        } catch (ServiceException | BadLocationException e) {
            try {
                doc.insertString(doc.getLength(), "문제 정보를 불러오는 중 오류가 발생했습니다: " + e.getMessage(), doc.getStyle("error"));
            } catch (BadLocationException ignored) {}
            e.printStackTrace();
        }

        add(new JScrollPane(textPane), BorderLayout.CENTER);

        JButton closeButton = new JButton("닫기");
        closeButton.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        closeButton.addActionListener(e -> dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5,0,5,5));
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * JTextPane에 사용할 스타일들을 정의합니다.
     */
    private void addStylesToDocument(StyledDocument doc) {
        Style regular = doc.addStyle("regular", null);
        StyleConstants.setFontFamily(regular, "맑은 고딕");
        StyleConstants.setFontSize(regular, 13);

        Style bold = doc.addStyle("bold", regular);
        StyleConstants.setBold(bold, true);

        Style italicHeader = doc.addStyle("italicHeader", regular);
        StyleConstants.setItalic(italicHeader, true);
        StyleConstants.setBold(italicHeader, true);
        StyleConstants.setForeground(italicHeader, Color.BLUE.darker());


        Style error = doc.addStyle("error", regular);
        StyleConstants.setForeground(error, Color.RED);
        StyleConstants.setBold(error, true);
    }
}