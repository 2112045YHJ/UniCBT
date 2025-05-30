package main.java.service;

import main.java.dao.DaoException;
import main.java.dao.SurveyDao;
import main.java.dao.SurveyDaoImpl;
import main.java.dao.SurveyQuestionDao;
import main.java.dao.SurveyQuestionDaoImpl;
import main.java.dao.SurveyQuestionOptionDao;
import main.java.dao.SurveyQuestionOptionDaoImpl;
import main.java.dao.SurveyResponseDao;
import main.java.dao.SurveyResponseDaoImpl;
import main.java.dto.SurveyFullDto;
import main.java.dto.SurveyQuestionOptionResultDto;
import main.java.dto.SurveyQuestionResultDto;
import main.java.model.Survey;
import main.java.model.SurveyQuestion;
import main.java.model.SurveyQuestionOption;
import main.java.model.SurveyResponse;
import main.java.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import main.java.model.SurveyResponse;


public class SurveyServiceImpl implements SurveyService {

    private final SurveyDao surveyDao = new SurveyDaoImpl();
    private final SurveyQuestionDao surveyQuestionDao = new SurveyQuestionDaoImpl();
    private final SurveyQuestionOptionDao surveyQuestionOptionDao = new SurveyQuestionOptionDaoImpl();
    private final SurveyResponseDao surveyResponseDao = new SurveyResponseDaoImpl();

    @Override
    public List<Survey> getAllSurveys() throws ServiceException {
        try (Connection conn = DBConnection.getConnection()) {
            // findAll은 기본적으로 최신 설문(시작일 기준)이 먼저 오도록 정렬하는 것이 좋음 (DAO에서 처리)
            return surveyDao.findAll(conn);
        } catch (DaoException | SQLException e) {
            throw new ServiceException("모든 설문조사 목록 조회 중 오류 발생: " + e.getMessage(), e);
        }
    }

    @Override
    public Survey getSurveyById(int surveyId) throws ServiceException {
        try (Connection conn = DBConnection.getConnection()) {
            return surveyDao.findById(surveyId, conn);
        } catch (DaoException | SQLException e) {
            throw new ServiceException("ID로 설문조사 정보 조회 중 오류 발생 (surveyId=" + surveyId + "): " + e.getMessage(), e);
        }
    }

    @Override
    public SurveyFullDto getSurveyFullById(int surveyId) throws ServiceException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection(); // 단일 작업이지만 여러 DAO 호출
            Survey survey = surveyDao.findById(surveyId, conn);
            if (survey == null) {
                // ServiceException을 던지거나 null을 반환 (호출하는 쪽에서 처리)
                throw new ServiceException("ID에 해당하는 설문조사를 찾을 수 없습니다: " + surveyId);
            }

            SurveyFullDto surveyFullDto = new SurveyFullDto();
            surveyFullDto.setSurvey(survey);

