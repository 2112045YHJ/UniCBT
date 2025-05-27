package main.java.dao;

import main.java.model.Department;
import main.java.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DepartmentDaoImpl implements DepartmentDao {
    @Override
    public List<Department> findAllDepartments() throws DaoException {
        List<Department> list = new ArrayList<>();
        String sql = "SELECT dpmt_id, dpmt_name FROM department ORDER BY dpmt_name";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Department d = new Department();
                d.setDpmtId(rs.getInt("dpmt_id"));
                d.setDpmtName(rs.getString("dpmt_name"));
                list.add(d);
            }
        } catch (SQLException e) {
            throw new DaoException("학과 목록 조회 실패", e);
        }

        return list;
    }
}
