package main.java.dao;

import main.java.model.SurveyResponse; // SurveyResponse 모델 임포트 (필요시)
import java.sql.Connection;
import java.util.List; // List 임포트 (필요시)


public interface SurveyResponseDao {
    int countBySurveyId(int surveyId, Connection conn) throws DaoException;
    int countBySurveyId(int surveyId) throws DaoException;

    void deleteBySurveyId(int surveyId, Connection conn) throws DaoException;
    void deleteBySurveyId(int surveyId) throws DaoException;

    // 학생이 설문 응답 제출 시 사용될 save 메서드
    void saveResponse(SurveyResponse response, Connection conn) throws DaoException; // SurveyResponse 모델 사용
    void saveResponse(SurveyResponse response) throws DaoException;

    // 또는 개별 응답들을 한 번에 저장 (Map<QuestionId, AnswerText>)
    // void saveAllResponses(int userId, int surveyId, Map<Integer, String> responses, Connection conn) throws DaoException;
    // void saveAllResponses(int userId, int surveyId, Map<Integer, String> responses) throws DaoException;


    // 설문 결과 분석을 위한 조회 메서드들 (예시)
    List<SurveyResponse> findResponsesBySurveyAndQuestion(int surveyId, int questionId, Connection conn) throws DaoException;
    List<SurveyResponse> findResponsesBySurveyAndQuestion(int surveyId, int questionId) throws DaoException;

    /**
     * 특정 사용자가 특정 설문조사에 응답했는지 여부를 확인합니다.
     * @param userId 사용자 ID
     * @param surveyId 설문조사 ID
     * @param conn 데이터베이스 연결 객체
     * @return 응답한 적이 있으면 true, 없으면 false
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    boolean hasUserResponded(int userId, int surveyId, Connection conn) throws DaoException;
    boolean hasUserResponded(int userId, int surveyId) throws DaoException; // 자체 Connection 관리 버전
}