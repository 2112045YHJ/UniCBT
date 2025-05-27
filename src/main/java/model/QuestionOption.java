package main.java.model;

public class QuestionOption {
    private int optionId;
    private int questionId;
    private char optionLabel;
    private String content;

    public QuestionOption() {
    }

    public QuestionOption(int optionId, int questionId, char optionLabel, String content) {
        this.optionId = optionId;
        this.questionId = questionId;
        this.optionLabel = optionLabel;
        this.content = content;
    }

    public int getOptionId() {
        return optionId;
    }

    public void setOptionId(int optionId) {
        this.optionId = optionId;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public char getOptionLabel() {
        return optionLabel;
    }

    public void setOptionLabel(char optionLabel) {
        this.optionLabel = optionLabel;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}