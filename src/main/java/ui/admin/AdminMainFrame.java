package main.java.ui.admin;

import main.java.model.User;
import main.java.ui.common.BaseFrame;
import javax.swing.SwingUtilities;

/**
 * 관리자 메인 프레임
 * - BaseFrame 상속
 * - 정보 수정, 학생 계정 관리, 시험 관리, 시험 진행 관리,
 *   시험 결과 통계, 설문 조사 관리, 공지 사항 관리 화면을 CardLayout으로 전환
 */
public class AdminMainFrame extends BaseFrame {
    private static final String[] MENU_LABELS = {
            "정보 수정", "학생 계정 관리", "시험 관리",
            "시험 진행 관리", "시험 결과 통계", "설문 조사 관리", "공지 사항 관리"
    };
    private static final String[] MENU_KEYS = {
            "Profile", "StudentMgmt", "ExamMgmt",
            "ExamProgress", "Stats", "SurveyMgmt", "NoticeMgmt"
    };

    public AdminMainFrame(User user) {
        super(user, "관리자", MENU_LABELS, MENU_KEYS);
        initScreens();
    }

    /**
     * BaseFrame의 contentPanel에 각 화면을 등록
     */
    private void initScreens() {
        addScreen("Profile", new ProfilePanel(user));
//        addScreen("StudentMgmt", new StudentMgmtPanel(user));
        addScreen("ExamMgmt", new ExamMgmtPanel());
//        addScreen("ExamProgress", new ExamProgressPanel(user));
//        addScreen("Stats", new ExamStatsPanel(user));
//        addScreen("SurveyMgmt", new SurveyMgmtPanel(user));
//        addScreen("NoticeMgmt", new NoticeMgmtPanel(user));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // 테스트용 더미 관리자 사용자
            User dummy = new User(0, 0, "관리자", "admin", 0, 0, true);
            new AdminMainFrame(dummy).setVisible(true);
        });
    }
}
