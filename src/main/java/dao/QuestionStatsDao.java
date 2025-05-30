package main.java.dao;

import main.java.model.QuestionStat; // QuestionStat 모델 사용 가정
import java.sql.Connection;
import java.util.List;

public interface QuestionStatsDao {
    /**
     * 특정 시험의 모든 문제 통계를 조회합니다.
     * @param examId 시험 ID
     * @param conn 데이터베이스 연결 객체
     * @return 문제 통계 목록
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    List<QuestionStat> findByExamId(int examId, Connection conn) throws DaoException;
    List<QuestionStat> findByExamId(int examId) throws DaoException;

    /**
     * 문제 통계 정보를 저장하거나 업데이트합니다 (UPSERT).
     * @param questionStat 저장 또는 업데이트할 QuestionStat 객체
     * @param conn 데이터베이스 연결 객체
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    void saveOrUpdate(QuestionStat questionStat, Connection conn) throws DaoException;
    void saveOrUpdate(QuestionStat questionStat) throws DaoException;

    /**
     * 특정 문제에 대한 통계 정보를 업데이트합니다. (응시자 수, 정답자 수 증가 등)
     * @param questionId 문제 ID
     * @param examId 시험 ID
     * @param isCorrect 정답 여부
     * @param conn 데이터베이스 연결 객체
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    void recordAttempt(int questionId, int examId, String questionType, boolean isCorrect, Connection conn) throws DaoException;
}