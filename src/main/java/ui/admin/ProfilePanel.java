// src/main/java/ui/admin/ProfilePanel.java

package main.java.ui.admin;

import main.java.model.User;
import main.java.service.UserService;
import main.java.service.UserServiceImpl;
import main.java.service.ServiceException;

import javax.swing.*;
import java.awt.*;

/**
 * 관리자 정보 수정 화면
 * - 로그인한 관리자(사용자)의 이름, 아이디, 비밀번호를 변경할 수 있습니다.
 */
public class ProfilePanel extends JPanel {
    private final User user;
    private final JTextField nameField;
    private final JTextField idField;
    private final JPasswordField passwordField;
    private final JButton saveButton;

    // DB 연동을 위한 서비스 객체
    private final UserService userService = new UserServiceImpl();

    public ProfilePanel(User user) {
        this.user = user;

        // 레이아웃 설정
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // 이름 라벨 + 필드
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("이름:"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(user.getName(), 20);
        add(nameField, gbc);

        // 아이디 라벨 + 필드
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel("아이디:"), gbc);
        gbc.gridx = 1;
        idField = new JTextField(user.getStudentNumber(), 20);
        add(idField, gbc);

        // 비밀번호 라벨 + 필드
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(new JLabel("비밀번호:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        passwordField.setToolTipText("변경할 경우에만 새 비밀번호를 입력하세요.");
        add(passwordField, gbc);

        // 저장 버튼
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        saveButton = new JButton("저장");
        add(saveButton, gbc);

        // 저장 버튼 클릭 이벤트
        saveButton.addActionListener(e -> {
            String newName = nameField.getText().trim();
            String newId = idField.getText().trim();
            String newPwd = new String(passwordField.getPassword()).trim();

            // 입력 검증
            if (newName.isEmpty() || newId.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "이름과 아이디를 모두 입력해주세요.",
                        "입력 오류", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                // DB 업데이트 로직 연결 (새로 추가된 서비스 메서드 호출)
                userService.updateAdminProfile(user.getUserId(), newName, newId, newPwd);

                // 성공 시, 현재 화면의 user 객체 정보도 동기화
                user.setName(newName);
                user.setStudentNumber(newId);
                // 비밀번호는 보안상 다시 채우지 않음

                JOptionPane.showMessageDialog(this,
                        "정보가 성공적으로 저장되었습니다.\n(변경된 정보는 재로그인 시 상단에 반영됩니다)",
                        "성공", JOptionPane.INFORMATION_MESSAGE);

                // 비밀번호 필드 초기화
                passwordField.setText("");

            } catch (ServiceException ex) {
                // 서비스 로직에서 발생한 예외(예: 아이디 중복) 처리
                JOptionPane.showMessageDialog(this,
                        "정보 저장 중 오류가 발생했습니다:\n" + ex.getMessage(),
                        "오류", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}