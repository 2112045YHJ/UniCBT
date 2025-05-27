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
//        addScreen("Notice", new NoticePanel(user));
//        addScreen("Survey", new SurveyPanel(user));
        addScreen("Exam", new ExamListPanel(user));
        addScreen("Result", new ResultListPanel(user));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // 테스트용 더미 사용자
            User dummy = new User(1, 1, "홍길동", "2023001", 101, 3, true);
            new ClientMainFrame(dummy).setVisible(true);
        });
    }
}
