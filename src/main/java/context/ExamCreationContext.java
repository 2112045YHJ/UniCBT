package main.java.context;

import main.java.model.Exam;
import main.java.model.QuestionFull;

import java.util.List;

public class ExamCreationContext {

    private boolean updateMode = false;
    private Exam exam;
    private List<QuestionFull> questions;

    // ✅ 추가: 응시 대상 학년 및 학과 ID 목록
    private List<Integer> targetGrades;
    private List<Integer> targetDepartments;

    public Exam getExam() {
        return exam;
    }

    public void setExam(Exam exam) {
        this.exam = exam;
    }

    public List<QuestionFull> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionFull> questions) {
        this.questions = questions;
    }

    // ✅ 추가 Getter/Setter
    public List<Integer> getTargetGrades() {
        return targetGrades;
    }

    public void setTargetGrades(List<Integer> targetGrades) {
        this.targetGrades = targetGrades;
    }

    public List<Integer> getTargetDepartments() {
        return targetDepartments;
    }

    public void setTargetDepartments(List<Integer> targetDepartments) {
        this.targetDepartments = targetDepartments;
    }
    public boolean isUpdateMode() {
        return updateMode;
    }

    public void setUpdateMode(boolean updateMode) {
        this.updateMode = updateMode;
    }
}
