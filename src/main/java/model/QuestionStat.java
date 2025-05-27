package main.java.model;

public class QuestionStat {
    private int questionId;          // 문제 ID (FK)
    private Integer examId;          // 시험 ID (FK, 전체 통계면 null 허용)
    private int attempts;            // 응시자 수
    private int correctCount;        // 정답자 수
    private float correctRate;       // 정답률
    private String questionType;     // 문제 유형
    private String subSubject;       // 세부 과목

    public QuestionStat() {}

    public QuestionStat(int questionId,
                        Integer examId,
                        int attempts,
                        int correctCount,
                        float correctRate,
                        String questionType,
                        String subSubject) {
        this.questionId   = questionId;
        this.examId       = examId;
        this.attempts     = attempts;
        this.correctCount = correctCount;
        this.correctRate  = correctRate;
        this.questionType = questionType;
        this.subSubject   = subSubject;
    }

    public int getQuestionId() {
        return questionId;
    }
    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public Integer getExamId() {
        return examId;
    }
    public void setExamId(Integer examId) {
        this.examId = examId;
    }

    public int getAttempts() {
        return attempts;
    }
    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public int getCorrectCount() {
        return correctCount;
    }
    public void setCorrectCount(int correctCount) {
        this.correctCount = correctCount;
    }

    public float getCorrectRate() {
        return correctRate;
    }
    public void setCorrectRate(float correctRate) {
        this.correctRate = correctRate;
    }

    public String getQuestionType() {
        return questionType;
    }
    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getSubSubject() {
        return subSubject;
    }
    public void setSubSubject(String subSubject) {
        this.subSubject = subSubject;
    }
}

