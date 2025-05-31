package main.java.dto;

import java.util.List;
import java.util.ArrayList; // ArrayList 임포트

/**
 * 엑셀 일괄 업데이트 시, 기존 정보와 변경될 정보를 비교하여 보여주기 위한 DTO입니다.
 */
public class UserBatchUpdatePreviewDto {
    private UserDto existingUserInDB;      // DB에 저장된 현재 사용자 정보 (신규 등록 시 null)
    private UserDto userInfoFromExcel;     // 엑셀에서 읽어온 (업데이트 될) 사용자 정보
    private List<String> changedFields;    // 변경된 필드명 목록 (예: "학년", "상태")
    private boolean isNewUser;             // 이 학생이 신규 등록 대상인지 여부
    private boolean selectedForUpdate = true; // UI에서 사용자가 이 항목을 실제 업데이트 대상으로 선택했는지 (기본값 true)

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

        // existingUserInDB와 userInfoFromExcel을 비교하여 changedFields 목록을 채우는 로직
        if (!isNewUser && existingUserInDB != null && userInfoFromExcel != null) {
            if (userInfoFromExcel.getName() != null && !userInfoFromExcel.getName().equals(existingUserInDB.getName())) {
                changedFields.add("이름");
            }
            if (userInfoFromExcel.getBirthDate() != null && !userInfoFromExcel.getBirthDate().equals(existingUserInDB.getBirthDate())) {
                changedFields.add("생년월일");
            }
            // 학과명으로 비교하거나, userInfoFromExcel에 dpmtId가 채워져 있다면 ID로 비교
            if (userInfoFromExcel.getDepartmentName() != null && !userInfoFromExcel.getDepartmentName().equals(existingUserInDB.getDepartmentName())) {
                if (userInfoFromExcel.getDpmtId() != existingUserInDB.getDpmtId()){ // ID가 있다면 ID 우선 비교
                    changedFields.add("학과");
                } else if (userInfoFromExcel.getDpmtId() == 0 && existingUserInDB.getDpmtId() !=0){ // 엑셀에서 학과명으로 ID를 못찾았지만 기존엔 있었던 경우
                    changedFields.add("학과 (ID 확인 필요)");
                } else if (userInfoFromExcel.getDepartmentName() != null && !userInfoFromExcel.getDepartmentName().equals(existingUserInDB.getDepartmentName())){
                    changedFields.add("학과");
                }
            } else if (userInfoFromExcel.getDpmtId() > 0 && userInfoFromExcel.getDpmtId() != existingUserInDB.getDpmtId()){
                changedFields.add("학과");
            }

            if (userInfoFromExcel.getGrade() > 0 && userInfoFromExcel.getGrade() != existingUserInDB.getGrade()) {
                changedFields.add("학년");
            }
            if (userInfoFromExcel.getStatus() != null && !userInfoFromExcel.getStatus().equals(existingUserInDB.getStatus())) {
                changedFields.add("상태");
            }
            // 비밀번호 변경은 이 DTO에서 직접 비교하지 않음 (별도 처리)
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
}