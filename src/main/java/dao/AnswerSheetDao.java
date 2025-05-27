package main.java.dao;

import main.java.model.AnswerSheet;
import java.util.List;

public interface AnswerSheetDao {
    void insert(AnswerSheet sheet) throws DaoException;
    List<AnswerSheet> findByUserAndExam(int userId, int examId) throws DaoException;
    void deleteByUserAndExam(int userId, int examId) throws DaoException;

    void insert(int userId, int examId, int questionId, String answer) throws DaoException;
}
