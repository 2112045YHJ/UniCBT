// src/main/java/main/java/service/SubmissionServiceImpl.java
package main.java.service;

import main.java.dao.AnswerSheetDao;
import main.java.dao.AnswerSheetDaoImpl;
import main.java.dao.DaoException;
import main.java.model.AnswerSheet;

/**
 * SubmissionService 구현체
 */
public class SubmissionServiceImpl implements SubmissionService {
    private final AnswerSheetDao dao = new AnswerSheetDaoImpl();

    @Override
    public void submitAnswer(int userId, int examId, int questionId, String answer) throws ServiceException {
        try {
            AnswerSheet sheet = new AnswerSheet();
            sheet.setUserId(userId);
            sheet.setExamId(examId);
            sheet.setQuestionId(questionId);
            sheet.setSelectedAnswer(answer);
            dao.insert(sheet);
        } catch (DaoException e) {
            throw new ServiceException("답안 제출 실패", e);
        }
    }
}
