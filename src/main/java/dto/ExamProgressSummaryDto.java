package main.java.dto; // 새로운 dto 패키지를 사용하거나 기존 model 패키지 활용 가능

import main.java.model.Exam;

/**
 * 시험 진행 상황 요약 정보를 담는 DTO 클래스입니다.
 */
public class ExamProgressSummaryDto {
    private Exam exam; // 시험 객체
    private int totalAssignedStudents; // 해당 시험에 배정된 총 학생 수
    private int completedStudents; // 해당 시험을 완료한 학생 수
    private String currentExamStatus; // 시험의 현재 상태 ("예정", "진행중", "완료")

    public ExamProgressSummaryDto(Exam exam, int totalAssignedStudents, int completedStudents, String currentExamStatus) {
        this.exam = exam;
        this.totalAssignedStudents = totalAssignedStudents;
        this.completedStudents = completedStudents;
        this.currentExamStatus = currentExamStatus;
    }

    // Getter 메서드들
    public Exam getExam() {
        return exam;
    }

    public int getTotalAssignedStudents() {
        return totalAssignedStudents;
    }

    public int getCompletedStudents() {
        return completedStudents;
    }

    public String getCurrentExamStatus() {
        return currentExamStatus;
    }

    // Setter는 필요에 따라 추가
}