// src/main/java/main/java/service/SubmissionServiceImpl.java
package main.java.service;

import main.java.dao.AnswerSheetDao;
import main.java.dao.AnswerSheetDaoImpl;
import main.java.dao.DaoException;
import main.java.model.AnswerSheet;

import java.util.Map;

/**
 * SubmissionService 구현체
 */
public class SubmissionServiceImpl implements SubmissionService {
    private final AnswerSheetDao answerSheetDao = new AnswerSheetDaoImpl();

    @Override
    public void submitAnswer(int userId, int examId, int questionId, String answer) throws ServiceException {
        answerSheetDao.insert(userId, examId, questionId, answer);
    }

    @Override
    public void submitAnswerBatch(int userId, int examId, Map<Integer, String> answers) throws ServiceException {
        for (Map.Entry<Integer, String> entry : answers.entrySet()) {
            int qId = entry.getKey();
            String ans = entry.getValue();
            answerSheetDao.insert(userId, examId, qId, ans);
        }
    }
}
