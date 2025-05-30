package main.java.dao;

import main.java.model.Exam;
import java.sql.Connection; // Connection import 추가
import java.util.List;

public interface ExamDao {
    Exam findById(int examId, Connection conn) throws DaoException; // conn 파라미터 추가
    List<Exam> findAll(Connection conn) throws DaoException; // conn 파라미터 추가
    List<Exam> findOpenExams(Connection conn) throws DaoException; // conn 파라미터 추가

    // insert 메서드는 exam 객체에 생성된 ID를 설정해야 함
    void insert(Exam exam, Connection conn) throws DaoException; // conn 파라미터 추가
    void update(Exam exam, Connection conn) throws DaoException; // conn 파라미터 추가
    void updateQuestionCount(int examId, int questionCnt, Connection conn) throws DaoException; // conn 파라미터 추가

    void disableExam(int examId, Connection conn) throws DaoException; // conn 파라미터 추가

    // Connection을 받는 다른 메서드 시그니처도 필요에 따라 추가 (예: findAllByDpmtAndGrade, findAllByUser 등)
    List<Exam> findAllByDpmtAndGrade(int dpmtId, int grade, Connection conn) throws DaoException;
    List<Exam> findAllByUser(int userId, Connection conn) throws DaoException;
    List<int[]> getAssignedDepartmentAndGradeIds(int examId, Connection conn) throws DaoException;
    List<String> findAssignedDepartmentsAndGrades(int examId, Connection conn) throws DaoException;
}