package main.java.app;

import javax.swing.*;
import java.awt.*;
import java.util.Enumeration;
import main.java.ui.login.LoginFrame;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            String os = System.getProperty("os.name").toLowerCase();
            String fontName = os.contains("win") ? "맑은 고딕"
                    : os.contains("mac") ? "Apple SD Gothic Neo"
                    : "NanumGothic";
            Font font = new Font(fontName, Font.PLAIN, 14);
            Enumeration<?> keys = UIManager.getDefaults().keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object val = UIManager.get(key);
                if (val instanceof Font) UIManager.put(key, font);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}