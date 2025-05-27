package main.java.dao;

import main.java.model.AnswerKey;
import main.java.util.DBConnection;

import java.sql.*;

public class AnswerKeyDaoImpl implements AnswerKeyDao {
    private final Connection conn = DBConnection.getConnection();

    @Override
    public AnswerKey findByQuestionId(int questionId) throws DaoException {
        String sql = "SELECT * FROM answer_key WHERE question_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, questionId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new AnswerKey(
                        rs.getInt("question_id"),
                        rs.getString("correct_label") != null ? rs.getString("correct_label").charAt(0) : null,
                        rs.getString("correct_text")
                );
            }
            return null;
        } catch (SQLException e) {
            throw new DaoException("Error finding AnswerKey by questionId", e);
        }
    }

    @Override
    public void insert(AnswerKey key) throws DaoException {
        String sql = "INSERT INTO answer_key(question_id, correct_label, correct_text) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, key.getQuestionId());
            if (key.getCorrectLabel() != null) pstmt.setString(2, key.getCorrectLabel().toString()); else pstmt.setNull(2, Types.CHAR);
            if (key.getCorrectText() != null) pstmt.setString(3, key.getCorrectText()); else pstmt.setNull(3, Types.VARCHAR);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Error inserting AnswerKey", e);
        }
    }

    @Override
    public void update(AnswerKey key) throws DaoException {
        String sql = "UPDATE answer_key SET correct_label = ?, correct_text = ? WHERE question_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (key.getCorrectLabel() != null) pstmt.setString(1, key.getCorrectLabel().toString()); else pstmt.setNull(1, Types.CHAR);
            if (key.getCorrectText() != null) pstmt.setString(2, key.getCorrectText()); else pstmt.setNull(2, Types.VARCHAR);
            pstmt.setInt(3, key.getQuestionId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Error updating AnswerKey", e);
        }
    }

    @Override
    public void deleteByQuestionId(int questionId) throws DaoException {
        String sql = "DELETE FROM answer_key WHERE question_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, questionId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Error deleting AnswerKey by questionId", e);
        }
    }
}
