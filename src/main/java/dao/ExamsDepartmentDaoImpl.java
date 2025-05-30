package main.java.dao;

import main.java.model.ExamsDepartment;
// import main.java.util.DBConnection; // 직접 사용하지 않도록 변경

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExamsDepartmentDaoImpl implements ExamsDepartmentDao {

    @Override
    public List<ExamsDepartment> findByExamId(int examId, Connection conn) throws DaoException {
        String sql = "SELECT exam_id, dpmt_id, grade FROM examsdepartment WHERE exam_id = ?";
        List<ExamsDepartment> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) { // 전달받은 conn 사용
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ExamsDepartment(
                            rs.getInt("exam_id"),
                            rs.getInt("dpmt_id"),
                            rs.getInt("grade")
                    ));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("findByExamId 실패: examId=" + examId, e);
        }
    }

    @Override
    public void save(ExamsDepartment ed, Connection conn) throws DaoException {
        String sql = "INSERT INTO examsdepartment (exam_id, dpmt_id, grade) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { // 전달받은 conn 사용
            ps.setInt(1, ed.getExamId());
            ps.setInt(2, ed.getDpmtId());
            ps.setInt(3, ed.getGrade());
            ps.executeUpdate();
        } catch (SQLException e) {
            // 기본 키 중복 예외(Duplicate entry) 등 특정 SQL 예외 처리 고려 가능
            throw new DaoException("ExamsDepartment save 실패: " + ed.toString(), e);
        }
    }

    @Override
    public void delete(int examId, int dpmtId, int grade, Connection conn) throws DaoException {
        String sql = "DELETE FROM examsdepartment WHERE exam_id = ? AND dpmt_id = ? AND grade = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { // 전달받은 conn 사용
            ps.setInt(1, examId);
            ps.setInt(2, dpmtId);
            ps.setInt(3, grade);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("ExamsDepartment delete 실패: examId=" + examId + ", dpmtId=" + dpmtId + ", grade=" + grade, e);
        }
    }

    @Override
    public void deleteByExamId(int examId, Connection conn) throws DaoException {
        String sql = "DELETE FROM examsdepartment WHERE exam_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { // 전달받은 conn 사용
            ps.setInt(1, examId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("deleteByExamId 실패: examId=" + examId, e);
        }
    }

    @Override
    public Map<Integer, List<Integer>> findDepartmentAndGradesGrouped(int examId, Connection conn) throws DaoException {
        String sql = "SELECT dpmt_id, grade FROM examsdepartment WHERE exam_id = ?";
        Map<Integer, List<Integer>> map = new HashMap<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) { // 전달받은 conn 사용
            pstmt.setInt(1, examId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int dpmtId = rs.getInt("dpmt_id");
                    int grade = rs.getInt("grade");
                    map.computeIfAbsent(dpmtId, k -> new ArrayList<>()).add(grade);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("findDepartmentAndGradesGrouped 실패: examId=" + examId, e);
        }
        return map;
    }
}