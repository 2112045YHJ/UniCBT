package main.java.service;

import main.java.dao.DaoException;
import main.java.dto.UserDto;
import main.java.dto.UserBatchUpdatePreviewDto; // 추가
import main.java.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface UserService {

    /**
     * 다양한 조건에 따라 학생 목록을 조회합니다.
     * @param searchConditions (Key: "grade", "departmentId", "status", "statuses"(List<String>), "nameSearch", "studentNumberSearch", "orderBy", "orderDirection")
     * @return 조건에 맞는 UserDto 목록 (학과명 포함)
     * @throws ServiceException 서비스 처리 중 오류 발생 시
     */
    List<UserDto> searchStudents(Map<String, Object> searchConditions) throws ServiceException;

    /**
     * 특정 학생의 상세 정보를 조회합니다.
     * @param userId 조회할 학생의 ID
     * @return UserDto 객체 (학과명 포함)
     * @throws ServiceException 학생을 찾을 수 없거나 오류 발생 시
     */
    UserDto getStudentDetails(int userId) throws ServiceException;

    /**
     * 신규 학생 계정을 등록합니다. 초기 비밀번호는 'a' + 생년월일 8자리로 자동 생성 및 해싱됩니다.
     * @param userDto 등록할 학생 정보 (이름, 학번, 생년월일, 학과ID, 학년, 초기상태 등)
     * @return 생성된 UserDto (userId 포함, 학과명 포함, passwordInput은 null)
     * @throws ServiceException 학번 중복 또는 오류 발생 시
     */
    UserDto registerStudent(UserDto userDto) throws ServiceException, SQLException, DaoException;

    /**
     * 기존 학생 정보를 업데이트합니다. (비밀번호 변경은 별도 메서드 사용)
     * @param userId 업데이트할 학생의 ID
     * @param userDto 업데이트할 정보 (이름, 학번(변경시 중복체크), 생년월일, 학과ID, 학년, 상태 등)
     * @return 업데이트된 UserDto (학과명 포함)
     * @throws ServiceException 학생을 찾을 수 없거나 학번 중복 등 오류 발생 시
     */
    UserDto updateStudentInfo(int userId, UserDto userDto) throws ServiceException, SQLException, DaoException;

    /**
     * 특정 학생의 비밀번호를 초기화합니다 ('a' + 생년월일 8자리).
     * @param userId 비밀번호를 초기화할 학생의 ID
     * @throws ServiceException 학생을 찾을 수 없거나 생년월일 정보가 없는 경우 등 오류 발생 시
     */
    void resetStudentPassword(int userId) throws ServiceException;

    /**
     * 특정 학생의 상태를 변경합니다.
     * @param userId 상태를 변경할 학생의 ID
     * @param newStatus 새로운 상태 문자열 ("재학", "휴학", "졸업", "자퇴", "퇴학")
     * @throws ServiceException 학생을 찾을 수 없거나 유효하지 않은 상태 변경 시 등 오류 발생 시
     */
    UserDto changeStudentStatus(int userId, String newStatus) throws ServiceException;
    UserDto updateStudent(int userId, UserDto userDto) throws ServiceException;
    /**
     * 엑셀 파일로부터 읽어온 학생 데이터 목록을 기반으로,
     * DB의 기존 정보와 비교하여 변경 사항 미리보기 DTO 목록을 생성합니다.
     * @param excelStudentList 엑셀에서 파싱된 UserDto 목록
     * @return UserBatchUpdatePreviewDto 목록
     * @throws ServiceException 서비스 처리 중 오류 발생 시
     */
    List<UserBatchUpdatePreviewDto> previewExcelStudentUpdates(List<UserDto> excelStudentList) throws ServiceException;

    /**
     * 미리보기에서 사용자가 선택한 학생들의 정보로 실제 DB에 일괄 등록 또는 업데이트를 수행합니다.
     * @param studentsToProcess UserBatchUpdatePreviewDto에서 isSelectedForUpdate=true 이고,
     * updatedInfoFromExcel 필드의 UserDto 목록
     * @throws ServiceException 서비스 처리 중 오류 발생 시 (트랜잭셔널하게 처리)
     */
    void batchUpdateStudents(List<UserDto> studentsToProcess) throws ServiceException, SQLException, DaoException;

    /**
     * 재학생들의 학년을 일괄적으로 진급시킵니다. (예: 1->2, 2->3, 3->졸업(상태변경))
     * @throws ServiceException 서비스 처리 중 오류 발생 시 (트랜잭셔널하게 처리)
     */
    void promoteStudentsToNextGrade() throws ServiceException;
}