package main.java.dao;

import main.java.model.Department;
import java.util.List;

public interface DepartmentDao {
    List<Department> findAllDepartments() throws DaoException;
}
