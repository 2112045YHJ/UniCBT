package main.java.dao;

// import main.java.util.DBConnection; // 직접 사용하지 않도록 변경

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExamAssignmentDaoImpl implements ExamAssignmentDao {

    @Override
    public void assignStudents(int examId, List<Integer> userIds, Connection conn) throws DaoException {
        // 이 메서드는 이미 ExamServiceImpl에서 기존 배정 삭제 후 새로 삽입하는 로직의 일부로 사용될 수 있음.
        // 또는, 이 메서드 자체가 기존 배정 삭제 + 신규 삽입을 모두 처리하도록 할 수 있음.
        // 현재 ExamServiceImpl.saveExamWithDetails의 업데이트 로직은 removeAssignments 후 assignStudents를 호출하므로,
        // 여기서는 삽입만 담당하거나, 호출 전에 removeAssignments가 되었음을 가정함.
        // 만약 이 메서드가 기존 레코드 삭제도 담당한다면 ExamServiceImpl의 중복 호출을 피해야 함.
        // 여기서는 ExamServiceImpl의 초안대로 이 메서드가 삽입만 담당한다고 가정하고,
        // ExamAssignmentDaoImpl의 기존 로직(기존 배정 삭제 후 일괄 삽입)을 유지하면서 Connection을 사용하도록 수정.

        // 1) 기존 배정 삭제 (이 메서드 내에서 트랜잭션의 일부로 수행)
        String deleteSql = "DELETE FROM exam_assignments WHERE exam_id = ?";
        try (PreparedStatement delStmt = conn.prepareStatement(deleteSql)) { // 전달받은 conn 사용
            delStmt.setInt(1, examId);
            delStmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("assignStudents 중 기존 배정 삭제 실패: examId=" + examId, e);
        }

        // 2) 새로운 배정 일괄 삽입
        if (userIds == null || userIds.isEmpty()) {
            return; // 배정할 학생이 없으면 종료
        }
        String insertSql = "INSERT INTO exam_assignments (user_id, exam_id, assigned_at) VALUES (?, ?, NOW())";
        try (PreparedStatement insStmt = conn.prepareStatement(insertSql)) { // 전달받은 conn 사용
            for (Integer userId : userIds) {
                insStmt.setInt(1, userId);
                insStmt.setInt(2, examId);
                insStmt.addBatch();
            }
            insStmt.executeBatch();
        } catch (SQLException e) {
            throw new DaoException("assignStudents 중 배정 삽입 실패: examId=" + examId, e);
        }
    }

    @Override
    public void removeAssignments(int examId, Connection conn) throws DaoException {
        String sql = "DELETE FROM exam_assignments WHERE exam_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { // 전달받은 conn 사용
            ps.setInt(1, examId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("removeAssignments 실패: examId=" + examId, e);
        }
    }

    @Override
    public List<Integer> findExamIdsByUser(int userId, Connection conn) throws DaoException {
        String sql = "SELECT exam_id FROM exam_assignments WHERE user_id = ?";
        List<Integer> examIds = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) { // 전달받은 conn 사용
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    examIds.add(rs.getInt("exam_id"));
                }
            }
            return examIds;
        } catch (SQLException e) {
            throw new DaoException("findExamIdsByUser 실패: userId=" + userId, e);
        }
    }

    @Override
    public List<Integer> findUserIdsByExam(int examId, Connection conn) throws DaoException {
        String sql = "SELECT user_id FROM exam_assignments WHERE exam_id = ?";
        List<Integer> userIds = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) { // 전달받은 conn 사용
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    userIds.add(rs.getInt("user_id"));
                }
            }
            return userIds;
        } catch (SQLException e) {
            throw new DaoException("findUserIdsByExam 실패: examId=" + examId, e);
        }
    }
}