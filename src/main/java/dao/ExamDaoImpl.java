package main.java.dao;

import main.java.model.Exam;
import main.java.dao.DaoException;
import main.java.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ExamDao 구현체 (JDBC)
 */
public class ExamDaoImpl implements ExamDao {
    private final Connection conn = DBConnection.getConnection();

    @Override
    public Exam findById(int examId) throws DaoException {
        String sql = """
            SELECT exam_id, subject, start_date, end_date, duration_minutes, question_cnt
              FROM exams
             WHERE exam_id = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Exam exam = new Exam();
                    exam.setExamId(rs.getInt("exam_id"));
                    exam.setSubject(rs.getString("subject"));
                    exam.setStartDate(rs.getTimestamp("start_date").toLocalDateTime());
                    exam.setEndDate(rs.getTimestamp("end_date").toLocalDateTime());
                    exam.setDurationMinutes(rs.getInt("duration_minutes"));
                    exam.setQuestionCnt(rs.getInt("question_cnt"));
                    return exam;
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DaoException("findById 실패: examId=" + examId, e);
        }
    }

    @Override
    public List<Exam> findAll() throws DaoException {
        String sql = "SELECT exam_id FROM exams";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<Exam> list = new ArrayList<>();
            while (rs.next()) {
                list.add(findById(rs.getInt("exam_id")));
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("findAll 실패", e);
        }
    }

    @Override
    public List<Exam> findOpenExams() throws DaoException {
        String sql = """
            SELECT exam_id
              FROM exams
             WHERE start_date <= NOW()
               AND end_date   >= NOW()
             ORDER BY start_date
        """;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<Exam> list = new ArrayList<>();
            while (rs.next()) {
                list.add(findById(rs.getInt("exam_id")));
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("findOpenExams 실패", e);
        }
    }

    @Override
    public void insert(Exam exam) throws DaoException {
        String sql = """
            INSERT INTO exams (subject, start_date, end_date, duration_minutes, question_cnt)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, exam.getSubject());
            ps.setTimestamp(2, Timestamp.valueOf(exam.getStartDate()));
            ps.setTimestamp(3, Timestamp.valueOf(exam.getEndDate()));
            ps.setInt(4, exam.getDurationMinutes());
            ps.setInt(5, exam.getQuestionCnt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    exam.setExamId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("insert Exam 실패", e);
        }
    }

    @Override
    public void updateQuestionCount(int examId, int questionCnt) throws DaoException {
        String sql = "UPDATE exams SET question_cnt = ? WHERE exam_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, questionCnt);
            ps.setInt(2, examId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("updateQuestionCount 실패", e);
        }
    }
    public List<Exam> findAllByDpmtAndGrade(int dpmtId, int grade) throws DaoException {
        String sql = "SELECT e.* " +
                "FROM exams e " +
                "JOIN examsdepartment ed ON e.exam_id = ed.exam_id " +
                "WHERE ed.dpmt_id = ? AND ed.grade = ? " +
                "ORDER BY e.start_date DESC";
        List<Exam> list = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, dpmtId);
            pstmt.setInt(2, grade);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Exam exam = new Exam();
                exam.setExamId(rs.getInt("exam_id"));
                exam.setSubject(rs.getString("subject"));
                exam.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                exam.setStartDate(rs.getTimestamp("start_date").toLocalDateTime());
                exam.setEndDate(rs.getTimestamp("end_date").toLocalDateTime());
                exam.setDurationMinutes(rs.getInt("duration_minutes"));
                exam.setQuestionCnt(rs.getInt("question_cnt"));
                list.add(exam);
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("Error finding all Exams by dpmtId and grade", e);
        }
    }

    @Override
    public List<Exam> findAllByUser(int userId) throws DaoException {
        String sql = "SELECT DISTINCT e.* FROM exams e " +
                "JOIN examresults r ON e.exam_id = r.exam_id " +
                "WHERE r.user_id = ? ORDER BY e.start_date DESC";
        List<Exam> list = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId); // ★★★ 반드시 바인딩!
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Exam exam = new Exam();
                exam.setExamId(rs.getInt("exam_id"));
                exam.setSubject(rs.getString("subject"));
                Timestamp createdTs = rs.getTimestamp("created_at");
                exam.setCreatedAt(createdTs != null ? createdTs.toLocalDateTime() : null);
                Timestamp startTs = rs.getTimestamp("start_date");
                exam.setStartDate(startTs != null ? startTs.toLocalDateTime() : null);
                Timestamp endTs = rs.getTimestamp("end_date");
                exam.setEndDate(endTs != null ? endTs.toLocalDateTime() : null);
                exam.setDurationMinutes(rs.getInt("duration_minutes"));
                exam.setQuestionCnt(rs.getInt("question_cnt"));
                list.add(exam);
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("Error finding all Exams by userId", e);
        }
    }
}