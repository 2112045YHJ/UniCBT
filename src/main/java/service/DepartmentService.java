package main.java.service;

import main.java.model.Department;
import java.util.List;

public interface DepartmentService {
    List<Department> getAllDepartments() throws ServiceException;
}
