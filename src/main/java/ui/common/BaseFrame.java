package main.java.ui.common;

import main.java.model.User;
import javax.swing.*;
import java.awt.*;

/**
 * 공통 프레임 골격 (로고/헤더, 네비게이션, 콘텐츠 영역)
 * 클라이언트/관리자 메인 프레임이 상속하여 사용
 */
public abstract class BaseFrame extends JFrame {
    protected final User user;
    public final CardLayout cardLayout = new CardLayout();
    public final JPanel contentPanel = new JPanel(cardLayout);

    /**
     * @param user        로그인한 사용자 정보
     * @param roleName    프레임 제목에 붙일 역할명 (예: "학생", "관리자")
     * @param menuLabels  우측 메뉴 버튼에 표시할 텍스트 배열
     * @param menuKeys    각 메뉴에 대응하는 CardLayout 키 배열
     */
    protected BaseFrame(User user, String roleName, String[] menuLabels, String[] menuKeys) {
        this.user = user;
        initLookAndFeel();
        setupFrame(roleName);
        initHeader();
        initNavigation(menuLabels, menuKeys);
        initContentArea();
    }
    /** Look and Feel 초기화 */
    private void initLookAndFeel() {
        Utils.initLookAndFeel();
    }

    /** JFrame 속성 설정 */
    private void setupFrame(String roleName) {
        setTitle(String.format("OO대학교 CBT 시스템 - %s", roleName));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
    }

    /** 상단 헤더 생성: 로고, 시스템명, 사용자 정보 */
    private void initHeader() {
        JPanel header = new JPanel(new BorderLayout());
        // 로고 + 시스템명
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        left.add(new JLabel(new ImageIcon("resources/logo.png")));
        JLabel title = new JLabel("OO대학교 CBT 시스템");
        title.setFont(Utils.getTitleFont());
        left.add(title);
        header.add(left, BorderLayout.WEST);
        // 사용자 정보
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JLabel info = new JLabel(String.format("%s · %s · %d학년 · 학과코드:%d",
                user.getName(), user.getStudentNumber(), user.getGrade(), user.getDpmtId()));
        info.setFont(Utils.getSmallFont());
        right.add(info);
        header.add(right, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
    }

    /** 우측 네비게이션 파널 생성 */
    private void initNavigation(String[] labels, String[] keys) {
        NavigationPanel navPanel = new NavigationPanel(labels, keys, contentPanel, cardLayout);
        add(navPanel, BorderLayout.EAST);
    }

    /** 중앙 콘텐츠 영역 초기화 */
    private void initContentArea() {
        add(contentPanel, BorderLayout.CENTER);
    }

    /** 자식 클래스에서 호출하여 화면을 등록 */
    protected void addScreen(String key, JPanel panel) {
        contentPanel.add(panel, key);
    }

    /** 프로그램 종료 */
    protected void close() {
        dispose();
    }
}
