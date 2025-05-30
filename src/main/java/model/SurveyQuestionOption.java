package main.java.model;

public class SurveyQuestionOption {
    private int optionId;
    private int questionId;
    private String optionText;
    private int optionOrder; // 선택적 필드

    public SurveyQuestionOption() {}

    public SurveyQuestionOption(int questionId, String optionText, int optionOrder) {
        this.questionId = questionId;
        this.optionText = optionText;
        this.optionOrder = optionOrder;
    }

    // Getter 및 Setter 메서드들
    public int getOptionId() { return optionId; }
    public void setOptionId(int optionId) { this.optionId = optionId; }
    public int getQuestionId() { return questionId; }
    public void setQuestionId(int questionId) { this.questionId = questionId; }
    public String getOptionText() { return optionText; }
    public void setOptionText(String optionText) { this.optionText = optionText; }
    public int getOptionOrder() { return optionOrder; }
    public void setOptionOrder(int optionOrder) { this.optionOrder = optionOrder; }
}