package main.java.dao;

import main.java.model.SurveyQuestionOption;
import main.java.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * SurveyQuestionOptionDao 인터페이스의 구현 클래스입니다.
 * 설문조사 객관식 질문의 선택지 데이터에 접근합니다.
 */
public class SurveyQuestionOptionDaoImpl implements SurveyQuestionOptionDao {

    /**
     * 특정 질문 ID에 해당하는 모든 선택지를 조회합니다.
     * @param questionId 질문 ID
     * @param conn 데이터베이스 연결 객체
     * @return 선택지 목록
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    @Override
    public List<SurveyQuestionOption> findByQuestionId(int questionId, Connection conn) throws DaoException {
        List<SurveyQuestionOption> options = new ArrayList<>();
        String sql = "SELECT option_id, question_id, option_text, option_order FROM survey_question_options WHERE question_id = ? ORDER BY option_order ASC, option_id ASC";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, questionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    SurveyQuestionOption option = new SurveyQuestionOption();
                    option.setOptionId(rs.getInt("option_id"));
                    option.setQuestionId(rs.getInt("question_id"));
                    option.setOptionText(rs.getString("option_text"));
                    option.setOptionOrder(rs.getInt("option_order"));
                    options.add(option);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("질문 ID로 선택지 조회 중 오류 발생: questionId=" + questionId, e);
        }
        return options;
    }

    /**
     * 특정 질문 ID에 해당하는 모든 선택지를 조회합니다. (자체 Connection 관리)
     * @param questionId 질문 ID
     * @return 선택지 목록
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    @Override
    public List<SurveyQuestionOption> findByQuestionId(int questionId) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return findByQuestionId(questionId, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 오류 또는 질문 ID로 선택지 조회 중 오류", e);
        }
    }

    /**
     * 단일 설문조사 질문 선택지를 저장합니다.
     * @param option 저장할 SurveyQuestionOption 객체 (optionId는 자동 생성 후 설정됨)
     * @param conn 데이터베이스 연결 객체
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    @Override
    public void save(SurveyQuestionOption option, Connection conn) throws DaoException {
        String sql = "INSERT INTO survey_question_options (question_id, option_text, option_order) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, option.getQuestionId());
            pstmt.setString(2, option.getOptionText());
            pstmt.setInt(3, option.getOptionOrder());
            pstmt.executeUpdate();
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    option.setOptionId(generatedKeys.getInt(1));
                } else {
                    throw new DaoException("선택지 저장 후 ID 가져오기 실패.", null);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("설문조사 선택지 저장 중 오류 발생", e);
        }
    }

    /**
     * 단일 설문조사 질문 선택지를 저장합니다. (자체 Connection 관리)
     * @param option 저장할 SurveyQuestionOption 객체
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    @Override
    public void save(SurveyQuestionOption option) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            // 단일 작업의 경우 auto-commit이거나, 필요시 여기서 트랜잭션 관리
            save(option, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 오류 또는 설문조사 선택지 저장 중 오류", e);
        }
    }


    /**
     * 여러 설문조사 질문 선택지를 일괄 저장합니다.
     * @param options 저장할 SurveyQuestionOption 객체 목록
     * @param conn 데이터베이스 연결 객체
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    @Override
    public void saveAll(List<SurveyQuestionOption> options, Connection conn) throws DaoException {
        // 모든 옵션이 동일한 questionId를 가진다고 가정하거나, 각 option 객체에 questionId가 설정되어 있어야 함
        String sql = "INSERT INTO survey_question_options (question_id, option_text, option_order) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (SurveyQuestionOption option : options) {
                pstmt.setInt(1, option.getQuestionId());
                pstmt.setString(2, option.getOptionText());
                pstmt.setInt(3, option.getOptionOrder());
                pstmt.addBatch();
            }
            pstmt.executeBatch();

            // 일괄 저장 시 생성된 ID를 각 객체에 설정하는 것은 더 복잡할 수 있음 (DB 드라이버 지원 여부 확인)
            // 여기서는 우선 ID 설정 로직은 생략하고, 필요하다면 개별 save 호출 또는 다른 방식 고려
            // 또는, 이 메서드는 ID를 반환하지 않고 저장만 수행하도록 단순화

        } catch (SQLException e) {
            throw new DaoException("설문조사 선택지 일괄 저장 중 오류 발생", e);
        }
    }

    /**
     * 여러 설문조사 질문 선택지를 일괄 저장합니다. (자체 Connection 관리)
     * @param options 저장할 SurveyQuestionOption 객체 목록
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    @Override
    public void saveAll(List<SurveyQuestionOption> options) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            saveAll(options, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 오류 또는 설문조사 선택지 일괄 저장 중 오류", e);
        }
    }


    /**
     * 특정 질문 ID에 해당하는 모든 선택지를 삭제합니다.
     * @param questionId 질문 ID
     * @param conn 데이터베이스 연결 객체
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    @Override
    public void deleteByQuestionId(int questionId, Connection conn) throws DaoException {
        String sql = "DELETE FROM survey_question_options WHERE question_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, questionId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("질문 ID로 선택지 삭제 중 오류 발생: questionId=" + questionId, e);
        }
    }

    /**
     * 특정 질문 ID에 해당하는 모든 선택지를 삭제합니다. (자체 Connection 관리)
     * @param questionId 질문 ID
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    @Override
    public void deleteByQuestionId(int questionId) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            deleteByQuestionId(questionId, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 오류 또는 질문 ID로 선택지 삭제 중 오류", e);
        }
    }
}