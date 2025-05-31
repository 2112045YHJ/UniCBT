package main.java.ui.client.dialog; // 학생 UI의 다이얼로그를 위한 새 패키지 또는 기존 client 패키지 사용

import main.java.model.Announcement;
import main.java.service.AnnouncementService;
import main.java.service.ServiceException; // ServiceException 임포트

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;

/**
 * 학생용 공지사항 상세 내용을 보여주는 JDialog 입니다.
 * 이 다이얼로그가 열릴 때 해당 공지사항의 조회수가 증가합니다.
 */
public class NoticeDetailDialog extends JDialog {

    private final AnnouncementService announcementService;
    private final int announcementId;
    private final Runnable onDisposeCallback; // 다이얼로그 닫힐 때 호출될 콜백 (예: 목록 새로고침)

    private JLabel titleLabel;
    private JLabel authorAndDateLabel;
    private JLabel readCountLabel;
    private JTextPane contentTextPane; // HTML 렌더링 또는 스타일 적용을 위해 JTextPane 사용 가능

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public NoticeDetailDialog(Frame owner, AnnouncementService announcementService, int announcementId, Runnable onDisposeCallback) {
        super(owner, "공지사항 상세 보기", true); // Modal 설정
        this.announcementService = announcementService;
        this.announcementId = announcementId;
        this.onDisposeCallback = onDisposeCallback;

        setSize(600, 500); // 다이얼로그 크기
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        initComponents();
        loadNoticeDetails(); // 공지사항 상세 정보 로드 및 조회수 증가
    }

    private void initComponents() {
        // 상단: 제목
        titleLabel = new JLabel("제목 로딩 중...", SwingConstants.LEFT);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        add(titleLabel, BorderLayout.NORTH);

        // 중앙: 내용 (스크롤 가능하도록)
        contentTextPane = new JTextPane();
        contentTextPane.setEditable(false);
        contentTextPane.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        // contentTextPane.setContentType("text/html"); // HTML 사용 시
        JScrollPane contentScrollPane = new JScrollPane(contentTextPane);
        add(contentScrollPane, BorderLayout.CENTER);

        // 하단: 작성자/작성일, 조회수, 닫기 버튼
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 3)); // 정보 2줄
        authorAndDateLabel = new JLabel("작성자: - | 작성일: -");
        authorAndDateLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        readCountLabel = new JLabel("조회수: -");
        readCountLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        infoPanel.add(authorAndDateLabel);
        infoPanel.add(readCountLabel);
        bottomPanel.add(infoPanel, BorderLayout.WEST);

        JButton closeButton = new JButton("닫기");
        closeButton.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        closeButton.addActionListener(e -> {
            if (onDisposeCallback != null) {
                onDisposeCallback.run(); // 목록 새로고침 콜백 실행
            }
            dispose(); // 다이얼로그 닫기
        });
        JPanel buttonWrapperPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT)); // 버튼 오른쪽 정렬
        buttonWrapperPanel.add(closeButton);
        bottomPanel.add(buttonWrapperPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * AnnouncementService를 통해 공지사항 상세 정보를 로드하고 UI에 표시합니다.
     * 이 과정에서 조회수가 증가됩니다.
     */
    private void loadNoticeDetails() {
        try {
            // getAnnouncementDetails 서비스 메서드는 내부적으로 조회수 증가 로직을 포함해야 함
            Announcement notice = announcementService.getAnnouncementDetails(announcementId);

            if (notice != null) {
                setTitle("공지: " + notice.getTitle()); // 다이얼로그 제목도 업데이트
                titleLabel.setText(notice.getTitle());
                // contentTextPane.setText(notice.getContent()); // 일반 텍스트용
                // HTML을 사용하려면:
                // contentTextPane.setText("<html><body style='font-family:\"맑은 고딕\"; font-size:10pt;'>" + notice.getContent().replace("\n", "<br>") + "</body></html>");
                // JTextPane에 일반 텍스트 설정 (줄바꿈은 JTextArea처럼 자동)
                contentTextPane.setText(notice.getContent());


                authorAndDateLabel.setText("작성자: 관리자 | 작성일: " + (notice.getCreatedAt() != null ? notice.getCreatedAt().format(dateTimeFormatter) : "-"));
                readCountLabel.setText("조회수: " + notice.getReadCount());
            } else {
                titleLabel.setText("공지사항을 찾을 수 없습니다.");
                contentTextPane.setText("해당 ID의 공지사항이 존재하지 않거나 삭제되었습니다.");
                authorAndDateLabel.setText("");
                readCountLabel.setText("");
            }
        } catch (ServiceException e) {
            JOptionPane.showMessageDialog(this, "공지사항 상세 정보를 불러오는 중 오류가 발생했습니다:\n" + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            titleLabel.setText("오류 발생");
            contentTextPane.setText("공지사항을 불러올 수 없습니다.");
            // e.printStackTrace(); // 개발 중 상세 오류 확인
        }
    }

    // JDialog가 닫힐 때 콜백이 항상 호출되도록 하려면 WindowListener를 사용할 수도 있습니다.
    // @Override
    // public void dispose() {
    //     if (onDisposeCallback != null) {
    //         onDisposeCallback.run();
    //     }
    //     super.dispose();
    // }
}