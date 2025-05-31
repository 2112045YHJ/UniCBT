package main.java.service;

import main.java.dao.UserDao;
import main.java.dao.UserDaoImpl;
import main.java.dao.DepartmentDao;
import main.java.dao.DepartmentDaoImpl;
import main.java.dao.DaoException;
import main.java.dto.UserDto;
import main.java.dto.UserBatchUpdatePreviewDto;
import main.java.model.User;
import main.java.model.Department;
import main.java.util.DBConnection;
import main.java.util.PasswordUtil; // 비밀번호 유틸리티

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

public class UserServiceImpl implements UserService {

    private final UserDao userDao = new UserDaoImpl();
    private final DepartmentDao departmentDao = new DepartmentDaoImpl();

    @Override
    public List<UserDto> searchStudents(Map<String, Object> searchConditions) throws ServiceException {
        List<UserDto> userDtoList = new ArrayList<>();
        // 검색 조건에 학생 레벨(1) 기본 추가
        searchConditions.putIfAbsent("level", 1);

        try (Connection conn = DBConnection.getConnection()) {
            List<User> users = userDao.findUsersByCriteria(searchConditions, conn);

            // 학과명 조회를 위한 학과 ID 목록 수집
            List<Integer> dpmtIds = users.stream()
                    .map(User::getDpmtId)
                    .filter(id -> id > 0) // 0 또는 음수는 유효하지 않은 학과 ID로 간주
                    .distinct()
                    .collect(Collectors.toList());
            Map<Integer, String> departmentNameMap = new HashMap<>();
            if (!dpmtIds.isEmpty()) {
                // DepartmentDao에 여러 ID로 학과 정보를 가져오는 메서드가 있다면 사용 (예: findByIds)
                // 여기서는 각 ID로 조회하는 예시 (N+1 문제 발생 가능성 있으나, 학과 수가 적다면 무방)
                for (int dpmtId : dpmtIds) {
                    Department dept = departmentDao.findById(dpmtId, conn); // Connection 받는 버전 사용
                    if (dept != null) {
                        departmentNameMap.put(dpmtId, dept.getDpmtName());
                    }
                }
            }

            for (User user : users) {
                String deptName = departmentNameMap.getOrDefault(user.getDpmtId(), "미지정");
                userDtoList.add(new UserDto(user, deptName));
            }
        } catch (DaoException | SQLException e) {
            throw new ServiceException("학생 목록 검색 중 오류 발생: " + e.getMessage(), e);
        }
        return userDtoList;
    }

    @Override
    public UserDto getStudentDetails(int userId) throws ServiceException {
        try (Connection conn = DBConnection.getConnection()) {
            User user = userDao.findById(userId, conn);
            if (user == null || user.getLevel() != 1) {
                throw new ServiceException("ID " + userId + "에 해당하는 학생 정보를 찾을 수 없습니다.");
            }
            String departmentName = "미지정";
            if (user.getDpmtId() > 0) {
                Department dept = departmentDao.findById(user.getDpmtId(), conn);
                if (dept != null) departmentName = dept.getDpmtName();
            }
            return new UserDto(user, departmentName);
        } catch (DaoException | SQLException e) {
            throw new ServiceException("학생 상세 정보 조회 중 오류 발생: " + e.getMessage(), e);
        }
    }

