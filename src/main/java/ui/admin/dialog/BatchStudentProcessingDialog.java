// src/main/java/ui/admin/dialog/BatchStudentProcessingDialog.java

package main.java.ui.admin.dialog; // StudentEditorDialog와 같은 패키지 또는 적절한 위치

import main.java.dao.DaoException;
import main.java.dto.UserDto;
import main.java.dto.UserBatchUpdatePreviewDto;
import main.java.service.UserService;
import main.java.service.ServiceException;
import main.java.util.ExcelParserUtil; // 엑셀 파싱 유틸리티

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors; // 추가

import javax.swing.table.DefaultTableCellRenderer; // 새로 추가
import java.awt.event.MouseAdapter; // 새로 추가
import java.awt.event.MouseEvent;  // 새로 추가
import java.util.function.Consumer; // 새로 추가

/**
 * 학생 정보 일괄 추가/업데이트를 위한 JDialog 입니다.
 * 엑셀 파일을 업로드하고, 변경 사항 미리보기 후 선택적으로 적용합니다.
 */
public class BatchStudentProcessingDialog extends JDialog {

    private final UserService userService;
    private final Runnable onProcessCompleteCallback; // 처리 완료 후 호출될 콜백
    private final Frame ownerFrame; // 부모 프레임

    private JLabel filePathLabel;
    private JButton fileSelectButton;
    private JTable previewTable;
    private DefaultTableModel previewTableModel;
    private JButton processButton;

    private File selectedExcelFile;
    private List<UserBatchUpdatePreviewDto> previewDataList = new ArrayList<>();

    public BatchStudentProcessingDialog(Frame owner, UserService userService, Runnable onProcessCompleteCallback) {
        super(owner, "학생 일괄 추가/업데이트 (엑셀)", true); // 제목에 (엑셀) 추가
        this.ownerFrame = owner;
        this.userService = userService;
        this.onProcessCompleteCallback = onProcessCompleteCallback;

        setSize(1100, 650); // 다이얼로그 크기 조정 (비고 컬럼 추가로 너비 확장)
        setResizable(false);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initComponents();
    }

