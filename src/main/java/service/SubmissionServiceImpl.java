package main.java.service;

import main.java.dao.*;
import main.java.model.AnswerKey; // AnswerKey 임포트 추가
import main.java.model.ExamResult;
import main.java.model.QuestionBank;
import main.java.util.DBConnection; // DBConnection 임포트

import java.sql.Connection; // Connection 임포트
import java.sql.SQLException; // SQLException 임포트
import java.util.Map;

/**
 * SubmissionService 구현체
 */
public class SubmissionServiceImpl implements SubmissionService {
    private final AnswerSheetDao answerSheetDao = new AnswerSheetDaoImpl();
    private final AnswerKeyDao answerKeyDao = new AnswerKeyDaoImpl();
    private final ExamResultDao examResultDao = new ExamResultDaoImpl();
    private final QuestionBankDao questionBankDao = new QuestionBankDaoImpl();
    private final QuestionStatsDao questionStatsDao = new QuestionStatsDaoImpl();


    /**
     * 단일 답안을 제출합니다. (자체 Connection 관리)
     */
    @Override
    public void submitAnswer(int userId, int examId, int questionId, String answer) throws ServiceException, DaoException {
        // (이전 턴에서 제공된 코드 참조)
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                answerSheetDao.insert(userId, examId, questionId, answer, conn);
                conn.commit();
            } catch (DaoException | SQLException e) {
                conn.rollback();
                if (e instanceof DaoException) throw (DaoException) e;
                throw new ServiceException("단일 답안 제출 중 오류 발생 (롤백됨)", e);
            }
        } catch (SQLException e) {
            throw new ServiceException("데이터베이스 연결 오류 (submitAnswer)", e);
        }
    }

    @Override
    public void submitAnswerBatch(int userId, int examId, Map<Integer, String> answers) throws ServiceException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // 트랜잭션 시작

            // 1. 모든 답안 저장
            for (Map.Entry<Integer, String> entry : answers.entrySet()) {
                int qId = entry.getKey();
                String ans = entry.getValue();
                answerSheetDao.insert(userId, examId, qId, ans, conn);
            }

            // 2. 정답 조회, 채점 및 QuestionStats 업데이트
            int correctCount = 0;
            for (Map.Entry<Integer, String> entry : answers.entrySet()) {
                int qId = entry.getKey();
                String submittedAnswer = entry.getValue();
                boolean isCorrect = false;

                AnswerKey correctAnswerKey = answerKeyDao.findByQuestionId(qId, conn);
                QuestionBank question = questionBankDao.findById(qId, conn); // 문제 유형(type)을 가져오기 위함

                if (question == null) {
                    // 해당 문제가 DB에 없는 경우, 오류 처리 또는 로그 남기고 계속 진행
                    System.err.println("경고: QuestionBank에서 ID " + qId + "에 해당하는 문제를 찾을 수 없습니다. 통계 업데이트에서 제외됩니다.");
                    continue;
                }
                String questionType = question.getType();


                if (correctAnswerKey != null) {
                    if (correctAnswerKey.getCorrectLabel() != null && correctAnswerKey.getCorrectLabel().toString().equals(submittedAnswer)) {
                        correctCount++;
                        isCorrect = true;
                    } else if (correctAnswerKey.getCorrectText() != null && correctAnswerKey.getCorrectText().equalsIgnoreCase(submittedAnswer)) {
                        correctCount++;
                        isCorrect = true;
                    }
                }
                // QuestionStats 업데이트 (DAO의 recordAttempt가 내부적으로 SELECT 후 UPDATE/INSERT ON DUPLICATE KEY UPDATE 수행)
                questionStatsDao.recordAttempt(qId, examId, questionType, isCorrect, conn);
            }

            // 3. 점수 계산
            int score = 0;
            if (answers.size() > 0) {
                score = (int) (((double) correctCount / answers.size()) * 100);
            }

            // 4. examresults 저장
            ExamResult result = new ExamResult(userId, examId, score);
            result.setCompletedAt(java.time.LocalDateTime.now());
            examResultDao.insert(result, conn);

            conn.commit(); // 모든 작업 성공 시 트랜잭션 커밋

        } catch (DaoException | SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    throw new ServiceException("트랜잭션 롤백 중 오류 (submitAnswerBatch): " + ex.getMessage(), ex);
                }
            }
            if (e instanceof ServiceException) throw (ServiceException) e;
            if (e instanceof DaoException) throw new ServiceException("일괄 답안 제출/채점 중 DAO 오류 (롤백됨): " + e.getMessage(), e);
            throw new ServiceException("일괄 답안 제출/채점 중 DB 오류 (롤백됨): " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    System.err.println("DB 연결 종료 중 오류 (submitAnswerBatch): " + ex.getMessage());
                }
            }
        }
    }
}