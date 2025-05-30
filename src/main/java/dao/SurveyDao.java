package main.java.dao;

import main.java.model.Survey;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

public interface SurveyDao {
    List<Survey> findAll(Connection conn) throws DaoException;
    List<Survey> findAll() throws DaoException;

    Survey findById(int surveyId, Connection conn) throws DaoException;
    Survey findById(int surveyId) throws DaoException;

    void save(Survey survey, Connection conn) throws DaoException; // surveyId가 자동 생성 후 survey 객체에 설정되어야 함
    void save(Survey survey) throws DaoException;

    void update(Survey survey, Connection conn) throws DaoException;
    void update(Survey survey) throws DaoException;

    void delete(int surveyId, Connection conn) throws DaoException;
    void delete(int surveyId) throws DaoException;

    /**
     * 주어진 기간 내에 (지정된 surveyId를 제외하고) 활성화되어 있거나 예정된 다른 설문조사가 있는지 확인합니다.
     * @param startDate 확인할 기간의 시작일
     * @param endDate 확인할 기간의 종료일
     * @param excludeSurveyId 검사에서 제외할 설문조사 ID (주로 수정 시 자신을 제외하기 위함, 0이면 모든 설문 검사)
     * @param conn 데이터베이스 연결 객체
     * @return 겹치는 설문조사가 있으면 true, 없으면 false
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    boolean hasOverlappingActiveOrScheduledSurvey(LocalDate startDate, LocalDate endDate, int excludeSurveyId, Connection conn) throws DaoException;
    boolean hasOverlappingActiveOrScheduledSurvey(LocalDate startDate, LocalDate endDate, int excludeSurveyId) throws DaoException;
}