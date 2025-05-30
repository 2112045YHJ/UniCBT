package main.java.dao;

import main.java.model.AnswerKey;
import java.sql.Connection; // Connection import 추가

public interface AnswerKeyDao {
    // Connection conn 파라미터 추가
    AnswerKey findByQuestionId(int questionId, Connection conn) throws DaoException;
    void insert(AnswerKey key, Connection conn) throws DaoException;
    void update(AnswerKey key, Connection conn) throws DaoException; // update도 필요시 Connection 받도록
    void deleteByQuestionId(int questionId, Connection conn) throws DaoException;
}