package main.java.dto;

/**
 * 특정 시험에 대한 학생의 상세 성적 정보를 담는 DTO 클래스입니다.
 */
public class StudentScoreDetailDto {
    private int userId;
    private String studentNumber;
    private String studentName;
    private String departmentName;
    private int score;
    private int rank; // 석차

    public StudentScoreDetailDto(int userId, String studentNumber, String studentName, String departmentName, int score, int rank) {
        this.userId = userId;
        this.studentNumber = studentNumber;
        this.studentName = studentName;
        this.departmentName = departmentName;
        this.score = score;
        this.rank = rank;
    }

    // Getter 메서드들
    public int getUserId() { return userId; }
    public String getStudentNumber() { return studentNumber; }
    public String getStudentName() { return studentName; }
    public String getDepartmentName() { return departmentName; }
    public int getScore() { return score; }
    public int getRank() { return rank; }

    // Setter (석차는 나중에 계산되어 설정될 수 있음)
    public void setRank(int rank) { this.rank = rank; }
}