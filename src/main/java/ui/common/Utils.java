package main.java.ui.common;

import javax.swing.*;
import java.awt.*;

/**
 * 공통 유틸리티 클래스
 * - Look & Feel 초기화
 * - 기본 폰트 제공
 */
public class Utils {
    /**
     * 시스템 Look and Feel을 적용하고, 주요 컴포넌트의 기본 폰트를 설정합니다.
     */
    public static void initLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            Font defaultFont = new Font("맑은 고딕", Font.PLAIN, 12);

            UIManager.put("Label.font", defaultFont);
            UIManager.put("Button.font", defaultFont);
            UIManager.put("TextField.font", defaultFont);
            UIManager.put("PasswordField.font", defaultFont);
            UIManager.put("TextArea.font", defaultFont);
            UIManager.put("Table.font", defaultFont);
            UIManager.put("TableHeader.font", defaultFont);
            UIManager.put("OptionPane.messageFont", defaultFont);
            UIManager.put("OptionPane.buttonFont", defaultFont);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 제목 등에 사용하기 적합한 굵은 글꼴을 반환합니다.
     */
    public static Font getTitleFont() {
        return new Font("맑은 고딕", Font.BOLD, 20);
    }

    /**
     * 보조 텍스트에 사용하기 적합한 작은 크기의 글꼴을 반환합니다.
     */
    public static Font getSmallFont() {
        return new Font("맑은 고딕", Font.PLAIN, 11);
    }
}