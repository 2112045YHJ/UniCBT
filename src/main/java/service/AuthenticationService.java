package main.java.service;

import main.java.model.User;

/**
 * 로그인 관련 비즈니스 로직을 처리하는 인터페이스
 */

public interface AuthenticationService {
    /**
     * 학번과 비밀번호로 사용자 인증
     * @param studentNumber 학번
     * @param password      비밀번호
     * @return 인증된 User 객체 (없으면 null)
     * @throws ServiceException 인증 중 오류 발생 시
     */
    User login(String studentNumber, String password) throws ServiceException;
}