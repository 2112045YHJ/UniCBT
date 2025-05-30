package main.java.dto;

import main.java.model.Survey;
import main.java.model.SurveyQuestion;
import main.java.model.SurveyQuestionOption;

import java.util.List;
import java.util.ArrayList; // ArrayList 임포트

public class SurveyFullDto {
    private Survey survey; // 설문 기본 정보
    private List<SurveyQuestionDto> questions; // 질문 목록 (각 질문은 선택지 목록을 가질 수 있음)

    public SurveyFullDto() {
        this.questions = new ArrayList<>();
    }

    // Getter 및 Setter
    public Survey getSurvey() {
        return survey;
    }

    public void setSurvey(Survey survey) {
        this.survey = survey;
    }

    public List<SurveyQuestionDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<SurveyQuestionDto> questions) {
        this.questions = questions;
    }

    /**
     * SurveyFullDto 내부에 사용될 질문 DTO입니다.
     * SurveyQuestion 모델과 함께 해당 질문의 선택지(options) 목록을 가집니다.
     */
    public static class SurveyQuestionDto {
        private SurveyQuestion question; // 질문 기본 정보
        private List<SurveyQuestionOption> options; // 해당 질문의 객관식 선택지 목록

        public SurveyQuestionDto() {
            this.options = new ArrayList<>();
        }

        // Getter 및 Setter
        public SurveyQuestion getQuestion() {
            return question;
        }

        public void setQuestion(SurveyQuestion question) {
            this.question = question;
        }

        public List<SurveyQuestionOption> getOptions() {
            return options;
        }

        public void setOptions(List<SurveyQuestionOption> options) {
            this.options = options;
        }
    }
}