    @Override
    public UserDto registerStudent(UserDto userDto) throws ServiceException, SQLException, DaoException {
        // 기본 유효성 검사
        if (userDto == null ||
                userDto.getStudentNumber() == null || userDto.getStudentNumber().trim().isEmpty() ||
                userDto.getName() == null || userDto.getName().trim().isEmpty() ||
                userDto.getBirthDate() == null) {
            throw new ServiceException("학번, 이름, 생년월일은 필수 입력 항목입니다.");
        }
        // 학과 ID, 학년, 상태 등에 대한 추가 유효성 검사도 필요할 수 있음

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 학번 중복 검사
            if (userDao.findByStudentNumber(userDto.getStudentNumber().trim(), conn) != null) {
                throw new ServiceException("이미 사용 중인 학번입니다: " + userDto.getStudentNumber());
            }

            User user = new User();
            user.setLevel(1); // 학생
            user.setName(userDto.getName().trim());
            user.setStudentNumber(userDto.getStudentNumber().trim());
            user.setBirthDate(userDto.getBirthDate());

            // 비밀번호 정책 적용 ('a' + 생년월일 8자리) 및 해싱
            String initialPassword = PasswordUtil.generateInitialPassword(userDto.getBirthDate());
            user.setPassword(PasswordUtil.hashPassword(initialPassword));

            user.setDpmtId(userDto.getDpmtId()); // 0 또는 유효한 ID여야 함
            user.setGrade(userDto.getGrade());   // 0 또는 유효한 학년이어야 함
            user.setStatus(userDto.getStatus() != null && !userDto.getStatus().trim().isEmpty() ? userDto.getStatus().trim() : "재학");

            // createdAt, updatedAt은 DB에서 DEFAULT CURRENT_TIMESTAMP로 처리되도록 설정했으므로,
            // User 모델에서 명시적으로 설정하지 않아도 될 수 있음. (DAO insert에서 null이면 NOW()로 설정)
            // user.setCreatedAt(java.time.LocalDateTime.now());
            // user.setUpdatedAt(java.time.LocalDateTime.now());

            userDao.insert(user, conn); // 이 메서드는 user 객체에 생성된 userId를 설정해야 함

            conn.commit();

            // 반환 DTO 준비
            String deptName = "미지정";
            if(user.getDpmtId() > 0) {
                // 새 Connection을 얻거나, 트랜잭션이 커밋되었으므로 이후 조회는 새 Connection 사용 가능
                try (Connection viewConn = DBConnection.getConnection()) {
                    Department d = departmentDao.findById(user.getDpmtId(), viewConn);
                    if(d != null) deptName = d.getDpmtName();
                } catch (Exception e) { /* 학과명 조회 실패 시 로깅 또는 기본값 사용 */ }
            }
            return new UserDto(user, deptName);

        } catch (DaoException | SQLException | ServiceException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { /* 로깅 */ }
            if (e instanceof ServiceException) throw e;
            throw new ServiceException("신규 학생 등록 중 오류 발생: " + e.getMessage(), e);
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { /* 로깅 */ }
        }
    }

    @Override
    public UserDto updateStudent(int userId, UserDto userDto) throws ServiceException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            User user = userDao.findById(userId, conn);
            if (user == null || user.getLevel() != 1) {
                throw new ServiceException("ID " + userId + "에 해당하는 학생 정보를 찾을 수 없습니다.");
            }

            // 학번 변경 시 중복 검사 (일반적으로 학번은 변경하지 않음)
            if (userDto.getStudentNumber() != null && !userDto.getStudentNumber().equals(user.getStudentNumber())) {
                if (userDao.findByStudentNumber(userDto.getStudentNumber(), conn) != null) {
                    throw new ServiceException("수정하려는 학번(" + userDto.getStudentNumber() + ")은 이미 사용 중입니다.");
                }
                user.setStudentNumber(userDto.getStudentNumber());
            }

            if (userDto.getName() != null) user.setName(userDto.getName());
            if (userDto.getBirthDate() != null) user.setBirthDate(userDto.getBirthDate());
            if (userDto.getDpmtId() > 0) user.setDpmtId(userDto.getDpmtId()); // 0은 학과 없음으로 간주할 수 있음
            if (userDto.getGrade() > 0) user.setGrade(userDto.getGrade());
            if (userDto.getStatus() != null) user.setStatus(userDto.getStatus());
            // 비밀번호는 별도 메서드(resetStudentPassword)로 처리

            userDao.update(user, conn);
            conn.commit();

            String deptName = "";
            if(user.getDpmtId() > 0) {
                Department d = departmentDao.findById(user.getDpmtId(), conn);
                if(d != null) deptName = d.getDpmtName();
            }
            return new UserDto(user, deptName);

        } catch (DaoException | SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { /* 로깅 */ }
            throw new ServiceException("학생 정보 수정 중 오류 발생: " + e.getMessage(), e);
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { /* 로깅 */ }
        }
    }

    @Override
    public UserDto updateStudentInfo(int userId, UserDto userDto) throws ServiceException, SQLException, DaoException {
        if (userDto == null) {
            throw new ServiceException("업데이트할 학생 정보가 없습니다.");
        }
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            User user = userDao.findById(userId, conn);
            if (user == null || user.getLevel() != 1) {
                throw new ServiceException("ID " + userId + "에 해당하는 학생 정보를 찾을 수 없습니다.");
            }

            // 학번 변경 시 중복 검사 (일반적으로 학번은 주요 식별자이므로 변경을 허용하지 않거나 신중해야 함)
            if (userDto.getStudentNumber() != null && !userDto.getStudentNumber().trim().isEmpty() &&
                    !userDto.getStudentNumber().trim().equals(user.getStudentNumber())) {
                if (userDao.findByStudentNumber(userDto.getStudentNumber().trim(), conn) != null) {
                    throw new ServiceException("수정하려는 학번(" + userDto.getStudentNumber() + ")은 이미 다른 사용자가 사용 중입니다.");
                }
                user.setStudentNumber(userDto.getStudentNumber().trim());
            }

            if (userDto.getName() != null && !userDto.getName().trim().isEmpty()) user.setName(userDto.getName().trim());
            if (userDto.getBirthDate() != null) user.setBirthDate(userDto.getBirthDate());
            if (userDto.getDpmtId() >= 0) user.setDpmtId(userDto.getDpmtId()); // 0은 학과 없음(미지정)으로 간주 가능
            if (userDto.getGrade() >= 0) user.setGrade(userDto.getGrade());     // 0은 학년 없음(미지정)으로 간주 가능
            if (userDto.getStatus() != null && !userDto.getStatus().trim().isEmpty()) user.setStatus(userDto.getStatus().trim());
            // 비밀번호 변경은 resetStudentPassword 또는 별도의 changePassword 메서드로 처리

            userDao.update(user, conn); // updated_at은 DB에서 자동 업데이트
            conn.commit();

            String deptName = "미지정";
            if(user.getDpmtId() > 0) {
                try (Connection viewConn = DBConnection.getConnection()) {
                    Department d = departmentDao.findById(user.getDpmtId(), viewConn);
                    if(d != null) deptName = d.getDpmtName();
                } catch (Exception e) { /* 학과명 조회 실패 시 로깅 또는 기본값 사용 */ }
            }
            return new UserDto(user, deptName);

        } catch (DaoException | SQLException | ServiceException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { /* 로깅 */ }
            if (e instanceof ServiceException) throw e;
            throw new ServiceException("학생 정보 수정 중 오류 발생 (ID: " + userId + "): " + e.getMessage(), e);
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { /* 로깅 */ }
        }
    }

    @Override
    public void resetStudentPassword(int userId) throws ServiceException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            User user = userDao.findById(userId, conn);
            if (user == null || user.getLevel() != 1) {
                throw new ServiceException("ID " + userId + "에 해당하는 학생 정보를 찾을 수 없습니다.");
            }
            if (user.getBirthDate() == null) {
                throw new ServiceException("비밀번호를 초기화하려면 학생의 생년월일 정보가 등록되어 있어야 합니다.");
            }

            String initialPassword = PasswordUtil.generateInitialPassword(user.getBirthDate());
            String hashedPassword = PasswordUtil.hashPassword(initialPassword); // 비밀번호 해싱
            userDao.updatePassword(userId, hashedPassword, conn); // updated_at은 DB에서 자동 업데이트

            conn.commit();
        } catch (DaoException | SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { /* 로깅 */ }
            throw new ServiceException("학생 비밀번호 초기화 중 오류 발생 (ID: " + userId + "): " + e.getMessage(), e);
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { /* 로깅 */ }
        }
    }

    @Override
    public UserDto changeStudentStatus(int userId, String newStatus) throws ServiceException {
        // newStatus 유효성 검사 (예: "재학", "휴학", "졸업", "자퇴", "퇴학" 중 하나인지)
        List<String> validStatuses = List.of("재학", "휴학", "졸업", "자퇴", "퇴학"); // 시스템에서 관리하는 상태 목록
        if (newStatus == null || !validStatuses.contains(newStatus)) {
            throw new ServiceException("유효하지 않은 학생 상태 값입니다: " + newStatus);
        }

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);
            User user = userDao.findById(userId, conn);
            if (user == null || user.getLevel() != 1) {
                throw new ServiceException("ID " + userId + "에 해당하는 학생 정보를 찾을 수 없습니다.");
            }
            user.setStatus(newStatus);
            userDao.update(user, conn); // update 메서드가 status를 포함하여 업데이트
            conn.commit();

            String deptName = "미지정";
            if(user.getDpmtId() > 0) {
                try (Connection viewConn = DBConnection.getConnection()) {
                    Department d = departmentDao.findById(user.getDpmtId(), viewConn);
                    if(d != null) deptName = d.getDpmtName();
                } catch (Exception e) { /* 로깅 */ }
            }
            return new UserDto(user, deptName);

        } catch (DaoException | SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { /* 로깅 */ }
            throw new ServiceException("학생 상태 변경 중 오류 발생 (ID: " + userId + "): " + e.getMessage(), e);
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { /* 로깅 */ }
        }
    }

    @Override
    public List<UserBatchUpdatePreviewDto> previewExcelStudentUpdates(List<UserDto> excelStudentList) throws ServiceException {
        List<UserBatchUpdatePreviewDto> previewList = new ArrayList<>();
        // 이 메서드는 DB Connection이 필요하며, 각 학생의 학과명을 조회하여 채워야 함.
        try (Connection conn = DBConnection.getConnection()){
            // 엑셀 데이터의 학과명 -> 학과 ID 변환을 위해 전체 학과 정보 로드 (효율적인 방식)
            Map<String, Integer> departmentNameToIdMap = new HashMap<>();
            List<Department> allDepartments = departmentDao.findAllDepartments(conn); // DepartmentDao에 findAllDepartments(conn) 필요
            for (Department dept : allDepartments) {
                departmentNameToIdMap.put(dept.getDpmtName(), dept.getDpmtId());
            }

            for (UserDto excelUserDto : excelStudentList) {
                if (excelUserDto.getStudentNumber() == null || excelUserDto.getStudentNumber().trim().isEmpty()) {
                    System.err.println("엑셀 데이터 학번 누락 (이름: " + excelUserDto.getName() + "). 미리보기에서 제외됩니다.");
                    // 또는 오류 DTO를 만들어 previewList에 추가하고 UI에 표시
                    continue;
                }

                // 엑셀에서 읽은 학과명을 학과 ID로 변환 시도
                if (excelUserDto.getDepartmentName() != null && !excelUserDto.getDepartmentName().trim().isEmpty()) {
                    Integer dpmtId = departmentNameToIdMap.get(excelUserDto.getDepartmentName().trim());
                    if (dpmtId != null) {
                        excelUserDto.setDpmtId(dpmtId);
                    } else {
                        System.err.println("경고: 엑셀의 학과명(" + excelUserDto.getDepartmentName() + ")에 해당하는 학과 ID를 찾을 수 없습니다. 학번: " + excelUserDto.getStudentNumber());
                        excelUserDto.setDpmtId(0); // 또는 오류 처리
                    }
                } else if (excelUserDto.getDpmtId() == 0 && excelUserDto.getDepartmentName() != null && !excelUserDto.getDepartmentName().trim().isEmpty()) {
                    // dpmtId가 없고 departmentName만 있는 경우 위 로직과 동일
                    Integer dpmtId = departmentNameToIdMap.get(excelUserDto.getDepartmentName().trim());
                    if (dpmtId != null) excelUserDto.setDpmtId(dpmtId);

                }


                User existingUser = userDao.findByStudentNumber(excelUserDto.getStudentNumber().trim(), conn);
                UserDto existingUserDto = null;
                boolean isNew = (existingUser == null);

                if (!isNew) {
                    String deptName = "";
                    if(existingUser.getDpmtId() > 0) {
                        Department d = departmentDao.findById(existingUser.getDpmtId(), conn);
                        if(d != null) deptName = d.getDpmtName();
                    }
                    existingUserDto = new UserDto(existingUser, deptName);
                }

                UserBatchUpdatePreviewDto previewDto = new UserBatchUpdatePreviewDto(existingUserDto, excelUserDto, isNew);
                // UserBatchUpdatePreviewDto 생성자에서 변경 필드 감지 로직이 이미 있다고 가정
                previewList.add(previewDto);
            }
        } catch (DaoException | SQLException e) {
            throw new ServiceException("엑셀 업데이트 미리보기 데이터 생성 중 오류: " + e.getMessage(), e);
        }
        return previewList;
    }

    @Override
    public void batchUpdateStudents(List<UserDto> studentsToProcess) throws ServiceException, SQLException, DaoException {
        // studentsToProcess는 UserBatchUpdatePreviewDto에서 isSelectedForUpdate=true 이고,
        // updatedInfoFromExcel 필드의 UserDto 목록이라고 가정.
        // 각 UserDto에는 학과 ID (dpmtId)가 이미 채워져 있어야 함 (preview 단계에서 변환 완료).
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            for (UserDto userDto : studentsToProcess) {
                // UserDto에는 학번, 이름, 생년월일, 학과ID, 학년, 상태 등이 모두 채워져 있다고 가정
                User existingUser = userDao.findByStudentNumber(userDto.getStudentNumber(), conn);
                if (existingUser == null) { // 신규 등록
                    registerStudentWithConnection(userDto, conn); // 비밀번호 생성/해싱 등 내부 처리
                } else { // 기존 사용자 업데이트
                    updateStudentWithConnection(existingUser, userDto, conn); // 기존 User 객체와 업데이트할 DTO 전달
                }
            }
            conn.commit();
        } catch (DaoException | SQLException | ServiceException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { /* 로깅 */ }
            if (e instanceof ServiceException) throw e;
            throw new ServiceException("학생 일괄 업데이트/등록 중 오류: " + e.getMessage(), e);
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { /* 로깅 */ }
        }
    }

    // registerStudent 로직 중 Connection을 받아 처리하고, User 객체를 반환하는 부분
    private User registerStudentWithConnection(UserDto userDto, Connection conn) throws DaoException, ServiceException {
        User user = new User();
        user.setLevel(1);
        user.setName(userDto.getName());
        user.setStudentNumber(userDto.getStudentNumber());
        if (userDto.getBirthDate() == null) throw new ServiceException("신규 학생 등록 시 생년월일은 필수입니다 (학번: " + userDto.getStudentNumber() + ")");
        user.setBirthDate(userDto.getBirthDate());

        String initialPassword = PasswordUtil.generateInitialPassword(userDto.getBirthDate());
        user.setPassword(PasswordUtil.hashPassword(initialPassword));

        user.setDpmtId(userDto.getDpmtId()); // DTO에 학과 ID가 채워져 있다고 가정
        user.setGrade(userDto.getGrade());
        user.setStatus(userDto.getStatus() != null ? userDto.getStatus() : "재학");
        // createdAt, updatedAt은 DB에서 자동 설정

        userDao.insert(user, conn); // 이 메서드는 user 객체에 생성된 userId를 설정해야 함
        return user;
    }

    // updateStudentInfo 로직 중 기존 User 객체와 Connection을 받아 처리하는 부분
    private User updateStudentWithConnection(User existingUser, UserDto userDto, Connection conn) throws DaoException, ServiceException {
        // 학번은 변경하지 않는다고 가정 (식별자이므로)
        if (userDto.getName() != null) existingUser.setName(userDto.getName());
        if (userDto.getBirthDate() != null) existingUser.setBirthDate(userDto.getBirthDate());
        if (userDto.getDpmtId() >= 0) existingUser.setDpmtId(userDto.getDpmtId()); // 0은 미지정
        if (userDto.getGrade() >= 0) existingUser.setGrade(userDto.getGrade());
        if (userDto.getStatus() != null) existingUser.setStatus(userDto.getStatus());
        // 비밀번호는 resetStudentPassword로 별도 처리

        userDao.update(existingUser, conn);
        return existingUser;
    }


    @Override
    public void promoteStudentsToNextGrade() throws ServiceException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            Map<String, Object> conditions = new HashMap<>();
            conditions.put("status", "재학"); // "재학" 상태인 학생들만 대상
            // conditions.put("level", 1); // findUsersByCriteria에서 이미 level=1로 가정할 수 있음
            List<User> studentsToPromote = userDao.findUsersByCriteria(conditions, conn);

            // 시스템의 최대 학년 (예: 4학년제면 4, 3학년제면 3)
            final int MAX_GRADE = 3; // 예시: 3학년이 최고 학년

            for (User student : studentsToPromote) {
                if (student.getGrade() >= MAX_GRADE) {
                    student.setStatus("졸업");
                    // student.setGrade(0); // 졸업 시 학년 정보를 어떻게 할지 정책 결정 (예: 0, null, 또는 마지막 학년 유지)
                } else if (student.getGrade() > 0 && student.getGrade() < MAX_GRADE) {
                    student.setGrade(student.getGrade() + 1);
                } else {
                    // 학년 정보가 없거나(0 또는 null) MAX_GRADE보다 큰 비정상적인 경우, 어떻게 처리할지?
                    // 여기서는 일단 변경하지 않음. 또는 로그를 남기거나 예외 처리.
                    System.err.println("경고: 학년 정보가 비정상적인 재학생 발견 (ID: " + student.getUserId() + ", 현재 학년: " + student.getGrade() + "). 진급 처리에서 제외됩니다.");
                    continue;
                }
                userDao.update(student, conn);
            }
            conn.commit();
        } catch (DaoException | SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { /* 로깅 */ }
            throw new ServiceException("학생 일괄 진급 처리 중 오류 발생: " + e.getMessage(), e);
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { /* 로깅 */ }
        }
    }
}