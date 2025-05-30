package main.java.dto;

/**
 * 특정 시험에 대한 학과별 통계 정보를 담는 DTO 클래스입니다.
 */
public class DepartmentExamStatDto {
    private int departmentId;       // 학과 ID
    private String departmentName;  // 학과 이름
    private int participantCount;   // 해당 학과 응시자 수
    private double averageScore;    // 해당 학과 평균 점수
    private int highestScore;       // 해당 학과 최고 점수
    private int lowestScore;        // 해당 학과 최저 점수

    public DepartmentExamStatDto(int departmentId, String departmentName, int participantCount, double averageScore, int highestScore, int lowestScore) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.participantCount = participantCount;
        this.averageScore = averageScore;
        this.highestScore = highestScore;
        this.lowestScore = lowestScore;
    }

    // Getter 메서드들
    public int getDepartmentId() {
        return departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public int getParticipantCount() {
        return participantCount;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public int getHighestScore() {
        return highestScore;
    }

    public int getLowestScore() {
        return lowestScore;
    }

    // Setter는 필요에 따라 추가 (예: 통계 계산 중 값 설정)
}