package main.java.dao;

import main.java.model.QuestionFull;
import java.util.List;

public interface QuestionDao {
    List<QuestionFull> findFullByExamId(int examId) throws DaoException;
}