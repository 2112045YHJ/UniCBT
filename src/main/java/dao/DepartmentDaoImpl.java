package main.java.dao;

import main.java.model.Department;
import main.java.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DepartmentDaoImpl implements DepartmentDao {

    // --- Connection을 파라미터로 받는 메서드 구현 ---
    @Override
    public List<Department> findAllDepartments(Connection conn) throws DaoException {
        // (이전 턴에서 제공된 코드)
        List<Department> list = new ArrayList<>();
        String sql = "SELECT dpmt_id, dpmt_name, faculty, contact_email, contact_phone, created_at FROM department ORDER BY dpmt_name";
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Department d = new Department();
                d.setDpmtId(rs.getInt("dpmt_id"));
                d.setDpmtName(rs.getString("dpmt_name"));
                d.setFaculty(rs.getString("faculty"));
                d.setContactEmail(rs.getString("contact_email"));
                d.setContactPhone(rs.getString("contact_phone"));
                Timestamp createdAtTs = rs.getTimestamp("created_at");
                if (createdAtTs != null) {
                    d.setCreatedAt(createdAtTs.toLocalDateTime());
                }
                list.add(d);
            }
        } catch (SQLException e) {
            throw new DaoException("모든 학과 조회 실패", e);
        }
        return list;
    }

    @Override
    public List<Department> findAllDepartments() throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return findAllDepartments(conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 실패 또는 모든 학과 조회 중 오류", e);
        }
    }

    @Override
    public Department findById(int dpmtId, Connection conn) throws DaoException {
        String sql = "SELECT dpmt_id, dpmt_name, faculty, contact_email, contact_phone, created_at FROM department WHERE dpmt_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, dpmtId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Department d = new Department();
                    d.setDpmtId(rs.getInt("dpmt_id"));
                    d.setDpmtName(rs.getString("dpmt_name"));
                    d.setFaculty(rs.getString("faculty"));
                    d.setContactEmail(rs.getString("contact_email"));
                    d.setContactPhone(rs.getString("contact_phone"));
                    Timestamp createdAtTs = rs.getTimestamp("created_at");
                    if (createdAtTs != null) {
                        d.setCreatedAt(createdAtTs.toLocalDateTime());
                    }
                    return d;
                }
                return null; // 해당 ID의 학과가 없는 경우
            }
        } catch (SQLException e) {
            throw new DaoException("ID로 학과 정보 조회 실패: dpmtId=" + dpmtId, e);
        }
    }

    @Override
    public Department findById(int dpmtId) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return findById(dpmtId, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 실패 또는 ID로 학과 정보 조회 중 오류", e);
        }
    }
}