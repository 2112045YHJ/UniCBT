// src/main/java/main/java/service/SubmissionService.java
package main.java.service;

import main.java.service.ServiceException;

/**
 * 사용자의 시험 응시(답안 제출) 관련 비즈니스 로직 인터페이스
 */
public interface SubmissionService {
    void submitAnswer(int userId, int examId, int questionId, String answer) throws ServiceException;
}
