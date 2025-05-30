package main.java.dao;

import main.java.model.SurveyQuestionOption;
import java.sql.Connection;
import java.util.List;

public interface SurveyQuestionOptionDao {
    // 특정 질문에 속한 모든 선택지 조회
    List<SurveyQuestionOption> findByQuestionId(int questionId, Connection conn) throws DaoException;
    List<SurveyQuestionOption> findByQuestionId(int questionId) throws DaoException;

    // 단일 선택지 저장 (질문 생성/수정 시 사용)
    void save(SurveyQuestionOption option, Connection conn) throws DaoException;
    void save(SurveyQuestionOption option) throws DaoException;

    // 여러 선택지 일괄 저장 (질문 생성/수정 시 사용)
    void saveAll(List<SurveyQuestionOption> options, Connection conn) throws DaoException;
    void saveAll(List<SurveyQuestionOption> options) throws DaoException;

    // 특정 질문에 속한 모든 선택지 삭제 (질문 수정/삭제 시 사용)
    void deleteByQuestionId(int questionId, Connection conn) throws DaoException;
    void deleteByQuestionId(int questionId) throws DaoException;
}