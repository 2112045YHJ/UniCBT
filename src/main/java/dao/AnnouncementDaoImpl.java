package main.java.dao;

import main.java.model.Announcement;
import main.java.util.DBConnection; // 자체 Connection 관리를 위해 필요할 수 있음

import java.sql.*;
import java.time.LocalDateTime; // createdAt, updatedAt 필드용
import java.util.ArrayList;
import java.util.List;

public class AnnouncementDaoImpl implements AnnouncementDao {

    // --- Connection을 파라미터로 받는 메서드 구현 (실제 DB 로직 수행) ---

    @Override
    public List<Announcement> findAll(Connection conn) throws DaoException {
        String sql = "SELECT announcement_id, title, content, created_at, updated_at, read_cnt FROM announcement ORDER BY created_at DESC";
        List<Announcement> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToAnnouncement(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("모든 공지사항 조회 실패", e);
        }
    }

    @Override
    public Announcement findById(int id, Connection conn) throws DaoException {
        String sql = "SELECT announcement_id, title, content, created_at, updated_at, read_cnt FROM announcement WHERE announcement_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToAnnouncement(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DaoException("ID로 공지사항 조회 실패: id=" + id, e);
        }
    }

    @Override
    public void save(Announcement announcement, Connection conn) throws DaoException {
        // created_at, updated_at은 DB 디폴트 또는 PreparedStatement에서 설정
        String sql = "INSERT INTO announcement(title, content, created_at, updated_at, read_cnt) VALUES(?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, announcement.getTitle());
            ps.setString(2, announcement.getContent());
            ps.setTimestamp(3, Timestamp.valueOf(announcement.getCreatedAt() != null ? announcement.getCreatedAt() : LocalDateTime.now()));
            ps.setTimestamp(4, Timestamp.valueOf(announcement.getUpdatedAt() != null ? announcement.getUpdatedAt() : LocalDateTime.now()));
            ps.setInt(5, announcement.getReadCount()); // 초기 조회수는 0일 수 있음

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    announcement.setAnnouncementId(keys.getInt(1));
                } else {
                    throw new DaoException("새 공지사항 ID 생성 실패.", null);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("공지사항 저장 실패", e);
        }
    }

    @Override
    public void update(Announcement announcement, Connection conn) throws DaoException {
        // updated_at은 DB에서 ON UPDATE CURRENT_TIMESTAMP로 자동 업데이트 되거나, 여기서 명시적 설정
        String sql = "UPDATE announcement SET title = ?, content = ?, updated_at = ?, read_cnt = ? WHERE announcement_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, announcement.getTitle());
            ps.setString(2, announcement.getContent());
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now())); // 수정 시각 현재로 업데이트
            ps.setInt(4, announcement.getReadCount());
            ps.setInt(5, announcement.getAnnouncementId());
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DaoException("공지사항 업데이트 실패: ID " + announcement.getAnnouncementId() + "를 찾을 수 없습니다.", null);
            }
        } catch (SQLException e) {
            throw new DaoException("공지사항 업데이트 실패: id=" + announcement.getAnnouncementId(), e);
        }
    }

    @Override
    public void delete(int id, Connection conn) throws DaoException {
        String sql = "DELETE FROM announcement WHERE announcement_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DaoException("공지사항 삭제 실패: ID " + id + "를 찾을 수 없습니다.", null);
            }
        } catch (SQLException e) {
            throw new DaoException("공지사항 삭제 실패: id=" + id, e);
        }
    }

    // --- 기존 시그니처 메서드 구현 (내부적으로 Connection 관리 또는 Connection 받는 버전 호출) ---

    @Override
    public List<Announcement> findAll() throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return findAll(conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 실패 또는 모든 공지사항 조회 중 오류", e);
        }
    }

    @Override
    public Announcement findById(int id) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return findById(id, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 실패 또는 ID로 공지사항 조회 중 오류", e);
        }
    }

    @Override
    public void save(Announcement announcement) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            // 단일 save 작업이므로 auto-commit을 사용하거나, 필요시 여기서 트랜잭션 관리
            // 여기서는 Connection 받는 버전을 호출하여 해당 버전의 트랜잭션 정책을 따름 (현재는 auto-commit 가정)
            if (announcement.getCreatedAt() == null) announcement.setCreatedAt(LocalDateTime.now());
            if (announcement.getUpdatedAt() == null) announcement.setUpdatedAt(LocalDateTime.now());
            save(announcement, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 실패 또는 공지사항 저장 중 오류", e);
        }
    }

    @Override
    public void update(Announcement announcement) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            update(announcement, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 실패 또는 공지사항 업데이트 중 오류", e);
        }
    }

    @Override
    public void delete(int id) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            delete(id, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 실패 또는 공지사항 삭제 중 오류", e);
        }
    }

    // ResultSet 행을 Announcement 객체로 매핑하는 헬퍼 메서드
    private Announcement mapRowToAnnouncement(ResultSet rs) throws SQLException {
        Announcement a = new Announcement();
        a.setAnnouncementId(rs.getInt("announcement_id"));
        a.setTitle(rs.getString("title"));
        a.setContent(rs.getString("content"));
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        if (createdAtTs != null) a.setCreatedAt(createdAtTs.toLocalDateTime());
        Timestamp updatedAtTs = rs.getTimestamp("updated_at");
        if (updatedAtTs != null) a.setUpdatedAt(updatedAtTs.toLocalDateTime());
        a.setReadCount(rs.getInt("read_cnt"));
        return a;
    }
}