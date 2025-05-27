package main.java.service;

import main.java.dao.*;
import main.java.model.*;
import main.java.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ExamServiceImpl – 시험 관련 비즈니스 로직 구현
 */
public class ExamServiceImpl implements ExamService {
    private final ExamDao              examDao              = new ExamDaoImpl();
    private final ExamsDepartmentDao   examsDeptDao   = new ExamsDepartmentDaoImpl(); // 이름 맞춤

    private final QuestionBankDao      questionBankDao      = new QuestionBankDaoImpl();
    private final QuestionOptionDao    questionOptionDao    = new QuestionOptionDaoImpl();
    private final AnswerKeyDao         answerKeyDao         = new AnswerKeyDaoImpl();
    private final AnswerSheetDao       answerSheetDao       = new AnswerSheetDaoImpl();
    private final ExamResultDao        examResultDao        = new ExamResultDaoImpl();
    @Override
    public List<Exam> getOpenExams() throws ServiceException {
        try {
            return examDao.findOpenExams();
        } catch (DaoException e) {
            throw new ServiceException("시험 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public List<Exam> getOpenExams(int dpmtId, int grade) throws ServiceException {
        try {
            // 전체 열려 있는 시험에서 학과·학년 매핑이 있는 것만 필터링
            return getOpenExams().stream()
                    .filter(exam -> {
                        try {
                            return examsDeptDao.findByExamId(exam.getExamId()).stream()
                                    .anyMatch(ed -> ed.getDpmtId() == dpmtId && ed.getGrade() == grade);
                        } catch (DaoException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());
        } catch (RuntimeException re) {
            // 매핑 조회 중 DaoException이 원인이라면 ServiceException으로 래핑
            if (re.getCause() instanceof DaoException) {
                throw new ServiceException("응시 대상 필터링 중 오류가 발생했습니다.", re.getCause());
            }
            throw re;
        }
    }

    @Override
    public Exam getExamById(int examId) throws ServiceException {
        try {
            return examDao.findById(examId);
        } catch (DaoException e) {
            throw new ServiceException("시험 정보 조회 중 오류가 발생했습니다.", e);
        }
    }
    @Override
    public void createFullExam(
            Exam exam,
            List<QuestionFull> questions,
            List<ExamsDepartment> targets
    ) throws ServiceException {
        Connection conn = DBConnection.getConnection();
        try {
            conn.setAutoCommit(false);

            // 1) exams 삽입
            examDao.insert(exam);
            int newExamId = exam.getExamId();

            // 2) 문제 + 옵션 + 정답키 삽입
            for (QuestionFull qf : questions) {
                QuestionBank qb = new QuestionBank();
                qb.setExamId(newExamId);
                qb.setType(qf.getType().name());
                qb.setQuestionText(qf.getQuestionText());
                questionBankDao.insert(qb);             // ← insert(QuestionBank) 호출
                int qId = qb.getQuestionId();            //  auto-generated 키를 꺼낸다

                // --- options 삽입 (객관식일 때만) ---
                if (qf.getType() == QuestionType.MCQ) {
                    for (QuestionOption opt : qf.getOptions()) {
                        opt.setQuestionId(qId);
                        questionOptionDao.insert(opt);    // ← insert(QuestionOption) 호출
                    }
                    // --- 정답키 삽입 (객관식) ---
                    answerKeyDao.insert(
                            new AnswerKey(qId, qf.getCorrectLabel(), null)
                    );
                } else {
                    // --- 정답키 삽입 (OX) ---
                    answerKeyDao.insert(
                            new AnswerKey(qId, null, qf.getCorrectText())
                    );
                }
            }

            // 3) 응시 대상 매핑 삽입
            for (ExamsDepartment ed : targets) {
                ed.setExamId(newExamId);
                examsDeptDao.save(ed);
            }

            // 4) 문제 개수 집계 & 업데이트
            int cnt = questionBankDao.countByExamId(newExamId);
            examDao.updateQuestionCount(newExamId, cnt);

            conn.commit();
        } catch (Exception e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            throw new ServiceException("시험 전체 저장 실패", e);
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }
    @Override
    public void submitAllAnswers(int userId, int examId, Map<Integer, String> answers) throws ServiceException {
        try {
            for (Map.Entry<Integer, String> entry : answers.entrySet()) {
                AnswerSheet answerSheet = new AnswerSheet();
                answerSheet.setUserId(userId);
                answerSheet.setExamId(examId);
                answerSheet.setQuestionId(entry.getKey());
                answerSheet.setSelectedAnswer(entry.getValue());
                answerSheetDao.insert(answerSheet);
            }
        } catch (DaoException e) {
            throw new ServiceException("답안 저장 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void saveExamResult(int userId, int examId, int score) throws ServiceException {
        try {
            ExamResult result = new ExamResult();
            result.setUserId(userId);
            result.setExamId(examId);
            result.setScore(score);
            result.setCompletedAt(LocalDateTime.now());
            examResultDao.insert(result);
        } catch (DaoException e) {
            throw new ServiceException("시험 결과 저장 중 오류가 발생했습니다.", e);
        }
    }
    @Override
    public Map<Integer, ExamResult> getExamResultsByUser(int userId) throws ServiceException {
        try {
            return examResultDao.findAllByUser(userId);
        } catch (DaoException e) {
            throw new ServiceException("시험 결과 조회 실패", e);
        }
    }

    @Override
    public boolean hasUserTakenExam(int userId, int examId) throws ServiceException {
        try {
            return examResultDao.existsByUserAndExam(userId, examId);
        } catch (DaoException e) {
            throw new ServiceException("시험 응시 이력 확인 중 오류", e);
        }
    }

    @Override
    public List<Exam> getAllExams(int dpmtId, int grade) throws ServiceException {
        try {
            return examDao.findAllByDpmtAndGrade(dpmtId, grade);
        } catch (DaoException e) {
            throw new ServiceException("전체 시험 조회 실패", e);
        }
    }

    @Override
    public List<Exam> getAllExamsForUser(User user) throws ServiceException {
        try {
            Set<Exam> allExamSet = new HashSet<>();
            // (1) 현재 학과/학년 시험
            allExamSet.addAll(examDao.findAllByDpmtAndGrade(user.getDpmtId(), user.getGrade()));
            // (2) 내가 응시한 시험 (과거 학년/학과 포함)
            allExamSet.addAll(examDao.findAllByUser(user.getUserId()));
            // toList & 정렬
            List<Exam> examList = new ArrayList<>(allExamSet);
            examList.sort(Comparator.comparing(Exam::getStartDate).reversed());
            return examList;
        } catch (DaoException e) {
            throw new ServiceException("전체 시험 조회 실패", e);
        }
    }

}
