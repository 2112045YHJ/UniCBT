package main.java.dao;

import main.java.model.QuestionOption;
import main.java.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionOptionDaoImpl implements QuestionOptionDao {
    private final Connection conn = DBConnection.getConnection();

    @Override
    public List<QuestionOption> findByQuestionId(int questionId) throws DaoException {
        String sql = "SELECT * FROM question_option WHERE question_id = ? ORDER BY option_label";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, questionId);
            ResultSet rs = pstmt.executeQuery();
            List<QuestionOption> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new QuestionOption(
                        rs.getInt("option_id"),
                        rs.getInt("question_id"),
                        rs.getString("option_label").charAt(0),
                        rs.getString("content")
                ));
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("Error finding QuestionOptions by questionId", e);
        }
    }

    @Override
    public void insert(QuestionOption option) throws DaoException {
        String sql = "INSERT INTO question_option(question_id, option_label, content) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, option.getQuestionId());
            pstmt.setString(2, String.valueOf(option.getOptionLabel()));
            pstmt.setString(3, option.getContent());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) option.setOptionId(rs.getInt(1));
        } catch (SQLException e) {
            throw new DaoException("Error inserting QuestionOption", e);
        }
    }

    @Override
    public void deleteByQuestionId(int questionId) throws DaoException {
        String sql = "DELETE FROM question_option WHERE question_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, questionId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Error deleting QuestionOptions by questionId", e);
        }
    }
}
