package main.java.service;

import main.java.dao.QuestionDao;
import main.java.dao.QuestionDaoImpl;
import main.java.dao.DaoException;
import main.java.model.QuestionFull;

import java.util.List;

public interface QuestionService {
    List<QuestionFull> getQuestionsByExam(int examId) throws ServiceException;
    String getCorrectAnswer(int questionId) throws ServiceException; // 추가
}