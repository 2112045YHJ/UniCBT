package main.java.dao;

import main.java.model.Exam;
import main.java.util.DBConnection; // DBConnection은 이제 직접 사용하지 않거나, conn이 null일 경우 대비용으로만 사용

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExamDaoImpl implements ExamDao {
    // 기존 private final Connection conn 멤버 변수는 제거하거나 사용하지 않도록 변경
    // 또는, conn 파라미터가 없는 기존 메서드들을 위해 남겨두고, conn 파라미터가 있는 메서드는 전달받은 conn을 사용

    @Override
    public Exam findById(int examId, Connection conn) throws DaoException {
        String sql = "SELECT exam_id, subject, created_at, start_date, end_date, duration_minutes, question_cnt FROM exams WHERE exam_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { // 전달받은 conn 사용
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Exam exam = new Exam();
                    exam.setExamId(rs.getInt("exam_id"));
                    exam.setSubject(rs.getString("subject"));
                    Timestamp createdAtTs = rs.getTimestamp("created_at");
                    if (createdAtTs != null) exam.setCreatedAt(createdAtTs.toLocalDateTime());
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
    public List<Exam> findAll(Connection conn) throws DaoException {
        String sql = "SELECT exam_id FROM exams ORDER BY start_date DESC"; // 예시 정렬 추가
        List<Exam> list = new ArrayList<>();
        try (Statement stmt = conn.createStatement(); // 전달받은 conn 사용
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                // findById 내부에서 다시 Connection을 얻지 않도록, 여기서 직접 Exam 객체를 만들거나
                // findById도 Connection을 받도록 수정된 것을 호출
                list.add(findById(rs.getInt("exam_id"), conn)); // 수정된 findById 호출
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("findAll 실패", e);
        }
    }

    @Override
    public List<Exam> findOpenExams(Connection conn) throws DaoException {
        String sql = "SELECT exam_id FROM exams WHERE start_date <= NOW() AND end_date >= NOW() ORDER BY start_date";
        List<Exam> list = new ArrayList<>();
        try (Statement stmt = conn.createStatement(); // 전달받은 conn 사용
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(findById(rs.getInt("exam_id"), conn)); // 수정된 findById 호출
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("findOpenExams 실패", e);
        }
    }


    @Override
    public void insert(Exam exam, Connection conn) throws DaoException {
        String sql = "INSERT INTO exams (subject, start_date, end_date, duration_minutes, question_cnt, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { // 전달받은 conn 사용
            ps.setString(1, exam.getSubject());
            ps.setTimestamp(2, Timestamp.valueOf(exam.getStartDate()));
            ps.setTimestamp(3, Timestamp.valueOf(exam.getEndDate()));
            ps.setInt(4, exam.getDurationMinutes());
            ps.setInt(5, exam.getQuestionCnt());
            ps.setTimestamp(6, exam.getCreatedAt() != null ? Timestamp.valueOf(exam.getCreatedAt()) : Timestamp.valueOf(java.time.LocalDateTime.now())); // 생성 시간 설정

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    exam.setExamId(keys.getInt(1)); // 생성된 ID를 exam 객체에 설정
                } else {
                    throw new DaoException("새 Exam ID 생성 실패.", null);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("insert Exam 실패", e);
        }
    }

    @Override
    public void update(Exam exam, Connection conn) throws DaoException {
        String sql = "UPDATE exams SET subject = ?, start_date = ?, end_date = ?, duration_minutes = ?, question_cnt = ? WHERE exam_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { // 전달받은 conn 사용
            ps.setString(1, exam.getSubject());
            ps.setTimestamp(2, Timestamp.valueOf(exam.getStartDate()));
            ps.setTimestamp(3, Timestamp.valueOf(exam.getEndDate()));
            ps.setInt(4, exam.getDurationMinutes());
            ps.setInt(5, exam.getQuestionCnt());
            ps.setInt(6, exam.getExamId());
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DaoException("Exam 업데이트 실패: examId=" + exam.getExamId() + "를 찾을 수 없거나 변경된 내용이 없습니다.", null);
            }
        } catch (SQLException e) {
            throw new DaoException("update Exam 실패: examId=" + exam.getExamId(), e);
        }
    }

    @Override
    public void updateQuestionCount(int examId, int questionCnt, Connection conn) throws DaoException {
        String sql = "UPDATE exams SET question_cnt = ? WHERE exam_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) { // 전달받은 conn 사용
            ps.setInt(1, questionCnt);
            ps.setInt(2, examId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("updateQuestionCount 실패: examId=" + examId, e);
        }
    }

    @Override
    public void disableExam(int examId, Connection conn) throws DaoException {
        // 마감일을 현재 시간으로 설정하여 비활성화
        String sql = "UPDATE exams SET end_date = NOW() WHERE exam_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) { // 전달받은 conn 사용
            pstmt.setInt(1, examId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("시험 비활성화 실패: examId=" + examId, e);
        }
    }

    @Override
    public List<Exam> findAllByDpmtAndGrade(int dpmtId, int grade, Connection conn) throws DaoException {
        String sql = "SELECT e.* FROM exams e JOIN examsdepartment ed ON e.exam_id = ed.exam_id WHERE ed.dpmt_id = ? AND ed.grade = ? ORDER BY e.start_date DESC";
        List<Exam> list = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, dpmtId);
            pstmt.setInt(2, grade);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapRowToExam(rs)); // 중복 로직 해소를 위해 헬퍼 메서드 사용 고려
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("Error finding all Exams by dpmtId and grade", e);
        }
    }

    @Override
    public List<Exam> findAllByUser(int userId, Connection conn) throws DaoException {
        String sql = "SELECT DISTINCT e.* FROM exams e " +
                "JOIN exam_assignments ea ON e.exam_id = ea.exam_id " + // examresults 대신 exam_assignments 로 변경 고려 (응시 예정 시험도 포함)
                "WHERE ea.user_id = ? ORDER BY e.start_date DESC";
        // 또는 JOIN examresults r ON e.exam_id = r.exam_id WHERE r.user_id = ? (기존 방식)
        List<Exam> list = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapRowToExam(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("Error finding all Exams by userId", e);
        }
    }

    @Override
    public List<int[]> getAssignedDepartmentAndGradeIds(int examId, Connection conn) throws DaoException {
        String sql = "SELECT dpmt_id, grade FROM examsdepartment WHERE exam_id = ?";
        List<int[]> result = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, examId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new int[]{rs.getInt("dpmt_id"), rs.getInt("grade")});
                }
            }
        } catch (SQLException e) {
            throw new DaoException("examsdepartment 조회 실패 (getAssignedDepartmentAndGradeIds)", e);
        }
        return result;
    }

    @Override
    public List<String> findAssignedDepartmentsAndGrades(int examId, Connection conn) throws DaoException {
        String sql = "SELECT d.dpmt_name, ed.grade FROM examsdepartment ed JOIN department d ON ed.dpmt_id = d.dpmt_id WHERE ed.exam_id = ? ORDER BY d.dpmt_name ASC, ed.grade ASC";
        List<String> list = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, examId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("dpmt_name") + " / " + rs.getInt("grade") + "학년");
                }
            }
        } catch (SQLException e) {
            throw new DaoException("응시 대상 조회 실패 (findAssignedDepartmentsAndGrades)", e);
        }
        return list;
    }

    // ResultSet에서 Exam 객체로 매핑하는 헬퍼 메서드 (중복 감소)
    private Exam mapRowToExam(ResultSet rs) throws SQLException {
        Exam exam = new Exam();
        exam.setExamId(rs.getInt("exam_id"));
        exam.setSubject(rs.getString("subject"));
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        if (createdAtTs != null) exam.setCreatedAt(createdAtTs.toLocalDateTime());
        exam.setStartDate(rs.getTimestamp("start_date").toLocalDateTime());
        exam.setEndDate(rs.getTimestamp("end_date").toLocalDateTime());
        exam.setDurationMinutes(rs.getInt("duration_minutes"));
        exam.setQuestionCnt(rs.getInt("question_cnt"));
        return exam;
    }
}