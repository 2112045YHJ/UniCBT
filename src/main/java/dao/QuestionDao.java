package main.java.dao;

import main.java.model.QuestionFull;
import java.sql.Connection; // 데이터베이스 연결을 위한 Connection
import java.util.List;

public interface QuestionDao {

    // --- Connection을 파라미터로 받는 메서드 ---
    /**
     * 특정 시험에 해당하는 모든 문제 정보(선택지, 정답 포함)를 조회합니다.
     * @param examId 시험 ID
     * @param conn 데이터베이스 연결 객체
     * @return 해당 시험의 전체 문제 정보 목록
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    List<QuestionFull> findFullByExamId(int examId, Connection conn) throws DaoException;

    // --- 기존 시그니처 메서드 ---
    List<QuestionFull> findFullByExamId(int examId) throws DaoException;
}