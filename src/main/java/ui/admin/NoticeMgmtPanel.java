package main.java.ui.admin;

import main.java.model.Announcement;
import main.java.model.User; // adminUser 타입
import main.java.service.AnnouncementService;
import main.java.service.AnnouncementServiceImpl;
import main.java.service.ServiceException;
import main.java.dao.DaoException;
import main.java.ui.admin.dialog.NoticeEditorDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 관리자용 공지사항 관리 패널입니다.
 * 공지사항 목록 조회, 생성, 수정, 삭제 기능을 제공합니다.
 * 시나리오 문서 Page 38, 39에 해당합니다.
 */
public class NoticeMgmtPanel extends JPanel {

    private final User adminUser;
    private final AdminMainFrame mainFrame; // 다이얼로그 부모 또는 화면 전환용
    private final AnnouncementService announcementService = new AnnouncementServiceImpl();

    private JTable noticeTable;
    private DefaultTableModel noticeTableModel;
    private List<Announcement> currentNoticeListOnPage = new ArrayList<>(); // 현재 페이지에 표시된 공지사항 목록

    // 페이지네이션 관련 컴포넌트 및 변수
    private int currentPage = 1;
    private final int itemsPerPage = 10; // 페이지 당 보여줄 공지사항 수
    private int totalNotices = 0;       // 전체 공지사항 수
    private JLabel paginationLabel;
    private JButton prevPageButton;
    private JButton nextPageButton;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public NoticeMgmtPanel(User adminUser, AdminMainFrame mainFrame) {
        this.adminUser = adminUser;
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initComponents();
        loadNotices(); // 초기 공지사항 목록 로드
    }

    /**
     * UI 컴포넌트를 초기화하고 배치합니다.
     */
    private void initComponents() {
        // 헤더 레이블
        JLabel headerLabel = new JLabel("공지사항 관리");
        headerLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        add(headerLabel, BorderLayout.NORTH);

        // 공지사항 목록 테이블
        String[] columnNames = {"ID", "제목", "작성자", "작성일", "조회수", "수정", "삭제"};
        noticeTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // "수정"(인덱스 5), "삭제"(인덱스 6) 버튼 컬럼만 상호작용 가능하도록 설정
                return column == 5 || column == 6;
            }

