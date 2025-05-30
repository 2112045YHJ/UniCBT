package main.java.ui.admin;

import main.java.dto.StudentExamStatusDto;
import main.java.model.User;
import main.java.service.ExamService;
import main.java.service.ExamServiceImpl;
import main.java.service.ServiceException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent; // ActionEvent 임포트
import java.awt.event.ActionListener; // ActionListener 임포트
import java.util.List;
import java.util.ArrayList;

/**
 * 특정 시험에 대한 학생별 응시 상태 상세 정보를 표시하고, 상태 초기화 기능을 제공하는 패널입니다.
 */
public class ExamProgressDetailPanel extends JPanel {
    private final User adminUser;
    private final int examId;
    private final String examSubject;
    private final AdminMainFrame mainFrame;
    private final ExamService examService = new ExamServiceImpl();
    private List<StudentExamStatusDto> currentDisplayedStatuses = new ArrayList<>();
    private JTable studentStatusTable;
    private DefaultTableModel tableModel;
    private JButton revertStatusButton; // 상태 초기화 버튼

    public ExamProgressDetailPanel(User adminUser, int examId, String examSubject, AdminMainFrame mainFrame) {
        this.adminUser = adminUser;
        this.examId = examId;
        this.examSubject = examSubject;
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initComponents();
        loadStudentStatuses();
    }

    private void initComponents() {
        // 헤더 레이블
        JLabel headerLabel = new JLabel("시험 상세 현황: " + this.examSubject, SwingConstants.CENTER);
        headerLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        add(headerLabel, BorderLayout.NORTH);

        // 테이블 모델 및 컬럼 정의
        String[] columnNames = {"선택", "학번", "이름", "학과", "응시 상태"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0; // "선택" 컬럼(체크박스)만 편집 가능
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return Boolean.class;
                }
                return super.getColumnClass(columnIndex);
            }
        };
        studentStatusTable = new JTable(tableModel);
        studentStatusTable.setRowHeight(28);
        studentStatusTable.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 14));
        studentStatusTable.setFont(new Font("맑은 고딕", Font.PLAIN, 13));

        TableColumn selectionColumn = studentStatusTable.getColumnModel().getColumn(0);
        selectionColumn.setPreferredWidth(50);
        selectionColumn.setMaxWidth(60);
        studentStatusTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        studentStatusTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        studentStatusTable.getColumnModel().getColumn(3).setPreferredWidth(180);
        studentStatusTable.getColumnModel().getColumn(4).setPreferredWidth(100);

        JScrollPane scrollPane = new JScrollPane(studentStatusTable);
        add(scrollPane, BorderLayout.CENTER);

        // 하단 버튼 패널
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0)); // 내부 간격 추가
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // 상태 초기화 버튼
        revertStatusButton = new JButton("선택 학생 응시상태 초기화");
        revertStatusButton.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        revertStatusButton.setPreferredSize(new Dimension(220, 35));
        revertStatusButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                processRevertStatus();
            }
        });
        // 초기에는 비활성화, 선택된 학생이 있을 때 활성화되도록 할 수 있음 (선택사항)
        // revertStatusButton.setEnabled(false);

        JPanel leftButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftButtonPanel.add(revertStatusButton);

        // 목록으로 돌아가기 버튼
        JButton backButton = new JButton("확인 (목록으로)");
        backButton.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        backButton.setPreferredSize(new Dimension(150, 35));
        backButton.addActionListener(e -> {
            if (mainFrame != null) {
                mainFrame.cardLayout.show(mainFrame.contentPanel, "ExamProgress");
            }
        });
        JPanel rightButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightButtonPanel.add(backButton);

        bottomPanel.add(leftButtonPanel, BorderLayout.WEST);
        bottomPanel.add(rightButtonPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * 학생들의 응시 상태 데이터를 로드하여 테이블에 표시합니다.
     */
    private void loadStudentStatuses() {
        tableModel.setRowCount(0);
        this.currentDisplayedStatuses.clear(); // 리스트 초기화

        try {
            List<StudentExamStatusDto> statuses = examService.getStudentStatusesForExam(this.examId);
            this.currentDisplayedStatuses.addAll(statuses); // 멤버 변수에 저장

            if (this.currentDisplayedStatuses.isEmpty()) {
                System.out.println("로드할 학생 응시 상태 정보가 없습니다. (시험 ID: " + this.examId + ")");
            } else {
                for (StudentExamStatusDto statusDto : this.currentDisplayedStatuses) {
                    tableModel.addRow(new Object[]{
                            false,
                            statusDto.getStudentNumber(),
                            statusDto.getStudentName(),
                            statusDto.getDepartmentName(),
                            statusDto.getCompletionStatus()
                    });
                }
            }
        } catch (ServiceException e) {
            JOptionPane.showMessageDialog(this, "학생 응시 상태 조회 중 오류: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 선택된 학생들의 시험 응시 상태를 "미응시"로 초기화합니다.
     */
    private void processRevertStatus() {
        List<Integer> selectedUserIds = new ArrayList<>();
        // List<Integer> rowsToRevert = new ArrayList<>(); // UI 직접 조작 대신 테이블 전체 새로고침 사용

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Boolean isSelected = (Boolean) tableModel.getValueAt(i, 0);
            if (isSelected != null && isSelected) {
                if (i < currentDisplayedStatuses.size()) { // 행 인덱스 유효성 검사
                    StudentExamStatusDto dto = currentDisplayedStatuses.get(i); // 저장된 리스트에서 DTO 가져오기
                    if ("응시 완료".equals(dto.getCompletionStatus())) {
                        selectedUserIds.add(dto.getUserId());
                    }
                }
            }
        }

        if (selectedUserIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "상태를 초기화할 '응시 완료' 상태의 학생을 선택해주세요.", "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "선택된 " + selectedUserIds.size() + "명 학생의 응시 상태를 '미응시'로 초기화하시겠습니까?\n" +
                        "해당 학생들의 시험 결과와 답안이 모두 삭제됩니다.",
                "응시 상태 초기화 확인",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            int successCount = 0;
            for (int userIdToRevert : selectedUserIds) {
                try {
                    examService.revertExamCompletionStatus(userIdToRevert, this.examId);
                    successCount++;
                } catch (ServiceException ex) {
                    JOptionPane.showMessageDialog(this,
                            "학생(ID: " + userIdToRevert + ")의 응시 상태 초기화 중 오류 발생:\n" + ex.getMessage(),
                            "오류", JOptionPane.ERROR_MESSAGE);
                    // 모든 학생 처리 후 부분 성공/실패 여부 알림
                }
            }

            if (successCount > 0) {
                JOptionPane.showMessageDialog(this, successCount + "명 학생의 응시 상태가 성공적으로 초기화되었습니다.", "초기화 완료", JOptionPane.INFORMATION_MESSAGE);
            }
            loadStudentStatuses(); // 테이블 새로고침하여 변경된 상태 표시
        }
    }
}