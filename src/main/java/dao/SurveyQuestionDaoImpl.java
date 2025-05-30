package main.java.dao;

import main.java.model.SurveyQuestion;
import main.java.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SurveyQuestionDaoImpl implements SurveyQuestionDao {

    /**
     * ResultSet의 현재 행에서 SurveyQuestion 객체를 생성하여 반환합니다.
     */
    private SurveyQuestion mapRowToSurveyQuestion(ResultSet rs) throws SQLException {
        SurveyQuestion sq = new SurveyQuestion();
        sq.setQuestionId(rs.getInt("question_id"));
        sq.setSurveyId(rs.getInt("survey_id"));
        sq.setQuestionText(rs.getString("question_text"));
        sq.setQuestionType(rs.getString("question_type"));
        // DB 스키마에 question_order 컬럼이 있다면 다음을 추가:
        // sq.setQuestionOrder(rs.getInt("question_order"));
        return sq;
    }

    @Override
    public List<SurveyQuestion> findBySurveyId(int surveyId, Connection conn) throws DaoException {
        List<SurveyQuestion> questions = new ArrayList<>();
        // 테이블 이름을 SurveyQuestions로 수정
        String sql = "SELECT question_id, survey_id, question_text, question_type FROM SurveyQuestions WHERE survey_id = ? ORDER BY question_id ASC";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, surveyId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    questions.add(mapRowToSurveyQuestion(rs));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("설문 ID로 질문 조회 실패: " + surveyId, e);
        }
        return questions;
    }

    @Override
    public List<SurveyQuestion> findBySurveyId(int surveyId) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return findBySurveyId(surveyId, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 오류 또는 설문 ID로 질문 조회 실패", e);
        }
    }

    @Override
    public SurveyQuestion findById(int questionId, Connection conn) throws DaoException {
        // 테이블 이름을 SurveyQuestions로 수정
        String sql = "SELECT question_id, survey_id, question_text, question_type FROM SurveyQuestions WHERE question_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, questionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToSurveyQuestion(rs);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("질문 ID로 질문 조회 실패: " + questionId, e);
        }
        return null;
    }

    @Override
    public SurveyQuestion findById(int questionId) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return findById(questionId, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 오류 또는 질문 ID로 질문 조회 실패", e);
        }
    }

    @Override
    public void save(SurveyQuestion question, Connection conn) throws DaoException {
        // 테이블 이름을 SurveyQuestions로 수정
        String sql = "INSERT INTO SurveyQuestions (survey_id, question_text, question_type) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, question.getSurveyId());
            pstmt.setString(2, question.getQuestionText());
            pstmt.setString(3, question.getQuestionType());
            pstmt.executeUpdate();
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    question.setQuestionId(generatedKeys.getInt(1));
                } else {
                    throw new DaoException("설문 질문 저장 후 ID 가져오기 실패.", null);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("설문 질문 저장 실패", e);
        }
    }

    @Override
    public void save(SurveyQuestion question) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            save(question, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 오류 또는 설문 질문 저장 실패", e);
        }
    }

    @Override
    public void deleteBySurveyId(int surveyId, Connection conn) throws DaoException {
        // 테이블 이름을 SurveyQuestions로 수정
        String sql = "DELETE FROM SurveyQuestions WHERE survey_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, surveyId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("설문 ID로 질문들 삭제 실패: " + surveyId, e);
        }
    }

    @Override
    public void deleteBySurveyId(int surveyId) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            deleteBySurveyId(surveyId, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 오류 또는 설문 ID로 질문들 삭제 실패", e);
        }
    }
}