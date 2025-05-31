package main.java.dto; // dto 패키지를 사용한다고 가정합니다.

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 사용자(주로 학생) 정보 전달을 위한 DTO 클래스입니다.
 * UI에 표시하거나 사용자 입력을 받아 서비스 계층으로 전달하는 데 사용됩니다.
 */
public class UserDto {
    private int userId;
    private int level; // 0: 관리자, 1: 학생
    private String name;
    private String studentNumber; // 학번
    private String passwordInput; // 비밀번호 입력/변경 시에만 사용 (실제 User 모델에는 해시된 값 저장)
    private LocalDate birthDate;  // 생년월일
    private int dpmtId;           // 학과 ID
    private String departmentName; // 학과명 (DB join 또는 별도 조회 후 채움)
    private int grade;            // 학년
    private String status;        // 상태 (예: "재학", "휴학", "졸업")
    private LocalDateTime createdAt; // 계정 생성일
    private LocalDateTime updatedAt; // 정보 수정일

    // 기본 생성자
    public UserDto() {}

    // User 모델 객체로부터 UserDto를 생성하는 편의 생성자 (학과명은 외부에서 주입)
    public UserDto(main.java.model.User user, String departmentName) {
        this.userId = user.getUserId();
        this.level = user.getLevel();
        this.name = user.getName();
        this.studentNumber = user.getStudentNumber();
        this.birthDate = user.getBirthDate();
        this.dpmtId = user.getDpmtId();
        this.departmentName = departmentName;
        this.grade = user.getGrade();
        this.status = user.getStatus();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }

    // Getter 및 Setter 메서드들
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStudentNumber() { return studentNumber; }
    public void setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; }

    public String getPasswordInput() { return passwordInput; }
    public void setPasswordInput(String passwordInput) { this.passwordInput = passwordInput; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public int getDpmtId() { return dpmtId; }
    public void setDpmtId(int dpmtId) { this.dpmtId = dpmtId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public int getGrade() { return grade; }
    public void setGrade(int grade) { this.grade = grade; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}