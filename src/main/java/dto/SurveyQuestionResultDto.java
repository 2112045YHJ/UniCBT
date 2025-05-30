package main.java.dto;

import main.java.model.SurveyQuestion;
import java.util.List;
import java.util.ArrayList; // ArrayList 임포트

/**
 * 특정 설문 질문에 대한 응답 결과 정보를 담는 DTO입니다.
 */
public class SurveyQuestionResultDto {
    private SurveyQuestion question; // 질문 객체
    private String questionType;     // 질문 유형 ("MCQ", "TEXT")
    private int totalResponsesForThisQuestion; // 이 질문에 대한 총 응답 수

    // MCQ 유형일 경우 사용될 선택지별 결과 목록
    private List<SurveyQuestionOptionResultDto> mcqOptionResults;

    // TEXT 유형일 경우 사용될 주관식 답변 목록
    private List<String> textResponses;

    public SurveyQuestionResultDto(SurveyQuestion question) {
        this.question = question;
        this.questionType = question.getQuestionType(); // SurveyQuestion 모델에 getType()이 있다고 가정
        this.mcqOptionResults = new ArrayList<>();
        this.textResponses = new ArrayList<>();
    }

    // Getter 및 Setter
    public SurveyQuestion getQuestion() {
        return question;
    }

    public void setQuestion(SurveyQuestion question) {
        this.question = question;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public int getTotalResponsesForThisQuestion() {
        return totalResponsesForThisQuestion;
    }

    public void setTotalResponsesForThisQuestion(int totalResponsesForThisQuestion) {
        this.totalResponsesForThisQuestion = totalResponsesForThisQuestion;
    }

    public List<SurveyQuestionOptionResultDto> getMcqOptionResults() {
        return mcqOptionResults;
    }

    public void setMcqOptionResults(List<SurveyQuestionOptionResultDto> mcqOptionResults) {
        this.mcqOptionResults = mcqOptionResults;
    }

    public List<String> getTextResponses() {
        return textResponses;
    }

    public void setTextResponses(List<String> textResponses) {
        this.textResponses = textResponses;
    }
}