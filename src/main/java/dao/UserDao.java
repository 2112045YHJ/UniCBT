package main.java.dao;

import main.java.model.User;
import java.sql.Connection;
import java.time.LocalDate; // LocalDate 임포트
import java.util.List;
import java.util.Map; // Map 임포트 (findUsersByIds 반환 타입용)

public interface UserDao {

    // --- Connection을 파라미터로 받는 메서드들 (트랜잭션 제어용) ---
    User findById(int userId, Connection conn) throws DaoException;
    User findByStudentNumber(String studentNumber, Connection conn) throws DaoException; // 학번으로 사용자 조회
    User findByStudentNumberAndPassword(String studentNumber, String password, Connection conn) throws DaoException; // 로그인 시 사용

    /**
     * 다양한 조건으로 사용자(주로 학생) 목록을 조회합니다.
     * @param conditions 필터링 조건 (예: 학년, 학과, 상태, 검색어 등). Map<String, Object> 형태.
     * @param conn 데이터베이스 연결 객체
     * @return 조건에 맞는 사용자 목록
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    List<User> findUsersByCriteria(Map<String, Object> conditions, Connection conn) throws DaoException;

    /**
     * 모든 사용자(또는 특정 레벨) 목록을 조회합니다.
     * @param conn 데이터베이스 연결 객체
     * @return 사용자 목록
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    List<User> findAll(Connection conn) throws DaoException; // 기존 findAll에서 조건 추가 가능하도록 변경 고려

    /**
     * 여러 사용자 ID에 해당하는 사용자 정보들을 조회합니다.
     * @param userIds 조회할 사용자 ID 목록
     * @param conn 데이터베이스 연결 객체
     * @return 사용자 ID를 키로, User 객체를 값으로 하는 Map
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    Map<Integer, User> findUsersByIds(List<Integer> userIds, Connection conn) throws DaoException;


    void insert(User user, Connection conn) throws DaoException;
    void update(User user, Connection conn) throws DaoException;
    void updatePassword(int userId, String newPassword, Connection conn) throws DaoException; // 비밀번호 변경/초기화용
    void delete(int userId, Connection conn) throws DaoException; // 물리적 삭제 대신 status 변경을 권장했으므로, 이 메서드의 역할 재고 필요

    // --- 기존 시그니처 메서드들 (자체 Connection 관리 또는 Connection 받는 버전 호출) ---
    User findById(int userId) throws DaoException;
    User findByStudentNumber(String studentNumber) throws DaoException;
    User findByStudentNumberAndPassword(String studentNumber, String password) throws DaoException;

    List<User> findUsersByCriteria(Map<String, Object> conditions) throws DaoException;
    List<User> findAll() throws DaoException;
    Map<Integer, User> findUsersByIds(List<Integer> userIds) throws DaoException;

    void insert(User user) throws DaoException;
    void update(User user) throws DaoException;
    void updatePassword(int userId, String newPassword) throws DaoException;
    void delete(int userId) throws DaoException;

    // 기존 findByDpmtAndGrade는 findUsersByCriteria로 통합되거나, 필요시 유지
    List<User> findByDpmtAndGrade(int dpmtId, int grade, Connection conn) throws DaoException;
    List<User> findByDpmtAndGrade(int dpmtId, int grade) throws DaoException;
}