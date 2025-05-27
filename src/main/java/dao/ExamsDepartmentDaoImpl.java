package main.java.dao;

import main.java.model.ExamsDepartment;
import main.java.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExamsDepartmentDaoImpl implements ExamsDepartmentDao {
    private final Connection conn = DBConnection.getConnection();

    @Override
    public List<ExamsDepartment> findByExamId(int examId) throws DaoException {
        String sql = """
            SELECT exam_id, dpmt_id, grade
              FROM examsdepartment
             WHERE exam_id = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ExamsDepartment> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new ExamsDepartment(
                            rs.getInt("exam_id"),
                            rs.getInt("dpmt_id"),
                            rs.getInt("grade")
                    ));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DaoException("findByExamId 실패: examId=" + examId, e);
        }
    }

    @Override
    public void save(ExamsDepartment ed) throws DaoException {
        String sql = """
            INSERT INTO examsdepartment (exam_id, dpmt_id, grade)
            VALUES (?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ed.getExamId());
            ps.setInt(2, ed.getDpmtId());
            ps.setInt(3, ed.getGrade());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("save 실패: " + ed, e);
        }
    }

    @Override
    public void delete(int examId, int dpmtId, int grade) throws DaoException {
        String sql = """
            DELETE FROM examsdepartment
             WHERE exam_id = ? AND dpmt_id = ? AND grade = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, examId);
            ps.setInt(2, dpmtId);
            ps.setInt(3, grade);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("delete 실패: examId=" + examId
                    + ", dpmtId=" + dpmtId + ", grade=" + grade, e);
        }
    }

    @Override
    public void deleteByExamId(int examId) throws DaoException {
        String sql = """
            DELETE FROM examsdepartment
             WHERE exam_id = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, examId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("deleteByExamId 실패: examId=" + examId, e);
        }
    }
}
