// AnnouncementDaoImpl.java
package main.java.dao;

import main.java.model.Announcement;
import main.java.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AnnouncementDaoImpl implements AnnouncementDao {
    private final Connection conn = DBConnection.getConnection();

    @Override
    public List<Announcement> findAll() throws DaoException {
        String sql = "SELECT * FROM announcement ORDER BY created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Announcement> list = new ArrayList<>();
            while (rs.next()) {
                Announcement a = new Announcement();
                a.setAnnouncementId(rs.getInt("announcement_id"));
                a.setTitle(rs.getString("title"));
                a.setContent(rs.getString("content"));
                a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                a.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                a.setReadCount(rs.getInt("read_cnt"));
                list.add(a);
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("findAll 공지사항 조회 실패", e);
        }
    }

    @Override
    public Announcement findById(int id) throws DaoException {
        String sql = "SELECT * FROM announcement WHERE announcement_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Announcement a = new Announcement();
                    a.setAnnouncementId(id);
                    a.setTitle(rs.getString("title"));
                    a.setContent(rs.getString("content"));
                    a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    a.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                    a.setReadCount(rs.getInt("read_cnt"));
                    return a;
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DaoException("findById 공지사항 조회 실패: id=" + id, e);
        }
    }

    @Override
    public void save(Announcement ann) throws DaoException {
        String sql = "INSERT INTO announcement(title, content) VALUES(?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ann.getTitle());
            ps.setString(2, ann.getContent());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) ann.setAnnouncementId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new DaoException("save 공지사항 저장 실패", e);
        }
    }

    @Override
    public void update(Announcement ann) throws DaoException {
        String sql = "UPDATE announcement SET title = ?, content = ? WHERE announcement_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ann.getTitle());
            ps.setString(2, ann.getContent());
            ps.setInt(3, ann.getAnnouncementId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("update 공지사항 수정 실패: id=" + ann.getAnnouncementId(), e);
        }
    }

    @Override
    public void delete(int id) throws DaoException {
        String sql = "DELETE FROM announcement WHERE announcement_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("delete 공지사항 삭제 실패: id=" + id, e);
        }
    }
}