            List<SurveyQuestion> questions = surveyQuestionDao.findBySurveyId(surveyId, conn);
            if (questions != null) {
                for (SurveyQuestion q : questions) {
                    SurveyFullDto.SurveyQuestionDto qDto = new SurveyFullDto.SurveyQuestionDto();
                    qDto.setQuestion(q);
                    // SurveyQuestion의 getType() 메서드가 "MCQ", "TEXT" 등의 문자열을 반환한다고 가정
                    if ("MCQ".equalsIgnoreCase(q.getQuestionType())) {
                        List<SurveyQuestionOption> options = surveyQuestionOptionDao.findByQuestionId(q.getQuestionId(), conn);
                        qDto.setOptions(options);
                    }
                    surveyFullDto.getQuestions().add(qDto);
                }
            }
            return surveyFullDto;

        } catch (DaoException e) {
            throw new ServiceException("ID로 설문조사 전체 정보 조회 중 오류 발생 (surveyId=" + surveyId + "): " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("DB 연결 종료 중 오류 (getSurveyFullById): " + e.getMessage());
                }
            }
        }
    }

    /**
     * 새로운 설문조사를 생성합니다.
     * 정책: 지정된 기간에 다른 활성/예정 설문이 있으면 등록 불가.
     */
    @Override
    public void createSurvey(SurveyFullDto surveyFullDto) throws ServiceException, SQLException, DaoException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            Survey survey = surveyFullDto.getSurvey();
            // 1. 입력 값 유효성 검사
            validateSurveyBasicInfo(survey);

            // 2. 활성화 정책 검사: "한번에 단 하나만 활성화" 및 "기간이 겹치면 등록 불가"
            // hasOverlappingActiveOrScheduledSurvey는 지정된 기간과 겹치는 *활성(is_active=true)* 설문이 있는지 확인
            if (surveyDao.hasOverlappingActiveOrScheduledSurvey(survey.getStartDate(), survey.getEndDate(), 0, conn)) {
                throw new ServiceException("지정한 기간(" + survey.getStartDate() + "~" + survey.getEndDate() + ")에 이미 다른 설문조사가 예정 또는 진행 중입니다.");
            }

            // 3. 설문 기본 정보 저장
            if (survey.getCreateDate() == null) {
                survey.setCreateDate(LocalDate.now());
            }
            survey.setActive(true); // 새 설문은 기본적으로 활성(기간에 따라 실제 진행 여부 결정)
            surveyDao.save(survey, conn); // 이 호출 후 survey 객체에 surveyId가 설정되어야 함
            int surveyId = survey.getSurveyId();
            if (surveyId <= 0) {
                throw new ServiceException("설문조사 저장 후 ID를 가져오는데 실패했습니다.");
            }

            // 4. 질문 및 (객관식의 경우) 선택지 저장
            saveQuestionsWithOptions(conn, surveyId, surveyFullDto.getQuestions());

            conn.commit();
        } catch (DaoException | SQLException | ServiceException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { /* 로깅 */ }
            if (e instanceof ServiceException) throw e; // 이미 ServiceException이면 그대로 던짐
            throw new ServiceException("새 설문조사 생성 중 오류 발생: " + e.getMessage(), e);
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { /* 로깅 */ }
        }
    }

    /**
     * 기존 설문조사를 수정합니다.
     * 정책: 종료된 설문은 수정 불가. 시작 전 설문은 업데이트. 진행 중 설문은 원본(수정됨+비활성) 처리 후 새 설문으로 생성.
     */
    @Override
    public void updateSurvey(SurveyFullDto surveyFullDto) throws ServiceException, SQLException, DaoException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            Survey surveyFromUi = surveyFullDto.getSurvey();
            int surveyIdToEdit = surveyFromUi.getSurveyId();

            // 1. 수정 대상 설문조사 조회
            Survey existingSurvey = surveyDao.findById(surveyIdToEdit, conn);
            if (existingSurvey == null) {
                throw new ServiceException("수정할 설문조사를 찾을 수 없습니다 (ID: " + surveyIdToEdit + ")");
            }

            // 2. 입력 값 유효성 검사
            validateSurveyBasicInfo(surveyFromUi);

            // 3. 수정 정책 적용
            LocalDate today = LocalDate.now();
            boolean isExistingSurveyEnded = existingSurvey.getEndDate() != null && today.isAfter(existingSurvey.getEndDate());
            boolean isExistingSurveyActiveNow = existingSurvey.getStartDate() != null && !today.isBefore(existingSurvey.getStartDate()) &&
                    existingSurvey.getEndDate() != null && !today.isAfter(existingSurvey.getEndDate()) &&
                    existingSurvey.isActive(); // DB의 is_active 플래그도 확인

            if (isExistingSurveyEnded && existingSurvey.isActive()) { // 기간이 지났으나 여전히 active=true로 되어있는 경우 (예: "(수정됨)" 처리 안된 원본)
                throw new ServiceException("기간이 종료된 설문조사는 수정할 수 없습니다.");
            }
            if (!existingSurvey.isActive() && existingSurvey.getTitle().contains("(수정됨)")) { // 이미 "(수정됨)" 처리된 비활성 설문
                throw new ServiceException("이미 수정 처리된 이전 버전의 설문조사입니다. 수정할 수 없습니다.");
            }


            // 4. 활성화 정책 검사 (기간 중복) - 자기 자신(surveyIdToEdit)은 제외하고 검사
            if (surveyDao.hasOverlappingActiveOrScheduledSurvey(surveyFromUi.getStartDate(), surveyFromUi.getEndDate(), surveyIdToEdit, conn)) {
                throw new ServiceException("수정하려는 기간(" + surveyFromUi.getStartDate() + "~" + surveyFromUi.getEndDate() + ")에 이미 다른 설문조사가 예정 또는 진행 중입니다.");
            }

            if (isExistingSurveyActiveNow) {
                // --- 진행 중인 설문 수정: 기존 것(E1)은 이름 변경 및 비활성화, 새 설문(E2)으로 생성 ---
                existingSurvey.setTitle(existingSurvey.getTitle() + " (수정됨)");
                existingSurvey.setActive(false); // 명시적 비활성화
                surveyDao.update(existingSurvey, conn);

                // 새 Survey 객체(E2) 생성
                Survey newSurveyCopy = new Survey();
                newSurveyCopy.setTitle(surveyFromUi.getTitle()); // UI에서 받은 새 제목
                newSurveyCopy.setStartDate(surveyFromUi.getStartDate()); // UI에서 받은 새 기간
                newSurveyCopy.setEndDate(surveyFromUi.getEndDate());
                newSurveyCopy.setCreateDate(LocalDate.now()); // 새 생성일
                newSurveyCopy.setActive(true); // 새 설문은 활성
                surveyDao.save(newSurveyCopy, conn); // E2 저장 (새 ID 발급)

                int newSurveyId = newSurveyCopy.getSurveyId();
                if (newSurveyId <= 0) throw new ServiceException("수정 중 새 설문조사 저장 후 ID를 가져오지 못했습니다.");

                // 질문 및 선택지도 새 설문 ID(newSurveyId)로 복사/저장
                saveQuestionsWithOptions(conn, newSurveyId, surveyFullDto.getQuestions());

            } else {
                // --- 시작 전 설문 또는 (기간은 안 지났지만 is_active=false인) 비활성 설문 수정: 기존 정보 업데이트 ---
                surveyFromUi.setCreateDate(existingSurvey.getCreateDate()); // 원본 생성일은 유지
                surveyFromUi.setActive(true); // 수정 시 다시 활성화 (기간에 따라 실제 진행 여부 결정)
                surveyDao.update(surveyFromUi, conn);

                // 기존 질문 및 선택지 모두 삭제 후 새로 삽입
                List<SurveyQuestion> oldQuestions = surveyQuestionDao.findBySurveyId(surveyIdToEdit, conn);
                for (SurveyQuestion oldQ : oldQuestions) {
                    if ("MCQ".equalsIgnoreCase(oldQ.getQuestionType())) {
                        surveyQuestionOptionDao.deleteByQuestionId(oldQ.getQuestionId(), conn);
                    }
                }
                surveyQuestionDao.deleteBySurveyId(surveyIdToEdit, conn);

                // 새 질문 및 선택지 저장
                saveQuestionsWithOptions(conn, surveyIdToEdit, surveyFullDto.getQuestions());
            }

            conn.commit();
        } catch (DaoException | SQLException | ServiceException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { /* 로깅 */ }
            if (e instanceof ServiceException) throw e;
            throw new ServiceException("설문조사 수정 중 오류 발생: " + e.getMessage(), e);
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { /* 로깅 */ }
        }
    }

    @Override
    public void deleteSurvey(int surveyId) throws ServiceException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. 응답 삭제
            surveyResponseDao.deleteBySurveyId(surveyId, conn);

            // 2. 질문에 속한 선택지들 삭제
            List<SurveyQuestion> questions = surveyQuestionDao.findBySurveyId(surveyId, conn);
            for (SurveyQuestion question : questions) {
                if ("MCQ".equalsIgnoreCase(question.getQuestionType())) {
                    surveyQuestionOptionDao.deleteByQuestionId(question.getQuestionId(), conn);
                }
            }
            // 3. 질문들 삭제
            surveyQuestionDao.deleteBySurveyId(surveyId, conn);

            // 4. 설문조사 삭제
            surveyDao.delete(surveyId, conn);

            conn.commit();
        } catch (DaoException | SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { /* 로깅 */ }
            throw new ServiceException("설문조사 삭제 중 오류 발생 (surveyId=" + surveyId + "): " + e.getMessage(), e);
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { /* 로깅 */ }
        }
    }

    @Override
    public int getResponseCount(int surveyId) throws ServiceException {
        try (Connection conn = DBConnection.getConnection()) {
            return surveyResponseDao.countBySurveyId(surveyId, conn);
        } catch (DaoException | SQLException e) {
            throw new ServiceException("설문조사 응답 수 조회 중 오류 발생 (surveyId=" + surveyId + "): " + e.getMessage(), e);
        }
    }

    /**
     * 설문조사의 기본 정보(제목, 기간)에 대한 유효성을 검사합니다.
     * @param survey 검사할 Survey 객체
     * @throws ServiceException 유효성 검사 실패 시
     */
    private void validateSurveyBasicInfo(Survey survey) throws ServiceException {
        if (survey == null) {
            throw new ServiceException("설문조사 정보가 없습니다.");
        }
        if (survey.getTitle() == null || survey.getTitle().trim().isEmpty()) {
            throw new ServiceException("설문조사 제목은 필수입니다.");
        }
        if (survey.getStartDate() == null || survey.getEndDate() == null) {
            throw new ServiceException("설문조사 시작일과 마감일은 필수입니다.");
        }
        if (survey.getEndDate().isBefore(survey.getStartDate())) {
            throw new ServiceException("설문조사 마감일은 시작일보다 이전일 수 없습니다.");
        }
        // 새 설문 또는 시작 전 설문 수정 시, 마감일이 오늘보다 이전인지 확인 (선택적 강화)
        // if ((survey.getSurveyId() == 0 || (survey.getStartDate() != null && LocalDate.now().isBefore(survey.getStartDate()))) &&
        //     survey.getEndDate().isBefore(LocalDate.now())) {
        //     throw new ServiceException("새 설문 또는 시작 전 설문의 마감일은 오늘보다 이전일 수 없습니다.");
        // }
    }

    /**
     * 주어진 설문 ID에 대해 질문 목록과 (객관식의 경우) 선택지들을 저장합니다.
     * @param conn 데이터베이스 연결 객체
     * @param surveyId 질문들이 속할 설문조사 ID
     * @param questionDtos 저장할 질문 DTO 목록 (각 DTO는 질문 정보와 선택지 정보 포함)
     * @throws DaoException 데이터 접근 오류 시
     * @throws ServiceException 유효성 오류 시
     */
    private void saveQuestionsWithOptions(Connection conn, int surveyId, List<SurveyFullDto.SurveyQuestionDto> questionDtos) throws DaoException, ServiceException {
        if (questionDtos == null || questionDtos.isEmpty()) {
            // 설문조사에 질문이 없는 것을 허용할지, 아니면 오류로 처리할지 정책에 따라 결정
            // throw new ServiceException("설문조사에는 최소 하나 이상의 질문이 필요합니다.");
            return; // 질문이 없어도 설문 자체는 생성 가능하도록 함 (선택)
        }

        int questionOrder = 1;
        for (SurveyFullDto.SurveyQuestionDto qDto : questionDtos) {
            SurveyQuestion question = qDto.getQuestion();
            if (question.getQuestionText() == null || question.getQuestionText().trim().isEmpty()) {
                throw new ServiceException("질문 내용은 필수입니다 (순서: " + questionOrder + ")");
            }
            if (question.getQuestionType() == null || question.getQuestionType().trim().isEmpty() ||
                    !(question.getQuestionType().equalsIgnoreCase("MCQ") || question.getQuestionType().equalsIgnoreCase("TEXT")) ) { // TEXT는 주관식 유형 예시
                throw new ServiceException("질문 유형은 'MCQ' 또는 'TEXT'여야 합니다 (순서: " + questionOrder + ")");
            }

            question.setSurveyId(surveyId);
            // question.setQuestionOrder(questionOrder++); // DB 스키마에 question_order가 있다면 설정
            surveyQuestionDao.save(question, conn); // questionId가 question 객체에 설정됨

            int questionId = question.getQuestionId();
            if (questionId <= 0) throw new ServiceException("설문 질문 저장 후 ID를 가져오지 못했습니다.");

            if ("MCQ".equalsIgnoreCase(question.getQuestionType()) && qDto.getOptions() != null) {
                if (qDto.getOptions().isEmpty()) {
                    throw new ServiceException("객관식 질문에는 최소 하나 이상의 선택지가 필요합니다 (질문: " + question.getQuestionText() + ")");
                }
                int optionOrder = 1;
                for (SurveyQuestionOption option : qDto.getOptions()) {
                    if (option.getOptionText() == null || option.getOptionText().trim().isEmpty()) {
                        throw new ServiceException("객관식 선택지 내용은 필수입니다 (질문: " + question.getQuestionText() + ")");
                    }
                    option.setQuestionId(questionId);
                    option.setOptionOrder(optionOrder++);
                    surveyQuestionOptionDao.save(option, conn);
                }
            }
        }
    }

    @Override
    public SurveyQuestionResultDto getQuestionResults(int surveyId, int questionId) throws ServiceException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();

            // 1. 질문 정보 가져오기
            // SurveyQuestionDao에 findById(questionId, conn)가 있다고 가정 (또는 findBySurveyId 후 필터링)
            SurveyQuestion question = surveyQuestionDao.findById(questionId, conn); // SurveyQuestionDao에 findById(id, conn) 구현 필요
            if (question == null || question.getSurveyId() != surveyId) {
                throw new ServiceException("요청한 질문 ID(" + questionId + ")가 해당 설문조사(ID:" + surveyId + ")에 속하지 않거나 존재하지 않습니다.");
            }

            SurveyQuestionResultDto resultDto = new SurveyQuestionResultDto(question);

            // 2. 해당 질문에 대한 모든 응답 가져오기
            List<SurveyResponse> responses = surveyResponseDao.findResponsesBySurveyAndQuestion(surveyId, questionId, conn);
            resultDto.setTotalResponsesForThisQuestion(responses.size());

            // 3. 질문 유형에 따라 결과 처리
            if ("MCQ".equalsIgnoreCase(question.getQuestionType())) {
                List<SurveyQuestionOption> options = surveyQuestionOptionDao.findByQuestionId(questionId, conn);
                Map<Integer, Integer> optionCounts = new HashMap<>(); // <OptionId, Count>
                int totalMcqResponses = 0;

                // 응답에서 각 선택지 ID 카운트
                for (SurveyResponse response : responses) {
                    try {
                        // answer_text에 option_id가 문자열로 저장되어 있다고 가정
                        int selectedOptionId = Integer.parseInt(response.getAnswerText());
                        optionCounts.put(selectedOptionId, optionCounts.getOrDefault(selectedOptionId, 0) + 1);
                        totalMcqResponses++; // 실제 MCQ 응답 수 (미응답/잘못된 형식 제외)
                    } catch (NumberFormatException e) {
                        // answer_text가 숫자로 변환될 수 없는 경우 (잘못된 데이터 또는 다른 유형의 답변) 무시
                        System.err.println("경고: MCQ 응답 처리 중 숫자 변환 오류 - surveyId: " + surveyId + ", questionId: " + questionId + ", answerText: " + response.getAnswerText());
                    }
                }
                resultDto.setTotalResponsesForThisQuestion(totalMcqResponses); // MCQ 응답 수로 업데이트

                for (SurveyQuestionOption option : options) {
                    int count = optionCounts.getOrDefault(option.getOptionId(), 0);
                    double rate = (totalMcqResponses > 0) ? ((double) count / totalMcqResponses) * 100.0 : 0.0;
                    rate = Math.round(rate * 100.0) / 100.0; // 소수점 둘째 자리
                    resultDto.getMcqOptionResults().add(new SurveyQuestionOptionResultDto(option, count, rate));
                }
                // 선택지 순서대로 정렬 (SurveyQuestionOption에 optionOrder가 있고, findByQuestionId에서 정렬해준다면 불필요)
                // resultDto.getMcqOptionResults().sort(Comparator.comparingInt(optResDto -> optResDto.getOption().getOptionOrder()));

            } else if ("TEXT".equalsIgnoreCase(question.getQuestionType())) { // 주관식(TEXT) 유형 처리
                List<String> textResponses = responses.stream()
                        .map(SurveyResponse::getAnswerText)
                        .filter(text -> text != null && !text.trim().isEmpty()) // 비어있지 않은 답변만
                        .collect(Collectors.toList());
                resultDto.setTextResponses(textResponses);
                resultDto.setTotalResponsesForThisQuestion(textResponses.size()); // 실제 텍스트 응답 수
            }
            // 다른 질문 유형이 있다면 여기에 추가 처리

            return resultDto;

        } catch (DaoException e) {
            throw new ServiceException("질문 결과 조회 중 오류 발생 (surveyId: " + surveyId + ", questionId: " + questionId + "): " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("DB 연결 종료 중 오류 (getQuestionResults): " + e.getMessage());
                }
            }
        }
    }
    @Override
    public SurveyFullDto getActiveSurveyForClient() throws ServiceException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            // SurveyDao에 현재 날짜 기준으로 활성화된 설문 하나를 가져오는 메서드가 필요할 수 있음
            // 예: Survey findActiveSurvey(LocalDate today, Connection conn)
            // 여기서는 getAllSurveys 후 필터링으로 간단히 구현 (실제로는 비효율적일 수 있음)

            List<Survey> allSurveys = surveyDao.findAll(conn); // is_active=true 인 것들만 가져오도록 DAO 수정 고려
            LocalDate today = LocalDate.now();
            Survey activeSurvey = null;

            for (Survey survey : allSurveys) {
                if (survey.isActive() && // DB의 is_active 플래그 확인
                        survey.getStartDate() != null && !today.isBefore(survey.getStartDate()) &&
                        survey.getEndDate() != null && !today.isAfter(survey.getEndDate())) {
                    activeSurvey = survey;
                    break; // "한번에 단 하나만 활성화" 정책이므로, 첫 번째로 찾은 활성 설문을 사용
                }
            }

            if (activeSurvey != null) {
                // getSurveyFullById는 내부적으로 Connection을 다시 열므로, 현재 Connection을 전달하도록 수정하거나,
                // 이 메서드 내에서 직접 SurveyFullDto를 구성하는 것이 트랜잭션 측면에서 더 나을 수 있음.
                // 여기서는 getSurveyFullById를 그대로 호출 (해당 메서드가 자체 Connection을 관리한다고 가정)
                // 또는, 이 conn을 getSurveyFullById에 전달하는 오버로드 메서드 사용.
                // 아래는 현재 conn을 사용하도록 getSurveyFullById 로직을 일부 가져온 형태:

                SurveyFullDto surveyFullDto = new SurveyFullDto();
                surveyFullDto.setSurvey(activeSurvey);

                List<SurveyQuestion> questions = surveyQuestionDao.findBySurveyId(activeSurvey.getSurveyId(), conn);
                if (questions != null) {
                    int questionOrderInSurvey = 1; // UI 표시용 순번
                    for (SurveyQuestion q : questions) {
                        // q.setQuestionOrderInSurvey(questionOrderInSurvey++); // 필요시 SurveyQuestion 모델에 순서 필드 추가
                        SurveyFullDto.SurveyQuestionDto qDto = new SurveyFullDto.SurveyQuestionDto();
                        qDto.setQuestion(q);
                        if ("MCQ".equalsIgnoreCase(q.getQuestionType())) {
                            List<SurveyQuestionOption> options = surveyQuestionOptionDao.findByQuestionId(q.getQuestionId(), conn);
                            // 옵션 순서(option_order)에 따라 정렬 (DAO에서 이미 처리했다면 불필요)
                            // options.sort(Comparator.comparingInt(SurveyQuestionOption::getOptionOrder));
                            qDto.setOptions(options);
                        }
                        surveyFullDto.getQuestions().add(qDto);
                    }
                }
                return surveyFullDto;
            }
            return null; // 참여 가능한 활성 설문 없음

        } catch (DaoException e) {
            throw new ServiceException("활성 설문조사 정보 조회 중 오류 발생: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("DB 연결 종료 중 오류 (getActiveSurveyForClient): " + e.getMessage());
                }
            }
        }
    }

    /**
     * 학생이 제출한 설문조사 응답들을 저장합니다.
     * 각 응답은 개별적으로 SurveyResponseDao를 통해 저장되며, 모든 저장은 단일 트랜잭션으로 처리됩니다.
     */
    @Override
    public void submitSurveyResponses(int userId, int surveyId, Map<Integer, String> answers) throws ServiceException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // 트랜잭션 시작

            if (answers == null || answers.isEmpty()) {
                throw new ServiceException("제출할 답변이 없습니다.");
            }

            // (선택적) 해당 사용자가 이미 이 설문에 응답했는지 확인하는 로직 추가 가능
            // if (surveyResponseDao.hasUserResponded(userId, surveyId, conn)) {
            //    throw new ServiceException("이미 이 설문조사에 응답하셨습니다.");
            // }


            for (Map.Entry<Integer, String> entry : answers.entrySet()) {
                int questionId = entry.getKey();
                String answerText = entry.getValue();

                if (answerText == null || answerText.trim().isEmpty()) {
                    // 비어있는 답변은 저장하지 않거나, 정책에 따라 다르게 처리
                    // 예를 들어, 모든 질문에 답변해야 한다면 여기서 유효성 검사 후 예외 발생
                    // 여기서는 비어있는 답변은 건너뛴다고 가정 (또는 null로 저장)
                    continue;
                }

                SurveyResponse response = new SurveyResponse();
                response.setUserId(userId);
                response.setSurveyId(surveyId);
                response.setQuestionId(questionId);
                response.setAnswerText(answerText); // 객관식의 경우 option_id 문자열, 주관식은 텍스트

                surveyResponseDao.saveResponse(response, conn); // Connection 전달
            }

            conn.commit(); // 모든 응답 저장 성공 시 커밋
        } catch (DaoException | SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    throw new ServiceException("트랜잭션 롤백 중 오류 (submitSurveyResponses): " + ex.getMessage(), ex);
                }
            }
            throw new ServiceException("설문조사 응답 제출 중 오류 발생: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    System.err.println("DB 연결 종료 중 오류 (submitSurveyResponses): " + ex.getMessage());
                }
            }
        }
    }
    @Override
    public boolean hasUserCompletedSurvey(int userId, int surveyId) throws ServiceException {
        try (Connection conn = DBConnection.getConnection()) {
            // SurveyResponseDao의 hasUserResponded 메서드를 호출합니다.
            return surveyResponseDao.hasUserResponded(userId, surveyId, conn);
        } catch (DaoException | SQLException e) {
            throw new ServiceException("사용자 설문조사 완료 여부 확인 중 오류 발생: userId=" + userId + ", surveyId=" + surveyId, e);
        }
    }
}