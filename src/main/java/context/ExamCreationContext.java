package main.java.context;

import main.java.model.Exam;
import main.java.model.QuestionFull;

import java.util.List;

public class ExamCreationContext {
    private Exam exam;
    private List<QuestionFull> questions;

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

    public void clear() {
        this.exam = null;
        this.questions = null;
    }
}
