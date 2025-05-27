package main.java.dao;

import main.java.model.AnswerKey;

public interface AnswerKeyDao {
    AnswerKey findByQuestionId(int questionId) throws DaoException;
    void insert(AnswerKey key) throws DaoException;
    void update(AnswerKey key) throws DaoException;
    void deleteByQuestionId(int questionId) throws DaoException;
}
