package main.java.model;

import java.util.List;

/**
 * QuestionFull 모델 클래스
 * - QuestionBank, QuestionOption 목록, AnswerKey를 통합한 DTO
 */
public class QuestionFull {
    private QuestionBank questionBank;
    private List<QuestionOption> options;
    private AnswerKey answerKey;

    /** 기본 생성자 */
    public QuestionFull() { }

    /**
     * 전체 필드를 사용하는 생성자
     */
    public QuestionFull(QuestionBank questionBank,
                        List<QuestionOption> options,
                        AnswerKey answerKey) {
        this.questionBank = questionBank;
        this.options = options;
        this.answerKey = answerKey;
    }

    // --- 내부 DTO 접근자 ---
    public QuestionBank getQuestionBank() {
        return questionBank;
    }
    public void setQuestionBank(QuestionBank questionBank) {
        this.questionBank = questionBank;
    }

    public List<QuestionOption> getOptions() {
        return options;
    }
    public void setOptions(List<QuestionOption> options) {
        this.options = options;
    }

    public AnswerKey getAnswerKey() {
        return answerKey;
    }
    public void setAnswerKey(AnswerKey answerKey) {
        this.answerKey = answerKey;
    }

    // --- 편의 메서드 (서비스/DAO 호출 시 사용) ---

    /** 시험 ID */
    public int getExamId() {
        return questionBank.getExamId();
    }

    /** 문제 ID */
    public int getQuestionId() {
        return questionBank.getQuestionId();
    }

    /**
     * 문제 유형 반환
     * DB에 저장된 "MCQ" 또는 "OX" 값을 QuestionType으로 변환
     */
    public QuestionType getType() {
        return QuestionType.valueOf(questionBank.getType());
    }

    /** 문제 텍스트 */
    public String getQuestionText() {
        return questionBank.getQuestionText();
    }

    /** 객관식 정답 레이블(1~5) */
    public Character getCorrectLabel() {
        return answerKey.getCorrectLabel();
    }

    /** OX 정답 텍스트("O" 또는 "X") */
    public String getCorrectText() {
        return answerKey.getCorrectText();
    }

    @Override
    public String toString() {
        return "QuestionFull{" +
                "questionBank=" + questionBank +
                ", options=" + options +
                ", answerKey=" + answerKey +
                '}';
    }
}