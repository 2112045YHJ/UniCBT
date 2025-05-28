package main.java.dao;

import main.java.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExamAssignmentDaoImpl implements ExamAssignmentDao {
    private final Connection conn = DBConnection.getConnection();

    @Override
    public void assignStudents(int examId, List<Integer> userIds) throws DaoException {
        // 1) 기존 배정 삭제
        String deleteSql = "DELETE FROM exam_assignments WHERE exam_id = ?";
        try (PreparedStatement del = conn.prepareStatement(deleteSql)) {
            del.setInt(1, examId);
            del.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("assignStudents: 기존 배정 삭제 실패", e);
        }

        // 2) 새로운 배정 일괄 삽입
        String insertSql = "INSERT INTO exam_assignments (user_id, exam_id) VALUES (?, ?)";
        try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
            for (Integer userId : userIds) {
                ins.setInt(1, userId);
                ins.setInt(2, examId);
                ins.addBatch();
            }
            ins.executeBatch();
        } catch (SQLException e) {
            throw new DaoException("assignStudents: 배정 삽입 실패", e);
        }
    }

    @Override
    public void removeAssignments(int examId) throws DaoException {
        String sql = "DELETE FROM exam_assignments WHERE exam_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, examId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("removeAssignments 실패", e);
        }
    }

    @Override
    public List<Integer> findExamIdsByUser(int userId) throws DaoException {
        String sql = "SELECT exam_id FROM exam_assignments WHERE user_id = ?";
        List<Integer> exams = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    exams.add(rs.getInt("exam_id"));
                }
            }
            return exams;
        } catch (SQLException e) {
            throw new DaoException("findExamIdsByUser 실패", e);
        }
    }

    @Override
    public List<Integer> findUserIdsByExam(int examId) throws DaoException {
        String sql = "SELECT user_id FROM exam_assignments WHERE exam_id = ?";
        List<Integer> users = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(rs.getInt("user_id"));
                }
            }
            return users;
        } catch (SQLException e) {
            throw new DaoException("findUserIdsByExam 실패", e);
        }
    }


}
