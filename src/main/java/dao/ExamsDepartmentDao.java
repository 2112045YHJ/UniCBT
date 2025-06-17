package main.java.dao;

import main.java.model.ExamsDepartment;
import java.sql.Connection; // Connection import 추가
import java.util.List;
import java.util.Map;

public interface ExamsDepartmentDao {
    List<ExamsDepartment> findByExamId(int examId, Connection conn) throws DaoException;
    void save(ExamsDepartment ed, Connection conn) throws DaoException;
    void delete(int examId, int dpmtId, int grade, Connection conn) throws DaoException; // 필요시 Connection 추가
    void deleteByExamId(int examId, Connection conn) throws DaoException;
    Map<Integer, List<Integer>> findDepartmentAndGradesGrouped(int examId, Connection conn) throws DaoException; // 필요시 Connection 추가
}