package main.java.service;

import main.java.context.ExamCreationContext;
import main.java.dao.*;
import main.java.dto.ExamOverallStatsDto;
import main.java.dto.ExamProgressSummaryDto;
import main.java.dto.StudentExamStatusDto;
import main.java.dto.StudentScoreDetailDto;
import main.java.model.*;
import main.java.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import main.java.dto.DepartmentExamStatDto;

public class ExamServiceImpl implements ExamService {
    private final ExamDao examDao = new ExamDaoImpl();
    private final ExamsDepartmentDao examsDepartmentDao = new ExamsDepartmentDaoImpl();
    private final QuestionBankDao questionBankDao = new QuestionBankDaoImpl();
    private final QuestionOptionDao questionOptionDao = new QuestionOptionDaoImpl();
    private final AnswerKeyDao answerKeyDao = new AnswerKeyDaoImpl();
    private final ExamAssignmentDao examAssignmentDao = new ExamAssignmentDaoImpl();
    private final UserDao userDao = new UserDaoImpl();
    private final ExamResultDao examResultDao = new ExamResultDaoImpl();
    private final DepartmentDao departmentDao = new DepartmentDaoImpl();
    private final AnswerSheetDao answerSheetDao = new AnswerSheetDaoImpl();
    private final QuestionStatsDao questionStatsDao = new QuestionStatsDaoImpl();

    // 기존 다른 메서드들은 유지됩니다 (getOpenExams, getExamById 등).
    // 편의상 전체 클래스가 아닌 saveExamWithDetails와 필요한 헬퍼 메서드 위주로 보여드립니다.
    @Override
    public void deactivateAndRenameExam(int examId, String newArchivedSubject) throws ServiceException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // 트랜잭션 시작

            // ExamDao를 통해 Connection을 받는 findById와 update 메서드를 호출한다고 가정
            Exam exam = examDao.findById(examId, conn);
            if (exam == null) {
                throw new ServiceException("시험을 찾을 수 없습니다 (ID: " + examId + ")");
            }

            exam.setSubject(newArchivedSubject); // 제목 변경
            exam.setEndDate(LocalDateTime.now()); // 현재 시간으로 마감일 설정 (비활성화)
            // examDao.update는 subject, startDate, endDate, durationMinutes, questionCnt를 업데이트함.
            // startDate, durationMinutes, questionCnt는 기존 값을 유지하며 subject와 endDate만 변경됨.
            examDao.update(exam, conn);

