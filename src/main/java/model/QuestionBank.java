package main.java.model;

public class QuestionBank {
    private int questionId;
    private int examId;
    private String type;         // 'MCQ' or 'OX'
    private String questionText;

    public QuestionBank() {
    }

    public QuestionBank(int questionId, int examId, String type, String questionText) {
        this.questionId = questionId;
        this.examId = examId;
        this.type = type;
        this.questionText = questionText;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public int getExamId() {
        return examId;
    }

    public void setExamId(int examId) {
        this.examId = examId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }
}