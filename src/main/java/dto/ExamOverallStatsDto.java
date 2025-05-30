package main.java.dto;

import main.java.model.Exam;

/**
 * 시험별 전체 결과 통계 요약 정보를 담는 DTO 클래스입니다.
 */
public class ExamOverallStatsDto {
    private Exam exam;
    private int totalParticipants; // 실제 응시자 수
    private int totalAssignedOrEligible; // 배정 또는 응시 가능했던 총 학생 수 (선택적)
    private double averageScore;   // 평균 점수
    private String currentExamStatus; // 시험의 현재 상태 ("예정", "진행중", "완료")


    public ExamOverallStatsDto(Exam exam, int totalParticipants, int totalAssignedOrEligible, double averageScore, String currentExamStatus) {
        this.exam = exam;
        this.totalParticipants = totalParticipants;
        this.totalAssignedOrEligible = totalAssignedOrEligible;
        this.averageScore = averageScore;
        this.currentExamStatus = currentExamStatus;
    }

    // Getter 메서드들
    public Exam getExam() { return exam; }
    public int getTotalParticipants() { return totalParticipants; }
    public int getTotalAssignedOrEligible() { return totalAssignedOrEligible; }
    public double getAverageScore() { return averageScore; }
    public String getCurrentExamStatus() { return currentExamStatus; }
}