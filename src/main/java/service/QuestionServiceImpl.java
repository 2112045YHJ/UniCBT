package main.java.service;

import main.java.dao.AnswerKeyDao;
import main.java.dao.AnswerKeyDaoImpl;
import main.java.dao.DaoException;
import main.java.dao.QuestionDao;
import main.java.dao.QuestionDaoImpl;
import main.java.model.AnswerKey;
import main.java.model.QuestionFull;
import main.java.util.DBConnection; // DBConnection 임포트

import java.sql.Connection; // Connection 임포트
import java.sql.SQLException; // SQLException 임포트
import java.util.List;

public class QuestionServiceImpl implements QuestionService {
    private final QuestionDao questionDao;
    private final AnswerKeyDao answerKeyDao;

    // 생성자에서 DAO 인스턴스 주입받는 것을 고려해볼 수 있습니다.
    public QuestionServiceImpl() {
        this.questionDao = new QuestionDaoImpl();
        this.answerKeyDao = new AnswerKeyDaoImpl();
    }


    @Override
    public List<QuestionFull> getQuestionsByExam(int examId) throws ServiceException {
        // QuestionDao의 findFullByExamId 메서드가 Connection을 받는다고 가정
        try (Connection conn = DBConnection.getConnection()) { // Connection 획득
            return questionDao.findFullByExamId(examId, conn); // Connection 전달
        } catch (SQLException e) {
            throw new ServiceException("데이터베이스 연결 오류가 발생했습니다.", e);
        } catch (DaoException e) {
            throw new ServiceException("문항 조회 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public String getCorrectAnswer(int questionId) throws ServiceException {
        // AnswerKeyDao의 findByQuestionId 메서드가 Connection을 받는다고 가정
        try (Connection conn = DBConnection.getConnection()) { // Connection 획득
            AnswerKey answerKey = answerKeyDao.findByQuestionId(questionId, conn); // Connection 전달

            if (answerKey == null) {
                return null;
            }
            if (answerKey.getCorrectLabel() != null) {
                return answerKey.getCorrectLabel().toString();
            } else if (answerKey.getCorrectText() != null) {
                return answerKey.getCorrectText();
            }
            return null;
        } catch (SQLException e) {
            throw new ServiceException("데이터베이스 연결 오류가 발생했습니다.", e);
        } catch (DaoException e) {
            throw new ServiceException("정답 조회 중 오류가 발생했습니다.", e);
        }
    }
}