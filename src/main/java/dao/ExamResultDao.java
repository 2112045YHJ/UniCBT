package main.java.dao;

import main.java.model.ExamResult;
import java.util.List;
import java.util.Map;

public interface ExamResultDao {
    void insert(ExamResult result) throws DaoException;
    ExamResult findByUserAndExam(int userId, int examId) throws DaoException;
    Map<Integer, ExamResult> findAllByUser(int userId) throws DaoException;
    boolean existsByUserAndExam(int userId, int examId) throws DaoException;
}
