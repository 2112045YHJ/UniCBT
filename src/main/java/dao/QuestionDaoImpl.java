package main.java.dao;

import main.java.model.*;
import java.util.List;
import java.util.ArrayList;

public class QuestionDaoImpl implements QuestionDao {
    private final QuestionBankDao qbDao = new QuestionBankDaoImpl();
    private final QuestionOptionDao qoDao = new QuestionOptionDaoImpl();
    private final AnswerKeyDao akDao = new AnswerKeyDaoImpl();

    @Override
    public List<QuestionFull> findFullByExamId(int examId) throws DaoException {
        List<QuestionBank> banks = qbDao.findByExamId(examId);
        List<QuestionFull> fullList = new ArrayList<>();
        for (QuestionBank qb : banks) {
            int qId = qb.getQuestionId();
            List<QuestionOption> opts = qoDao.findByQuestionId(qId);
            AnswerKey ak = akDao.findByQuestionId(qId);
            fullList.add(new QuestionFull(qb, opts, ak));
        }
        return fullList;
    }
}
