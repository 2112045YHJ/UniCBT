package main.java.service;

import main.java.dao.DaoException;
import main.java.dao.UserDao;
import main.java.dao.UserDaoImpl;
import main.java.model.User;

/**
 * AuthenticationService 구현체
 */
public class AuthenticationServiceImpl implements AuthenticationService {
    private final UserDao userDao = new UserDaoImpl();

    @Override
    public User login(String studentNumber, String password) throws ServiceException {
        try {
            User user = userDao.findByStudentNumberAndPassword(studentNumber, password);
            if (user == null) {
                throw new ServiceException("학번 또는 비밀번호가 올바르지 않습니다.");
            }
            return user;
        } catch (DaoException e) {
            throw new ServiceException("로그인 처리 중 오류가 발생했습니다.", e);
        }
    }
}
