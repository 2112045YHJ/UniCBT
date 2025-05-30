package main.java.dao;

import main.java.model.QuestionStat;
import main.java.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionStatsDaoImpl implements QuestionStatsDao {

    private QuestionStat mapRowToQuestionStat(ResultSet rs) throws SQLException {
        QuestionStat stat = new QuestionStat();
        stat.setQuestionId(rs.getInt("question_id"));
        stat.setExamId(rs.getInt("exam_id")); // 테이블에 exam_id 컬럼이 있다고 가정 (스키마 확인 필요)
        stat.setAttempts(rs.getInt("attempts"));
        stat.setCorrectCount(rs.getInt("correct_count"));
        stat.setCorrectRate(rs.getFloat("correct_rate"));
        stat.setQuestionType(rs.getString("question_type"));
        // stat.setSubSubject(rs.getString("sub_subject")); // 스키마에 있다면 추가
        return stat;
    }

    @Override
    public List<QuestionStat> findByExamId(int examId, Connection conn) throws DaoException {
        List<QuestionStat> stats = new ArrayList<>();
        // questionstats 테이블 스키마에 exam_id가 포함되어 있어야 함.
        // 데이터베이스 PDF에는 question_id, exam_id가 PK로 되어 있음.
        String sql = "SELECT question_id, exam_id, attempts, correct_count, correct_rate, question_type FROM questionstats WHERE exam_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, examId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                stats.add(mapRowToQuestionStat(rs));
            }
        } catch (SQLException e) {
            throw new DaoException("시험 ID별 문제 통계 조회 실패: examId=" + examId, e);
        }
        return stats;
    }

    @Override
    public List<QuestionStat> findByExamId(int examId) throws DaoException {
        try(Connection conn = DBConnection.getConnection()){
            return findByExamId(examId, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 실패 또는 시험 ID별 문제 통계 조회 실패", e);
        }
    }

    @Override
    public void saveOrUpdate(QuestionStat questionStat, Connection conn) throws DaoException {
        // questionstats 테이블의 PK는 (question_id, exam_id)
        String sql = "INSERT INTO questionstats (question_id, exam_id, attempts, correct_count, correct_rate, question_type) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "attempts = VALUES(attempts), correct_count = VALUES(correct_count), " +
                "correct_rate = VALUES(correct_rate), question_type = VALUES(question_type)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, questionStat.getQuestionId());
            pstmt.setInt(2, questionStat.getExamId());
            pstmt.setInt(3, questionStat.getAttempts());
            pstmt.setInt(4, questionStat.getCorrectCount());
            pstmt.setFloat(5, questionStat.getCorrectRate());
            pstmt.setString(6, questionStat.getQuestionType());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("문제 통계 저장/업데이트 실패", e);
        }
    }

    @Override
    public void saveOrUpdate(QuestionStat questionStat) throws DaoException {
        try(Connection conn = DBConnection.getConnection()){
            // 이 메서드가 단독 트랜잭션으로 처리되어야 한다면 여기서 conn.setAutoCommit(false), commit, rollback 처리 필요.
            saveOrUpdate(questionStat, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 실패 또는 문제 통계 저장/업데이트 실패", e);
        }
    }

    @Override
    public void recordAttempt(int questionId, int examId, String questionType, boolean isCorrect, Connection conn) throws DaoException {
        // 이 메서드는 한 학생의 단일 문제 제출 결과를 questionstats에 반영합니다.
        // 먼저 현재 값을 읽고, 값을 증가시킨 후, correct_rate를 재계산하여 업데이트합니다.
        // 동시성 문제가 발생할 수 있으므로 SELECT FOR UPDATE를 사용하거나, DB 레벨 락, 또는 프로시저 사용을 고려할 수 있습니다.
        // 간단한 구현은 SELECT 후 UPDATE 이지만, 정확한 통계를 위해서는 더 견고한 방법이 필요합니다.
        // 여기서는 간단한 UPSERT (INSERT ON DUPLICATE KEY UPDATE) 방식을 활용하여 attempts와 correct_count를 증가시키는 예시를 보입니다.
        // (주의: 이 방식은 attempts와 correct_count를 독립적으로 증가시키지만, correct_rate는 별도 계산 후 업데이트해야 합니다.)

        // MySQL의 INSERT ... ON DUPLICATE KEY UPDATE 활용
        String sql = "INSERT INTO questionstats (question_id, exam_id, attempts, correct_count, question_type, correct_rate) " +
                "VALUES (?, ?, 1, ?, ?, 0) " + // 최초 시도시 correct_rate는 임시로 0
                "ON DUPLICATE KEY UPDATE " +
                "attempts = attempts + 1, " +
                "correct_count = correct_count + ?";
        // correct_rate는 이 쿼리 후 별도로 SELECT 하여 재계산 후 UPDATE 필요

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int correctIncrement = isCorrect ? 1 : 0;
            pstmt.setInt(1, questionId);
            pstmt.setInt(2, examId);
            pstmt.setInt(3, correctIncrement); // 최초 INSERT 시 correct_count
            pstmt.setString(4, questionType);  // 최초 INSERT 시 question_type
            pstmt.setInt(5, correctIncrement); // UPDATE 시 correct_count 증가분
            pstmt.executeUpdate();

            // correct_rate 재계산 및 업데이트
            String updateRateSql = "UPDATE questionstats SET correct_rate = (correct_count / attempts) * 100 " +
                    "WHERE question_id = ? AND exam_id = ? AND attempts > 0";
            try (PreparedStatement ratePstmt = conn.prepareStatement(updateRateSql)) {
                ratePstmt.setInt(1, questionId);
                ratePstmt.setInt(2, examId);
                ratePstmt.executeUpdate();
            }

        } catch (SQLException e) {
            throw new DaoException("문제 시도 기록 및 통계 업데이트 실패 (questionId: " + questionId + ", examId: " + examId + ")", e);
        }
    }
}