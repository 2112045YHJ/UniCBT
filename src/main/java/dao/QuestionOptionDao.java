package main.java.dao;

import main.java.model.QuestionOption;
import java.util.List;

public interface QuestionOptionDao {
    List<QuestionOption> findByQuestionId(int questionId) throws DaoException;
    void insert(QuestionOption option) throws DaoException;
    void deleteByQuestionId(int questionId) throws DaoException;
}

