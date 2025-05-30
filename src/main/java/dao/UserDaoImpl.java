package main.java.dao;

import main.java.model.User;
import main.java.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDaoImpl implements UserDao {

    // --- Connection을 파라미터로 받는 메서드 구현 (실제 DB 로직 수행) ---

    @Override
    public User findById(int userId, Connection conn) throws DaoException {
        String sql = "SELECT user_id, level, name, student_number, password, dpmt_id, grade, is_active FROM users WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapRowToUser(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new DaoException("ID로 사용자 조회 실패: " + userId, e);
        }
    }

    @Override
    public User findByStudentNumberAndPassword(String studentNumber, String password, Connection conn) throws DaoException {
        String sql = "SELECT user_id, level, name, student_number, password, dpmt_id, grade, is_active FROM users WHERE student_number = ? AND password = ? AND is_active = TRUE";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentNumber);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapRowToUser(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new DaoException("학번/비밀번호로 사용자 조회 실패", e);
        }
    }

    @Override
    public List<User> findAll(Connection conn) throws DaoException {
        String sql = "SELECT user_id, level, name, student_number, password, dpmt_id, grade, is_active FROM users ORDER BY user_id";
        List<User> list = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRowToUser(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("모든 사용자 조회 실패", e);
        }
    }

    @Override
    public void insert(User user, Connection conn) throws DaoException {
        String sql = "INSERT INTO users(level, name, student_number, password, dpmt_id, grade, is_active) VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, user.getLevel());
            pstmt.setString(2, user.getName());
            pstmt.setString(3, user.getStudentNumber());
            pstmt.setString(4, user.getPassword()); // Consider hashing passwords
            if (user.getDpmtId() == 0) { // dpmt_id가 0이거나 없을 경우 NULL로 처리 (관리자 계정 등)
                pstmt.setNull(5, Types.INTEGER);
            } else {
                pstmt.setInt(5, user.getDpmtId());
            }
            if (user.getGrade() == 0) { // grade가 0이거나 없을 경우 NULL로 처리 (관리자 계정 등)
                pstmt.setNull(6, Types.INTEGER);
            } else {
                pstmt.setInt(6, user.getGrade());
            }
            pstmt.setBoolean(7, user.isActive());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                user.setUserId(rs.getInt(1)); // 생성된 ID를 User 객체에 설정
            } else {
                throw new DaoException("사용자 삽입 후 ID 가져오기 실패.", null);
            }
        } catch (SQLException e) {
            throw new DaoException("사용자 삽입 실패", e);
        }
    }

    @Override
    public void update(User user, Connection conn) throws DaoException {
        String sql = "UPDATE users SET level=?, name=?, student_number=?, password=?, dpmt_id=?, grade=?, is_active=? WHERE user_id=?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, user.getLevel());
            pstmt.setString(2, user.getName());
            pstmt.setString(3, user.getStudentNumber());
            pstmt.setString(4, user.getPassword()); // Consider hashing passwords
            if (user.getDpmtId() == 0) {
                pstmt.setNull(5, Types.INTEGER);
            } else {
                pstmt.setInt(5, user.getDpmtId());
            }
            if (user.getGrade() == 0) {
                pstmt.setNull(6, Types.INTEGER);
            } else {
                pstmt.setInt(6, user.getGrade());
            }
            pstmt.setBoolean(7, user.isActive());
            pstmt.setInt(8, user.getUserId());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DaoException("사용자 업데이트 실패: userId=" + user.getUserId() + "를 찾을 수 없습니다.", null);
            }
        } catch (SQLException e) {
            throw new DaoException("사용자 업데이트 실패", e);
        }
    }

    @Override
    public void delete(int userId, Connection conn) throws DaoException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DaoException("사용자 삭제 실패: userId=" + userId + "를 찾을 수 없습니다.", null);
            }
        } catch (SQLException e) {
            throw new DaoException("사용자 삭제 실패", e);
        }
    }

    @Override
    public List<User> findByDpmtAndGrade(int dpmtId, int grade, Connection conn) throws DaoException {
        String sql = "SELECT user_id, level, name, student_number, password, dpmt_id, grade, is_active FROM users WHERE dpmt_id = ? AND grade = ? AND is_active = TRUE";
        List<User> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dpmtId);
            ps.setInt(2, grade);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRowToUser(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("학과/학년별 학생 조회 실패 (Connection 파라미터 사용): dpmtId=" + dpmtId + ", grade=" + grade, e);
        }
    }

    // --- 기존 시그니처 메서드 구현 (내부적으로 Connection 관리 또는 Connection 받는 버전 호출) ---

    @Override
    public User findById(int userId) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return findById(userId, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 실패 또는 ID로 사용자 조회 중 오류", e);
        }
    }

    @Override
    public User findByStudentNumberAndPassword(String studentNumber, String password) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return findByStudentNumberAndPassword(studentNumber, password, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 실패 또는 학번/비밀번호로 사용자 조회 중 오류", e);
        }
    }

    @Override
    public List<User> findAll() throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return findAll(conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 실패 또는 모든 사용자 조회 중 오류", e);
        }
    }

    @Override
    public void insert(User user) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            insert(user, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 실패 또는 사용자 삽입 중 오류", e);
        }
    }

    @Override
    public Map<Integer, User> findUsersByIds(List<Integer> userIds, Connection conn) throws DaoException {
        Map<Integer, User> userMap = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return userMap;
        }

        // IN 절 구성을 위한 StringBuilder
        StringBuilder sqlBuilder = new StringBuilder("SELECT user_id, level, name, student_number, password, dpmt_id, grade, is_active FROM users WHERE user_id IN (");
        for (int i = 0; i < userIds.size(); i++) {
            sqlBuilder.append("?");
            if (i < userIds.size() - 1) {
                sqlBuilder.append(",");
            }
        }
        sqlBuilder.append(")");

        try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            for (int i = 0; i < userIds.size(); i++) {
                pstmt.setInt(i + 1, userIds.get(i));
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                User user = mapRowToUser(rs); // 기존 mapRowToUser 헬퍼 메서드 사용
                userMap.put(user.getUserId(), user);
            }
        } catch (SQLException e) {
            throw new DaoException("여러 ID로 사용자 조회 실패", e);
        }
        return userMap;
    }

    @Override
    public void update(User user) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            // insert와 마찬가지로 트랜잭션 관리 고려 필요
            update(user, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 실패 또는 사용자 업데이트 중 오류", e);
        }
    }

    @Override
    public void delete(int userId) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            // insert와 마찬가지로 트랜잭션 관리 고려 필요
            delete(userId, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 실패 또는 사용자 삭제 중 오류", e);
        }
    }

    @Override
    public List<User> findByDpmtAndGrade(int dpmtId, int grade) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return findByDpmtAndGrade(dpmtId, grade, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 실패 또는 학과/학년별 학생 조회 중 오류", e);
        }
    }

    // ResultSet 행을 User 객체로 매핑하는 헬퍼 메서드
    private User mapRowToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setLevel(rs.getInt("level"));
        user.setName(rs.getString("name"));
        user.setStudentNumber(rs.getString("student_number"));
        user.setPassword(rs.getString("password")); // 주의: password를 User 객체에 그대로 담는 것은 보안상 좋지 않을 수 있음
        user.setDpmtId(rs.getInt("dpmt_id"));
        user.setGrade(rs.getInt("grade"));
        user.setActive(rs.getBoolean("is_active"));
        return user;
    }
}