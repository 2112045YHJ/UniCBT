package main.java.dao;

import main.java.model.Announcement;
import java.sql.Connection; // Connection 임포트
import java.util.List;

public interface AnnouncementDao {

    // --- Connection을 파라미터로 받는 메서드들 ---

    /**
     * 지정된 페이지와 페이지당 항목 수에 해당하는 공지사항 목록을 조회합니다.
     * @param conn 데이터베이스 연결 객체
     * @param page 조회할 페이지 번호 (1부터 시작)
     * @param itemsPerPage 페이지당 표시할 항목 수
     * @return 해당 페이지의 공지사항 목록
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    List<Announcement> findAll(Connection conn, int page, int itemsPerPage) throws DaoException;

    /**
     * 전체 공지사항의 개수를 조회합니다.
     * @param conn 데이터베이스 연결 객체
     * @return 전체 공지사항 수
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    int countAll(Connection conn) throws DaoException;

    Announcement findById(int id, Connection conn) throws DaoException;
    void save(Announcement ann, Connection conn) throws DaoException;
    void update(Announcement ann, Connection conn) throws DaoException;
    void delete(int id, Connection conn) throws DaoException;
    void incrementReadCount(int id, Connection conn) throws DaoException;



    /**
     * 지정된 페이지와 페이지당 항목 수에 해당하는 공지사항 목록을 조회합니다. (자체 Connection 관리)
     * @param page 조회할 페이지 번호 (1부터 시작)
     * @param itemsPerPage 페이지당 표시할 항목 수
     * @return 해당 페이지의 공지사항 목록
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    List<Announcement> findAll(int page, int itemsPerPage) throws DaoException;

    /**
     * 전체 공지사항의 개수를 조회합니다. (자체 Connection 관리)
     * @return 전체 공지사항 수
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    int countAll() throws DaoException;


    Announcement findById(int id) throws DaoException;
    void save(Announcement ann) throws DaoException;
    void update(Announcement ann) throws DaoException;
    void delete(int id) throws DaoException;
    void incrementReadCount(int id) throws DaoException;
}