package main.java.ui.admin;

import main.java.model.User;
import main.java.ui.admin.surveys.SurveyResultsViewerPanel;
import main.java.ui.common.BaseFrame;
import javax.swing.SwingUtilities;

/**
 * 관리자 메인 프레임
 * - BaseFrame 상속
 * - 정보 수정, 학생 계정 관리, 시험 관리, 시험 진행 관리,
 *   시험 결과 통계, 설문 조사 관리, 공지 사항 관리 화면을 CardLayout으로 전환
 */
public class AdminMainFrame extends BaseFrame {
    // 기존 MENU_LABELS와 MENU_KEYS에 "시험 결과 통계"가 이미 포함되어 있다고 가정 ("Stats" 키)
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

    private void initScreens() {
        addScreen("Profile", new ProfilePanel(user));
        addScreen("StudentMgmt", new StudentMgmtPanel(user, this));
        addScreen("ExamMgmt", new ExamMgmtPanel());
        addScreen("ExamProgress", new ExamProgressPanel(user, this));
        addScreen("Stats", new ExamStatsPanel(user, this));
        addScreen("SurveyMgmt", new SurveyMgmtPanel(user, this)); // SurveyMgmtPanel 추가
        addScreen("NoticeMgmt", new NoticeMgmtPanel(user, this));
    }
    public void showSurveyResultsViewerScreen(int surveyId, String surveyTitle) {
        String viewerScreenKey = "SurveyResultsViewer_" + surveyId;


        SurveyResultsViewerPanel viewerPanel = new SurveyResultsViewerPanel(this.user, surveyId, surveyTitle, this);
        contentPanel.add(viewerPanel, viewerScreenKey);
        cardLayout.show(contentPanel, viewerScreenKey);
    }
    /**
     * 특정 시험의 상세 진행 현황 화면으로 전환합니다. (기존 메서드)
     */
    public void showExamProgressDetailScreen(int examId, String examSubject) {
        String detailScreenKey = "ExamProgressDetail_" + examId;
        ExamProgressDetailPanel detailPanel = new ExamProgressDetailPanel(this.user, examId, examSubject, this);
        contentPanel.add(detailPanel, detailScreenKey);
        cardLayout.show(contentPanel, detailScreenKey);
    }

    /**
     * 특정 시험의 상세 결과 통계 화면으로 전환합니다.
     * @param examId 상세 통계를 조회할 시험 ID
     * @param examSubject 상세 통계를 조회할 시험 과목명 (화면 제목 등에 사용)
     */
    public void showExamSpecificStatsScreen(int examId, String examSubject) {
        String statsScreenKey = "ExamSpecificStats_" + examId; // 각 시험 상세 통계 화면을 위한 고유 키

        // ExamSpecificStatsPanel은 아직 생성하지 않았으므로, 우선 플레이스홀더로 처리하거나
        // 다음 단계에서 이 패널을 만들고 아래 코드를 활성화합니다.
        ExamSpecificStatsPanel statsPanel = new ExamSpecificStatsPanel(this.user, examId, examSubject, this);
        contentPanel.add(statsPanel, statsScreenKey);
        cardLayout.show(contentPanel, statsScreenKey);

        // JOptionPane.showMessageDialog(this, "선택된 시험: " + examSubject + " (ID: " + examId + ")\n상세 통계 화면으로 이동합니다.");
    }

    public static void main(String[] args) {

    }
}
