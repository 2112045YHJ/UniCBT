package main.java.model;

public class ExamsDepartment {
    private int examId;
    private int dpmtId;
    private int grade;

    public ExamsDepartment() {}

    public ExamsDepartment(int examId, int dpmtId, int grade) {
        this.examId = examId;
        this.dpmtId = dpmtId;
        this.grade = grade;
    }

    public int getExamId() {
        return examId;
    }

    public void setExamId(int examId) {
        this.examId = examId;
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
}