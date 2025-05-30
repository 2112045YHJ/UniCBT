package main.java.dao;

import main.java.model.SurveyQuestion;
import java.sql.Connection;
import java.util.List;

public interface SurveyQuestionDao {
    List<SurveyQuestion> findBySurveyId(int surveyId, Connection conn) throws DaoException;
    List<SurveyQuestion> findBySurveyId(int surveyId) throws DaoException;

    /**
     * ID로 특정 설문 질문 정보를 조회합니다.
     * @param questionId 조회할 질문 ID
     * @param conn 데이터베이스 연결 객체
     * @return 조회된 SurveyQuestion 객체 (없으면 null)
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    SurveyQuestion findById(int questionId, Connection conn) throws DaoException;

    /**
     * ID로 특정 설문 질문 정보를 조회합니다. (자체 Connection 관리)
     * @param questionId 조회할 질문 ID
     * @return 조회된 SurveyQuestion 객체 (없으면 null)
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    SurveyQuestion findById(int questionId) throws DaoException;

    void save(SurveyQuestion question, Connection conn) throws DaoException;
    void save(SurveyQuestion question) throws DaoException;

    void deleteBySurveyId(int surveyId, Connection conn) throws DaoException;
    void deleteBySurveyId(int surveyId) throws DaoException;

    // 필요시 update(SurveyQuestion question, Connection conn) 등 추가
}