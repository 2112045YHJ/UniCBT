package main.java.service;

import main.java.dao.DepartmentDao;
import main.java.dao.DepartmentDaoImpl;
import main.java.model.Department;

import java.util.List;

public class DepartmentServiceImpl implements DepartmentService {
    private final DepartmentDao departmentDao = new DepartmentDaoImpl();

    @Override
    public List<Department> getAllDepartments() throws ServiceException {
        try {
            return departmentDao.findAllDepartments();
        } catch (Exception e) {
            throw new ServiceException("학과 목록 서비스 오류", e);
        }
    }
}
