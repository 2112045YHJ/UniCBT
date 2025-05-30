package main.java.dao;

import main.java.model.ExamResult;
import main.java.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExamResultDaoImpl implements ExamResultDao {

    // --- Connection을 파라미터로 받는 메서드 구현 ---

    @Override
    public void insert(ExamResult result, Connection conn) throws DaoException {
        String sql = "INSERT INTO examresults(exam_id, user_id, score, completed_at) VALUES(?,?,?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, result.getExamId());
            pstmt.setInt(2, result.getUserId());
            pstmt.setInt(3, result.getScore());
            pstmt.setTimestamp(4, result.getCompletedAt() != null ? Timestamp.valueOf(result.getCompletedAt()) : Timestamp.valueOf(java.time.LocalDateTime.now()));
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                result.setResultId(rs.getInt(1));
            } else {
                throw new DaoException("시험 결과 저장 후 ID 가져오기 실패.", null);
            }
        } catch (SQLException e) {
            throw new DaoException("ExamResult 삽입 오류", e);
        }
    }

    @Override
    public ExamResult findByUserAndExam(int userId, int examId, Connection conn) throws DaoException {
        String sql = "SELECT result_id, exam_id, user_id, score, completed_at FROM examresults WHERE user_id=? AND exam_id=?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, examId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapRowToExamResult(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new DaoException("사용자 및 시험별 ExamResult 조회 오류", e);
        }
    }

    @Override
    public Map<Integer, ExamResult> findAllByUser(int userId, Connection conn) throws DaoException {
        String sql = "SELECT result_id, exam_id, user_id, score, completed_at FROM examresults WHERE user_id = ?";
        Map<Integer, ExamResult> map = new HashMap<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ExamResult er = mapRowToExamResult(rs);
                map.put(er.getExamId(), er);
            }
            return map;
        } catch (SQLException e) {
            throw new DaoException("사용자별 모든 ExamResults 조회 오류", e);
        }
    }

    @Override
    public boolean existsByUserAndExam(int userId, int examId, Connection conn) throws DaoException {
        String sql = "SELECT COUNT(*) FROM examresults WHERE user_id = ? AND exam_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, examId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DaoException("사용자 시험 응시 이력 확인 오류", e);
        }
    }

    // --- 기존 시그니처 메서드 구현 ---

    @Override
    public void insert(ExamResult result) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            // completed_at이 null이면 현재 시간으로 설정
            if (result.getCompletedAt() == null) {
                result.setCompletedAt(java.time.LocalDateTime.now());
            }
            insert(result, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 실패 또는 ExamResult 삽입 중 오류", e);
        }
    }

    @Override
    public ExamResult findByUserAndExam(int userId, int examId) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return findByUserAndExam(userId, examId, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 실패 또는 사용자 및 시험별 ExamResult 조회 중 오류", e);
        }
    }

    @Override
    public Map<Integer, ExamResult> findAllByUser(int userId) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return findAllByUser(userId, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 실패 또는 사용자별 모든 ExamResults 조회 중 오류", e);
        }
    }

    @Override
    public boolean existsByUserAndExam(int userId, int examId) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return existsByUserAndExam(userId, examId, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 실패 또는 사용자 시험 응시 이력 확인 중 오류", e);
        }
    }

    // ResultSet 행을 ExamResult 객체로 매핑하는 헬퍼 메서드
    private ExamResult mapRowToExamResult(ResultSet rs) throws SQLException {
        ExamResult er = new ExamResult();
        er.setResultId(rs.getInt("result_id"));
        er.setExamId(rs.getInt("exam_id"));
        er.setUserId(rs.getInt("user_id"));
        er.setScore(rs.getInt("score"));
        Timestamp completedAtTs = rs.getTimestamp("completed_at");
        if (completedAtTs != null) {
            er.setCompletedAt(completedAtTs.toLocalDateTime());
        }
        return er;
    }

    @Override
    public int countResultsByExam(int examId, Connection conn) throws DaoException {
        String sql = "SELECT COUNT(*) FROM examresults WHERE exam_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, examId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new DaoException("시험 ID별 결과 수 집계 오류: examId=" + examId, e);
        }
    }

    @Override
    public int deleteByUserAndExam(int userId, int examId, Connection conn) throws DaoException {
        String sql = "DELETE FROM examresults WHERE user_id = ? AND exam_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, examId);
            return pstmt.executeUpdate(); // 삭제된 행의 수를 반환
        } catch (SQLException e) {
            throw new DaoException("사용자 및 시험별 ExamResult 삭제 오류: userId=" + userId + ", examId=" + examId, e);
        }
    }

    @Override
    public int deleteByUserAndExam(int userId, int examId) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            // 이 작업은 트랜잭션의 일부가 될 가능성이 높으므로,
            // 서비스 계층에서 Connection을 받아 처리하는 것이 더 일반적입니다.
            // 여기서는 단독 실행될 경우를 대비해 자체 Connection을 사용합니다.
            return deleteByUserAndExam(userId, examId, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 실패 또는 사용자 및 시험별 ExamResult 삭제 중 오류", e);
        }
    }

    @Override
    public List<ExamResult> findAllByExam(int examId, Connection conn) throws DaoException {
        List<ExamResult> results = new ArrayList<>();
        String sql = "SELECT result_id, exam_id, user_id, score, completed_at FROM examresults WHERE exam_id = ? ORDER BY score DESC, user_id ASC"; // 점수 내림차순, 동점 시 사용자 ID 오름차순 (석차 계산 용이)
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, examId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                results.add(mapRowToExamResult(rs));
            }
        } catch (SQLException e) {
            throw new DaoException("시험 ID별 모든 ExamResult 조회 실패: examId=" + examId, e);
        }
        return results;
    }

    @Override
    public List<ExamResult> findAllByExam(int examId) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return findAllByExam(examId, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 오류 또는 시험 ID별 모든 ExamResult 조회 실패", e);
        }
    }
}