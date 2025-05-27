package main.java.dao;

import main.java.model.User;
import main.java.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDaoImpl implements UserDao {
    private final Connection conn = DBConnection.getConnection();
    @Override
    public User findById(int userId) throws DaoException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                User u = new User();
                u.setUserId(rs.getInt("user_id"));
                u.setLevel(rs.getInt("level"));
                u.setName(rs.getString("name"));
                u.setStudentNumber(rs.getString("student_number"));
                u.setPassword(rs.getString("password"));
                u.setDpmtId(rs.getInt("dpmt_id"));
                u.setGrade(rs.getInt("grade"));
                u.setActive(rs.getBoolean("is_active"));
                return u;
            }
            return null;
        } catch (SQLException e) {
            throw new DaoException("Error finding User by id", e);
        }
    }
    @Override
    public User findByStudentNumberAndPassword(String studentNumber, String password) throws DaoException {
        String sql = "SELECT * FROM users WHERE student_number = ? AND password = ? AND is_active = TRUE";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentNumber);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return findById(rs.getInt("user_id"));
            return null;
        } catch (SQLException e) {
            throw new DaoException("Error finding User by credentials", e);
        }
    }
    @Override
    public List<User> findAll() throws DaoException {
        String sql = "SELECT * FROM users";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            List<User> list = new ArrayList<>();
            while (rs.next()) list.add(findById(rs.getInt("user_id")));
            return list;
        } catch (SQLException e) {
            throw new DaoException("Error retrieving all Users", e);
        }
    }
    @Override
    public void insert(User user) throws DaoException {
        String sql = "INSERT INTO users(level, name, student_number, password, dpmt_id, grade, is_active) VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, user.getLevel());
            pstmt.setString(2, user.getName());
            pstmt.setString(3, user.getStudentNumber());
            pstmt.setString(4, user.getPassword());
            pstmt.setInt(5, user.getDpmtId());
            pstmt.setInt(6, user.getGrade());
            pstmt.setBoolean(7, user.isActive());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys(); if (rs.next()) user.setUserId(rs.getInt(1));
        } catch (SQLException e) {
            throw new DaoException("Error inserting User", e);
        }
    }
    @Override
    public void update(User user) throws DaoException {
        String sql = "UPDATE users SET level=?, name=?, student_number=?, password=?, dpmt_id=?, grade=?, is_active=? WHERE user_id=?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, user.getLevel());
            pstmt.setString(2, user.getName());
            pstmt.setString(3, user.getStudentNumber());
            pstmt.setString(4, user.getPassword());
            pstmt.setInt(5, user.getDpmtId());
            pstmt.setInt(6, user.getGrade());
            pstmt.setBoolean(7, user.isActive());
            pstmt.setInt(8, user.getUserId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Error updating User", e);
        }
    }
    @Override
    public void delete(int userId) throws DaoException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Error deleting User", e);
        }
    }
}
