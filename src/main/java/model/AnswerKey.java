package main.java.model;

public class AnswerKey {
    private int questionId;
    private Character correctLabel;  // MCQ 정답
    private String correctText;      // SA 정답

    public AnswerKey() {
    }

    public AnswerKey(int questionId, Character correctLabel, String correctText) {
        this.questionId = questionId;
        this.correctLabel = correctLabel;
        this.correctText = correctText;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public Character getCorrectLabel() {
        return correctLabel;
    }

    public void setCorrectLabel(Character correctLabel) {
        this.correctLabel = correctLabel;
    }

    public String getCorrectText() {
        return correctText;
    }

    public void setCorrectText(String correctText) {
        this.correctText = correctText;
    }
}