package main.java.ui.admin.dialog; // 다이얼로그를 위한 패키지 (예시)

import main.java.dao.DaoException;
import main.java.model.Announcement;
import main.java.service.AnnouncementService;
import main.java.service.ServiceException; // ServiceException 임포트

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDateTime; // LocalDateTime 임포트

/**
 * 공지사항을 작성하거나 수정하기 위한 JDialog 입니다.
 */
public class NoticeEditorDialog extends JDialog {

    private final AnnouncementService announcementService;
    private final Announcement noticeToEdit; // 수정 대상 공지사항 (신규 작성 시 null)
    private final Runnable onSaveSuccessCallback; // 저장 성공 시 호출될 콜백 (목록 새로고침)

    // UI 컴포넌트
    private JTextField titleField;
    private JTextArea contentArea;

    private boolean isSaved = false; // 저장 성공 여부 플래그

    /**
     * 공지사항 편집 다이얼로그 생성자입니다.
     * @param owner 부모 프레임
     * @param announcementService 공지사항 서비스 객체
     * @param noticeToEdit 수정할 공지사항 객체 (새로 작성 시 null)
     * @param onSaveSuccessCallback 저장 성공 시 실행될 콜백
     */
    public NoticeEditorDialog(Frame owner, AnnouncementService announcementService, Announcement noticeToEdit, Runnable onSaveSuccessCallback) {
        super(owner, (noticeToEdit == null ? "새 공지사항 작성" : "공지사항 수정"), true); // Modal 설정
        this.announcementService = announcementService;
        this.noticeToEdit = noticeToEdit;
        this.onSaveSuccessCallback = onSaveSuccessCallback;

        setSize(600, 450); // 다이얼로그 크기
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        // JDialog의 contentPane에 직접 접근하여 여백 설정
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        initComponents();

        if (noticeToEdit != null) {
            populateFields(); // 수정 모드일 경우 기존 데이터로 필드 채우기
        }
    }

    /**
     * 다이얼로그의 UI 컴포넌트들을 초기화하고 배치합니다.
     */
    private void initComponents() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        Font labelFont = new Font("맑은 고딕", Font.BOLD, 13);
        Font fieldFont = new Font("맑은 고딕", Font.PLAIN, 13);

        // 제목
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel titleLabel = new JLabel("제목:");
        titleLabel.setFont(labelFont);
        formPanel.add(titleLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        titleField = new JTextField(30);
        titleField.setFont(fieldFont);
        formPanel.add(titleField, gbc);
        gbc.weightx = 0;

        // 내용
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.NORTHWEST; // 레이블을 상단에 위치
        JLabel contentLabel = new JLabel("내용:");
        contentLabel.setFont(labelFont);
        formPanel.add(contentLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        gbc.weighty = 1.0; // 내용 영역이 세로 공간을 차지하도록
        gbc.fill = GridBagConstraints.BOTH; // 가로 세로 모두 채움
        contentArea = new JTextArea(10, 30); // 기본 행, 열 크기
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setFont(fieldFont);
        JScrollPane contentScrollPane = new JScrollPane(contentArea);
        formPanel.add(contentScrollPane, gbc);
        gbc.weighty = 0; // 기본값으로 리셋
        gbc.fill = GridBagConstraints.HORIZONTAL; // 기본값으로 리셋

        add(formPanel, BorderLayout.CENTER);

        // 하단 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton saveButton = new JButton("저장");
        saveButton.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        JButton cancelButton = new JButton("취소");
        cancelButton.setFont(new Font("맑은 고딕", Font.PLAIN, 13));

        saveButton.addActionListener(e -> saveNotice());
        cancelButton.addActionListener(e -> {
            isSaved = false; // 저장 안됨 플래그
            dispose(); // 다이얼로그 닫기
        });

        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * 수정 모드일 때, 전달받은 Announcement 객체의 정보로 UI 필드를 채웁니다.
     */
    private void populateFields() {
        if (noticeToEdit == null) return;
        titleField.setText(noticeToEdit.getTitle());
        contentArea.setText(noticeToEdit.getContent());
    }

    /**
     * UI 필드에서 입력된 정보로 Announcement 객체를 생성하고, AnnouncementService를 통해 저장/수정합니다.
     */
    private void saveNotice() {
        String title = titleField.getText().trim();
        String content = contentArea.getText().trim();

        // 유효성 검사
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "제목을 입력해주세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
            titleField.requestFocus();
            return;
        }
        if (content.isEmpty()) {
            JOptionPane.showMessageDialog(this, "내용을 입력해주세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
            contentArea.requestFocus();
            return;
        }

        Announcement announcement;
        if (noticeToEdit == null) { // 새 공지사항 생성
            announcement = new Announcement();
            // announcement.setCreatedAt(LocalDateTime.now()); // 서비스 또는 DAO에서 설정
            // announcement.setReadCount(0); // 서비스 또는 DAO에서 설정
        } else { // 기존 공지사항 수정
            announcement = noticeToEdit; // 기존 객체 사용 (ID, 생성일 등 유지)
        }

        announcement.setTitle(title);
        announcement.setContent(content);
        // announcement.setUpdatedAt(LocalDateTime.now()); // 서비스 또는 DAO에서 설정

        try {
            if (noticeToEdit == null) {
                announcementService.createAnnouncement(announcement);
                JOptionPane.showMessageDialog(this, "공지사항이 성공적으로 등록되었습니다.", "등록 완료", JOptionPane.INFORMATION_MESSAGE);
            } else {
                announcementService.modifyAnnouncement(announcement);
                JOptionPane.showMessageDialog(this, "공지사항이 성공적으로 수정되었습니다.", "수정 완료", JOptionPane.INFORMATION_MESSAGE);
            }
            isSaved = true; // 저장 성공
            if (onSaveSuccessCallback != null) {
                onSaveSuccessCallback.run(); // NoticeMgmtPanel의 목록 새로고침
            }
            dispose(); // 다이얼로그 닫기

        } catch (ServiceException e) { // DaoException도 처리 (Service에서 래핑 안 했을 경우 대비)
            JOptionPane.showMessageDialog(this, "공지사항 저장 중 오류 발생:\n" + e.getMessage(), "저장 오류", JOptionPane.ERROR_MESSAGE);
            // e.printStackTrace(); // 개발 중 상세 오류 확인
        }
    }

    /**
     * 다이얼로그가 성공적으로 저장되었는지 여부를 반환합니다. (선택적 사용)
     * @return 저장 성공 시 true
     */
    public boolean isSaved() {
        return isSaved;
    }
}