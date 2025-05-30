package main.java.dto; // 새로운 dto 패키지를 사용하거나 기존 model 패키지 활용 가능

/**
 * 특정 시험에 대한 학생의 응시 상태 정보를 담는 DTO 클래스입니다.
 */
public class StudentExamStatusDto {
    private int userId;             // 사용자 ID
    private String studentName;     // 학생 이름
    private String studentNumber;   // 학번
    private String departmentName;  // 학과 이름
    private String completionStatus; // 응시 상태 ("응시 완료", "미응시")

    public StudentExamStatusDto(int userId, String studentName, String studentNumber, String departmentName, String completionStatus) {
        this.userId = userId;
        this.studentName = studentName;
        this.studentNumber = studentNumber;
        this.departmentName = departmentName;
        this.completionStatus = completionStatus;
    }

    // Getter 메서드들
    public int getUserId() {
        return userId;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getCompletionStatus() {
        return completionStatus;
    }

    // Setter는 필요에 따라 추가
}