package main.java.service;

import main.java.dao.DaoException; // DaoException 유지 또는 ServiceException으로 래핑
import main.java.model.Announcement;
import java.util.List;

public interface AnnouncementService {

    /**
     * 지정된 페이지와 페이지당 항목 수에 해당하는 공지사항 목록을 가져옵니다.
     * @param page 조회할 페이지 번호 (1부터 시작)
     * @param itemsPerPage 페이지당 표시할 항목 수
     * @return 해당 페이지의 공지사항 목록
     * @throws ServiceException 서비스 처리 중 오류 발생 시
     */
    List<Announcement> getAllAnnouncements(int page, int itemsPerPage) throws ServiceException;

    /**
     * 전체 공지사항의 개수를 반환합니다.
     * @return 전체 공지사항 수
     * @throws ServiceException 서비스 처리 중 오류 발생 시
     */
    int getTotalAnnouncementCount() throws ServiceException;

    /**
     * 특정 ID의 공지사항 정보를 가져옵니다.
     * 이 메서드 호출 시 해당 공지사항의 조회수가 증가될 수 있습니다.
     * @param announcementId 조회할 공지사항 ID
     * @return Announcement 객체
     * @throws ServiceException 서비스 처리 중 오류 발생 시
     */
    Announcement getAnnouncementDetails(int announcementId) throws ServiceException; // 이름 변경 및 조회수 증가 기능 암시

    /**
     * 새 공지사항을 생성합니다.
     * @param announcement 저장할 Announcement 객체
     * @throws ServiceException 서비스 처리 중 오류 발생 시
     */
    void createAnnouncement(Announcement announcement) throws ServiceException;

    /**
     * 기존 공지사항을 수정합니다.
     * @param announcement 수정할 Announcement 객체
     * @throws ServiceException 서비스 처리 중 오류 발생 시
     */
    void modifyAnnouncement(Announcement announcement) throws ServiceException;

    /**
     * 특정 ID의 공지사항을 삭제합니다.
     * @param announcementId 삭제할 공지사항 ID
     * @throws ServiceException 서비스 처리 중 오류 발생 시
     */
    void removeAnnouncement(int announcementId) throws ServiceException;
}