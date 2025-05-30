package main.java.dao;

import main.java.model.*;
import main.java.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

public class QuestionDaoImpl implements QuestionDao {
    // QuestionDaoImpl은 다른 DAO들을 사용하므로,
    // 이 DAO들의 인스턴스를 생성하거나 주입받아야 합니다.
    // 그리고 Connection을 받는 메서드를 호출해야 합니다.
    private final QuestionBankDao qbDao;
    private final QuestionOptionDao qoDao;
    private final AnswerKeyDao akDao;

    public QuestionDaoImpl() {
        // 실제 애플리케이션에서는 의존성 주입(DI)을 사용하는 것이 좋습니다.
        this.qbDao = new QuestionBankDaoImpl();
        this.qoDao = new QuestionOptionDaoImpl();
        this.akDao = new AnswerKeyDaoImpl();
    }

    // 생성자를 통해 DAO 인스턴스를 주입받는 방식 (권장)
    public QuestionDaoImpl(QuestionBankDao qbDao, QuestionOptionDao qoDao, AnswerKeyDao akDao) {
        this.qbDao = qbDao;
        this.qoDao = qoDao;
        this.akDao = akDao;
    }


    // --- Connection을 파라미터로 받는 메서드 구현 ---
    @Override
    public List<QuestionFull> findFullByExamId(int examId, Connection conn) throws DaoException {
        // 이 메서드는 전달받은 Connection을 하위 DAO들에게 전달해야 합니다.
        List<QuestionBank> banks = qbDao.findByExamId(examId, conn);
        List<QuestionFull> fullList = new ArrayList<>();
        for (QuestionBank qb : banks) {
            int qId = qb.getQuestionId();
            // QuestionOptionDao와 AnswerKeyDao의 findByQuestionId 메서드도 Connection을 받아야 함
            List<QuestionOption> opts = null;
            if (QuestionType.valueOf(qb.getType()) == QuestionType.MCQ) { // MCQ 유형일 때만 옵션 조회
                opts = qoDao.findByQuestionId(qId, conn);
            }
            AnswerKey ak = akDao.findByQuestionId(qId, conn);

            QuestionFull qf = new QuestionFull();
            qf.setQuestionBank(qb);
            qf.setOptions(opts); // MCQ가 아니면 opts는 null
            qf.setAnswerKey(ak);

            // QuestionFull DTO에 직접 값을 설정하는 로직 추가 (편의성을 위해)
            // QuestionFull의 내부 로직이 QuestionBank, AnswerKey 등을 통해 값을 가져온다면 이 부분은 생략 가능
            qf.setQuestionText(qb.getQuestionText());
            qf.setType(QuestionType.valueOf(qb.getType()));
            if (ak != null) {
                if (qf.getType() == QuestionType.MCQ && ak.getCorrectLabel() != null) {
                    qf.setCorrectLabel(String.valueOf(ak.getCorrectLabel()));
                } else if (qf.getType() == QuestionType.OX && ak.getCorrectText() != null) {
                    qf.setCorrectText(ak.getCorrectText());
                }
            }
            fullList.add(qf);
        }
        return fullList;
    }

    // --- 기존 시그니처 메서드 구현 ---
    @Override
    public List<QuestionFull> findFullByExamId(int examId) throws DaoException {
        try (Connection conn = DBConnection.getConnection()) {
            return findFullByExamId(examId, conn);
        } catch (SQLException e) {
            throw new DaoException("DB 연결 실패 또는 시험별 전체 문제 조회 중 오류", e);
        }
    }
}