package main.java.service;

import main.java.dao.AnnouncementDao;
import main.java.dao.AnnouncementDaoImpl;
import main.java.dao.DaoException;
import main.java.model.Announcement;
import main.java.util.DBConnection; // DBConnection 임포트

import java.sql.Connection; // Connection 임포트
import java.sql.SQLException; // SQLException 임포트
import java.time.LocalDateTime; // LocalDateTime 임포트
import java.util.List;

public class AnnouncementServiceImpl implements AnnouncementService {
    private final AnnouncementDao announcementDao = new AnnouncementDaoImpl(); // final로 선언 권장

    @Override
    public List<Announcement> getAllAnnouncements(int page, int itemsPerPage) throws ServiceException {
        // DAO 계층에서 이미 Connection을 받는 findAll(conn, page, itemsPerPage)이 구현되었다고 가정
        // 여기서는 Service 레벨에서 Connection을 열고 닫음 (또는 DAO가 자체 관리하는 findAll(page, itemsPerPage) 호출)
        try (Connection conn = DBConnection.getConnection()) {
            // 만약 AnnouncementDao의 findAll(int, int) 메서드가 Connection을 받지 않고 자체 관리한다면:
            // return announcementDao.findAll(page, itemsPerPage);
            // 만약 AnnouncementDao의 findAll(Connection, int, int) 메서드를 사용한다면:
            return announcementDao.findAll(conn, page, itemsPerPage);
        } catch (DaoException | SQLException e) { // SQLException 추가
            throw new ServiceException("공지사항 목록 조회 중 오류 발생 (페이지: " + page + "): " + e.getMessage(), e);
        }
    }

    @Override
    public int getTotalAnnouncementCount() throws ServiceException {
        try (Connection conn = DBConnection.getConnection()) {
            // 만약 AnnouncementDao의 countAll() 메서드가 Connection을 받지 않고 자체 관리한다면:
            // return announcementDao.countAll();
            // 만약 AnnouncementDao의 countAll(Connection) 메서드를 사용한다면:
            return announcementDao.countAll(conn);
        } catch (DaoException | SQLException e) { // SQLException 추가
            throw new ServiceException("전체 공지사항 수 조회 중 오류 발생: " + e.getMessage(), e);
        }
    }

    /**
     * 특정 공지사항의 상세 정보를 가져오고, 조회수를 1 증가시킵니다.
     * 이 두 작업은 하나의 트랜잭션으로 처리됩니다.
     */
    @Override
    public Announcement getAnnouncementDetails(int announcementId) throws ServiceException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // 트랜잭션 시작

            // 조회수 증가 (DAO의 incrementReadCount(id, conn) 호출)
            announcementDao.incrementReadCount(announcementId, conn);

            // 공지사항 정보 조회 (DAO의 findById(id, conn) 호출)
            Announcement announcement = announcementDao.findById(announcementId, conn);

            if (announcement == null) {
                conn.rollback(); // 공지사항이 없으면 롤백 (조회수 증가도 취소)
                throw new ServiceException("ID " + announcementId + "에 해당하는 공지사항을 찾을 수 없습니다.");
            }

            conn.commit(); // 모든 작업 성공 시 커밋
            return announcement;

        } catch (DaoException | SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("트랜잭션 롤백 중 오류 (getAnnouncementDetails): " + ex.getMessage());
                }
            }
            throw new ServiceException("공지사항 상세 정보 조회 중 오류 발생 (ID: " + announcementId + "): " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    System.err.println("DB 연결 종료 중 오류 (getAnnouncementDetails): " + ex.getMessage());
                }
            }
        }
    }

    @Override
    public void createAnnouncement(Announcement announcement) throws ServiceException {
        // 유효성 검사 (예: 제목, 내용 필수)
        if (announcement == null || announcement.getTitle() == null || announcement.getTitle().trim().isEmpty() ||
                announcement.getContent() == null || announcement.getContent().trim().isEmpty()) {
            throw new ServiceException("공지사항 제목과 내용은 필수 입력 항목입니다.");
        }
        // createdAt, updatedAt은 DAO 또는 DB에서 처리되도록 설정 가능
        if (announcement.getCreatedAt() == null) {
            announcement.setCreatedAt(LocalDateTime.now());
        }
        if (announcement.getUpdatedAt() == null) {
            announcement.setUpdatedAt(LocalDateTime.now());
        }
        announcement.setReadCount(0); // 새 공지 조회수는 0

        try (Connection conn = DBConnection.getConnection()) {
            // 단일 insert 작업이므로, DAO가 자체 Connection을 관리하거나, 여기서 전달할 수 있음
            // 트랜잭션이 복잡하지 않다면 DAO의 Connection 없는 버전 호출도 가능
            // announcementDao.save(announcement);
            // 또는 Connection 전달
            announcementDao.save(announcement, conn);
        } catch (DaoException | SQLException e) {
            throw new ServiceException("공지사항 생성 중 오류 발생: " + e.getMessage(), e);
        }
    }

    @Override
    public void modifyAnnouncement(Announcement announcement) throws ServiceException {
        // 유효성 검사
        if (announcement == null || announcement.getAnnouncementId() <= 0 ||
                announcement.getTitle() == null || announcement.getTitle().trim().isEmpty() ||
                announcement.getContent() == null || announcement.getContent().trim().isEmpty()) {
            throw new ServiceException("유효하지 않은 공지사항 정보입니다. (ID, 제목, 내용 확인 필요)");
        }
        announcement.setUpdatedAt(LocalDateTime.now()); // 수정 시각 업데이트

        try (Connection conn = DBConnection.getConnection()) {
            // announcementDao.update(announcement);
            announcementDao.update(announcement, conn); // Connection 전달 버전 사용
        } catch (DaoException | SQLException e) {
            throw new ServiceException("공지사항 수정 중 오류 발생 (ID: " + announcement.getAnnouncementId() + "): " + e.getMessage(), e);
        }
    }

    @Override
    public void removeAnnouncement(int announcementId) throws ServiceException {
        if (announcementId <= 0) {
            throw new ServiceException("유효하지 않은 공지사항 ID입니다.");
        }
        try (Connection conn = DBConnection.getConnection()) {
            // announcementDao.delete(announcementId);
            announcementDao.delete(announcementId, conn); // Connection 전달 버전 사용
        } catch (DaoException | SQLException e) {
            throw new ServiceException("공지사항 삭제 중 오류 발생 (ID: " + announcementId + "): " + e.getMessage(), e);
        }
    }
}