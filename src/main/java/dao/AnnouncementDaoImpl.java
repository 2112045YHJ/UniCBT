package main.java.dao;

import main.java.model.Announcement;
import main.java.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AnnouncementDaoImpl implements AnnouncementDao {

    /**
     * ResultSet의 현재 행에서 Announcement 객체를 생성하여 반환합니다.
     */
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

    @Override
    public List<Announcement> findAll(Connection conn, int page, int itemsPerPage) throws DaoException {
        List<Announcement> list = new ArrayList<>();
        // 페이지네이션을 위한 SQL (MariaDB/MySQL 기준 LIMIT OFFSET 사용)
        // 최신글이 위로 오도록 정렬
        String sql = "SELECT announcement_id, title, content, created_at, updated_at, read_cnt " +
                "FROM announcements ORDER BY created_at DESC, announcement_id DESC LIMIT ? OFFSET ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemsPerPage); // LIMIT (가져올 행 수)
            ps.setInt(2, (page - 1) * itemsPerPage); // OFFSET (건너뛸 행 수, 페이지는 1부터 시작)

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToAnnouncement(rs));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("페이지별 공지사항 조회 실패 (page=" + page + ")", e);
        }
        return list;
    }

    @Override
    public List<Announcement> findAll(int page, int itemsPerPage) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return findAll(conn, page, itemsPerPage);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 오류 또는 페이지별 공지사항 조회 실패", e);
        }
    }

    @Override
    public int countAll(Connection conn) throws DaoException {
        String sql = "SELECT COUNT(*) FROM announcements";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DaoException("전체 공지사항 수 조회 실패", e);
        }
        return 0;
    }

    @Override
    public int countAll() throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return countAll(conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 오류 또는 전체 공지사항 수 조회 실패", e);
        }
    }

    @Override
    public Announcement findById(int id, Connection conn) throws DaoException {
        String sql = "SELECT announcement_id, title, content, created_at, updated_at, read_cnt FROM announcements WHERE announcement_id = ?";
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
    public Announcement findById(int id) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return findById(id, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 오류 또는 ID로 공지사항 조회 실패", e);
        }
    }


    @Override
    public void save(Announcement ann, Connection conn) throws DaoException {
        String sql = "INSERT INTO announcements(title, content, created_at, updated_at, read_cnt) VALUES(?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ann.getTitle());
            ps.setString(2, ann.getContent());
            // 생성 시에는 createdAt과 updatedAt을 동일하게 현재 시간으로 설정하거나, DB 기본값 활용
            LocalDateTime now = LocalDateTime.now();
            ps.setTimestamp(3, ann.getCreatedAt() != null ? Timestamp.valueOf(ann.getCreatedAt()) : Timestamp.valueOf(now));
            ps.setTimestamp(4, ann.getUpdatedAt() != null ? Timestamp.valueOf(ann.getUpdatedAt()) : Timestamp.valueOf(now));
            ps.setInt(5, ann.getReadCount()); // 보통 새로 생성 시 0

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    ann.setAnnouncementId(keys.getInt(1));
                } else {
                    throw new DaoException("공지사항 저장 후 ID 가져오기 실패.", null);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("공지사항 저장 실패", e);
        }
    }

    @Override
    public void save(Announcement ann) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            // DB 기본값(CURRENT_TIMESTAMP)을 활용하려면 모델에서 null로 전달
            if (ann.getCreatedAt() == null) ann.setCreatedAt(LocalDateTime.now());
            if (ann.getUpdatedAt() == null) ann.setUpdatedAt(LocalDateTime.now());
            save(ann, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 오류 또는 공지사항 저장 실패", e);
        }
    }


    @Override
    public void update(Announcement ann, Connection conn) throws DaoException {
        // updatedAt은 DB에서 ON UPDATE CURRENT_TIMESTAMP로 자동 업데이트되므로 SQL에서 제외 가능
        // 또는 명시적으로 현재 시간으로 설정
        String sql = "UPDATE announcements SET title = ?, content = ?, updated_at = NOW(), read_cnt = ? WHERE announcement_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ann.getTitle());
            ps.setString(2, ann.getContent());
            ps.setInt(3, ann.getReadCount());
            ps.setInt(4, ann.getAnnouncementId());
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DaoException("공지사항 업데이트 실패: ID " + ann.getAnnouncementId() + "를 찾을 수 없습니다.", null);
            }
        } catch (SQLException e) {
            throw new DaoException("공지사항 업데이트 실패: id=" + ann.getAnnouncementId(), e);
        }
    }

    @Override
    public void update(Announcement ann) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            update(ann, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 오류 또는 공지사항 업데이트 실패", e);
        }
    }

    @Override
    public void delete(int id, Connection conn) throws DaoException {
        String sql = "DELETE FROM announcements WHERE announcement_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("공지사항 삭제 실패: id=" + id, e);
        }
    }

    @Override
    public void delete(int id) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            delete(id, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 오류 또는 공지사항 삭제 실패", e);
        }
    }

    @Override
    public void incrementReadCount(int id, Connection conn) throws DaoException {
        String sql = "UPDATE announcements SET read_cnt = read_cnt + 1 WHERE announcement_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("공지사항 조회수 증가 실패: id=" + id, e);
        }
    }

    @Override
    public void incrementReadCount(int id) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            // 이 작업은 단일 업데이트이므로, 서비스에서 트랜잭션으로 묶을 필요가 없다면 자체 Connection 사용 가능
            incrementReadCount(id, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 오류 또는 공지사항 조회수 증가 실패", e);
        }
    }
}