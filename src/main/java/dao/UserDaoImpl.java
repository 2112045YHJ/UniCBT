package main.java.dao;

import main.java.model.User;
import main.java.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap; // HashMap 임포트

public class UserDaoImpl implements UserDao {

    /**
     * ResultSet의 현재 행에서 User 객체를 생성하여 반환합니다.
     */
    private User mapRowToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setLevel(rs.getInt("level"));
        user.setName(rs.getString("name"));
        user.setStudentNumber(rs.getString("student_number"));
        user.setPassword(rs.getString("password")); // 주의: 실제 애플리케이션에서는 password를 User 객체에 담아 전달하지 않는 것이 좋음

        Date birthDateDb = rs.getDate("birth_date");
        if (birthDateDb != null) {
            user.setBirthDate(birthDateDb.toLocalDate());
        }
        user.setDpmtId(rs.getInt("dpmt_id")); // NULL 가능성이 있으므로 rs.getObject 고려 가능
        user.setGrade(rs.getInt("grade"));   // NULL 가능성이 있으므로 rs.getObject 고려 가능
        user.setStatus(rs.getString("status"));

        Timestamp createdAtTs = rs.getTimestamp("created_at");
        if (createdAtTs != null) {
            user.setCreatedAt(createdAtTs.toLocalDateTime());
        }
        Timestamp updatedAtTs = rs.getTimestamp("updated_at");
        if (updatedAtTs != null) {
            user.setUpdatedAt(updatedAtTs.toLocalDateTime());
        }
        return user;
    }

    @Override
    public User findById(int userId, Connection conn) throws DaoException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("ID로 사용자 조회 실패: " + userId, e);
        }
        return null;
    }

    @Override
    public User findByStudentNumber(String studentNumber, Connection conn) throws DaoException {
        String sql = "SELECT * FROM users WHERE student_number = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("학번으로 사용자 조회 실패: " + studentNumber, e);
        }
        return null;
    }

    @Override
    public User findByStudentNumberAndPassword(String studentNumber, String password, Connection conn) throws DaoException {
        // 실제 운영에서는 password를 DB에 해싱하여 저장하고, 입력된 password도 해싱하여 비교해야 함
        String sql = "SELECT * FROM users WHERE student_number = ? AND password = ? AND (status = '재학' OR status = '휴학' OR level = 0)"; // 관리자 또는 활성 학생만 로그인
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentNumber);
            pstmt.setString(2, password); // 실제로는 해싱된 비밀번호와 비교
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("학번/비밀번호로 사용자 조회 실패", e);
        }
        return null;
    }

    @Override
    public List<User> findUsersByCriteria(Map<String, Object> conditions, Connection conn) throws DaoException {
        List<User> users = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM users WHERE 1=1");
        List<Object> params = new ArrayList<>();

        // 조건에 따라 WHERE 절 동적 구성
        if (conditions.containsKey("level")) {
            sqlBuilder.append(" AND level = ?");
            params.add(conditions.get("level"));
        }
        if (conditions.containsKey("dpmtId") && (Integer)conditions.get("dpmtId") > 0) { // dpmtId가 0이면 전체 학과로 간주
            sqlBuilder.append(" AND dpmt_id = ?");
            params.add(conditions.get("dpmtId"));
        }
        if (conditions.containsKey("grade") && (Integer)conditions.get("grade") > 0) { // grade가 0이면 전체 학년으로 간주
            sqlBuilder.append(" AND grade = ?");
            params.add(conditions.get("grade"));
        }
        if (conditions.containsKey("status")) {
            sqlBuilder.append(" AND status = ?");
            params.add(conditions.get("status"));
        }
        if (conditions.containsKey("statuses")) { // 여러 상태 조회 (예: "재학", "휴학")
            List<String> statusList = (List<String>) conditions.get("statuses");
            if (!statusList.isEmpty()) {
                sqlBuilder.append(" AND status IN (");
                for (int i = 0; i < statusList.size(); i++) {
                    sqlBuilder.append("?");
                    if (i < statusList.size() - 1) sqlBuilder.append(",");
                    params.add(statusList.get(i));
                }
                sqlBuilder.append(")");
            }
        }
        if (conditions.containsKey("nameSearch")) { // "name" -> "nameSearch"로 변경
            sqlBuilder.append(" AND name LIKE ?");
            params.add("%" + conditions.get("nameSearch") + "%");
        }
        if (conditions.containsKey("studentNumberSearch")) { // "studentNumber" -> "studentNumberSearch"로 변경
            sqlBuilder.append(" AND student_number LIKE ?");
            params.add("%" + conditions.get("studentNumberSearch") + "%");
        }

        // 정렬 조건 (예시: 학번 오름차순 기본)
        sqlBuilder.append(" ORDER BY ");
        if (conditions.containsKey("orderBy") && conditions.containsKey("orderDirection")) {
            sqlBuilder.append(conditions.get("orderBy")).append(" ").append(conditions.get("orderDirection"));
        } else {
            sqlBuilder.append("student_number ASC");
        }
        // 페이지네이션 조건 (필요시)
        // if (conditions.containsKey("limit") && conditions.containsKey("offset")) { ... }


        try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("조건별 사용자 조회 실패", e);
        }
        return users;
    }

    @Override
    public List<User> findAll(Connection conn) throws DaoException {
        return findUsersByCriteria(new HashMap<>(), conn); // 조건 없이 전체 조회
    }

    @Override
    public Map<Integer, User> findUsersByIds(List<Integer> userIds, Connection conn) throws DaoException {
        Map<Integer, User> userMap = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return userMap;
        }
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM users WHERE user_id IN (");
        for (int i = 0; i < userIds.size(); i++) {
            sqlBuilder.append("?");
            if (i < userIds.size() - 1) sqlBuilder.append(",");
        }
        sqlBuilder.append(")");

        try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            for (int i = 0; i < userIds.size(); i++) {
                pstmt.setInt(i + 1, userIds.get(i));
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                User user = mapRowToUser(rs);
                userMap.put(user.getUserId(), user);
            }
        } catch (SQLException e) {
            throw new DaoException("여러 ID로 사용자 조회 실패", e);
        }
        return userMap;
    }


    @Override
    public void insert(User user, Connection conn) throws DaoException {
        String sql = "INSERT INTO users(level, name, student_number, password, birth_date, dpmt_id, grade, status, created_at, updated_at) VALUES(?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, user.getLevel());
            pstmt.setString(2, user.getName());
            pstmt.setString(3, user.getStudentNumber());
            pstmt.setString(4, user.getPassword()); // 실제로는 해싱된 비밀번호 저장
            pstmt.setDate(5, user.getBirthDate() != null ? Date.valueOf(user.getBirthDate()) : null);

            if (user.getDpmtId() == 0) pstmt.setNull(6, Types.INTEGER); else pstmt.setInt(6, user.getDpmtId());
            if (user.getGrade() == 0) pstmt.setNull(7, Types.INTEGER); else pstmt.setInt(7, user.getGrade());

            pstmt.setString(8, user.getStatus() != null ? user.getStatus() : "재학"); // 기본값 설정
            // created_at, updated_at은 DB 기본값 또는 CURRENT_TIMESTAMP 사용
            pstmt.setTimestamp(9, user.getCreatedAt() != null ? Timestamp.valueOf(user.getCreatedAt()) : Timestamp.valueOf(java.time.LocalDateTime.now()));
            pstmt.setTimestamp(10, user.getUpdatedAt() != null ? Timestamp.valueOf(user.getUpdatedAt()) : Timestamp.valueOf(java.time.LocalDateTime.now()));

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setUserId(rs.getInt(1));
                } else {
                    throw new DaoException("사용자 삽입 후 ID 가져오기 실패.", null);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("사용자 삽입 실패", e);
        }
    }

    @Override
    public void update(User user, Connection conn) throws DaoException {
        // updated_at은 DB에서 ON UPDATE CURRENT_TIMESTAMP로 자동 업데이트 되므로 SQL에서 제외 가능
        String sql = "UPDATE users SET level=?, name=?, student_number=?, password=?, birth_date=?, dpmt_id=?, grade=?, status=? WHERE user_id=?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, user.getLevel());
            pstmt.setString(2, user.getName());
            pstmt.setString(3, user.getStudentNumber());
            pstmt.setString(4, user.getPassword());
            pstmt.setDate(5, user.getBirthDate() != null ? Date.valueOf(user.getBirthDate()) : null);
            if (user.getDpmtId() == 0) pstmt.setNull(6, Types.INTEGER); else pstmt.setInt(6, user.getDpmtId());
            if (user.getGrade() == 0) pstmt.setNull(7, Types.INTEGER); else pstmt.setInt(7, user.getGrade());
            pstmt.setString(8, user.getStatus());
            pstmt.setInt(9, user.getUserId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DaoException("사용자 업데이트 실패: userId=" + user.getUserId() + "를 찾을 수 없습니다.", null);
            }
        } catch (SQLException e) {
            throw new DaoException("사용자 업데이트 실패", e);
        }
    }

    @Override
    public void updatePassword(int userId, String newPassword, Connection conn) throws DaoException {
        String sql = "UPDATE users SET password = ? WHERE user_id = ?"; // updated_at은 자동 갱신
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newPassword); // 실제로는 해싱된 새 비밀번호
            pstmt.setInt(2, userId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DaoException("비밀번호 업데이트 실패: userId=" + userId + "를 찾을 수 없습니다.", null);
            }
        } catch (SQLException e) {
            throw new DaoException("비밀번호 업데이트 실패", e);
        }
    }


    @Override
    public void delete(int userId, Connection conn) throws DaoException {
        // 물리적 삭제 대신 status를 '탈퇴' 등으로 변경하는 것을 권장.
        // 만약 정말로 물리적 삭제가 필요하다면 이 메서드 사용.
        // 관련된 다른 테이블의 데이터(예: 시험 결과, 답안지) 처리 정책도 함께 고려해야 함 (외래키 ON DELETE 옵션 등).
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                // 이미 없거나 삭제된 경우이므로, 예외 대신 로깅 또는 int 반환값으로 처리 가능
                // throw new DaoException("사용자 물리적 삭제 실패: userId=" + userId + "를 찾을 수 없습니다.", null);
                System.out.println("User with ID " + userId + " not found or already deleted for physical deletion.");
            }
        } catch (SQLException e) {
            throw new DaoException("사용자 물리적 삭제 실패", e);
        }
    }

    // --- Connection 안 받는 기존 시그니처 메서드들 구현 ---
    // (내부적으로 Connection을 열고 Connection 받는 버전을 호출)

    @Override
    public User findById(int userId) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return findById(userId, conn);
        } catch (SQLException e) { throw new DaoException("DB 연결 오류", e); }
    }

    @Override
    public User findByStudentNumber(String studentNumber) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return findByStudentNumber(studentNumber, conn);
        } catch (SQLException e) { throw new DaoException("DB 연결 오류", e); }
    }

    @Override
    public User findByStudentNumberAndPassword(String studentNumber, String password) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return findByStudentNumberAndPassword(studentNumber, password, conn);
        } catch (SQLException e) { throw new DaoException("DB 연결 오류", e); }
    }

    @Override
    public List<User> findUsersByCriteria(Map<String, Object> conditions) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return findUsersByCriteria(conditions, conn);
        } catch (SQLException e) { throw new DaoException("DB 연결 오류", e); }
    }

    @Override
    public List<User> findAll() throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return findAll(conn);
        } catch (SQLException e) { throw new DaoException("DB 연결 오류", e); }
    }

    @Override
    public Map<Integer, User> findUsersByIds(List<Integer> userIds) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return findUsersByIds(userIds, conn);
        } catch (SQLException e) { throw new DaoException("DB 연결 오류", e); }
    }

    @Override
    public void insert(User user) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            // 단일 insert 시 트랜잭션이 필요하다면 여기서 conn.setAutoCommit(false) 등 처리
            insert(user, conn);
        } catch (SQLException e) { throw new DaoException("DB 연결 오류", e); }
    }

    @Override
    public void update(User user) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            // 단일 update 시 트랜잭션이 필요하다면 여기서 conn.setAutoCommit(false) 등 처리
            update(user, conn);
        } catch (SQLException e) { throw new DaoException("DB 연결 오류", e); }
    }

    @Override
    public void updatePassword(int userId, String newPassword) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            updatePassword(userId, newPassword, conn);
        } catch (SQLException e) { throw new DaoException("DB 연결 오류", e); }
    }

    @Override
    public void delete(int userId) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            // 단일 delete 시 트랜잭션이 필요하다면 여기서 conn.setAutoCommit(false) 등 처리
            delete(userId, conn);
        } catch (SQLException e) { throw new DaoException("DB 연결 오류", e); }
    }

    @Override
    public List<User> findByDpmtAndGrade(int dpmtId, int grade, Connection conn) throws DaoException {
        // is_active 대신 status 컬럼 사용 (예: 재학생과 휴학생 대상)
        String sql = "SELECT * FROM users WHERE dpmt_id = ? AND grade = ? AND (status = '재학' OR status = '휴학')";
        List<User> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dpmtId);
            ps.setInt(2, grade);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRowToUser(rs)); // mapRowToUser는 이미 status를 처리함
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("학과/학년별 학생 조회 실패 (Connection 파라미터 사용): dpmtId=" + dpmtId + ", grade=" + grade, e);
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
}