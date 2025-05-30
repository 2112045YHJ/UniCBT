package main.java.dao;

import main.java.model.SurveyResponse; // SurveyResponse 모델 임포트
import main.java.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SurveyResponseDaoImpl implements SurveyResponseDao {

    @Override
    public int countBySurveyId(int surveyId, Connection conn) throws DaoException {
        // 테이블 이름을 SurveyResponses로 수정
        String sql = "SELECT COUNT(DISTINCT user_id) FROM SurveyResponses WHERE survey_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, surveyId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            // DaoException 생성 시 원인 예외(e)를 전달하는 것이 좋습니다.
            throw new DaoException("설문 ID로 응답 수 조회 실패: " + surveyId, e);
        }
        return 0;
    }

    @Override
    public int countBySurveyId(int surveyId) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return countBySurveyId(surveyId, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 오류 또는 설문 ID로 응답 수 조회 실패", e);
        }
    }

    @Override
    public void deleteBySurveyId(int surveyId, Connection conn) throws DaoException {
        // 테이블 이름을 SurveyResponses로 수정
        String sql = "DELETE FROM SurveyResponses WHERE survey_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, surveyId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("설문 ID로 응답 삭제 실패: " + surveyId, e);
        }
    }

    @Override
    public void deleteBySurveyId(int surveyId) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            deleteBySurveyId(surveyId, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 오류 또는 설문 ID로 응답 삭제 실패", e);
        }
    }

    @Override
    public void saveResponse(SurveyResponse response, Connection conn) throws DaoException {
        // 테이블 이름을 SurveyResponses로 수정
        String sql = "INSERT INTO SurveyResponses (user_id, survey_id, question_id, answer_text) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, response.getUserId());
            pstmt.setInt(2, response.getSurveyId());
            pstmt.setInt(3, response.getQuestionId());
            pstmt.setString(4, response.getAnswerText());
            pstmt.executeUpdate();
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    response.setResponseId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("설문 응답 저장 실패", e);
        }
    }

    @Override
    public void saveResponse(SurveyResponse response) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            saveResponse(response, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 오류 또는 설문 응답 저장 실패", e);
        }
    }

    @Override
    public List<SurveyResponse> findResponsesBySurveyAndQuestion(int surveyId, int questionId, Connection conn) throws DaoException {
        List<SurveyResponse> responses = new ArrayList<>();
        // 테이블 이름을 SurveyResponses로 수정
        String sql = "SELECT response_id, user_id, survey_id, question_id, answer_text FROM SurveyResponses WHERE survey_id = ? AND question_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, surveyId);
            pstmt.setInt(2, questionId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                SurveyResponse response = new SurveyResponse();
                response.setResponseId(rs.getInt("response_id"));
                response.setUserId(rs.getInt("user_id"));
                response.setSurveyId(rs.getInt("survey_id"));
                response.setQuestionId(rs.getInt("question_id"));
                response.setAnswerText(rs.getString("answer_text"));
                responses.add(response);
            }
        } catch (SQLException e) {
            throw new DaoException("설문 및 질문 ID로 응답 조회 실패", e);
        }
        return responses;
    }

    @Override
    public List<SurveyResponse> findResponsesBySurveyAndQuestion(int surveyId, int questionId) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return findResponsesBySurveyAndQuestion(surveyId, questionId, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 오류 또는 설문 및 질문 ID로 응답 조회 실패", e);
        }
    }

    @Override
    public boolean hasUserResponded(int userId, int surveyId, Connection conn) throws DaoException {
        // COUNT(*) 대신 EXISTS 또는 LIMIT 1을 사용하는 것이 더 효율적일 수 있습니다.
        String sql = "SELECT COUNT(*) FROM SurveyResponses WHERE user_id = ? AND survey_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, surveyId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new DaoException("사용자 설문 응답 여부 확인 중 오류: userId=" + userId + ", surveyId=" + surveyId, e);
        }
        return false;
    }

    @Override
    public boolean hasUserResponded(int userId, int surveyId) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return hasUserResponded(userId, surveyId, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 오류 또는 사용자 설문 응답 여부 확인 중 오류", e);
        }
    }
}