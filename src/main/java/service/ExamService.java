package main.java.service;

import main.java.dao.DaoException;
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
}
