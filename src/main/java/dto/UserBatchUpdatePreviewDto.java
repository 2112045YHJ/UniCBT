package main.java.dto;

import java.util.List;
import java.util.ArrayList; // ArrayList 임포트

/**
 * 엑셀 일괄 업데이트 시, 기존 정보와 변경될 정보를 비교하여 보여주기 위한 DTO입니다.
 */
public class UserBatchUpdatePreviewDto {
    private UserDto existingUserInDB;
    private UserDto userInfoFromExcel;
    private List<String> changedFields;
    private boolean isNewUser;
    private boolean selectedForUpdate = true; // UI 체크박스 기본값
    private String validationMessage = "";

    /**
     * 생성자입니다.
     * @param existingUserInDB DB의 기존 사용자 정보 (신규이면 null)
     * @param userInfoFromExcel 엑셀에서 읽은 사용자 정보
     * @param isNewUser 신규 사용자 여부
     */
    public UserBatchUpdatePreviewDto(UserDto existingUserInDB, UserDto userInfoFromExcel, boolean isNewUser) {
        this.existingUserInDB = existingUserInDB;
        this.userInfoFromExcel = userInfoFromExcel;
        this.isNewUser = isNewUser;
        this.changedFields = new ArrayList<>();

        validateAndDetectChanges(); // 생성자에서 유효성 검사 및 변경 감지 실행
    }

    private void validateAndDetectChanges() {
        UserDto excelData = this.userInfoFromExcel;
        StringBuilder errors = new StringBuilder();

        // 1. 필수 필드 유효성 검사 (요구사항: 빈 데이터 학생들도 불러오기)
        if (excelData.getName() == null || excelData.getName().isEmpty()) {
            errors.append("이름 누락; ");
        }
        if (excelData.getBirthDate() == null) {
            errors.append("생년월일 누락/형식오류; ");
        }

        // 2. 학과 데이터 유효성 검사 (요구사항: 학과 데이터가 안 맞으면 체크박스 해제)
        // UserServiceImpl.previewExcelStudentUpdates에서 학과명을 ID로 변환 실패 시 dpmtId를 0으로 설정하는 로직 활용
        if (excelData.getDpmtId() == 0 && excelData.getDepartmentName() != null && !excelData.getDepartmentName().isEmpty()) {
            errors.append("일치하는 학과 없음(").append(excelData.getDepartmentName()).append("); ");
        }

        // 유효성 검사 실패 시
        if (errors.length() > 0) {
            this.validationMessage = errors.toString();
            this.selectedForUpdate = false; // 적용 체크박스 해제
            return; // 변경 감지는 더 이상 진행하지 않음
        }

        // 3. 변경 필드 감지 (신규 사용자가 아니고, 유효성 검사를 통과했을 때만)
        if (!isNewUser && existingUserInDB != null) {
            if (!excelData.getName().equals(existingUserInDB.getName())) {
                changedFields.add("이름");
            }
            if (!excelData.getBirthDate().equals(existingUserInDB.getBirthDate())) {
                changedFields.add("생년월일");
            }
            if (excelData.getDpmtId() != existingUserInDB.getDpmtId()) {
                changedFields.add("학과");
            }
            if (excelData.getGrade() != existingUserInDB.getGrade()) {
                changedFields.add("학년");
            }
            if (!excelData.getStatus().equals(existingUserInDB.getStatus())) {
                changedFields.add("상태");
            }
        }
    }

    // Getter 및 Setter 메서드들
    public UserDto getExistingUserInDB() { return existingUserInDB; }
    public void setExistingUserInDB(UserDto existingUserInDB) { this.existingUserInDB = existingUserInDB; }
    public UserDto getUserInfoFromExcel() { return userInfoFromExcel; }
    public void setUserInfoFromExcel(UserDto userInfoFromExcel) { this.userInfoFromExcel = userInfoFromExcel; }
    public List<String> getChangedFields() { return changedFields; }
    public void setChangedFields(List<String> changedFields) { this.changedFields = changedFields; }
    public boolean isNewUser() { return isNewUser; }
    public void setNewUser(boolean newUser) { isNewUser = newUser; }
    public boolean isSelectedForUpdate() { return selectedForUpdate; }
    public void setSelectedForUpdate(boolean selectedForUpdate) { this.selectedForUpdate = selectedForUpdate; }
    public String getValidationMessage() {
        return validationMessage;
    }
}