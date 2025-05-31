package main.java.ui.client;

import main.java.model.Announcement;
import main.java.model.User; // 생성자에서 User를 받을 경우 필요
import main.java.service.AnnouncementService;
import main.java.service.AnnouncementServiceImpl;
import main.java.service.ServiceException;
import main.java.dao.DaoException; // ServiceException이 DaoException을 래핑할 수 있음
import main.java.ui.client.dialog.NoticeDetailDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 클라이언트(학생)용 공지사항 목록을 표시하는 패널입니다.
 * 시나리오 문서 Page 2, 3에 해당합니다.
 */
public class NoticePanel extends JPanel {

    private final User currentUser; // 현재 로그인한 사용자 (필요시)
    private final ClientMainFrame mainFrame; // 상세 보기 후 돌아오거나, 다른 화면 전환용
    private final AnnouncementService announcementService = new AnnouncementServiceImpl();

    private JTable noticeTable;
    private DefaultTableModel noticeTableModel;
    private List<Announcement> currentNoticeListOnPage = new ArrayList<>();

    // 페이지네이션
    private int currentPage = 1;
    private final int itemsPerPage = 7; // 시나리오 페이지 2에 보이는 항목 수 기준
    private int totalNotices = 0;
    private JLabel paginationLabel;
    private JButton prevPageButton;
    private JButton nextPageButton;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public NoticePanel(User user, ClientMainFrame mainFrame) {
        this.currentUser = user;
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initComponents();
        loadAnnouncements(); // 초기 공지사항 목록 로드
    }

    private void initComponents() {
        // 헤더 레이블
        JLabel headerLabel = new JLabel("공지사항");
        headerLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        headerLabel.setHorizontalAlignment(SwingConstants.CENTER); // 중앙 정렬
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        add(headerLabel, BorderLayout.NORTH);

        // 공지사항 목록 테이블
        String[] columnNames = {"번호", "제목", "작성자", "작성일", "조회수"};
        noticeTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 모든 셀 편집 불가
            }
        };
        noticeTable = new JTable(noticeTableModel);
        noticeTable.setRowHeight(28);
        noticeTable.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 14));
        noticeTable.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        noticeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // 컬럼 너비 설정 (예시)
        noticeTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // 번호
        noticeTable.getColumnModel().getColumn(1).setPreferredWidth(400); // 제목
        noticeTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // 작성자
        noticeTable.getColumnModel().getColumn(3).setPreferredWidth(100); // 작성일
        noticeTable.getColumnModel().getColumn(4).setPreferredWidth(70);  // 조회수

        // 테이블 행 더블 클릭 시 상세 보기 이벤트 처리
        noticeTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() == 2) { // 더블 클릭
                    JTable target = (JTable)me.getSource();
                    int row = target.getSelectedRow();
                    if (row != -1) {
                        int modelRow = noticeTable.convertRowIndexToModel(row);
                        if (modelRow >= 0 && modelRow < currentNoticeListOnPage.size()){
                            Announcement selectedNotice = currentNoticeListOnPage.get(modelRow);
                            showNoticeDetail(selectedNotice.getAnnouncementId());
                        }
                    }
                }
            }
        });


        JScrollPane scrollPane = new JScrollPane(noticeTable);
        add(scrollPane, BorderLayout.CENTER);

        // 하단 페이지네이션 컨트롤
        JPanel paginationOuterPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); // 중앙 정렬
        JPanel paginationControlsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0)); // 버튼 간 간격

        prevPageButton = new JButton("<< 이전");
        nextPageButton = new JButton("다음 >>");
        paginationLabel = new JLabel("페이지: 1 / 1");
        Font paginationFont = new Font("맑은 고딕", Font.PLAIN, 12);
        prevPageButton.setFont(paginationFont);
        nextPageButton.setFont(paginationFont);
        paginationLabel.setFont(paginationFont);

        prevPageButton.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                loadAnnouncements();
            }
        });
        nextPageButton.addActionListener(e -> {
            int totalPages = (int) Math.ceil((double) totalNotices / itemsPerPage);
            if (currentPage < totalPages) {
                currentPage++;
                loadAnnouncements();
            }
        });
        paginationControlsPanel.add(prevPageButton);
        paginationControlsPanel.add(paginationLabel);
        paginationControlsPanel.add(nextPageButton);
        paginationOuterPanel.add(paginationControlsPanel);

        add(paginationOuterPanel, BorderLayout.SOUTH);
    }

    /**
     * 서비스로부터 현재 페이지에 해당하는 공지사항 목록을 로드하여 테이블에 표시합니다.
     */
    public void loadAnnouncements() {
        noticeTableModel.setRowCount(0);
        currentNoticeListOnPage.clear();

        try {
            totalNotices = announcementService.getTotalAnnouncementCount();
            List<Announcement> noticesOnPage = announcementService.getAllAnnouncements(currentPage, itemsPerPage);
            currentNoticeListOnPage.addAll(noticesOnPage);

            // 테이블에 표시될 번호 (실제 ID가 아닌, 현재 페이지에서의 순번 + 이전 페이지 항목 수)
            int displayNumStart = (currentPage - 1) * itemsPerPage + 1;

            for (int i = 0; i < currentNoticeListOnPage.size(); i++) {
                Announcement notice = currentNoticeListOnPage.get(i);
                noticeTableModel.addRow(new Object[]{
                        // totalNotices - ((currentPage - 1) * itemsPerPage + i), // 전체 기준 역순 번호
                        displayNumStart + i, // 현재 페이지 기준 순번
                        notice.getTitle(),
                        "관리자", // 작성자
                        notice.getCreatedAt() != null ? notice.getCreatedAt().format(dateFormatter) : "-",
                        notice.getReadCount()
                });
            }
            updatePaginationControls();

        } catch (ServiceException e) {
            JOptionPane.showMessageDialog(this, "공지사항 목록 로드 중 오류: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            // e.printStackTrace();
        }
    }

    /**
     * 페이지네이션 컨트롤(버튼, 레이블) 상태를 업데이트합니다.
     */
    private void updatePaginationControls() {
        int totalPages = (int) Math.ceil((double) totalNotices / itemsPerPage);
        if (totalPages == 0) totalPages = 1;
        paginationLabel.setText("페이지: " + currentPage + " / " + totalPages);
        prevPageButton.setEnabled(currentPage > 1);
        nextPageButton.setEnabled(currentPage < totalPages);
    }

    /**
     * 선택된 공지사항의 상세 내용을 보여주는 다이얼로그를 엽니다.
     * @param announcementId 상세 내용을 볼 공지사항 ID
     */
    private void showNoticeDetail(int announcementId) {
        // NoticeDetailDialog는 다음 단계에서 상세 구현될 클래스입니다.
        NoticeDetailDialog detailDialog = new NoticeDetailDialog(
                mainFrame, // 부모 프레임 (ClientMainFrame)
                announcementService,
                announcementId,
                this::loadAnnouncements // 상세 보기 후 돌아왔을 때 조회수 반영된 목록 새로고침
        );
        detailDialog.setVisible(true);
    }
}