package main.java.service;

import main.java.model.Exam;
import main.java.model.ExamResult;
import main.java.model.ExamsDepartment;
import main.java.model.QuestionFull;

import java.util.List;
import java.util.Map;

public interface ResultService {
    Map<Integer, ExamResult> getResultsByUser(int userId) throws ServiceException;
}