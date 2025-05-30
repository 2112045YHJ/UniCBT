package main.java.dao;

import main.java.model.QuestionBank;
// import main.java.util.DBConnection; // 직접 사용하지 않도록 변경

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionBankDaoImpl implements QuestionBankDao {

    @Override
    public QuestionBank findById(int questionId, Connection conn) throws DaoException {
        String sql = "SELECT * FROM question_bank WHERE question_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) { // 전달받은 conn 사용
            pstmt.setInt(1, questionId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new QuestionBank(
                        rs.getInt("question_id"),
                        rs.getInt("exam_id"),
                        rs.getString("type"),
                        rs.getString("question_text")
                );
            }
            return null;
        } catch (SQLException e) {
            throw new DaoException("Error finding QuestionBank by id: " + questionId, e);
        }
    }

    @Override
    public List<QuestionBank> findByExamId(int examId, Connection conn) throws DaoException {
        String sql = "SELECT * FROM question_bank WHERE exam_id = ? ORDER BY question_id";
        List<QuestionBank> list = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) { // 전달받은 conn 사용
            pstmt.setInt(1, examId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new QuestionBank(
                        rs.getInt("question_id"),
                        rs.getInt("exam_id"),
                        rs.getString("type"),
                        rs.getString("question_text")
                ));
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("Error finding QuestionBanks by examId: " + examId, e);
        }
    }

    @Override
    public void insert(QuestionBank qb, Connection conn) throws DaoException {
        String sql = "INSERT INTO question_bank(exam_id, type, question_text) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { // 전달받은 conn 사용
            pstmt.setInt(1, qb.getExamId());
            pstmt.setString(2, qb.getType());
            pstmt.setString(3, qb.getQuestionText());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                qb.setQuestionId(rs.getInt(1)); // 생성된 ID를 qb 객체에 설정
            } else {
                throw new DaoException("새 QuestionBank ID 생성 실패.", null);
            }
        } catch (SQLException e) {
            throw new DaoException("Error inserting QuestionBank", e);
        }
    }

    @Override
    public void deleteByExamId(int examId, Connection conn) throws DaoException {
        // CASCADE DELETE가 question_option, answer_key에 설정되어 있지 않다면,
        // 이들을 먼저 삭제하는 로직이 ExamServiceImpl에 이미 포함되어 있어야 함.
        // 여기서는 question_bank 테이블의 레코드만 삭제.
        String sql = "DELETE FROM question_bank WHERE exam_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) { // 전달받은 conn 사용
            pstmt.setInt(1, examId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Error deleting QuestionBanks by examId: " + examId, e);
        }
    }

    @Override
    public int countByExamId(int examId, Connection conn) throws DaoException {
        String sql = "SELECT COUNT(*) FROM question_bank WHERE exam_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { // 전달받은 conn 사용
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DaoException("countByExamId 실패: examId=" + examId, e);
        }
    }
}