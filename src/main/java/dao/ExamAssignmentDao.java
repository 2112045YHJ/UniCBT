package main.java.dao;

import java.util.List;

public interface ExamAssignmentDao {
    /**
     * 주어진 exam_id에 대해 기존 할당을 모두 삭제하고,
     * userIds에 있는 학생들을 일괄 배정(insert)한다.
     */
    void assignStudents(int examId, List<Integer> userIds) throws DaoException;

    /** 특정 exam_id에 대한 모든 배정 레코드를 삭제한다. */
    void removeAssignments(int examId) throws DaoException;

    /** 주어진 user_id가 배정된 exam_id 리스트를 반환한다. */
    List<Integer> findExamIdsByUser(int userId) throws DaoException;

    /** 주어진 exam_id에 배정된 user_id 리스트를 반환한다. */
    List<Integer> findUserIdsByExam(int examId) throws DaoException;
}
