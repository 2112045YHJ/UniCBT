package main.java.dao;

import main.java.model.QuestionOption;
// import main.java.util.DBConnection; // 직접 사용하지 않도록 변경

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionOptionDaoImpl implements QuestionOptionDao {

    @Override
    public List<QuestionOption> findByQuestionId(int questionId, Connection conn) throws DaoException {
        String sql = "SELECT * FROM question_option WHERE question_id = ? ORDER BY option_label";
        List<QuestionOption> list = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) { // 전달받은 conn 사용
            pstmt.setInt(1, questionId);
            ResultSet rs = pstmt.executeQuery();
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
            throw new DaoException("Error finding QuestionOptions by questionId: " + questionId, e);
        }
    }

    @Override
    public void insert(QuestionOption option, Connection conn) throws DaoException {
        String sql = "INSERT INTO question_option(question_id, option_label, content) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { // 전달받은 conn 사용
            pstmt.setInt(1, option.getQuestionId());
            pstmt.setString(2, String.valueOf(option.getOptionLabel()));
            pstmt.setString(3, option.getContent());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                option.setOptionId(rs.getInt(1)); // 생성된 ID를 option 객체에 설정
            } else {
                throw new DaoException("새 QuestionOption ID 생성 실패.", null);
            }
        } catch (SQLException e) {
            throw new DaoException("Error inserting QuestionOption", e);
        }
    }

    @Override
    public void deleteByQuestionId(int questionId, Connection conn) throws DaoException {
        String sql = "DELETE FROM question_option WHERE question_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) { // 전달받은 conn 사용
            pstmt.setInt(1, questionId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Error deleting QuestionOptions by questionId: " + questionId, e);
        }
    }
}