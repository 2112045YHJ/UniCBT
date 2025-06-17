package main.java.dao;

import main.java.model.Department;
import java.sql.Connection; // 데이터베이스 연결을 위한 Connection
import java.util.List;

public interface DepartmentDao {

    /**
     * 모든 학과 정보를 조회합니다.
     * @param conn 데이터베이스 연결 객체
     * @return 학과 정보 목록
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    List<Department> findAllDepartments(Connection conn) throws DaoException;
    /**
     * ID로 특정 학과 정보를 조회합니다.
     * @param dpmtId 조회할 학과 ID
     * @param conn 데이터베이스 연결 객체
     * @return 조회된 학과 객체 (없으면 null)
     * @throws DaoException 데이터 접근 오류 발생 시
     */
    Department findById(int dpmtId, Connection conn) throws DaoException;
    Department findById(int dpmtId) throws DaoException;
    List<Department> findAllDepartments() throws DaoException;
}