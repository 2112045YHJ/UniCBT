// ExamCreationContext.java
package main.java.context;

import main.java.model.Exam;
import main.java.model.QuestionFull;

import java.util.List;

public class ExamCreationContext {

    private boolean updateMode = false;
    private Exam exam;
    private List<QuestionFull> questions;
    private List<Integer> targetGrades;
    private List<Integer> targetDepartments;
    private int originalExamIdToClearAssignments = 0; // 이전 시험 ID를 저장할 필드

    public boolean isUpdateMode() {
        return updateMode;
    }

    public void setUpdateMode(boolean updateMode) {
        this.updateMode = updateMode;
    }

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

    public int getOriginalExamIdToClearAssignments() {
        return originalExamIdToClearAssignments;
    }

    public void setOriginalExamIdToClearAssignments(int originalExamIdToClearAssignments) {
        this.originalExamIdToClearAssignments = originalExamIdToClearAssignments;
    }
}