// src/main/java/main/java/service/SubmissionServiceImpl.java
package main.java.service;

import main.java.dao.*;
import main.java.model.AnswerSheet;
import main.java.model.ExamResult;

import java.util.Map;

/**
 * SubmissionService 구현체
 */
public class SubmissionServiceImpl implements SubmissionService {
    private final AnswerSheetDao answerSheetDao = new AnswerSheetDaoImpl();

    @Override
    public void submitAnswer(int userId, int examId, int questionId, String answer) throws ServiceException, DaoException {
        answerSheetDao.insert(userId, examId, questionId, answer);
    }

    @Override
    public void submitAnswerBatch(int userId, int examId, Map<Integer, String> answers) throws ServiceException, DaoException {
        // 1. 모든 답안 저장
        for (Map.Entry<Integer, String> entry : answers.entrySet()) {
            int qId = entry.getKey();
            String ans = entry.getValue();
            answerSheetDao.insert(userId, examId, qId, ans);
        }

        // 2. 정답 조회 및 채점
        int correctCount = 0;
        AnswerKeyDao answerKeyDao = new AnswerKeyDaoImpl();
        for (Map.Entry<Integer, String> entry : answers.entrySet()) {
            int qId = entry.getKey();
            String ans = entry.getValue();

            var key = answerKeyDao.findByQuestionId(qId);
            if (key != null) {
                if (key.getCorrectLabel() != null && key.getCorrectLabel().toString().equals(ans)) {
                    correctCount++;
                } else if (key.getCorrectText() != null && key.getCorrectText().equalsIgnoreCase(ans)) {
                    correctCount++;
                }
            }
        }

        // 3. 점수 계산: (맞힌 개수 / 전체) * 100
        int score = (int) (((double) correctCount / answers.size()) * 100);

        // 4. examresults 저장
        ExamResultDao examResultDao = new ExamResultDaoImpl();
        ExamResult result = new ExamResult(userId, examId, score);
        result.setCompletedAt(java.time.LocalDateTime.now()); // 🛠️ 완료 시간 설정
        examResultDao.insert(result);
    }

}
