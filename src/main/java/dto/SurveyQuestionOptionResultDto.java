package main.java.dto;

import main.java.model.SurveyQuestionOption;

/**
 * 객관식 설문 질문의 한 선택지에 대한 응답 결과 정보를 담는 DTO입니다.
 */
public class SurveyQuestionOptionResultDto {
    private SurveyQuestionOption option; // 선택지 객체
    private int responseCount;           // 이 선택지를 선택한 응답 수
    private double selectionRate;        // 이 선택지의 선택 비율 (0.0 ~ 100.0)

    public SurveyQuestionOptionResultDto(SurveyQuestionOption option, int responseCount, double selectionRate) {
        this.option = option;
        this.responseCount = responseCount;
        this.selectionRate = selectionRate;
    }

    // Getter 메서드들
    public SurveyQuestionOption getOption() {
        return option;
    }

    public int getResponseCount() {
        return responseCount;
    }

    public double getSelectionRate() {
        return selectionRate;
    }

    // Setter는 필요에 따라 추가
}