package main.java.dao;

import main.java.model.QuestionBank;
import main.java.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionBankDaoImpl implements QuestionBankDao {
    private final Connection conn = DBConnection.getConnection();

    @Override
    public QuestionBank findById(int questionId) throws DaoException {
        String sql = "SELECT * FROM question_bank WHERE question_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, questionId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return new QuestionBank(
                    rs.getInt("question_id"), rs.getInt("exam_id"), rs.getString("type"), rs.getString("question_text")
            );
            return null;
        } catch (SQLException e) {
            throw new DaoException("Error finding QuestionBank by id", e);
        }

    }

    @Override
    public List<QuestionBank> findByExamId(int examId) throws DaoException {
        String sql = "SELECT * FROM question_bank WHERE exam_id = ? ORDER BY question_id";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, examId);
            ResultSet rs = pstmt.executeQuery();
            List<QuestionBank> list = new ArrayList<>();
            while (rs.next()) list.add(new QuestionBank(
                    rs.getInt("question_id"), rs.getInt("exam_id"), rs.getString("type"), rs.getString("question_text")
            ));
            return list;
        } catch (SQLException e) {
            throw new DaoException("Error finding QuestionBanks by examId", e);
        }
    }

    @Override
    public void insert(QuestionBank qb) throws DaoException {
        String sql = "INSERT INTO question_bank(exam_id, type, question_text) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, qb.getExamId());
            pstmt.setString(2, qb.getType());
            pstmt.setString(3, qb.getQuestionText());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) qb.setQuestionId(rs.getInt(1));
        } catch (SQLException e) {
            throw new DaoException("Error inserting QuestionBank", e);
        }
    }

    @Override
    public void deleteByExamId(int examId) throws DaoException {
        try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM question_bank WHERE exam_id = ?")) {
            pstmt.setInt(1, examId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Error deleting QuestionBanks by examId", e);
        }
    }

    @Override
    public int countByExamId(int examId) throws DaoException {
        String sql = "SELECT COUNT(*) FROM question_bank WHERE exam_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DaoException("countByExamId 실패", e);
        }
    }
}