            @Override
            public void setValueAt(Object aValue, int row, int column) {
                // 버튼 컬럼(수정, 삭제)에 대해서는 모델에 값을 저장하지 않음
                if (column == 5 || column == 6) {
                    // System.out.println("setValueAt 호출됨 (버튼 컬럼 " + column + "), 무시함.");
                    return; // 값을 설정하지 않고 반환하여 예외 방지
                }
                super.setValueAt(aValue, row, column);
            }
        };
        noticeTable = new JTable(noticeTableModel);
        noticeTable.setRowHeight(28);
        noticeTable.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 14));
        noticeTable.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        noticeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // 단일 행 선택

        // 컬럼 너비 설정 (예시)
        TableColumn idCol = noticeTable.getColumnModel().getColumn(0);
        idCol.setPreferredWidth(50);  idCol.setMaxWidth(70);
        noticeTable.getColumnModel().getColumn(1).setPreferredWidth(350); // 제목
        noticeTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // 작성자
        noticeTable.getColumnModel().getColumn(3).setPreferredWidth(120); // 작성일
        noticeTable.getColumnModel().getColumn(4).setPreferredWidth(70);  // 조회수

        // 버튼 컬럼 렌더러 및 에디터 설정
        NoticeListButtonRenderer buttonRenderer = new NoticeListButtonRenderer();
        TableColumn modifyColumn = noticeTable.getColumn("수정");
        modifyColumn.setCellRenderer(buttonRenderer);
        modifyColumn.setCellEditor(new NoticeListButtonEditor(new JCheckBox(), "수정"));
        modifyColumn.setPreferredWidth(80);

        TableColumn deleteColumn = noticeTable.getColumn("삭제");
        deleteColumn.setCellRenderer(buttonRenderer);
        deleteColumn.setCellEditor(new NoticeListButtonEditor(new JCheckBox(), "삭제"));
        deleteColumn.setPreferredWidth(80);

        JScrollPane scrollPane = new JScrollPane(noticeTable);
        add(scrollPane, BorderLayout.CENTER);

        // 하단 패널: 글쓰기 버튼 및 페이지네이션 컨트롤
        JPanel bottomOuterPanel = new JPanel(new BorderLayout());

        JPanel addNoticePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addNoticeButton = new JButton("새 공지사항 작성 (글쓰기)");
        addNoticeButton.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        addNoticeButton.addActionListener(e -> openNoticeEditor(null)); // 새 공지 작성
        addNoticePanel.add(addNoticeButton);
        bottomOuterPanel.add(addNoticePanel, BorderLayout.WEST);

        JPanel paginationControlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
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
                loadNotices();
            }
        });
        nextPageButton.addActionListener(e -> {
            int totalPages = (int) Math.ceil((double) totalNotices / itemsPerPage);
            if (currentPage < totalPages) {
                currentPage++;
                loadNotices();
            }
        });
        paginationControlsPanel.add(prevPageButton);
        paginationControlsPanel.add(paginationLabel);
        paginationControlsPanel.add(nextPageButton);
        bottomOuterPanel.add(paginationControlsPanel, BorderLayout.EAST);

        add(bottomOuterPanel, BorderLayout.SOUTH);
    }

    /**
     * 서비스로부터 현재 페이지에 해당하는 공지사항 목록을 로드하여 테이블에 표시합니다.
     */
    public void loadNotices() {
        noticeTableModel.setRowCount(0); // 테이블 데이터 초기화
        currentNoticeListOnPage.clear(); // 현재 페이지 목록 초기화

        try {
            // 서비스 계층에서 전체 공지사항 수와 현재 페이지의 공지사항 목록을 가져옴
            totalNotices = announcementService.getTotalAnnouncementCount();
            List<Announcement> noticesOnPage = announcementService.getAllAnnouncements(currentPage, itemsPerPage);
            currentNoticeListOnPage.addAll(noticesOnPage);

            for (Announcement notice : currentNoticeListOnPage) {
                noticeTableModel.addRow(new Object[]{
                        notice.getAnnouncementId(),
                        notice.getTitle(),
                        "관리자", // 작성자는 현재 시스템에서 항상 "관리자"
                        notice.getCreatedAt() != null ? notice.getCreatedAt().format(dateFormatter) : "-",
                        notice.getReadCount(),
                        "수정", // 버튼에 표시될 텍스트
                        "삭제"  // 버튼에 표시될 텍스트
                });
            }
            updatePaginationControls(); // 페이지네이션 UI 업데이트

        } catch (ServiceException e) { // DaoException도 ServiceException으로 래핑되었다고 가정
            JOptionPane.showMessageDialog(this, "공지사항 목록 로드 중 오류: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace(); // 개발 중 상세 오류 확인
        }
    }

    /**
     * 페이지네이션 컨트롤(버튼, 레이블) 상태를 업데이트합니다.
     */
    private void updatePaginationControls() {
        int totalPages = (int) Math.ceil((double) totalNotices / itemsPerPage);
        if (totalPages == 0) totalPages = 1; // 공지사항이 없어도 1페이지로 표시
        paginationLabel.setText("페이지: " + currentPage + " / " + totalPages);
        prevPageButton.setEnabled(currentPage > 1);
        nextPageButton.setEnabled(currentPage < totalPages);
    }

    /**
     * 공지사항 편집 다이얼로그를 엽니다.
     * @param noticeToEdit 수정할 Announcement 객체 (새로 생성 시 null)
     */
    private void openNoticeEditor(Announcement noticeToEdit) {
        NoticeEditorDialog editorDialog = new NoticeEditorDialog(
                mainFrame, // 부모 프레임 (AdminMainFrame)
                announcementService,
                noticeToEdit,
                this::loadNotices // 저장 성공 시 공지사항 목록 새로고침 콜백
        );
        editorDialog.setVisible(true);
    }

    /**
     * 공지사항 목록 테이블의 버튼 렌더러입니다.
     */
    private static class NoticeListButtonRenderer extends JButton implements TableCellRenderer {
        public NoticeListButtonRenderer() {
            setOpaque(true);
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    /**
     * 공지사항 목록 테이블의 버튼("수정", "삭제") 에디터입니다.
     */
    private class NoticeListButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String actionCommand; // 버튼에 표시될 텍스트이자, 수행할 액션 구분
        private boolean isPushed;
        private int selectedRowInView; // JTable 뷰에서의 행 인덱스

        public NoticeListButtonEditor(JCheckBox checkBox, String actionCommand) {
            super(checkBox);
            this.actionCommand = actionCommand;
            button = new JButton(actionCommand); // 버튼 텍스트를 초기 actionCommand로 설정
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            // value는 DefaultTableModel에 저장된 값 (예: "수정", "삭제")
            // button.setText((value == null) ? "" : value.toString()); // 이 방식도 가능
            button.setText(actionCommand); // 일관성을 위해 actionCommand 사용
            this.selectedRowInView = row;
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                // JTable의 정렬/필터링 상태를 고려하여 실제 모델의 인덱스를 가져옴
                int modelRow = noticeTable.convertRowIndexToModel(selectedRowInView);
                // currentNoticeListOnPage는 현재 화면에 보이는 페이지의 공지사항 목록
                if (modelRow >= 0 && modelRow < currentNoticeListOnPage.size()) {
                    Announcement selectedNotice = currentNoticeListOnPage.get(modelRow);

                    if ("수정".equals(actionCommand)) {
                        openNoticeEditor(selectedNotice);
                    } else if ("삭제".equals(actionCommand)) {
                        int confirm = JOptionPane.showConfirmDialog(NoticeMgmtPanel.this,
                                "공지사항 \"" + selectedNotice.getTitle() + "\"\n(ID: " + selectedNotice.getAnnouncementId() + ") 을(를) 삭제하시겠습니까?",
                                "공지사항 삭제 확인", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        if (confirm == JOptionPane.YES_OPTION) {
                            try {
                                announcementService.removeAnnouncement(selectedNotice.getAnnouncementId());
                                JOptionPane.showMessageDialog(NoticeMgmtPanel.this, "공지사항이 성공적으로 삭제되었습니다.");
                                loadNotices(); // 목록 새로고침
                            } catch (ServiceException ex) {
                                JOptionPane.showMessageDialog(NoticeMgmtPanel.this, "공지사항 삭제 중 오류 발생: " + ex.getMessage(), "삭제 오류", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                }
            }
            isPushed = false;
            return actionCommand; // 버튼 텍스트 (또는 실제 셀에 저장될 값) 반환
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }
}