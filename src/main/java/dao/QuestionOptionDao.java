package main.java.dao;

import main.java.model.QuestionOption;
import java.sql.Connection; // Connection import 추가
import java.util.List;

public interface QuestionOptionDao {
    // Connection conn 파라미터 추가
    List<QuestionOption> findByQuestionId(int questionId, Connection conn) throws DaoException;
    void insert(QuestionOption option, Connection conn) throws DaoException;
    void deleteByQuestionId(int questionId, Connection conn) throws DaoException;
}