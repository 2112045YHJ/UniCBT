package main.java.ui.admin.dialog; // 다이얼로그를 위한 새 패키지 또는 기존 admin 패키지 사용

import main.java.model.AnswerSheet;
import main.java.model.QuestionFull;
import main.java.model.QuestionOption;
import main.java.model.AnswerKey; // AnswerKey 임포트
import main.java.service.ExamService; // 전체 시험 정보를 가져오기 위함 (선택적)
import main.java.service.ExamServiceImpl;
import main.java.service.QuestionService;
import main.java.service.QuestionServiceImpl;
import main.java.service.ServiceException;
import main.java.dao.AnswerSheetDao; // 학생 답안 조회를 위해 필요
import main.java.dao.AnswerSheetDaoImpl;
import main.java.dao.DaoException;


import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap; // 추가

/**
 * 학생의 개별 시험 답안지 상세 내용을 보여주는 다이얼로그입니다.
 */
public class StudentAnswerSheetDialog extends JDialog {
    private final int examId;
    private final int userId;
    // private final String titleInfo; // 생성자에서 직접 제목 설정

    // 서비스 계층 또는 DAO 직접 사용 (여기서는 Service를 통해 QuestionFull을, AnswerSheetDao를 통해 답안을 가져옴)
    private final QuestionService questionService = new QuestionServiceImpl();
    private final AnswerSheetDao answerSheetDao = new AnswerSheetDaoImpl(); // 학생 답안 직접 조회

    public StudentAnswerSheetDialog(Frame owner, int examId, int userId, String studentDisplayInfo) {
        super(owner, "학생 답안지: " + studentDisplayInfo + " (시험 ID: " + examId + ")", true);
        this.examId = examId;
        this.userId = userId;

        setSize(750, 600); // 다이얼로그 크기 조정
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        // 내용을 표시할 JTextPane 사용 (스타일 적용 용이)
        JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        StyledDocument doc = textPane.getStyledDocument();
        addStylesToDocument(doc); // 스타일 정의

        try {
            List<QuestionFull> questions = questionService.getQuestionsByExam(examId);
            // 학생의 답안들을 Map<QuestionId, SubmittedAnswerString> 형태로 가져옴
            Map<Integer, String> studentAnswersMap = getStudentAnswersMap(userId, examId);

            if (questions.isEmpty()) {
                doc.insertString(doc.getLength(), "이 시험에 출제된 문제가 없습니다.", doc.getStyle("regular"));
            } else {
                int questionNumber = 1;
                for (QuestionFull qf : questions) {
                    doc.insertString(doc.getLength(), "문제 " + (questionNumber++) + ". ", doc.getStyle("bold"));
                    doc.insertString(doc.getLength(), qf.getQuestionText() + "\n", doc.getStyle("regular"));

                    String correctAnswerDisplay = "";
                    AnswerKey ak = qf.getAnswerKey(); // QuestionFull 내부에 AnswerKey가 있다고 가정

                    if (qf.getType() == main.java.model.QuestionType.MCQ) {
                        if (qf.getOptions() != null) {
                            doc.insertString(doc.getLength(), "  보기:\n", doc.getStyle("italic"));
                            for (QuestionOption opt : qf.getOptions()) {
                                doc.insertString(doc.getLength(), "    " + opt.getOptionLabel() + ". " + opt.getContent() + "\n", doc.getStyle("regular"));
                            }
                        }
                        if (ak != null && ak.getCorrectLabel() != null) {
                            correctAnswerDisplay = String.valueOf(ak.getCorrectLabel());
                        }
                    } else if (qf.getType() == main.java.model.QuestionType.OX) {
                        if (ak != null && ak.getCorrectText() != null) {
                            correctAnswerDisplay = ak.getCorrectText();
                        }
                    }

                    String studentSelectedAnswer = studentAnswersMap.getOrDefault(qf.getQuestionId(), "미응답");
                    boolean isStudentCorrect = studentSelectedAnswer.equals(correctAnswerDisplay);

                    doc.insertString(doc.getLength(), "  학생 답: ", doc.getStyle("italic"));
                    doc.insertString(doc.getLength(), studentSelectedAnswer, isStudentCorrect ? doc.getStyle("correct") : doc.getStyle("incorrect"));
                    if (!isStudentCorrect && !studentSelectedAnswer.equals("미응답")) {
                        doc.insertString(doc.getLength(), " (오답)", doc.getStyle("incorrectBold"));
                    } else if (isStudentCorrect) {
                        doc.insertString(doc.getLength(), " (정답)", doc.getStyle("correctBold"));
                    }
                    doc.insertString(doc.getLength(), "\n", doc.getStyle("regular"));
                    doc.insertString(doc.getLength(), "  정답: " + correctAnswerDisplay + "\n\n", doc.getStyle("italic"));
                }
            }
        } catch (ServiceException | DaoException | BadLocationException e) {
            try {
                doc.insertString(doc.getLength(), "답안 정보를 불러오는 중 오류가 발생했습니다: " + e.getMessage(), doc.getStyle("error"));
            } catch (BadLocationException ignored) {}
            e.printStackTrace(); // 개발 중 상세 오류 확인
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

        Style italic = doc.addStyle("italic", regular);
        StyleConstants.setItalic(italic, true);
        StyleConstants.setForeground(italic, Color.DARK_GRAY);


        Style correct = doc.addStyle("correct", regular);
        StyleConstants.setForeground(correct, new Color(0, 128, 0)); // Green

        Style correctBold = doc.addStyle("correctBold", correct);
        StyleConstants.setBold(correctBold, true);

        Style incorrect = doc.addStyle("incorrect", regular);
        StyleConstants.setForeground(incorrect, Color.RED);

        Style incorrectBold = doc.addStyle("incorrectBold", incorrect);
        StyleConstants.setBold(incorrectBold, true);

        Style error = doc.addStyle("error", regular);
        StyleConstants.setForeground(error, Color.RED);
        StyleConstants.setBold(error, true);
    }

    /**
     * 특정 사용자의 특정 시험에 대한 답안들을 Map 형태로 가져옵니다.
     * @param userId 사용자 ID
     * @param examId 시험 ID
     * @return 문제 ID를 키로, 학생이 제출한 답을 값으로 하는 Map
     * @throws DaoException 데이터 접근 오류
     */
    private Map<Integer, String> getStudentAnswersMap(int userId, int examId) throws DaoException {
        Map<Integer, String> answersMap = new HashMap<>();
        // AnswerSheetDao는 Connection을 받는 버전과 안 받는 버전이 있음.
        // 이 다이얼로그는 단일 조회이므로 자체 Connection 관리하는 버전 사용 가능.
        List<AnswerSheet> answerSheets = answerSheetDao.findByUserAndExam(userId, examId);
        for (AnswerSheet sheet : answerSheets) {
            answersMap.put(sheet.getQuestionId(), sheet.getSelectedAnswer());
        }
        return answersMap;
    }
}