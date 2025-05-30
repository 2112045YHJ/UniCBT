package main.java.dao;

import main.java.model.AnswerSheet;
import java.sql.Connection; // 데이터베이스 연결을 위한 Connection
import java.util.List;

public interface AnswerSheetDao {

    // --- Connection을 파라미터로 받는 메서드들 ---

    /**
     * 사용자의 답안 정보를 저장합니다.
     * @param sheet 저장할 AnswerSheet 객체
     * @param conn 데이터베이스 연결 객체
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    void insert(AnswerSheet sheet, Connection conn) throws DaoException;

    /**
     * 특정 사용자의 특정 시험에 대한 모든 답안을 조회합니다.
     * @param userId 사용자 ID
     * @param examId 시험 ID
     * @param conn 데이터베이스 연결 객체
     * @return 해당 사용자의 시험 답안 목록
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    List<AnswerSheet> findByUserAndExam(int userId, int examId, Connection conn) throws DaoException;

    /**
     * 특정 사용자의 특정 시험에 대한 모든 답안을 삭제합니다.
     * @param userId 사용자 ID
     * @param examId 시험 ID
     * @param conn 데이터베이스 연결 객체
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    void deleteByUserAndExam(int userId, int examId, Connection conn) throws DaoException;

    /**
     * 개별 파라미터로 사용자의 답안 정보를 저장합니다.
     * @param userId 사용자 ID
     * @param examId 시험 ID
     * @param questionId 문제 ID
     * @param answer 선택한 답
     * @param conn 데이터베이스 연결 객체
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    void insert(int userId, int examId, int questionId, String answer, Connection conn) throws DaoException;


    // --- 기존 시그니처 메서드들 ---

    void insert(AnswerSheet sheet) throws DaoException;
    List<AnswerSheet> findByUserAndExam(int userId, int examId) throws DaoException;
    void deleteByUserAndExam(int userId, int examId) throws DaoException;
    void insert(int userId, int examId, int questionId, String answer) throws DaoException;
}