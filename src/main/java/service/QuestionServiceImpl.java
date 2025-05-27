package main.java.service;

import main.java.dao.*;
import main.java.model.AnswerKey;
import main.java.model.QuestionFull;

import java.util.List;


public class QuestionServiceImpl implements QuestionService {
    private final QuestionDao questionDao = new QuestionDaoImpl();
    private final AnswerKeyDao answerKeyDao = new AnswerKeyDaoImpl(); // 추가

    @Override
    public List<QuestionFull> getQuestionsByExam(int examId) throws ServiceException {
        try {
            return questionDao.findFullByExamId(examId);
        } catch (DaoException e) {
            throw new ServiceException("문항 조회 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public String getCorrectAnswer(int questionId) throws ServiceException {
        try {
            AnswerKey answerKey = answerKeyDao.findByQuestionId(questionId);
            // 객관식은 correctLabel, OX문제는 correctText
            if (answerKey == null) return null;
            if (answerKey.getCorrectLabel() != null) {
                return answerKey.getCorrectLabel().toString(); // 예: "1", "2" ... or "A", "B"
            } else if (answerKey.getCorrectText() != null) {
                return answerKey.getCorrectText(); // 예: "O", "X"
            }
            return null;
        } catch (DaoException e) {
            throw new ServiceException("정답 조회 중 오류가 발생했습니다.", e);
        }
    }
}