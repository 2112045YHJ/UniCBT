package main.java.dao;

import main.java.model.ExamsDepartment;
import java.util.List;
import java.util.Map;

public interface ExamsDepartmentDao {
    /**
     * 주어진 examId에 매핑된 모든 학과·학년 정보를 반환
     */
    List<ExamsDepartment> findByExamId(int examId) throws DaoException;

    /**
     * 새 (examId, dpmtId, grade) 매핑을 저장
     */
    void save(ExamsDepartment ed) throws DaoException;

    /**
     * 특정 (examId, dpmtId, grade) 매핑을 삭제
     */
    void delete(int examId, int dpmtId, int grade) throws DaoException;

    /**
     * examId에 해당하는 모든 매핑 삭제 (시험 삭제 시 연동)
     */
    void deleteByExamId(int examId) throws DaoException;
    Map<Integer, List<Integer>> findDepartmentAndGradesGrouped(int examId) throws DaoException;

    }