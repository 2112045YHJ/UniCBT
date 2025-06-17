package main.java.dao;

import main.java.model.AnswerSheet;
import main.java.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnswerSheetDaoImpl implements AnswerSheetDao {

    @Override
    public void insert(AnswerSheet sheet, Connection conn) throws DaoException {
        String sql = "INSERT INTO answersheets(user_id, exam_id, question_id, selected_answer) VALUES(?,?,?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, sheet.getUserId());
            pstmt.setInt(2, sheet.getExamId());
            pstmt.setInt(3, sheet.getQuestionId());
            pstmt.setString(4, sheet.getSelectedAnswer());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                sheet.setAnswerId(rs.getInt(1));
            } else {
                throw new DaoException("답안 저장 후 ID 가져오기 실패.", null);
            }
        } catch (SQLException e) {
            throw new DaoException("AnswerSheet 삽입 오류", e);
        }
    }

    @Override
    public List<AnswerSheet> findByUserAndExam(int userId, int examId, Connection conn) throws DaoException {
        String sql = "SELECT answer_id, user_id, exam_id, question_id, selected_answer FROM answersheets WHERE user_id=? AND exam_id=? ORDER BY question_id";
        List<AnswerSheet> list = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, examId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                AnswerSheet sheet = new AnswerSheet();
                sheet.setAnswerId(rs.getInt("answer_id"));
                sheet.setUserId(rs.getInt("user_id"));
                sheet.setExamId(rs.getInt("exam_id"));
                sheet.setQuestionId(rs.getInt("question_id"));
                sheet.setSelectedAnswer(rs.getString("selected_answer"));
                list.add(sheet);
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("사용자 및 시험별 AnswerSheets 조회 오류", e);
        }
    }

    @Override
    public void deleteByUserAndExam(int userId, int examId, Connection conn) throws DaoException {
        String sql = "DELETE FROM answersheets WHERE user_id=? AND exam_id=?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, examId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("사용자 및 시험별 AnswerSheets 삭제 오류", e);
        }
    }

    @Override
    public void insert(int userId, int examId, int questionId, String answer, Connection conn) throws DaoException {
        AnswerSheet sheet = new AnswerSheet();
        sheet.setUserId(userId);
        sheet.setExamId(examId);
        sheet.setQuestionId(questionId);
        sheet.setSelectedAnswer(answer);
        insert(sheet, conn); // Connection 받는 insert(AnswerSheet, Connection) 호출
    }

    // --- 기존 시그니처 메서드 구현 ---

    @Override
    public void insert(AnswerSheet sheet) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            insert(sheet, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 실패 또는 AnswerSheet 삽입 중 오류", e);
        }
    }

    @Override
    public List<AnswerSheet> findByUserAndExam(int userId, int examId) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return findByUserAndExam(userId, examId, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 실패 또는 사용자 및 시험별 AnswerSheets 조회 중 오류", e);
        }
    }

    @Override
    public void deleteByUserAndExam(int userId, int examId) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            deleteByUserAndExam(userId, examId, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 실패 또는 사용자 및 시험별 AnswerSheets 삭제 중 오류", e);
        }
    }

    @Override
    public void insert(int userId, int examId, int questionId, String answer) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            insert(userId, examId, questionId, answer, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 실패 또는 개별 파라미터 답안 삽입 중 오류", e);
        }
    }
}