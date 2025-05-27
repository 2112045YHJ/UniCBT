package main.java.service;

import main.java.dao.ExamResultDao;
import main.java.dao.ExamResultDaoImpl;
import main.java.dao.DaoException;
import main.java.model.ExamResult;

import java.util.List;
import java.util.Map;

public class ResultServiceImpl implements ResultService {
    private final ExamResultDao resultDao = new ExamResultDaoImpl();

    @Override
    public Map<Integer, ExamResult> getResultsByUser(int userId) throws ServiceException {
        try {
            return resultDao.findAllByUser(userId);
        } catch (DaoException e) {
            throw new ServiceException("과거 시험 결과 조회 중 오류가 발생했습니다.", e);
        }
    }

}

