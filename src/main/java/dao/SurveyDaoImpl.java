package main.java.dao;

import main.java.model.Survey;
import main.java.util.DBConnection;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SurveyDaoImpl implements SurveyDao {

    private Survey mapRowToSurvey(ResultSet rs) throws SQLException {
        Survey survey = new Survey();
        survey.setSurveyId(rs.getInt("survey_id"));
        survey.setTitle(rs.getString("title"));
        Date createDate = rs.getDate("create_date");
        if (createDate != null) survey.setCreateDate(createDate.toLocalDate());
        Date startDate = rs.getDate("start_date");
        if (startDate != null) survey.setStartDate(startDate.toLocalDate());
        Date endDate = rs.getDate("end_date");
        if (endDate != null) survey.setEndDate(endDate.toLocalDate());
        survey.setActive(rs.getBoolean("is_active")); // is_active 컬럼이 DB에 있다고 가정
        return survey;
    }

    @Override
    public List<Survey> findAll(Connection conn) throws DaoException {
        List<Survey> surveys = new ArrayList<>();
        String sql = "SELECT survey_id, title, create_date, start_date, end_date, is_active FROM Surveys ORDER BY start_date DESC, survey_id DESC"; // Surveys로 변경
        // ... (이하 PreparedStatement 및 ResultSet 처리 동일) ...
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                surveys.add(mapRowToSurvey(rs));
            }
        } catch (SQLException e) {
            throw new DaoException("모든 설문조사 조회 실패", e);
        }
        return surveys;
    }

    @Override
    public List<Survey> findAll() throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return findAll(conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 오류 또는 모든 설문조사 조회 실패", e);
        }
    }

    @Override
    public Survey findById(int surveyId, Connection conn) throws DaoException {
        String sql = "SELECT survey_id, title, create_date, start_date, end_date, is_active FROM Surveys WHERE survey_id = ?"; // Surveys로 변경
        // ... (이하 동일) ...
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, surveyId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToSurvey(rs);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("ID로 설문조사 조회 실패: " + surveyId, e);
        }
        return null;
    }

    @Override
    public Survey findById(int surveyId) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return findById(surveyId, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 오류 또는 ID로 설문조사 조회 실패", e);
        }
    }

    @Override
    public void save(Survey survey, Connection conn) throws DaoException {
        String sql = "INSERT INTO Surveys (title, create_date, start_date, end_date, is_active) VALUES (?, ?, ?, ?, ?)"; // Surveys로 변경
        // ... (이하 동일) ...
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, survey.getTitle());
            pstmt.setDate(2, survey.getCreateDate() != null ? Date.valueOf(survey.getCreateDate()) : null);
            pstmt.setDate(3, survey.getStartDate() != null ? Date.valueOf(survey.getStartDate()) : null);
            pstmt.setDate(4, survey.getEndDate() != null ? Date.valueOf(survey.getEndDate()) : null);
            pstmt.setBoolean(5, survey.isActive());

            pstmt.executeUpdate();
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    survey.setSurveyId(generatedKeys.getInt(1));
                } else {
                    throw new DaoException("설문조사 저장 후 ID 가져오기 실패.", null);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("설문조사 저장 실패", e);
        }
    }

    @Override
    public void save(Survey survey) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            save(survey, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 오류 또는 설문조사 저장 실패", e);
        }
    }

    @Override
    public void update(Survey survey, Connection conn) throws DaoException {
        String sql = "UPDATE Surveys SET title = ?, create_date = ?, start_date = ?, end_date = ?, is_active = ? WHERE survey_id = ?"; // Surveys로 변경
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, survey.getTitle());
            pstmt.setDate(2, survey.getCreateDate() != null ? Date.valueOf(survey.getCreateDate()) : null);
            pstmt.setDate(3, survey.getStartDate() != null ? Date.valueOf(survey.getStartDate()) : null);
            pstmt.setDate(4, survey.getEndDate() != null ? Date.valueOf(survey.getEndDate()) : null);
            pstmt.setBoolean(5, survey.isActive());
            pstmt.setInt(6, survey.getSurveyId());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DaoException("설문조사 업데이트 실패: surveyId=" + survey.getSurveyId() + "를 찾을 수 없습니다.", null);
            }
        } catch (SQLException e) {
            throw new DaoException("설문조사 업데이트 실패", e);
        }
    }

    @Override
    public void update(Survey survey) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            update(survey, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 오류 또는 설문조사 업데이트 실패", e);
        }
    }

    @Override
    public void delete(int surveyId, Connection conn) throws DaoException {
        String sql = "DELETE FROM Surveys WHERE survey_id = ?"; // Surveys로 변경
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, surveyId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("설문조사 삭제 실패", e);
        }
    }

    @Override
    public void delete(int surveyId) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            delete(surveyId, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 오류 또는 설문조사 삭제 실패", e);
        }
    }

    @Override
    public boolean hasOverlappingActiveOrScheduledSurvey(LocalDate startDate, LocalDate endDate, int excludeSurveyId, Connection conn) throws DaoException {
        String sql = "SELECT COUNT(*) FROM Surveys " + // Surveys로 변경
                "WHERE survey_id != ? " +
                "AND is_active = TRUE " +
                "AND (start_date <= ? AND end_date >= ?)";
        // ... (이하 동일) ...
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, excludeSurveyId);
            pstmt.setDate(2, Date.valueOf(endDate));
            pstmt.setDate(3, Date.valueOf(startDate));

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new DaoException("겹치는 활성/예정 설문조사 확인 중 오류", e);
        }
        return false;
    }

    @Override
    public boolean hasOverlappingActiveOrScheduledSurvey(LocalDate startDate, LocalDate endDate, int excludeSurveyId) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return hasOverlappingActiveOrScheduledSurvey(startDate, endDate, excludeSurveyId, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 오류 또는 겹치는 설문조사 확인 중 오류", e);
        }
    }
}