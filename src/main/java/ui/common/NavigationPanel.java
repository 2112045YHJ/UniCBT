package main.java.ui.common;

import javax.swing.*;
import java.awt.*;

/**
 * 공통 네비게이션 패널
 * - BoxLayout(Y_AXIS) 기반 우측 메뉴바
 * - CardLayout 기반 contentPanel 전환 지원
 */
public class NavigationPanel extends JPanel {
    /**
     * @param labels       버튼에 표시할 텍스트 배열
     * @param keys         CardLayout에 등록된 화면 키 배열 (labels 순서와 동일)
     * @param contentPanel CardLayout이 적용된 콘텐츠 패널
     * @param cardLayout   contentPanel에 설정된 CardLayout
     */
    public NavigationPanel(String[] labels, String[] keys, JPanel contentPanel, CardLayout cardLayout) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));

        for (int i = 0; i < labels.length; i++) {
            String text = labels[i];
            String key = keys[i];
            JButton btn = new JButton(text);
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setMaximumSize(new Dimension(120, 30));
            btn.addActionListener(e -> cardLayout.show(contentPanel, key));
            add(btn);
            add(Box.createVerticalStrut(10));
        }
    }
}