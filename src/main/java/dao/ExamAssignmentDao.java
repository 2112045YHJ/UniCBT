package main.java.dao;

import java.sql.Connection; // Connection import 추가
import java.util.List;

public interface ExamAssignmentDao {
    // Connection conn 파라미터 추가
    void assignStudents(int examId, List<Integer> userIds, Connection conn) throws DaoException;
    void removeAssignments(int examId, Connection conn) throws DaoException;
    List<Integer> findExamIdsByUser(int userId, Connection conn) throws DaoException;
    List<Integer> findUserIdsByExam(int examId, Connection conn) throws DaoException;
}