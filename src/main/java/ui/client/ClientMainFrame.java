package main.java.ui.client;

import main.java.model.User;
import main.java.ui.common.BaseFrame;
import main.java.ui.client.NoticePanel;
import main.java.ui.client.SurveyPanel;
import main.java.ui.client.ExamListPanel;
import main.java.ui.client.ResultListPanel;
import javax.swing.SwingUtilities;
import java.awt.*;

/**
 * 클라이언트(학생) 메인 프레임
 * - BaseFrame 상속
 * - 공지, 설문, 시험 응시, 결과 조회 화면을 CardLayout으로 전환
 */
public class ClientMainFrame extends BaseFrame {
    private static final String[] MENU_LABELS = { "공지 사항", "설문 조사", "시험 응시", "시험 결과" };
    private static final String[] MENU_KEYS   = { "Notice",   "Survey",   "Exam",     "Result" };


    public ClientMainFrame(User user) {
        super(user, "학생", MENU_LABELS, MENU_KEYS);
        initScreens();
    }

    /**
     * BaseFrame의 contentPanel에 각 화면을 등록
     */
    private void initScreens() {
        addScreen("Notice", new NoticePanel(user, this));
        addScreen("Survey", new SurveyPanel(user, this));
        addScreen("Exam", new ExamListPanel(user));
        addScreen("Result", new ResultListPanel(user));
    }

    public void switchToResultsPanel() {
        if (cardLayout != null && contentPanel != null) {
            cardLayout.show(contentPanel, "Result"); // "Result"는 ResultListPanel의 키
        }
    }

}
