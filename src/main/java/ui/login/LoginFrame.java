package main.java.ui.login;

import main.java.dao.UserDao;
import main.java.dao.UserDaoImpl;
import main.java.dao.DaoException;
import main.java.model.User;
import main.java.ui.common.Utils;
import main.java.ui.client.ClientMainFrame;
import main.java.ui.admin.AdminMainFrame;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private final UserDao userDao = new UserDaoImpl();

    private JTextField studentField;
    private JPasswordField passwordField;
    private JButton loginBtn;

    public LoginFrame() {
        initComponents();
    }

    private void initComponents() {
        setTitle("OO대학교 CBT 시스템 로그인");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(450, 300);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // ── 상단 로고 & 타이틀 ──
        JPanel north = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        north.add(new JLabel(new ImageIcon("resources/logo.png")));
        JLabel title = new JLabel("OO대학교 CBT 시스템");
        title.setFont(Utils.getTitleFont());
        north.add(title);
        add(north, BorderLayout.NORTH);

        // ── 중앙 폼 ──
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // 학번 입력
        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("학번:"), gbc);
        gbc.gridx = 1;
        studentField = new JTextField(15);
        form.add(studentField, gbc);

        // 비밀번호 입력
        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("비밀번호:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        form.add(passwordField, gbc);

        // ID/PW 문의
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JLabel help = new JLabel("ID/PW 문의: 123-456-7890");
        help.setFont(Utils.getSmallFont());
        form.add(help, gbc);

        add(form, BorderLayout.CENTER);

        // ── 하단 버튼 ──
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        loginBtn = new JButton("로그인");
        loginBtn.setPreferredSize(new Dimension(100, 30));
        south.add(loginBtn);
        add(south, BorderLayout.SOUTH);

        // ── 이벤트 ──
        loginBtn.addActionListener(e -> attemptLogin());
        getRootPane().setDefaultButton(loginBtn);
    }

    private void attemptLogin() {
        String student = studentField.getText().trim();
        String pwd = new String(passwordField.getPassword()).trim();

        if (student.isEmpty() || pwd.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "학번과 비밀번호를 모두 입력해주세요.",
                    "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            User user = userDao.findByStudentNumberAndPassword(student, pwd);
            if (user == null) {
                JOptionPane.showMessageDialog(this,
                        "학번 또는 비밀번호가 올바르지 않거나, 계정이 비활성화되었습니다.",
                        "로그인 실패", JOptionPane.ERROR_MESSAGE);
            } else {
                // 로그인 성공 → 역할별 메인 화면 호출
                dispose();
                if (user.getLevel() == 0) {
                    // 관리자
                    SwingUtilities.invokeLater(() ->
                            new AdminMainFrame(user).setVisible(true));
                } else {
                    // 학생(클라이언트)
                    SwingUtilities.invokeLater(() ->
                            new ClientMainFrame(user).setVisible(true));
                }
            }
        } catch (DaoException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "로그인 중 오류가 발생했습니다:\n" + ex.getMessage(),
                    "서버 오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // 전체 Look & Feel 설정은 Utils 클래스에서 처리
            Utils.initLookAndFeel();
            new LoginFrame().setVisible(true);
        });
    }
}

