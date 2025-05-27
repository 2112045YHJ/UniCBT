package main.java.dao;

import main.java.model.ExamResult;
import main.java.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExamResultDaoImpl implements ExamResultDao {
    private final Connection conn = DBConnection.getConnection();

    @Override
    public void insert(ExamResult result) throws DaoException {
        String sql = "INSERT INTO examresults(exam_id, user_id, score, completed_at) VALUES(?,?,?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, result.getExamId());
            pstmt.setInt(2, result.getUserId());
            pstmt.setInt(3, result.getScore());
            pstmt.setTimestamp(4, Timestamp.valueOf(result.getCompletedAt()));
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) result.setResultId(rs.getInt(1));
        } catch (SQLException e) {
            throw new DaoException("Error inserting ExamResult", e);
        }
    }

    @Override
    public ExamResult findByUserAndExam(int userId, int examId) throws DaoException {
        String sql = "SELECT * FROM examresults WHERE user_id=? AND exam_id=?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, examId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                ExamResult er = new ExamResult();
                er.setResultId(rs.getInt("result_id"));
                er.setExamId(examId);
                er.setUserId(userId);
                er.setScore(rs.getInt("score"));
                er.setCompletedAt(rs.getTimestamp("completed_at").toLocalDateTime());
                return er;
            }
            return null;
        } catch (SQLException e) {
            throw new DaoException("Error finding ExamResult by user and exam", e);
        }
    }

    @Override
    public boolean existsByUserAndExam(int userId, int examId) throws DaoException {
        String sql = "SELECT COUNT(*) FROM examresults WHERE user_id = ? AND exam_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, examId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DaoException("시험 응시 이력 조회 실패", e);
        }
    }

    @Override
    public Map<Integer, ExamResult> findAllByUser(int userId) throws DaoException {
        String sql = "SELECT * FROM examresults WHERE user_id = ?";
        Map<Integer, ExamResult> map = new HashMap<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ExamResult er = new ExamResult();
                er.setResultId(rs.getInt("result_id"));
                er.setExamId(rs.getInt("exam_id"));
                er.setUserId(userId);
                er.setScore(rs.getInt("score"));
                er.setCompletedAt(rs.getTimestamp("completed_at").toLocalDateTime());
                map.put(er.getExamId(), er);
            }
            return map;
        } catch (SQLException e) {
            throw new DaoException("Error finding all ExamResults by user", e);
        }
    }
}