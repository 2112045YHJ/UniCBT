package main.java.dao;

import main.java.model.Announcement;
import java.sql.Connection; // 데이터베이스 연결을 위한 Connection 임포트
import java.util.List;

public interface AnnouncementDao {

    // --- Connection을 파라미터로 받는 메서드들 (트랜잭션 제어용) ---

    /**
     * 모든 공지사항을 조회합니다.
     * @param conn 데이터베이스 연결 객체
     * @return 공지사항 목록
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    List<Announcement> findAll(Connection conn) throws DaoException;

    /**
     * ID로 특정 공지사항을 조회합니다.
     * @param id 조회할 공지사항 ID
     * @param conn 데이터베이스 연결 객체
     * @return 조회된 공지사항 객체 (없으면 null)
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    Announcement findById(int id, Connection conn) throws DaoException;

    /**
     * 새 공지사항을 저장하고, 생성된 ID를 announcement 객체에 설정합니다.
     * @param announcement 저장할 공지사항 객체
     * @param conn 데이터베이스 연결 객체
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    void save(Announcement announcement, Connection conn) throws DaoException;

    /**
     * 기존 공지사항 정보를 수정합니다.
     * @param announcement 수정할 공지사항 객체
     * @param conn 데이터베이스 연결 객체
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    void update(Announcement announcement, Connection conn) throws DaoException;

    /**
     * ID로 특정 공지사항을 삭제합니다.
     * @param id 삭제할 공지사항 ID
     * @param conn 데이터베이스 연결 객체
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    void delete(int id, Connection conn) throws DaoException;


    // --- 기존 시그니처 메서드들 (자체 Connection 관리 또는 Connection 받는 버전 호출) ---

    List<Announcement> findAll() throws DaoException;
    Announcement findById(int id) throws DaoException;
    void save(Announcement announcement) throws DaoException;
    void update(Announcement announcement) throws DaoException;
    void delete(int id) throws DaoException;
}