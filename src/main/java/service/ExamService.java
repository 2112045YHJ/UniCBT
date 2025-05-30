package main.java.service;

import main.java.context.ExamCreationContext;
import main.java.dao.DaoException;
import main.java.dto.ExamOverallStatsDto;
import main.java.dto.ExamProgressSummaryDto;
import main.java.dto.StudentExamStatusDto;
import main.java.dto.StudentScoreDetailDto;
import main.java.dto.DepartmentExamStatDto;
import main.java.model.*;

import java.util.List;
import java.util.Map;

/**
 * 시험 관련 비즈니스 로직 인터페이스
 */
public interface ExamService {
    /**
     * 열려 있는 모든 시험 목록을 반환
     */
    List<Exam> getOpenExams() throws ServiceException;

    /**
     * 지정된 학과(dpmtId) 및 학년(grade)에 응시 가능한 시험만 반환
     */
    List<Exam> getOpenExams(int dpmtId, int grade) throws ServiceException;

    /**
     * 시험 ID로 단일 시험 정보를 조회
     */
    Exam getExamById(int examId) throws ServiceException;
    void createFullExam(
            Exam exam,
            List<QuestionFull> questions,
            List<ExamsDepartment> targets
    ) throws ServiceException;

    void submitAllAnswers(int userId, int examId, Map<Integer, String> answers) throws ServiceException;

    // 추가된 메서드: 시험 결과 저장
    void saveExamResult(int userId, int examId, int score) throws ServiceException;
    boolean hasUserTakenExam(int userId, int examId) throws ServiceException;
    Map<Integer, ExamResult> getExamResultsByUser(int userId) throws ServiceException, DaoException;
    List<Exam> getAllExams(int dpmtId, int grade) throws ServiceException;
    public List<Exam> getAllExamsForUser(User user) throws ServiceException;

    // === 새 메서드 추가 ===
    /** 할당된(open) 시험 목록 (기간 내) **/
    List<Exam> getAssignedOpenExams(int userId) throws ServiceException;
    /** 학생에게 할당된 모든 시험 목록 **/
    List<Exam> getAssignedExams(int userId) throws ServiceException;
    /** 시험에 학생 배정 **/
    void assignExamToUsers(int examId, List<Integer> userIds) throws ServiceException;
    List<Exam> getAllExams() throws ServiceException;

    void deactivateAndRenameExam(int examId, String newArchivedSubject) throws ServiceException;
    void disableExam(int examId) throws ServiceException;
    List<String> getAssignedDepartmentsAndGrades(int examId) throws ServiceException;
    List<int[]> getAssignedDepartmentAndGradeIds(int examId) throws ServiceException;
    /**
     * 지정된 연도의 모든 시험에 대한 진행 상황 요약 정보를 반환합니다.
     * @param year 조회할 연도
     * @return 해당 연도의 시험 진행 상황 요약 DTO 목록
     * @throws ServiceException 서비스 처리 중 오류 발생 시
     */
    List<ExamProgressSummaryDto> getExamProgressSummariesByYear(int year) throws ServiceException;

    /**
     * 모든 시험에 대한 진행 상황 요약 정보를 반환합니다. (연도 구분 없이)
     * @return 모든 시험 진행 상황 요약 DTO 목록
     * @throws ServiceException 서비스 처리 중 오류 발생 시
     */
    List<ExamProgressSummaryDto> getAllExamProgressSummaries() throws ServiceException;


    /**
     * 특정 시험에 배정된 모든 학생들의 응시 상태 정보를 반환합니다.
     * @param examId 조회할 시험 ID
     * @return 해당 시험의 학생별 응시 상태 DTO 목록
     * @throws ServiceException 서비스 처리 중 오류 발생 시
     */
    List<StudentExamStatusDto> getStudentStatusesForExam(int examId) throws ServiceException;
    /**
     * 특정 학생의 특정 시험 응시 상태를 초기화합니다 (미응시 상태로 변경).
     * 이 작업은 해당 학생의 시험 결과와 제출 답안을 삭제합니다.
     * @param userId 초기화할 학생의 ID
     * @param examId 초기화할 시험의 ID
     * @throws ServiceException 서비스 처리 중 오류 발생 시
     */
    void revertExamCompletionStatus(int userId, int examId) throws ServiceException;

    /**
     * 특정 시험에 대한 학과별 통계 정보(응시자 수, 평균, 최고/최저점)를 반환합니다.
     * @param examId 통계를 조회할 시험 ID
     * @return 학과별 시험 통계 DTO 목록
     * @throws ServiceException 서비스 처리 중 오류 발생 시
     */
    List<DepartmentExamStatDto> getDepartmentalExamStats(int examId) throws ServiceException;

    /**
     * 모든 시험에 대한 전체 결과 통계 요약 정보(응시자 수, 평균 점수 등)를 반환합니다.
     * @return 모든 시험의 전체 결과 통계 DTO 목록
     * @throws ServiceException 서비스 처리 중 오류 발생 시
     */
    List<ExamOverallStatsDto> getAllExamOverallStats() throws ServiceException;

    /**
     * 특정 연도의 모든 시험에 대한 전체 결과 통계 요약 정보를 반환합니다.
     * @param year 조회할 연도
     * @return 해당 연도 시험의 전체 결과 통계 DTO 목록
     * @throws ServiceException 서비스 처리 중 오류 발생 시
     */
    List<ExamOverallStatsDto> getExamOverallStatsByYear(int year) throws ServiceException;

    /**
     * 특정 시험에 응시한 학생들의 상세 성적 정보(석차 포함)를 반환합니다.
     * @param examId 조회할 시험 ID
     * @param departmentIdFilter 필터링할 학과 ID (0 또는 음수일 경우 전체 학과)
     * @return 해당 시험의 학생별 상세 성적 DTO 목록
     * @throws ServiceException 서비스 처리 중 오류 발생 시
     */
    List<StudentScoreDetailDto> getStudentScoreDetailsForExam(int examId, int departmentIdFilter) throws ServiceException;

    /**
     * 특정 시험의 문제별 통계 정보를 반환합니다.
     * @param examId 조회할 시험 ID
     * @return 해당 시험의 문제별 통계(QuestionStat) 목록
     * @throws ServiceException 서비스 처리 중 오류 발생 시
     */
    List<QuestionStat> getQuestionStatsForExam(int examId) throws ServiceException;
    void saveExamWithDetails(ExamCreationContext context) throws ServiceException;
    List<QuestionFull> getQuestionsByExamId(int examId) throws ServiceException;
}
