package main.java.dao;

import main.java.model.Exam;
import java.util.List;

/**
 * 시험 관련 데이터 접근 인터페이스
 */
public interface ExamDao {
    /** ID로 단일 시험 조회 */
    Exam findById(int examId) throws DaoException;

    /** 모든 시험 조회 */
    List<Exam> findAll() throws DaoException;

    /** 열려 있는(OPEN) 시험 목록 조회 */
    List<Exam> findOpenExams() throws DaoException;

    /**
     * 새 Exam을 저장하고, 생성된 PK를 exam.examId에 설정한다.
     * @param exam 저장할 Exam 객체(subject, startDate, endDate, durationMinutes, questionCnt)
     */
    void insert(Exam exam) throws DaoException;

    /**
     * 문제 개수(question_cnt)를 갱신한다.
     * @param examId       갱신할 시험 ID
     * @param questionCnt  해당 시험의 문제 개수
     */
    void updateQuestionCount(int examId, int questionCnt) throws DaoException;
    List<Exam> findAllByDpmtAndGrade(int dpmtId, int grade) throws DaoException;
    List<Exam> findAllByUser(int userId) throws DaoException;

    void disableExam(int examId) throws DaoException;

    List<String> findAssignedDepartmentsAndGrades(int examId) throws DaoException;
}
