package main.java.dao;

import main.java.model.User;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

public interface UserDao {

    /**
     * 주어진 사용자 ID 목록에 해당하는 사용자 정보들을 조회합니다.
     * @param userIds 조회할 사용자 ID 목록
     * @param conn 데이터베이스 연결 객체
     * @return 사용자 ID를 키로, User 객체를 값으로 하는 Map
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    Map<Integer, User> findUsersByIds(List<Integer> userIds, Connection conn) throws DaoException;
    User findById(int userId, Connection conn) throws DaoException;
    User findByStudentNumberAndPassword(String studentNumber, String password, Connection conn) throws DaoException;
    List<User> findAll(Connection conn) throws DaoException;
    /**
     * 사용자를 삽입하고, 생성된 userId를 user 객체에 설정합니다.
     */
    void insert(User user, Connection conn) throws DaoException;
    void update(User user, Connection conn) throws DaoException;
    void delete(int userId, Connection conn) throws DaoException;
    List<User> findByDpmtAndGrade(int dpmtId, int grade, Connection conn) throws DaoException;

    User findById(int userId) throws DaoException;
    User findByStudentNumberAndPassword(String studentNumber, String password) throws DaoException;
    List<User> findAll() throws DaoException;
    /**
     * 사용자를 삽입하고, 생성된 userId를 user 객체에 설정합니다.
     */
    void insert(User user) throws DaoException;
    void update(User user) throws DaoException;
    void delete(int userId) throws DaoException;
    List<User> findByDpmtAndGrade(int dpmtId, int grade) throws DaoException;
}