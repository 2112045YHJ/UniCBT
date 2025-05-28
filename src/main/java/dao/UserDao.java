package main.java.dao;

import main.java.model.User;
import java.util.List;

public interface UserDao {
    User findById(int userId) throws DaoException;
    User findByStudentNumberAndPassword(String studentNumber, String password) throws DaoException;
    List<User> findAll() throws DaoException;
    void insert(User user) throws DaoException;
    void update(User user) throws DaoException;
    void delete(int userId) throws DaoException;

    List<User> findByDpmtAndGrade(int dpmtId, int grade) throws DaoException;
}