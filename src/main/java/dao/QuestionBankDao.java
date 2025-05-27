package main.java.dao;

import main.java.model.QuestionBank;
import java.util.List;

public interface QuestionBankDao {
    QuestionBank findById(int questionId) throws DaoException;
    List<QuestionBank> findByExamId(int examId) throws DaoException;
    void insert(QuestionBank qb) throws DaoException;
    void deleteByExamId(int examId) throws DaoException;
    int countByExamId(int examId) throws DaoException;
}