            conn.commit(); // 트랜잭션 커밋
        } catch (DaoException | SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    // 롤백 실패 시 로그 또는 추가 예외 처리
                    System.err.println("트랜잭션 롤백 실패 (deactivateAndRenameExam): " + ex.getMessage());
                }
            }
            throw new ServiceException("시험 비활성화 및 이름 변경 중 오류 발생: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    System.err.println("데이터베이스 커넥션 종료 중 오류 (deactivateAndRenameExam): " + ex.getMessage());
                }
            }
        }
    }

    @Override
    public void revertExamCompletionStatus(int userId, int examId) throws ServiceException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // 트랜잭션 시작

            // 1. 시험 결과 삭제 (ExamResultDao)
            // deleteByUserAndExam 메서드는 삭제된 행의 수를 반환하지만, 여기서는 성공 여부만 중요.
            int deletedResults = examResultDao.deleteByUserAndExam(userId, examId, conn);
            // 필요하다면 deletedResults 값으로 로직 분기 (예: 결과가 원래 없었던 경우 등)

            // 2. 제출 답안 삭제 (AnswerSheetDao)
            // AnswerSheetDao의 deleteByUserAndExam 메서드는 Connection을 받도록 수정되었다고 가정합니다.
            answerSheetDao.deleteByUserAndExam(userId, examId, conn);

            conn.commit(); // 모든 작업 성공 시 트랜잭션 커밋

        } catch (DaoException | SQLException e) { // SQLException도 catch하여 롤백 처리
            if (conn != null) {
                try {
                    conn.rollback(); // 오류 발생 시 롤백
                } catch (SQLException ex) {
                    throw new ServiceException("트랜잭션 롤백 중 오류 발생 (revertExamCompletionStatus): " + ex.getMessage(), ex);
                }
            }
            if (e instanceof DaoException) {
                throw new ServiceException("학생 응시 상태 초기화 중 DAO 작업 실패 (롤백됨): " + e.getMessage(), e);
            } else { // SQLException 등 기타 DB 관련 예외
                throw new ServiceException("학생 응시 상태 초기화 중 데이터베이스 오류 발생 (롤백됨): " + e.getMessage(), e);
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // 커넥션 풀 사용 시 중요
                    conn.close();
                } catch (SQLException ex) {
                    // 로깅 등으로 처리
                    System.err.println("데이터베이스 커넥션 종료 중 오류 (revertExamCompletionStatus): " + ex.getMessage());
                }
            }
        }
    }
    @Override
    public List<ExamProgressSummaryDto> getAllExamProgressSummaries() throws ServiceException {
        List<ExamProgressSummaryDto> summaries = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            // 트랜잭션이 반드시 필요하진 않지만, 여러 DAO 호출 시 일관된 Connection 사용
            List<Exam> allExams = examDao.findAll(conn); // 모든 시험 정보 가져오기

            for (Exam exam : allExams) {
                List<Integer> assignedUserIds = examAssignmentDao.findUserIdsByExam(exam.getExamId(), conn);
                int totalAssigned = assignedUserIds.size();
                int completedCount = examResultDao.countResultsByExam(exam.getExamId(), conn);

                String status;
                LocalDateTime now = LocalDateTime.now();
                if (now.isBefore(exam.getStartDate())) status = "예정";
                else if (now.isAfter(exam.getEndDate())) status = "완료";
                else status = "진행중";

                summaries.add(new ExamProgressSummaryDto(exam, totalAssigned, completedCount, status));
            }
            // 필요시 정렬 (예: 시험 시작일 최신순)
            // summaries.sort(Comparator.comparing(dto -> dto.getExam().getStartDate(), Comparator.reverseOrder()));
            return summaries;
        } catch (DaoException e) {
            throw new ServiceException("전체 시험 진행 상황 요약 조회 중 오류 발생", e);
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { /* 로깅 */ }
            }
        }
    }

    @Override
    public List<ExamProgressSummaryDto> getExamProgressSummariesByYear(int year) throws ServiceException {
        // getAllExamProgressSummaries를 호출하고 year로 필터링하거나,
        // DAO에 findAllByYear 메서드를 추가하여 최적화할 수 있습니다.
        // 여기서는 getAll 후 필터링하는 간단한 예시를 보여드립니다.
        return getAllExamProgressSummaries().stream()
                .filter(summary -> summary.getExam().getStartDate().getYear() == year)
                .collect(Collectors.toList());
    }


    @Override
    public List<StudentExamStatusDto> getStudentStatusesForExam(int examId) throws ServiceException {
        List<StudentExamStatusDto> studentStatuses = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            // 트랜잭션이 반드시 필요하진 않지만, 여러 DAO 호출 시 일관된 Connection 사용

            // 1. 해당 시험에 배정된 학생 ID 목록 조회
            List<Integer> assignedUserIds = examAssignmentDao.findUserIdsByExam(examId, conn);
            if (assignedUserIds.isEmpty()) {
                return studentStatuses; // 배정된 학생이 없으면 빈 리스트 반환
            }

            // 2. 학생 ID 목록으로 학생 정보 일괄 조회
            Map<Integer, User> userMap = userDao.findUsersByIds(assignedUserIds, conn);

            // 3. 학생들의 학과 ID를 수집하여 학과 정보 일괄 조회 (효율성 위해)
            List<Integer> departmentIds = userMap.values().stream()
                    .map(User::getDpmtId)
                    .filter(id -> id > 0) // 유효한 학과 ID만
                    .distinct()
                    .collect(Collectors.toList());
            Map<Integer, Department> departmentMap = new HashMap<>();
            if (!departmentIds.isEmpty()) {
                // DepartmentDao에 findByIds 와 같은 메서드가 있다면 더 효율적
                // 여기서는 각 ID로 findById 호출 (간단한 예시)
                for(int deptId : departmentIds) {
                    Department dept = departmentDao.findById(deptId, conn);
                    if (dept != null) {
                        departmentMap.put(deptId, dept);
                    }
                }
            }

            // 4. 각 학생별 응시 상태 확인 및 DTO 생성
            for (int userId : assignedUserIds) {
                User user = userMap.get(userId);
                if (user == null) continue; // 학생 정보가 없는 경우 건너뛰기

                Department department = departmentMap.get(user.getDpmtId());
                String departmentName = (department != null) ? department.getDpmtName() : "정보 없음";

                boolean hasTaken = examResultDao.existsByUserAndExam(userId, examId, conn);
                String completionStatus = hasTaken ? "응시 완료" : "미응시";

                studentStatuses.add(new StudentExamStatusDto(
                        user.getUserId(),
                        user.getName(),
                        user.getStudentNumber(),
                        departmentName,
                        completionStatus
                ));
            }
            // 필요시 정렬 (예: 학번순 또는 이름순)
            // studentStatuses.sort(Comparator.comparing(StudentExamStatusDto::getStudentNumber));
            return studentStatuses;

        } catch (DaoException e) {
            throw new ServiceException("특정 시험의 학생별 응시 상태 조회 중 오류 발생: examId=" + examId, e);
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { /* 로깅 */ }
            }
        }
    }
    @Override
    public void saveExamWithDetails(ExamCreationContext context) throws ServiceException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // 트랜잭션 시작

            Exam exam = context.getExam();
            List<QuestionFull> questions = context.getQuestions();
            List<Integer> targetGrades = context.getTargetGrades();
            List<Integer> targetDepartments = context.getTargetDepartments();
            int examId;

            if (context.isUpdateMode()) {
                // === 시험 업데이트 모드 ===
                // ... (이전과 동일한 업데이트 로직) ...
                examId = exam.getExamId();
                if (examId <= 0) {
                    throw new ServiceException("업데이트 모드에서는 Exam ID가 반드시 필요합니다.");
                }
                exam.setQuestionCnt(questions != null ? questions.size() : 0);
                examDao.update(exam, conn);

                List<QuestionBank> oldQuestionBanks = questionBankDao.findByExamId(examId, conn);
                for (QuestionBank oldQb : oldQuestionBanks) {
                    questionOptionDao.deleteByQuestionId(oldQb.getQuestionId(), conn);
                    answerKeyDao.deleteByQuestionId(oldQb.getQuestionId(), conn);
                }
                questionBankDao.deleteByExamId(examId, conn);
                examsDepartmentDao.deleteByExamId(examId, conn);
                examAssignmentDao.removeAssignments(examId, conn);

                if (questions != null && !questions.isEmpty()) {
                    insertQuestionsAndRelatedEntities(questions, examId, conn);
                }
                if (targetGrades != null && targetDepartments != null) {
                    insertTargetDepartments(targetGrades, targetDepartments, examId, conn);
                    assignStudentsToExam(targetGrades, targetDepartments, examId, conn);
                }

            } else {
                // === 새 시험 생성 모드 ===
                exam.setQuestionCnt(questions != null ? questions.size() : 0);
                if(exam.getCreatedAt() == null) exam.setCreatedAt(java.time.LocalDateTime.now()); // 생성 시간 설정
                examDao.insert(exam, conn);
                examId = exam.getExamId();
                if (examId <= 0) {
                    throw new ServiceException("새 시험 ID를 가져오는데 실패했습니다.");
                }

                if (questions != null && !questions.isEmpty()) {
                    insertQuestionsAndRelatedEntities(questions, examId, conn);
                }
                if (targetGrades != null && targetDepartments != null) {
                    insertTargetDepartments(targetGrades, targetDepartments, examId, conn);
                    assignStudentsToExam(targetGrades, targetDepartments, examId, conn);
                }
            }

            // Case 3 ("진행 중 시험 수정"으로 인해 새 시험이 생성된 경우) 처리:
            // 새 시험 생성이 완료된 후, originalExamIdToClearAssignments가 있다면 이전 시험의 학생 배정 삭제
            if (!context.isUpdateMode() && context.getOriginalExamIdToClearAssignments() > 0) {
                examAssignmentDao.removeAssignments(context.getOriginalExamIdToClearAssignments(), conn);
            }

            conn.commit(); // 트랜잭션 성공

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    throw new ServiceException("트랜잭션 롤백 중 오류가 발생했습니다: " + ex.getMessage(), ex);
                }
            }
            if (e instanceof ServiceException) throw (ServiceException) e;
            if (e instanceof DaoException) throw new ServiceException("시험 저장/업데이트 중 DAO 작업 실패: " + e.getMessage(), e);
            throw new ServiceException("시험 저장/업데이트 중 예상치 못한 오류 발생: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    System.err.println("데이터베이스 커넥션 종료 중 오류: " + ex.getMessage());
                }
            }
        }
    }

    private void insertQuestionsAndRelatedEntities(List<QuestionFull> questions, int examId, Connection conn) throws DaoException, ServiceException {
        for (QuestionFull qf : questions) {
            QuestionBank qb = qf.getQuestionBank();
            if (qb == null) throw new ServiceException("QuestionBank 객체가 누락되었습니다.");
            qb.setExamId(examId);

            // QuestionFull의 type을 QuestionBank의 type과 동기화 또는 QuestionBank의 type을 사용
            // 예시: qb.setType(qf.getType().name()); (QuestionFull에 type getter/setter가 있고, QuestionType enum을 사용한다고 가정)
            // 현재 QuestionFull.getType()은 QuestionBank.getType()을 참조하므로, QuestionBank에 type이 설정되어 있어야 함.
            // QuestionRowPanel.toQuestionFull()에서 QuestionBank 객체를 만들 때 type이 설정됨.

            questionBankDao.insert(qb, conn);
            int questionId = qb.getQuestionId();
            if (questionId <= 0) {
                throw new DaoException("새 문제 ID를 가져오는데 실패했습니다 (시험 ID: " + examId + ")");
            }
            qf.getQuestionBank().setQuestionId(questionId); // QuestionFull 객체에도 반영 (필요시)


            // QuestionType enum 값을 QuestionFull에서 가져오거나 QuestionBank에서 변환
            QuestionType questionType = QuestionType.valueOf(qb.getType());


            AnswerKey ak = new AnswerKey();
            ak.setQuestionId(questionId);

            if (questionType == QuestionType.MCQ) {
                if (qf.getOptions() != null) {
                    for (QuestionOption opt : qf.getOptions()) {
                        opt.setQuestionId(questionId);
                        questionOptionDao.insert(opt, conn);
                    }
                }
                Character mcqAnswerChar = qf.getCorrectLabel();
                if (mcqAnswerChar == null){
                    throw new ServiceException("객관식 문제의 정답 레이블이 누락되었습니다 (문제 ID: " + questionId + ")");
                }
                ak.setCorrectLabel(mcqAnswerChar);
            } else if (questionType == QuestionType.OX) {
                if (qf.getCorrectText() == null || qf.getCorrectText().isEmpty()) {
                    throw new ServiceException("OX 문제의 정답 텍스트가 누락되었습니다 (문제 ID: " + questionId + ")");
                }
                ak.setCorrectText(qf.getCorrectText()); // "O" 또는 "X"
            }
            answerKeyDao.insert(ak, conn);
        }
    }


    private void insertTargetDepartments(List<Integer> targetGrades, List<Integer> targetDepartments, int examId, Connection conn) throws DaoException {
        for (int grade : targetGrades) {
            for (int dpmtId : targetDepartments) {
                ExamsDepartment ed = new ExamsDepartment();
                ed.setExamId(examId);
                ed.setDpmtId(dpmtId);
                ed.setGrade(grade);
                examsDepartmentDao.save(ed, conn);
            }
        }
    }

    private void assignStudentsToExam(List<Integer> targetGrades, List<Integer> targetDepartments, int examId, Connection conn) throws DaoException {
        List<Integer> assignedUserIds = new ArrayList<>();
        for (int grade : targetGrades) {
            for (int dpmtId : targetDepartments) {
                // UserDao의 findByDpmtAndGrade 메서드도 Connection 객체를 받아 트랜잭션에 참여하도록 수정 필요
                // List<User> users = userDao.findByDpmtAndGrade(dpmtId, grade, conn);
                // 현재 userDao는 Connection을 받지 않으므로, 이 부분은 트랜잭션 보장이 안 될 수 있음.
                // 임시로 기존 메서드 호출 (별도 트랜잭션으로 동작 가능성)
                List<User> users = userDao.findByDpmtAndGrade(dpmtId, grade);
                for (User u : users) {
                    assignedUserIds.add(u.getUserId());
                }
            }
        }

        if (!assignedUserIds.isEmpty()) {
            // assignStudents는 내부적으로 기존 배정 삭제 후 새로 삽입하므로,
            // 업데이트 시 examAssignmentDao.removeAssignments(examId, conn) 호출 후 이걸 또 호출하면 중복 삭제 시도.
            // assignStudents가 알아서 처리하도록 하거나, remove 후 add하는 로직으로 분리 필요.
            // ExamAssignmentDaoImpl.assignStudents는 이미 기존 배정 삭제 로직을 포함하고 있음.
            examAssignmentDao.assignStudents(examId, assignedUserIds, conn);
        }
    }

    // ===== 기존에 있던 다른 ExamService 메서드들 =====
    // getOpenExams(), getExamById(int examId), createFullExam(...) 등등은 여기에 포함됩니다.
    // 이들도 필요에 따라 Connection conn 파라미터를 받도록 수정하여 트랜잭션 제어가 가능하게 만들 수 있습니다.
    // 여기서는 생략합니다.


    @Override
    public List<Exam> getOpenExams() throws ServiceException {
        try (Connection conn = DBConnection.getConnection()){ // 각 메서드가 자체 Connection 관리
            return examDao.findOpenExams(conn);
        } catch (DaoException e) {
            throw new ServiceException("시험 목록 조회 중 오류가 발생했습니다.", e);
        } catch (SQLException e){
            throw new ServiceException("DB 연결 오류.",e);
        }
    }

    @Override
    public List<Exam> getOpenExams(int dpmtId, int grade) throws ServiceException {
        // 이 메서드는 로직상 여러 DAO 호출이 엮이므로 트랜잭션 관리가 필요할 수 있으나,
        // 현재는 단순 조회이므로 개별 Connection 사용도 큰 문제는 없을 수 있습니다.
        // getOpenExams() 내부에서 Connection을 사용하므로, 여기서는 해당 메서드를 호출하는 방식으로 유지
        try {
            // 전체 열려 있는 시험에서 학과·학년 매핑이 있는 것만 필터링
            return getOpenExams().stream()
                    .filter(exam -> {
                        try (Connection conn = DBConnection.getConnection()){
                            return examsDepartmentDao.findByExamId(exam.getExamId(), conn).stream()
                                    .anyMatch(ed -> ed.getDpmtId() == dpmtId && ed.getGrade() == grade);
                        } catch (DaoException | SQLException e) {
                            // Stream 내부에서 체크 예외를 던지기 어렵기 때문에 RuntimeException으로 변환
                            throw new RuntimeException("응시 대상 필터링 중 오류", e);
                        }
                    })
                    .collect(java.util.stream.Collectors.toList());
        } catch (RuntimeException re) {
            // 매핑 조회 중 (DaoException | SQLException)이 원인이라면 ServiceException으로 래핑
            if (re.getCause() instanceof DaoException || re.getCause() instanceof SQLException) {
                throw new ServiceException("응시 대상 필터링 중 오류가 발생했습니다.", re.getCause());
            }
            throw re; // 그 외 RuntimeException은 그대로 던짐
        }
    }


    @Override
    public Exam getExamById(int examId) throws ServiceException {
        try (Connection conn = DBConnection.getConnection()){
            return examDao.findById(examId, conn);
        } catch (DaoException e) {
            throw new ServiceException("시험 정보 조회 중 오류가 발생했습니다.", e);
        } catch (SQLException e){
            throw new ServiceException("DB 연결 오류.",e);
        }
    }

    @Override
    public void createFullExam(Exam exam, List<QuestionFull> questions, List<ExamsDepartment> targets) throws ServiceException {
        // 이 메서드는 saveExamWithDetails와 유사한 로직을 수행할 수 있으므로,
        // ExamCreationContext를 사용하여 saveExamWithDetails를 호출하도록 통합하는 것을 고려할 수 있습니다.
        // 여기서는 기존 메서드 시그니처를 유지하고, 내부적으로 트랜잭션 관리를 한다고 가정합니다.
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            examDao.insert(exam, conn); // exam에 ID 세팅됨
            int newExamId = exam.getExamId();

            if (questions != null) {
                insertQuestionsAndRelatedEntities(questions, newExamId, conn);
            }

            if (targets != null) {
                List<Integer> targetGrades = new ArrayList<>();
                List<Integer> targetDepartments = new ArrayList<>();
                // targets를 기반으로 targetGrades와 targetDepartments를 채우는 로직 필요
                // 예시: targets.forEach(t -> { targetGrades.add(t.getGrade()); targetDepartments.add(t.getDpmtId()); });
                // 중복 제거 및 구조화 필요
                Map<Integer, List<Integer>> dpmtToGradesMap = new HashMap<>();
                for (ExamsDepartment ed : targets) {
                    dpmtToGradesMap.computeIfAbsent(ed.getDpmtId(), k -> new ArrayList<>()).add(ed.getGrade());
                }
                targetDepartments.addAll(dpmtToGradesMap.keySet());
                // targetGrades는 모든 학과에 걸쳐 있는 모든 학년 목록이 될 수도 있고,
                // 아니면 examsDepartmentDao.save를 직접 호출할 수도 있음.
                // 여기서는 examsDepartmentDao.save를 직접 호출하는 것이 더 명확해 보임.
                for (ExamsDepartment ed : targets) {
                    ed.setExamId(newExamId);
                    examsDepartmentDao.save(ed, conn);
                }
            }

            // 문제 개수 업데이트
            if(questions != null) {
                examDao.updateQuestionCount(newExamId, questions.size(), conn);
            } else {
                examDao.updateQuestionCount(newExamId, 0, conn);
            }


            conn.commit();
        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
            throw new ServiceException("시험 전체 저장 실패 (createFullExam)", e);
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
        }
    }


    @Override
    public void submitAllAnswers(int userId, int examId, Map<Integer, String> answers) throws ServiceException {
        // SubmissionServiceImpl로 책임이 이전되었으므로 여기서는 호출하지 않거나, 해당 서비스를 호출.
        // 여기서는 해당 메서드가 SubmissionService에 있다고 가정하고 구현은 생략.
        throw new UnsupportedOperationException("submitAllAnswers는 SubmissionService를 통해 처리됩니다.");
    }

    @Override
    public void saveExamResult(int userId, int examId, int score) throws ServiceException {
        // SubmissionServiceImpl로 책임이 이전되었으므로 여기서는 호출하지 않거나, 해당 서비스를 호출.
        // 여기서는 해당 메서드가 SubmissionService에 있다고 가정하고 구현은 생략.
        throw new UnsupportedOperationException("saveExamResult는 SubmissionService를 통해 처리됩니다.");
    }


    @Override
    public Map<Integer, ExamResult> getExamResultsByUser(int userId) throws ServiceException {
        try (Connection conn = DBConnection.getConnection()) { // ExamResultDao가 Connection을 받는다고 가정
            return examResultDao.findAllByUser(userId, conn);
        } catch (DaoException e) {
            throw new ServiceException("시험 결과 조회 실패", e);
        } catch (SQLException e) {
            throw new ServiceException("DB 연결 오류.", e);
        }
    }

    @Override
    public boolean hasUserTakenExam(int userId, int examId) throws ServiceException {
        try (Connection conn = DBConnection.getConnection()) { // ExamResultDao가 Connection을 받는다고 가정
            return examResultDao.existsByUserAndExam(userId, examId, conn);
        } catch (DaoException e) {
            throw new ServiceException("시험 응시 이력 확인 중 오류", e);
        } catch (SQLException e) {
            throw new ServiceException("DB 연결 오류.", e);
        }
    }


    @Override
    public List<Exam> getAllExams(int dpmtId, int grade) throws ServiceException {
        try (Connection conn = DBConnection.getConnection()){
            return examDao.findAllByDpmtAndGrade(dpmtId, grade, conn);
        } catch (DaoException e) {
            throw new ServiceException("전체 시험 조회 실패 (학과/학년별)", e);
        } catch (SQLException e){
            throw new ServiceException("DB 연결 오류.",e);
        }
    }


    @Override
    public List<Exam> getAllExamsForUser(User user) throws ServiceException {
        try (Connection conn = DBConnection.getConnection()){
            Set<Exam> allExamSet = new HashSet<>();
            allExamSet.addAll(examDao.findAllByDpmtAndGrade(user.getDpmtId(), user.getGrade(), conn));
            allExamSet.addAll(examDao.findAllByUser(user.getUserId(), conn));
            List<Exam> examList = new ArrayList<>(allExamSet);
            examList.sort(Comparator.comparing(Exam::getStartDate).reversed());
            return examList;
        } catch (DaoException e) {
            throw new ServiceException("사용자 전체 시험 조회 실패", e);
        } catch (SQLException e){
            throw new ServiceException("DB 연결 오류.",e);
        }
    }

    @Override
    public List<Exam> getAssignedOpenExams(int userId) throws ServiceException {
        try (Connection conn = DBConnection.getConnection()){
            List<Integer> assignedIds = examAssignmentDao.findExamIdsByUser(userId, conn);
            List<Exam> exams = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            for (Integer id : assignedIds) {
                Exam exam = examDao.findById(id, conn);
                if (exam != null && !exam.getStartDate().isAfter(now) && !exam.getEndDate().isBefore(now) && exam.isActive()) { // isActive()도 확인
                    exams.add(exam);
                }
            }
            return exams;
        } catch (DaoException e) {
            throw new ServiceException("할당된 오픈 시험 조회 실패", e);
        } catch (SQLException e){
            throw new ServiceException("DB 연결 오류.",e);
        }
    }

    @Override
    public List<Exam> getAssignedExams(int userId) throws ServiceException {
        try (Connection conn = DBConnection.getConnection()){
            List<Integer> assignedIds = examAssignmentDao.findExamIdsByUser(userId, conn);
            List<Exam> exams = new ArrayList<>();
            for (Integer id : assignedIds) {
                Exam exam = examDao.findById(id, conn);
                if (exam != null) {
                    exams.add(exam);
                }
            }
            exams.sort(Comparator.comparing(Exam::getStartDate).reversed());
            return exams;
        } catch (DaoException e) {
            throw new ServiceException("할당된 시험 전체 조회 실패", e);
        } catch (SQLException e){
            throw new ServiceException("DB 연결 오류.",e);
        }
    }

    @Override
    public void assignExamToUsers(int examId, List<Integer> userIds) throws ServiceException {
        try (Connection conn = DBConnection.getConnection()){ // 단일 작업이지만, 명시적 트랜잭션 제어 가능
            conn.setAutoCommit(false);
            examAssignmentDao.assignStudents(examId, userIds, conn);
            conn.commit();
        } catch (DaoException e) {
            // 롤백은 assignStudents 내부 또는 여기서 처리
            throw new ServiceException("시험 학생 배정 실패", e);
        } catch (SQLException e){
            throw new ServiceException("DB 연결 또는 트랜잭션 오류.",e);
        }
    }


    @Override
    public List<Exam> getAllExams() throws ServiceException {
        try (Connection conn = DBConnection.getConnection()){
            return examDao.findAll(conn);
        } catch (DaoException e) {
            throw new ServiceException("전체 시험 조회 실패", e);
        } catch (SQLException e){
            throw new ServiceException("DB 연결 오류.",e);
        }
    }

    @Override
    public void disableExam(int examId) throws ServiceException {
        try (Connection conn = DBConnection.getConnection()){
            conn.setAutoCommit(false);
            examDao.disableExam(examId, conn);
            conn.commit();
        } catch (DaoException e) {
            throw new ServiceException("시험 비활성화 실패", e);
        } catch (SQLException e){
            throw new ServiceException("DB 연결 또는 트랜잭션 오류.",e);
        }
    }

    @Override
    public List<String> getAssignedDepartmentsAndGrades(int examId) throws ServiceException {
        try (Connection conn = DBConnection.getConnection()){
            return examDao.findAssignedDepartmentsAndGrades(examId, conn);
        } catch (DaoException e) {
            throw new ServiceException("응시 대상 조회 실패", e);
        } catch (SQLException e){
            throw new ServiceException("DB 연결 오류.",e);
        }
    }

    @Override
    public List<int[]> getAssignedDepartmentAndGradeIds(int examId) throws ServiceException {
        try (Connection conn = DBConnection.getConnection()){
            return examDao.getAssignedDepartmentAndGradeIds(examId, conn);
        } catch (DaoException e) {
            throw new ServiceException("응시 대상 ID 조회 실패", e);
        } catch (SQLException e){
            throw new ServiceException("DB 연결 오류.",e);
        }
    }

    @Override
    public List<QuestionFull> getQuestionsByExamId(int examId) throws ServiceException {
        // QuestionServiceImpl을 사용하거나, 여기서 직접 QuestionDao를 사용하여 Connection 전달
        QuestionService qs = new QuestionServiceImpl(); // QuestionServiceImpl도 Connection을 받도록 수정하거나,
        // 내부에서 Connection을 관리해야 함.
        // 여기서는 QuestionServiceImpl이 자체적으로 Connection을 관리한다고 가정.
        return qs.getQuestionsByExam(examId);
        /* 또는:
        try (Connection conn = DBConnection.getConnection()){
            return questionDao.findFullByExamId(examId, conn); // questionDao도 conn을 받는다고 가정
        } catch (DaoException e) {
            throw new ServiceException("기존 문제 불러오기 실패", e);
        } catch (SQLException e){
            throw new ServiceException("DB 연결 오류.",e);
        }
        */
    }

    @Override
    public List<ExamOverallStatsDto> getAllExamOverallStats() throws ServiceException {
        List<ExamOverallStatsDto> statsList = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            List<Exam> allExams = examDao.findAll(conn);

            for (Exam exam : allExams) {
                List<ExamResult> results = examResultDao.findAllByExam(exam.getExamId(), conn);
                int totalParticipants = results.size();
                double totalScore = 0;
                for (ExamResult result : results) {
                    totalScore += result.getScore();
                }
                double averageScore = (totalParticipants > 0) ? (totalScore / totalParticipants) : 0.0;

                // 총 배정자 수 (선택적 정보)
                int totalAssigned = examAssignmentDao.findUserIdsByExam(exam.getExamId(), conn).size();

                String status;
                LocalDateTime now = LocalDateTime.now();
                if (now.isBefore(exam.getStartDate())) status = "예정";
                else if (now.isAfter(exam.getEndDate())) status = "완료";
                else status = "진행중";

                // 평균 점수는 소수점 둘째 자리까지 반올림 (예시)
                averageScore = Math.round(averageScore * 100.0) / 100.0;

                statsList.add(new ExamOverallStatsDto(exam, totalParticipants, totalAssigned, averageScore, status));
            }
            // 필요시 정렬
            statsList.sort(Comparator.comparing(dto -> dto.getExam().getStartDate(), Comparator.reverseOrder()));
            return statsList;
        } catch (DaoException e) {
            throw new ServiceException("전체 시험 결과 통계 요약 조회 중 오류", e);
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { /* 로깅 */ }
            }
        }
    }

    @Override
    public List<ExamOverallStatsDto> getExamOverallStatsByYear(int year) throws ServiceException {
        return getAllExamOverallStats().stream()
                .filter(dto -> dto.getExam().getStartDate().getYear() == year)
                .collect(Collectors.toList());
    }

    @Override
    public List<StudentScoreDetailDto> getStudentScoreDetailsForExam(int examId, int departmentIdFilter) throws ServiceException {
        List<StudentScoreDetailDto> scoreDetails = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            List<ExamResult> results = examResultDao.findAllByExam(examId, conn); // 점수 내림차순 정렬됨

            if (results.isEmpty()) {
                return scoreDetails;
            }

            List<Integer> userIds = results.stream().map(ExamResult::getUserId).collect(Collectors.toList());
            Map<Integer, User> userMap = userDao.findUsersByIds(userIds, conn);

            // 학과 정보 로딩 (필요한 학과만)
            List<Integer> dpmtIds = userMap.values().stream()
                    .map(User::getDpmtId)
                    .filter(id -> id > 0)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Integer, String> departmentNameMap = new HashMap<>();
            if(!dpmtIds.isEmpty()){
                for(int dpmtId : dpmtIds){
                    Department dept = departmentDao.findById(dpmtId, conn);
                    if(dept != null) departmentNameMap.put(dpmtId, dept.getDpmtName());
                }
            }


            int rank = 0;
            int lastScore = -1;
            int sameRankCount = 0;

            for (ExamResult result : results) {
                User user = userMap.get(result.getUserId());
                if (user == null) continue;

                // 학과 필터링
                if (departmentIdFilter > 0 && user.getDpmtId() != departmentIdFilter) {
                    continue;
                }

                String departmentName = departmentNameMap.getOrDefault(user.getDpmtId(), "정보 없음");

                // 석차 계산 (동점자 처리: 1, 2, 2, 4 방식)
                if (result.getScore() != lastScore) {
                    rank += (sameRankCount + 1);
                    sameRankCount = 0;
                    lastScore = result.getScore();
                } else {
                    sameRankCount++;
                }

                scoreDetails.add(new StudentScoreDetailDto(
                        user.getUserId(),
                        user.getStudentNumber(),
                        user.getName(),
                        departmentName,
                        result.getScore(),
                        rank
                ));
            }

            // 학과 필터링 시 석차 재계산이 필요하면, 필터링된 리스트를 대상으로 다시 석차 로직 수행
            if (departmentIdFilter > 0) {
                // scoreDetails 리스트는 이미 필터링된 학생들만 포함. 이 리스트를 점수 기준으로 다시 정렬 후 석차 재부여.
                scoreDetails.sort(Comparator.comparingInt(StudentScoreDetailDto::getScore).reversed());
                rank = 0;
                lastScore = -1;
                sameRankCount = 0;
                for (int i = 0; i < scoreDetails.size(); i++) {
                    StudentScoreDetailDto dto = scoreDetails.get(i);
                    if (dto.getScore() != lastScore) {
                        rank += (sameRankCount + 1);
                        sameRankCount = 0;
                        lastScore = dto.getScore();
                    } else {
                        sameRankCount++;
                    }
                    dto.setRank(rank); // 계산된 새 석차 설정
                }
            }


            return scoreDetails;
        } catch (DaoException e) {
            throw new ServiceException("학생 상세 성적 조회 중 오류 (examId: " + examId + ")", e);
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { /* 로깅 */ }
            }
        }
    }
    @Override
    public List<DepartmentExamStatDto> getDepartmentalExamStats(int examId) throws ServiceException {
        List<DepartmentExamStatDto> departmentalStats = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();

            // 1. 해당 시험의 모든 결과 조회
            List<ExamResult> allResultsForExam = examResultDao.findAllByExam(examId, conn);
            if (allResultsForExam.isEmpty()) {
                return departmentalStats; // 응시자가 없으면 빈 리스트 반환
            }

            // 2. 응시자들의 User 정보 조회 (학과 ID를 얻기 위함)
            List<Integer> participantUserIds = allResultsForExam.stream()
                    .map(ExamResult::getUserId)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Integer, User> userMap = userDao.findUsersByIds(participantUserIds, conn);

            // 3. 응시자들의 학과 ID 및 학과명 매핑 정보 준비
            List<Integer> departmentIdsInExam = userMap.values().stream()
                    .map(User::getDpmtId)
                    .filter(id -> id > 0) // 유효한 학과 ID
                    .distinct()
                    .collect(Collectors.toList());
            Map<Integer, String> departmentNameMap = new HashMap<>();
            if (!departmentIdsInExam.isEmpty()) {
                for (int deptId : departmentIdsInExam) {
                    Department dept = departmentDao.findById(deptId, conn);
                    if (dept != null) {
                        departmentNameMap.put(deptId, dept.getDpmtName());
                    } else {
                        departmentNameMap.put(deptId, "알 수 없는 학과(ID:" + deptId + ")");
                    }
                }
            }

            // 4. 학과별로 결과 그룹핑
            Map<Integer, List<ExamResult>> resultsByDepartment = new HashMap<>();
            for (ExamResult result : allResultsForExam) {
                User user = userMap.get(result.getUserId());
                if (user != null && user.getDpmtId() > 0) { // 유효한 학과 ID를 가진 사용자의 결과만
                    resultsByDepartment.computeIfAbsent(user.getDpmtId(), k -> new ArrayList<>()).add(result);
                }
            }

            // 5. 각 학과별 통계 계산
            for (Map.Entry<Integer, List<ExamResult>> entry : resultsByDepartment.entrySet()) {
                int departmentId = entry.getKey();
                List<ExamResult> deptResults = entry.getValue();
                String departmentName = departmentNameMap.getOrDefault(departmentId, "학과 정보 없음");

                if (deptResults.isEmpty()) continue;

                int participantCount = deptResults.size();
                double totalScore = 0;
                int highestScore = Integer.MIN_VALUE;
                int lowestScore = Integer.MAX_VALUE;

                for (ExamResult result : deptResults) {
                    totalScore += result.getScore();
                    if (result.getScore() > highestScore) {
                        highestScore = result.getScore();
                    }
                    if (result.getScore() < lowestScore) {
                        lowestScore = result.getScore();
                    }
                }
                double averageScore = (participantCount > 0) ? (totalScore / participantCount) : 0.0;
                averageScore = Math.round(averageScore * 100.0) / 100.0; // 소수점 둘째 자리

                departmentalStats.add(new DepartmentExamStatDto(
                        departmentId,
                        departmentName,
                        participantCount,
                        averageScore,
                        highestScore,
                        lowestScore
                ));
            }

            // 학과명 순으로 정렬 (선택적)
            departmentalStats.sort(Comparator.comparing(DepartmentExamStatDto::getDepartmentName));

            return departmentalStats;

        } catch (DaoException e) {
            throw new ServiceException("학과별 시험 통계 조회 중 오류 발생 (examId: " + examId + ")", e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("DB 연결 종료 중 오류 (getDepartmentalExamStats): " + e.getMessage());
                }
            }
        }
    }
    @Override
    public List<QuestionStat> getQuestionStatsForExam(int examId) throws ServiceException {
        try (Connection conn = DBConnection.getConnection()) {
            // QuestionStatsDao를 통해 해당 시험의 문제 통계 가져오기
            return questionStatsDao.findByExamId(examId, conn);
        } catch (DaoException | SQLException e) {
            throw new ServiceException("문제별 통계 조회 중 오류 (examId: " + examId + ")", e);
        }
    }
}