    /**
     * [수정된 메서드]
     * 요구사항 반영: '비고' 컬럼 추가 및 컬럼 너비 조정
     */
    private void initComponents() {
        // 상단: 파일 선택 영역
        JPanel filePanel = new JPanel(new BorderLayout(10, 0));
        filePathLabel = new JLabel("선택된 엑셀 파일 없음");
        filePathLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        filePathLabel.setBorder(BorderFactory.createEtchedBorder());
        filePathLabel.setPreferredSize(new Dimension(700, 28)); // 높이 조정

        fileSelectButton = new JButton("엑셀 파일 열기..."); // 버튼 텍스트 변경
        fileSelectButton.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        fileSelectButton.addActionListener(e -> selectExcelFile());

        filePanel.add(new JLabel("대상 파일: "), BorderLayout.WEST);
        filePanel.add(filePathLabel, BorderLayout.CENTER);
        filePanel.add(fileSelectButton, BorderLayout.EAST);
        filePanel.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
        add(filePanel, BorderLayout.NORTH);

        // 중앙: 미리보기 테이블 영역
        String[] columnNames = {"적용", "구분", "학번", "이름", "엑셀 학과", "엑셀 학년", "엑셀 상태", "기존 학과", "기존 학년", "기존 상태", "변경사항", "비고"};
        previewTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0; // "적용" 체크박스만 편집 가능
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return Boolean.class; // 체크박스로 렌더링
                }
                return super.getColumnClass(columnIndex);
            }
        };
        previewTable = new JTable(previewTableModel);
        previewTable.setRowHeight(25);
        previewTable.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 12));
        previewTable.setFont(new Font("맑은 고딕", Font.PLAIN, 12));

        // [새로 추가된 코드] 커스텀 렌더러 적용
        previewTable.setDefaultRenderer(Object.class, new BatchPreviewTableCellRenderer());

        // [새로 추가된 코드] 더블클릭 이벤트 리스너 추가
        previewTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handleDoubleClickOnTable();
                }
            }
        });

        // 컬럼 너비 설정
        TableColumn selectionColumn = previewTable.getColumnModel().getColumn(0); // 적용
        selectionColumn.setPreferredWidth(40);
        previewTable.getColumnModel().getColumn(1).setPreferredWidth(50);  // 구분
        previewTable.getColumnModel().getColumn(2).setPreferredWidth(90); // 학번
        previewTable.getColumnModel().getColumn(3).setPreferredWidth(80); // 이름
        previewTable.getColumnModel().getColumn(4).setPreferredWidth(120); // 엑셀 학과
        previewTable.getColumnModel().getColumn(5).setPreferredWidth(60);  // 엑셀 학년
        previewTable.getColumnModel().getColumn(6).setPreferredWidth(60);  // 엑셀 상태
        previewTable.getColumnModel().getColumn(7).setPreferredWidth(120); // 기존 학과
        previewTable.getColumnModel().getColumn(8).setPreferredWidth(60);  // 기존 학년
        previewTable.getColumnModel().getColumn(9).setPreferredWidth(60);  // 기존 상태
        previewTable.getColumnModel().getColumn(10).setPreferredWidth(120);// 변경사항
        previewTable.getColumnModel().getColumn(11).setPreferredWidth(150); // 비고 (새 컬럼)


        JScrollPane scrollPane = new JScrollPane(previewTable);
        add(scrollPane, BorderLayout.CENTER);

        // 하단: 처리 버튼 영역
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        processButton = new JButton("선택 항목 일괄 적용");
        processButton.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        processButton.setEnabled(false); // 파일 로드 및 미리보기 후 활성화
        processButton.addActionListener(e -> processBatchUpdate());

        JButton cancelButton = new JButton("취소");
        cancelButton.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        cancelButton.addActionListener(e -> dispose());

        bottomPanel.add(processButton);
        bottomPanel.add(cancelButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private class BatchPreviewTableCellRenderer extends DefaultTableCellRenderer {
        // [수정된 코드] 사용자의 요청에 따라 색상을 더 눈에 띄게 변경
        private final Color MODIFIED_COLOR = new Color(255, 235, 238); // 연한 분홍색/붉은 계열
        private final Color ERROR_COLOR = new Color(224, 224, 224);    // 좀 더 진한 회색

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            // 선택된 행은 기본 선택 색상 유지
            if (isSelected) {
                // c.setBackground(table.getSelectionBackground()); // 이 부분은 super 클래스가 처리하므로 명시적으로 호출할 필요 없음
                return c;
            }

            // 모델 인덱스로 변환하여 DTO 가져오기
            int modelRow = table.convertRowIndexToModel(row);
            if (modelRow >= 0 && modelRow < previewDataList.size()) {
                UserBatchUpdatePreviewDto previewDto = previewDataList.get(modelRow);

                if (previewDto.getValidationMessage() != null && !previewDto.getValidationMessage().isEmpty()) {
                    // 유효성 검사 오류가 있는 행
                    c.setBackground(ERROR_COLOR);
                } else if (!previewDto.isNewUser() && previewDto.getChangedFields() != null && !previewDto.getChangedFields().isEmpty()) {
                    // 기존 학생 중 변경사항이 있는 행
                    c.setBackground(MODIFIED_COLOR);
                } else {
                    // 그 외 (신규 또는 변경없는 기존 학생)
                    c.setBackground(table.getBackground());
                }
            } else {
                c.setBackground(table.getBackground());
            }

            return c;
        }
    }

    private void selectExcelFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("학생 정보 일괄 처리를 위한 엑셀 파일 선택");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel 통합 문서 (*.xlsx, *.xls)", "xlsx", "xls"));
        fileChooser.setAcceptAllFileFilterUsed(true); // 모든 파일 보기 옵션도 제공 (선택적)

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedExcelFile = fileChooser.getSelectedFile();
            filePathLabel.setText(selectedExcelFile.getName()); // 파일 이름만 표시
            filePathLabel.setToolTipText(selectedExcelFile.getAbsolutePath()); // 전체 경로는 툴팁으로
            loadAndPreviewExcelData();
        } else {
            // 선택 취소 시 기존 상태 유지 또는 초기화
            if (selectedExcelFile == null) { // 이전에 선택된 파일이 없었다면
                filePathLabel.setText("선택된 엑셀 파일 없음");
                filePathLabel.setToolTipText(null);
                previewTableModel.setRowCount(0);
                previewDataList.clear();
                processButton.setEnabled(false);
            }
        }
    }

    private void handleDoubleClickOnTable() {
        int viewRow = previewTable.getSelectedRow();
        if (viewRow < 0) return;

        int modelRow = previewTable.convertRowIndexToModel(viewRow);
        if (modelRow >= 0 && modelRow < previewDataList.size()) {
            UserBatchUpdatePreviewDto originalPreviewDto = previewDataList.get(modelRow);
            UserDto dtoToEdit = originalPreviewDto.getUserInfoFromExcel();

            // 미리보기 수정용 생성자로 StudentEditorDialog 호출
            StudentEditorDialog editorDialog = new StudentEditorDialog(this, dtoToEdit, updatedDto -> {
                // 수정 완료 후 콜백 로직: 미리보기 테이블의 데이터를 업데이트
                try {
                    // 수정된 정보로 새로운 Preview DTO를 만들어 기존 것을 대체
                    UserBatchUpdatePreviewDto newPreviewDto = new UserBatchUpdatePreviewDto(
                            originalPreviewDto.getExistingUserInDB(), // 기존 DB 정보는 동일
                            updatedDto,                               // 수정된 엑셀 정보
                            originalPreviewDto.isNewUser()            // 신규 여부는 동일
                    );

                    previewDataList.set(modelRow, newPreviewDto);
                    updateTableModelRow(modelRow, newPreviewDto);

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "미리보기 업데이트 중 오류 발생:\n" + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
                }
            });
            editorDialog.setVisible(true);
        }
    }

    /**
     * [새로 추가된 메서드]
     * 미리보기 테이블의 특정 행을 새로운 DTO 정보로 업데이트합니다.
     * @param modelRow 업데이트할 테이블 모델의 행 인덱스
     * @param dto 새로운 UserBatchUpdatePreviewDto 데이터
     */
    private void updateTableModelRow(int modelRow, UserBatchUpdatePreviewDto dto) {
        UserDto existing = dto.getExistingUserInDB();
        UserDto fromExcel = dto.getUserInfoFromExcel();

        previewTableModel.setValueAt(dto.isSelectedForUpdate(), modelRow, 0);
        previewTableModel.setValueAt(dto.isNewUser() ? "신규" : "변경", modelRow, 1);
        previewTableModel.setValueAt(fromExcel.getStudentNumber(), modelRow, 2);
        previewTableModel.setValueAt(fromExcel.getName(), modelRow, 3);
        previewTableModel.setValueAt(fromExcel.getDepartmentName() != null ? fromExcel.getDepartmentName() : "-", modelRow, 4);
        previewTableModel.setValueAt(fromExcel.getGrade() > 0 ? fromExcel.getGrade() + "학년" : "-", modelRow, 5);
        previewTableModel.setValueAt(fromExcel.getStatus(), modelRow, 6);
        previewTableModel.setValueAt(existing != null ? (existing.getDepartmentName() != null ? existing.getDepartmentName() : "-") : "N/A", modelRow, 7);
        previewTableModel.setValueAt(existing != null ? (existing.getGrade() > 0 ? existing.getGrade() + "학년" : "-") : "N/A", modelRow, 8);
        previewTableModel.setValueAt(existing != null ? existing.getStatus() : "N/A", modelRow, 9);
        previewTableModel.setValueAt(dto.isNewUser() ? "신규 등록 대상" : String.join(", ", dto.getChangedFields()), modelRow, 10);
        previewTableModel.setValueAt(dto.getValidationMessage(), modelRow, 11);

        // 변경 후 테이블을 다시 그려서 색상 등이 즉시 반영되도록 함
        previewTable.revalidate();
        previewTable.repaint();
    }
    /**
     * [수정된 메서드]
     * 요구사항 반영: '비고' 컬럼에 유효성 검사 메시지 추가
     */
    private void loadAndPreviewExcelData() {
        if (selectedExcelFile == null) {
            JOptionPane.showMessageDialog(this, "먼저 엑셀 파일을 선택해주세요.", "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        previewTableModel.setRowCount(0); // 테이블 초기화
        previewDataList.clear();          // 내부 데이터 리스트 초기화

        try {
            // ExcelParserUtil을 사용하여 엑셀 데이터를 UserDto 리스트로 변환
            List<UserDto> excelStudentList = ExcelParserUtil.parseStudentExcel(selectedExcelFile);
            if (excelStudentList.isEmpty()) {
                JOptionPane.showMessageDialog(this, "엑셀 파일에서 학생 정보를 읽어오지 못했거나 유효한 데이터가 없습니다.", "정보 없음", JOptionPane.INFORMATION_MESSAGE);
                processButton.setEnabled(false);
                return;
            }

            // 서비스 계층을 통해 DB 데이터와 비교하여 UserBatchUpdatePreviewDto 리스트 생성
            // 이 과정에서 Excel의 학과명이 학과 ID로 변환되고 UserDto에 설정되어야 함 (UserServiceImpl에서 처리)
            previewDataList = userService.previewExcelStudentUpdates(excelStudentList);

            if (previewDataList.isEmpty()) {
                JOptionPane.showMessageDialog(this, "미리보기할 데이터가 없습니다.\n(모든 학생 정보가 최신이거나, 엑셀 내용에 문제가 있을 수 있습니다.)", "정보 없음", JOptionPane.INFORMATION_MESSAGE);
                processButton.setEnabled(false);
                return;
            }

            // 미리보기 테이블 채우기
            for (UserBatchUpdatePreviewDto previewDto : previewDataList) {
                UserDto existing = previewDto.getExistingUserInDB();
                UserDto fromExcel = previewDto.getUserInfoFromExcel();

                previewTableModel.addRow(new Object[]{
                        previewDto.isSelectedForUpdate(), // 선택 체크박스 (UserBatchUpdatePreviewDto의 기본값 사용)
                        previewDto.isNewUser() ? "신규" : "변경",
                        fromExcel.getStudentNumber(),
                        fromExcel.getName(),
                        fromExcel.getDepartmentName() != null ? fromExcel.getDepartmentName() : "-", // 엑셀에서 읽은 학과명
                        fromExcel.getGrade() > 0 ? fromExcel.getGrade() + "학년" : "-",
                        fromExcel.getStatus(),
                        existing != null ? (existing.getDepartmentName() !=null ? existing.getDepartmentName() : "-") : "N/A",
                        existing != null ? (existing.getGrade() > 0 ? existing.getGrade() + "학년" : "-") : "N/A",
                        existing != null ? existing.getStatus() : "N/A",
                        previewDto.isNewUser() ? "신규 등록 대상" : String.join(", ", previewDto.getChangedFields()),
                        previewDto.getValidationMessage() // 비고 컬럼에 유효성 검사 메시지 표시
                });
            }
            processButton.setEnabled(true); // 데이터가 있으면 일괄 적용 버튼 활성화

        } catch (ServiceException e) {
            JOptionPane.showMessageDialog(this, "엑셀 데이터 처리 중 서비스 오류 발생:\n" + e.getMessage(), "처리 오류", JOptionPane.ERROR_MESSAGE);
            processButton.setEnabled(false);
        } catch (IllegalArgumentException | IOException e) { // ExcelParserUtil 등에서 발생할 수 있는 예외
            JOptionPane.showMessageDialog(this, "엑셀 파일 분석 중 오류가 발생했습니다:\n" + e.getMessage(), "파일 오류", JOptionPane.ERROR_MESSAGE);
            processButton.setEnabled(false);
        } catch (Exception e) { // 그 외 예외
            JOptionPane.showMessageDialog(this, "알 수 없는 오류 발생:\n" + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            processButton.setEnabled(false);
            e.printStackTrace(); // 개발 중 상세 오류 확인
        }
    }

    private void processBatchUpdate() {
        List<UserDto> studentsToActuallyProcess = new ArrayList<>();
        for (int i = 0; i < previewTableModel.getRowCount(); i++) {
            Boolean isSelected = (Boolean) previewTableModel.getValueAt(i, 0); // "적용" 체크박스
            if (isSelected != null && isSelected) {
                if (i < previewDataList.size()) { // 데이터 일관성 확인
                    studentsToActuallyProcess.add(previewDataList.get(i).getUserInfoFromExcel());
                }
            }
        }

        if (studentsToActuallyProcess.isEmpty()) {
            JOptionPane.showMessageDialog(this, "일괄 적용할 학생을 선택해주세요.", "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // [새로 추가된 코드] 학과 미지정 학생 확인 로직
        List<String> studentsWithoutDeptNames = studentsToActuallyProcess.stream()
                .filter(dto -> dto.getDpmtId() == 0)
                .map(UserDto::getName)
                .collect(Collectors.toList());

        StringBuilder confirmationMessage = new StringBuilder();
        confirmationMessage.append("선택된 ").append(studentsToActuallyProcess.size())
                .append("명의 학생 정보를 일괄 적용(신규 등록 또는 정보 업데이트)하시겠습니까?");

        if (!studentsWithoutDeptNames.isEmpty()) {
            confirmationMessage.append("\n\n[주의] 아래 학생들은 학과가 지정되지 않았습니다:\n")
                    .append(String.join(", ", studentsWithoutDeptNames))
                    .append("\n\n이 학생들을 포함하여 그대로 진행하시겠습니까?");
        }
        // [수정된 코드 끝]

        int confirm = JOptionPane.showConfirmDialog(this,
                confirmationMessage.toString(), // 동적으로 생성된 메시지 사용
                "일괄 적용 확인",
                JOptionPane.YES_NO_OPTION,
                studentsWithoutDeptNames.isEmpty() ? JOptionPane.QUESTION_MESSAGE : JOptionPane.WARNING_MESSAGE); // 경고 아이콘 표시

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                userService.batchUpdateStudents(studentsToActuallyProcess); // 서비스 호출
                JOptionPane.showMessageDialog(this, "선택된 학생 정보가 성공적으로 적용되었습니다.", "적용 완료", JOptionPane.INFORMATION_MESSAGE);

                if (onProcessCompleteCallback != null) {
                    onProcessCompleteCallback.run(); // StudentMgmtPanel의 목록 새로고침
                }
                dispose(); // 다이얼로그 닫기

            } catch (ServiceException | SQLException | DaoException e) {
                JOptionPane.showMessageDialog(this, "일괄 적용 중 오류 발생:\n" + e.getMessage(), "적용 오류", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}