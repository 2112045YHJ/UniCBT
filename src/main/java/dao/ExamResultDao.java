package main.java.dao;

import main.java.model.ExamResult;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

public interface ExamResultDao {

    void insert(ExamResult result, Connection conn) throws DaoException;
    ExamResult findByUserAndExam(int userId, int examId, Connection conn) throws DaoException;
    Map<Integer, ExamResult> findAllByUser(int userId, Connection conn) throws DaoException;
    boolean existsByUserAndExam(int userId, int examId, Connection conn) throws DaoException;
    int countResultsByExam(int examId, Connection conn) throws DaoException; // 이전 단계에서 추가된 메서드

    void insert(ExamResult result) throws DaoException;
    ExamResult findByUserAndExam(int userId, int examId) throws DaoException;
    Map<Integer, ExamResult> findAllByUser(int userId) throws DaoException;
    boolean existsByUserAndExam(int userId, int examId) throws DaoException;
    /**
     * 특정 시험 ID에 해당하는 모든 시험 결과 목록을 조회합니다.
     * @param examId 시험 ID
     * @param conn 데이터베이스 연결 객체
     * @return 해당 시험의 모든 ExamResult 목록
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    List<ExamResult> findAllByExam(int examId, Connection conn) throws DaoException;
    List<ExamResult> findAllByExam(int examId) throws DaoException; // Connection 안 받는 버전
    /**
     * 특정 사용자의 특정 시험에 대한 시험 결과를 삭제합니다.
     * @param userId 사용자 ID
     * @param examId 시험 ID
     * @param conn 데이터베이스 연결 객체
     * @return 삭제된 행의 수 (보통 1 또는 0)
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    int deleteByUserAndExam(int userId, int examId, Connection conn) throws DaoException;

    /**
     * 특정 사용자의 특정 시험에 대한 시험 결과를 삭제합니다. (자체 Connection 관리)
     * @param userId 사용자 ID
     * @param examId 시험 ID
     * @return 삭제된 행의 수 (보통 1 또는 0)
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    int deleteByUserAndExam(int userId, int examId) throws DaoException;
}