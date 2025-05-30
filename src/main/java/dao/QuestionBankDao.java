package main.java.dao;

import main.java.model.QuestionBank;
import java.sql.Connection; // Connection import 추가
import java.util.List;

public interface QuestionBankDao {
    QuestionBank findById(int questionId, Connection conn) throws DaoException; // conn 파라미터 추가
    List<QuestionBank> findByExamId(int examId, Connection conn) throws DaoException; // conn 파라미터 추가
    void insert(QuestionBank qb, Connection conn) throws DaoException; // conn 파라미터 추가
    void deleteByExamId(int examId, Connection conn) throws DaoException; // conn 파라미터 추가
    int countByExamId(int examId, Connection conn) throws DaoException; // conn 파라미터 추가
}