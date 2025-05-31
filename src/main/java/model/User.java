package main.java.model;

import java.time.LocalDate; // 생년월일 필드를 위해 추가
import java.time.LocalDateTime; // 생성일, 수정일 필드를 위해 추가

public class User {
    private int userId;
    private int level;               // 0=관리자, 1=학생
    private String name;
    private String studentNumber;    // 학번
    private String password;         // 비밀번호
    private LocalDate birthDate;     // 생년월일 (신규 추가)
    private int dpmtId;              // 학과 ID
    private int grade;               // 학년
    private String status;           // 학생 상태 (신규 추가, 예: "재학", "휴학", "졸업")
    private LocalDateTime createdAt; // 계정 생성일 (신규 추가)
    private LocalDateTime updatedAt; // 정보 수정일 (신규 추가)
    // private boolean isActive; // is_active 필드는 status로 대체되어 삭제

    // 기본 생성자
    public User() {
    }

    // 모든 필드를 받는 생성자 (필요에 따라 조정)
    public User(int userId, int level, String name, String studentNumber, String password,
                LocalDate birthDate, int dpmtId, int grade, String status,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId = userId;
        this.level = level;
        this.name = name;
        this.studentNumber = studentNumber;
        this.password = password;
        this.birthDate = birthDate;
        this.dpmtId = dpmtId;
        this.grade = grade;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // 로그인 또는 학생 정보 표시 등 주요 정보만 받는 생성자 (예시)
    public User(int userId, int level, String name, String studentNumber,
                int dpmtId, int grade, String status) {
        this.userId = userId;
        this.level = level;
        this.name = name;
        this.studentNumber = studentNumber;
        this.dpmtId = dpmtId;
        this.grade = grade;
        this.status = status;
    }


    // Getter 및 Setter 메서드들
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public int getDpmtId() {
        return dpmtId;
    }

    public void setDpmtId(int dpmtId) {
        this.dpmtId = dpmtId;
    }

    public int getGrade() {
        return grade;
    }

    public void setGrade(int grade) {
        this.grade = grade;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // isActive() 및 setActive(boolean active) 메서드는 삭제됩니다.
    // 필요하다면 status 값을 기반으로 로그인 가능 여부 등을 판단하는 헬퍼 메서드를 추가할 수 있습니다.
    // 예: public boolean يمكنه تسجيل الدخول() { return "재학".equals(status) || "휴학".equals(status); }
}