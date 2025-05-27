package main.java.dao;

import main.java.model.AnswerSheet;
import main.java.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnswerSheetDaoImpl implements AnswerSheetDao {
    private final Connection conn = DBConnection.getConnection();
    @Override
    public void insert(AnswerSheet sheet) throws DaoException {
        String sql = "INSERT INTO answersheets(user_id, exam_id, question_id, selected_answer) VALUES(?,?,?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, sheet.getUserId()); pstmt.setInt(2, sheet.getExamId()); pstmt.setInt(3, sheet.getQuestionId()); pstmt.setString(4, sheet.getSelectedAnswer());
            pstmt.executeUpdate(); ResultSet rs = pstmt.getGeneratedKeys(); if (rs.next()) sheet.setAnswerId(rs.getInt(1));
        } catch (SQLException e) { throw new DaoException("Error inserting AnswerSheet", e); }
    }
    @Override
    public List<AnswerSheet> findByUserAndExam(int userId, int examId) throws DaoException {
        String sql = "SELECT * FROM answersheets WHERE user_id=? AND exam_id=? ORDER BY question_id";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId); pstmt.setInt(2, examId);
            ResultSet rs = pstmt.executeQuery(); List<AnswerSheet> list = new ArrayList<>();
            while (rs.next()) { AnswerSheet sheet = new AnswerSheet(); sheet.setAnswerId(rs.getInt("answer_id")); sheet.setUserId(userId); sheet.setExamId(examId); sheet.setQuestionId(rs.getInt("question_id")); sheet.setSelectedAnswer(rs.getString("selected_answer")); list.add(sheet);} return list;
        } catch (SQLException e) { throw new DaoException("Error finding AnswerSheets by user and exam", e); }
    }
    @Override
    public void deleteByUserAndExam(int userId, int examId) throws DaoException {
        try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM answersheets WHERE user_id=? AND exam_id=?")) {
            pstmt.setInt(1, userId); pstmt.setInt(2, examId); pstmt.executeUpdate();
        } catch (SQLException e) { throw new DaoException("Error deleting AnswerSheets by user and exam", e); }
    }
}