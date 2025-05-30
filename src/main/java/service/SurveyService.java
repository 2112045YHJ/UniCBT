package main.java.service;

import main.java.dao.DaoException;
import main.java.dto.SurveyQuestionResultDto;
import main.java.model.Survey;
import main.java.model.SurveyQuestion; // SurveyQuestion 모델 임포트
import main.java.model.SurveyQuestionOption; // SurveyQuestionOption 모델 임포트
import main.java.dto.SurveyFullDto; // 설문 전체 정보(질문, 선택지 포함)를 담을 DTO (생성 필요)

import java.sql.SQLException;
import java.util.List;
import java.time.LocalDate; // LocalDate 임포트
import main.java.model.User; // User 모델 임포트 (필요시)
import java.util.Map;

public interface SurveyService {

    /**
     * 모든 설문조사 목록을 가져옵니다.
     * @return 설문조사 목록
     * @throws ServiceException 서비스 처리 중 오류 발생 시
     */
    List<Survey> getAllSurveys() throws ServiceException;

    /**
     * ID로 특정 설문조사 정보를 가져옵니다 (질문 및 선택지 미포함).
     * @param surveyId 조회할 설문조사 ID
     * @return Survey 객체 (없으면 null)
     * @throws ServiceException 서비스 처리 중 오류 발생 시
     */
    Survey getSurveyById(int surveyId) throws ServiceException;

    /**
     * ID로 특정 설문조사의 전체 정보(질문 및 객관식 선택지 포함)를 가져옵니다.
     * @param surveyId 조회할 설문조사 ID
     * @return SurveyFullDto 객체 (없으면 null)
     * @throws ServiceException 서비스 처리 중 오류 발생 시
     */
    SurveyFullDto getSurveyFullById(int surveyId) throws ServiceException;

    /**
     * 새로운 설문조사를 생성합니다.
     * 활성화 정책(기간 중복 불가, 단일 활성화)을 검사합니다.
     * @param surveyFullDto 생성할 설문조사의 전체 정보 (Survey 기본 정보, SurveyQuestion 목록, 각 Question의 SurveyQuestionOption 목록 포함)
     * @throws ServiceException 서비스 처리 중 오류 발생 시 (예: 기간 중복, 데이터 유효성 오류 등)
     */
    void createSurvey(SurveyFullDto surveyFullDto) throws ServiceException, SQLException, DaoException;

    /**
     * 기존 설문조사를 수정합니다.
     * 수정 정책(시작 전, 진행 중, 종료 후) 및 활성화 정책을 적용합니다.
     * @param surveyFullDto 수정할 설문조사의 전체 정보
     * @throws ServiceException 서비스 처리 중 오류 발생 시 (예: 수정 불가 조건, 기간 중복 등)
     */
    void updateSurvey(SurveyFullDto surveyFullDto) throws ServiceException, SQLException, DaoException;

    /**
     * 설문조사를 삭제합니다.
     * 관련된 질문, 선택지, 응답도 모두 삭제됩니다.
     * @param surveyId 삭제할 설문조사 ID
     * @throws ServiceException 서비스 처리 중 오류 발생 시
     */
    void deleteSurvey(int surveyId) throws ServiceException;

    /**
     * 특정 설문조사의 응답 수를 반환합니다.
     * @param surveyId 조회할 설문조사 ID
     * @return 응답 수
     * @throws ServiceException 서비스 처리 중 오류 발생 시
     */
    int getResponseCount(int surveyId) throws ServiceException;
    /**
     * 특정 설문조사의 특정 질문에 대한 집계된 응답 결과를 반환합니다.
     * @param surveyId 설문조사 ID
     * @param questionId 질문 ID
     * @return 해당 질문의 응답 결과 DTO
     * @throws ServiceException 서비스 처리 중 오류 발생 시
     */
    SurveyQuestionResultDto getQuestionResults(int surveyId, int questionId) throws ServiceException;
    /**
     * 현재 클라이언트(학생)가 참여할 수 있는 활성화된 설문조사의 전체 정보를 가져옵니다.
     * "한번에 단 하나만 활성화" 정책에 따라, 조건에 맞는 설문이 없거나 하나만 반환됩니다.
     * @return 현재 참여 가능한 SurveyFullDto 객체 (없으면 null)
     * @throws ServiceException 서비스 처리 중 오류 발생 시
     */
    SurveyFullDto getActiveSurveyForClient() throws ServiceException;

    /**
     * 학생이 제출한 설문조사 응답들을 저장합니다.
     * @param userId 응답을 제출한 사용자 ID
     * @param surveyId 응답한 설문조사 ID
     * @param answers 학생의 답변들 (Key: questionId, Value: answerText - 객관식의 경우 option_id 문자열)
     * @throws ServiceException 서비스 처리 중 오류 발생 시
     */
    void submitSurveyResponses(int userId, int surveyId, Map<Integer, String> answers) throws ServiceException;

    /**
     * 특정 사용자가 특정 설문조사를 이미 완료했는지 확인합니다.
     * @param userId 확인할 사용자 ID
     * @param surveyId 확인할 설문조사 ID
     * @return 완료했으면 true, 아니면 false
     * @throws ServiceException 서비스 처리 중 오류 발생 시
     */
    boolean hasUserCompletedSurvey(int userId, int surveyId) throws ServiceException;
}