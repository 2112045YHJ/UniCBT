package main.java.ui.admin;

import main.java.dto.UserDto;
import main.java.model.Department;
import main.java.model.User; // adminUser 타입
import main.java.service.DepartmentService;
import main.java.service.DepartmentServiceImpl;
import main.java.service.UserService;
import main.java.service.UserServiceImpl;
import main.java.service.ServiceException;
import main.java.ui.admin.dialog.BatchStudentProcessingDialog; // 다이얼로그 임포트
import main.java.ui.admin.dialog.StudentEditorDialog;       // 다이얼로그 임포트

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 관리자용 학생 계정 관리 패널입니다.
 * 탭, 필터, 검색을 통해 학생 목록을 조회하고 관리 기능을 제공합니다.
 */
public class StudentMgmtPanel extends JPanel {
    private final User adminUser;
    private final AdminMainFrame mainFrame;
    private final UserService userService = new UserServiceImpl();
    private final DepartmentService departmentService = new DepartmentServiceImpl();

    private JTabbedPane mainTabbedPane; // 상태/학년별 필터링을 위한 탭
    private JTable studentTable;        // 학생 목록을 표시할 단일 테이블
    private DefaultTableModel studentTableModel;
    private TableRowSorter<DefaultTableModel> sorter;

    // 필터 및 검색 컴포넌트
    private JComboBox<DepartmentItem> departmentFilterComboBox;
    private JComboBox<GradeItem> gradeFilterComboBox;
    private JTextField searchTextField;
    private JButton resetButton;

    // 학생 관리 버튼
    private JButton addStudentButton;
    private JButton batchAddStudentButton;
    private JButton editStudentButton;
    private JButton changeStatusButton;
    private JButton resetPasswordButton;

    private List<UserDto> currentStudentListInTable = new ArrayList<>(); // 현재 테이블에 표시된 데이터

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public StudentMgmtPanel(User adminUser, AdminMainFrame mainFrame) {
        this.adminUser = adminUser;
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(10, 10)); // 패널 기본 레이아웃
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // 패널 여백
        initComponents(); // UI 컴포넌트 초기화
        loadInitialUIData(); // 학과 필터 등 초기 UI 데이터 로드
        setupTabsAndFilters(); // 탭 및 필터 설정, 초기 목록 로드
    }

    private void initComponents() {
        // --- 상단 컨트롤 섹션 패널 ---
        // JPanel topSectionPanel = new JPanel(new BorderLayout(0, 5)); // 기존 BorderLayout
        JPanel topSectionPanel = new JPanel(); // 새 레이아웃 적용 위해 변경
        topSectionPanel.setLayout(new BoxLayout(topSectionPanel, BoxLayout.Y_AXIS)); // Y축으로 쌓도록 변경

        // 1. 필터 및 검색 패널
        JPanel filterSearchPanel = createTopControlPanel();
        // filterSearchPanel의 최대 높이를 선호하는 크기로 고정 (내부 컴포넌트 크기에 맞게)
        filterSearchPanel.setAlignmentX(Component.LEFT_ALIGNMENT); // BoxLayout 사용 시 정렬
        // filterSearchPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, filterSearchPanel.getPreferredSize().height));


        // 2. 탭 패널
        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        // --- 아래 라인 추가 시도 ---
        mainTabbedPane.setPreferredSize(new Dimension(100, 35)); // 너비는 중요하지 않음, 높이를 탭 헤더만큼 작게
        // --- 위 라인 추가 시도 ---
        topSectionPanel.add(mainTabbedPane, BorderLayout.CENTER);


        topSectionPanel.add(filterSearchPanel);
        topSectionPanel.add(mainTabbedPane); // BoxLayout은 순서대로 추가됨

        add(topSectionPanel, BorderLayout.NORTH);

        // --- 중앙: 학생 목록 테이블 ---
        String[] columnNames = {"학번", "이름", "학년", "학과", "상태", "생년월일", "계정생성일"};
        studentTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 테이블 직접 편집 불가
            }
        };
        studentTable = new JTable(studentTableModel);
        studentTable.setRowHeight(28);
        studentTable.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 14));
        studentTable.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        studentTable.setAutoCreateRowSorter(true);
        sorter = new TableRowSorter<>(studentTableModel);
        studentTable.setRowSorter(sorter);
        setTableSorters();

        JScrollPane studentTableScrollPane = new JScrollPane(studentTable);
        add(studentTableScrollPane, BorderLayout.CENTER); // 테이블(스크롤 포함)을 중앙에 배치

        // --- 하단: 학생 관리 버튼 영역 ---
        JPanel bottomButtonPanel = createBottomButtonPanel(); // 이전과 동일하게 생성
        add(bottomButtonPanel, BorderLayout.SOUTH);
    }

    private void setTableSorters() {
        // 학년 정렬 (문자 "학년", "-" 제거 후 숫자 비교)
        sorter.setComparator(2, (Object o1, Object o2) -> {
            try {
                String s1 = o1.toString().replace("학년", "").replace("-", "0").trim();
                String s2 = o2.toString().replace("학년", "").replace("-", "0").trim();
                Integer g1 = Integer.parseInt(s1);
                Integer g2 = Integer.parseInt(s2);
                return g1.compareTo(g2);
            } catch (NumberFormatException e) {
                return o1.toString().compareTo(o2.toString()); // 숫자 변환 실패 시 문자열 비교
            }
        });
        // TODO: 필요시 다른 숫자 또는 날짜 컬럼에 대한 Comparator 추가
    }

    private JPanel createTopControlPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // 1행: 학과 필터, 학년 필터
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("학과:"), gbc);
        gbc.gridx = 1;
        departmentFilterComboBox = new JComboBox<>();
        departmentFilterComboBox.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        departmentFilterComboBox.setPreferredSize(new Dimension(180, 28));
        departmentFilterComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                loadStudentsForCurrentTab();
            }
        });
        panel.add(departmentFilterComboBox, gbc);

        gbc.gridx = 2; gbc.gridy = 0;
        panel.add(new JLabel("학년:"), gbc);
        gbc.gridx = 3;
        gradeFilterComboBox = new JComboBox<>();
        gradeFilterComboBox.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        gradeFilterComboBox.setPreferredSize(new Dimension(120, 28));
        gradeFilterComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                loadStudentsForCurrentTab();
            }
        });
        panel.add(gradeFilterComboBox, gbc);

        // 1행의 빈 공간을 채우기 위한 더미 컴포넌트 (버튼들과 정렬 맞추기 위함)
        gbc.gridx = 4; gbc.gridy = 0; gbc.weightx = 1.0; // 이 컴포넌트가 남은 가로 공간을 차지하도록
        panel.add(new JLabel(""), gbc); // 빈 레이블
        gbc.weightx = 0; // 기본값으로 리셋


        // 2행: 검색어 입력, 검색 버튼, 초기화 버튼
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("검색:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        searchTextField = new JTextField(); // 너비는 GridBagLayout에 의해 조절됨
        searchTextField.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        searchTextField.addActionListener(e -> loadStudentsForCurrentTab());
        panel.add(searchTextField, gbc);
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE; // fill 기본값으로 리셋

        // 검색 및 초기화 버튼을 담을 패널 (오른쪽 정렬 유지)
        JPanel searchButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JButton performSearchButton = new JButton("검색 실행");
        performSearchButton.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        performSearchButton.addActionListener(e -> loadStudentsForCurrentTab());
        searchButtonsPanel.add(performSearchButton);

        resetButton = new JButton("필터/검색 초기화");
        resetButton.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        resetButton.addActionListener(e -> resetFiltersAndSearch());
        searchButtonsPanel.add(resetButton);

        // 버튼 패널을 GridBagLayout의 오른쪽에 배치
        gbc.gridx = 3; // 학년 필터 오른쪽부터 시작 (이전에는 gridx=4 였음)
        gbc.gridy = 1;
        gbc.gridwidth = 2; // 버튼 2개를 포함할 수 있도록 너비 설정 (조정 필요)
        gbc.anchor = GridBagConstraints.EAST; // 이 셀 내에서 동쪽(오른쪽) 정렬
        // gbc.weightx = 1.0; // 버튼 패널이 오른쪽으로 밀리도록 할 필요 없음 (이미 gbc.gridx = 4인 더미 레이블이 weightx=1.0 가짐)
        panel.add(searchButtonsPanel, gbc);

        return panel;
    }

    private JPanel createBottomButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));

        addStudentButton = new JButton("학생 개별 추가");
        batchAddStudentButton = new JButton("학생 일괄 처리");
        editStudentButton = new JButton("선택 학생 수정");
        changeStatusButton = new JButton("선택 학생 상태변경");
        resetPasswordButton = new JButton("선택 학생 PW초기화");

        Font btnFont = new Font("맑은 고딕", Font.PLAIN, 13);
        addStudentButton.setFont(btnFont); batchAddStudentButton.setFont(btnFont);
        editStudentButton.setFont(btnFont); changeStatusButton.setFont(btnFont);
        resetPasswordButton.setFont(btnFont);

        addStudentButton.addActionListener(e -> openStudentEditor(null));
        batchAddStudentButton.addActionListener(e -> openBatchStudentProcessor());
        editStudentButton.addActionListener(e -> editSelectedStudent());
        changeStatusButton.addActionListener(e -> changeSelectedStudentStatus());
        resetPasswordButton.addActionListener(e -> resetSelectedStudentPassword());

        panel.add(addStudentButton); panel.add(batchAddStudentButton);
        panel.add(editStudentButton); panel.add(changeStatusButton);
        panel.add(resetPasswordButton);

        enableContextualButtons(false);

        studentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                enableContextualButtons(studentTable.getSelectedRow() != -1);
            }
        });
        return panel;
    }

    private void enableContextualButtons(boolean enabled) {
        editStudentButton.setEnabled(enabled);
        changeStatusButton.setEnabled(enabled);
        resetPasswordButton.setEnabled(enabled);
    }

    private void loadInitialUIData() {
        // 학과 필터 채우기
        departmentFilterComboBox.addItem(new DepartmentItem(0, "전체 학과"));
        try {
            List<Department> departments = departmentService.getAllDepartments();
            if (departments != null) {
                departments.sort(Comparator.comparing(Department::getDpmtName));
                for (Department dept : departments) {
                    departmentFilterComboBox.addItem(new DepartmentItem(dept.getDpmtId(), dept.getDpmtName()));
                }
            }
        } catch (ServiceException e) {
            JOptionPane.showMessageDialog(this, "학과 목록 로드 중 오류: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }

        // 학년 필터 채우기
        gradeFilterComboBox.addItem(new GradeItem(0, "전체 학년"));
        for (int i = 1; i <= 4; i++) { // 최대 학년은 시스템 정책에 따라
            gradeFilterComboBox.addItem(new GradeItem(i, i + "학년"));
        }
    }

    private void setupTabsAndFilters() { // 메서드 이름 변경 제안
        mainTabbedPane.removeAll();

        // 탭 추가 (두 번째 인자로 컴포넌트를 전달하지 않음 - 탭은 필터 역할만)
        mainTabbedPane.addTab("전체(재학/휴학)", null);
        mainTabbedPane.addTab("1학년", null);
        mainTabbedPane.addTab("2학년", null);
        mainTabbedPane.addTab("3학년", null);
        mainTabbedPane.addTab("졸업자", null);
        mainTabbedPane.addTab("기타(자퇴/퇴학)", null);

        mainTabbedPane.addChangeListener(e -> {
            // 탭 변경 시 항상 현재 선택된 탭과 필터 기준으로 데이터를 다시 로드
            if (mainTabbedPane.getSelectedIndex() != -1) {
                System.out.println("탭 변경됨 (ChangeListener): " + mainTabbedPane.getTitleAt(mainTabbedPane.getSelectedIndex()));

                // 탭 변경 시, 학년 콤보박스 연동 (선택적 UI 개선)
                // 이 부분은 loadStudentsForCurrentTab 내부에서 conditions를 설정할 때
                // 탭의 학년 조건을 우선시하고, 탭이 특정 학년을 지정하지 않을 때만
                // 콤보박스 값을 사용하도록 하는 것이 더 안전하고 중복 호출을 피할 수 있습니다.
                // 여기서는 탭 변경 시 무조건 loadStudentsForCurrentTab 호출.
                loadStudentsForCurrentTab();
            }
        });

        // 초기 탭 선택 및 데이터 로드
        if (mainTabbedPane.getTabCount() > 0) {
            mainTabbedPane.setSelectedIndex(0); // ChangeListener가 호출되어 첫 탭 데이터 로드
        } else {
            // 탭이 없는 극단적인 경우 (기본 조건으로 로드)
            loadStudentsForCurrentTab();
        }
    }

    private void setComboBoxSelectedItem(JComboBox<GradeItem> comboBox, int gradeValue, boolean fireListener) {
        ItemListener[] listeners = null;
        if (!fireListener) { // 리스너를 임시로 제거하여 setSelectedIndex로 인한 이벤트 발생 방지
            listeners = comboBox.getItemListeners();
            for (ItemListener l : listeners) {
                comboBox.removeItemListener(l);
            }
        }

        for (int i = 0; i < comboBox.getItemCount(); i++) {
            if (comboBox.getItemAt(i).getGradeValue() == gradeValue) {
                if (comboBox.getSelectedIndex() != i) {
                    comboBox.setSelectedIndex(i);
                }
                break;
            }
        }

        if (!fireListener && listeners != null) { // 제거했던 리스너 다시 추가
            for (ItemListener l : listeners) {
                comboBox.addItemListener(l);
            }
        }
    }

    private void loadStudentsForCurrentTab() {
        studentTableModel.setRowCount(0);
        currentStudentListInTable.clear();

        Map<String, Object> conditions = new HashMap<>();
        conditions.put("level", 1);

        DepartmentItem selectedDept = (DepartmentItem) departmentFilterComboBox.getSelectedItem();
        GradeItem selectedGradeFromComboBox = (GradeItem) gradeFilterComboBox.getSelectedItem();
        String searchText = searchTextField.getText().trim();
        if (!searchText.isEmpty()) {
            if (searchText.matches("\\d+")) { // 숫자로만 구성되어 있으면 학번으로 간주
                conditions.put("studentNumberSearch", searchText);
                System.out.println("학번으로 검색 시도: " + searchText);
            } else { // 그 외 (문자 포함 등)는 이름으로 간주
                conditions.put("nameSearch", searchText);
                System.out.println("이름으로 검색 시도: " + searchText);
            }
        }

        int selectedTabIndex = mainTabbedPane.getSelectedIndex();
        String selectedTabTitle = (selectedTabIndex != -1) ? mainTabbedPane.getTitleAt(selectedTabIndex) : "전체(재학/휴학)";

        System.out.println("loadStudentsForCurrentTab - 탭: '" + selectedTabTitle + "', 학과필터: " + (selectedDept != null ? selectedDept.getName() : "전체") + ", 학년필터(콤보): " + (selectedGradeFromComboBox != null ? selectedGradeFromComboBox.toString() : "전체"));

        boolean isSpecificGradeTabSetByTab = false;

        switch (selectedTabTitle) {
            case "전체(재학/휴학)":
                conditions.put("statuses", List.of("재학", "휴학"));
                break;
            case "1학년":
                conditions.put("grade", 1); conditions.put("statuses", List.of("재학", "휴학"));
                isSpecificGradeTabSetByTab = true; break;
            case "2학년":
                conditions.put("grade", 2); conditions.put("statuses", List.of("재학", "휴학"));
                isSpecificGradeTabSetByTab = true; break;
            case "3학년":
                conditions.put("grade", 3); conditions.put("statuses", List.of("재학", "휴학"));
                isSpecificGradeTabSetByTab = true; break;
            case "졸업자":
                conditions.put("status", "졸업"); break;
            case "기타(자퇴/퇴학)":
                conditions.put("statuses", List.of("자퇴", "퇴학")); break;
            default:
                conditions.put("statuses", List.of("재학", "휴학")); break;
        }

        if (selectedDept != null && selectedDept.getId() > 0) {
            conditions.put("dpmtId", selectedDept.getId());
        }

        if (!isSpecificGradeTabSetByTab && selectedGradeFromComboBox != null && selectedGradeFromComboBox.getGradeValue() > 0) {
            conditions.put("grade", selectedGradeFromComboBox.getGradeValue());
        }

        if (!searchText.isEmpty()) {
            if (searchText.matches("^\\d{4,}$")) {
                conditions.put("studentNumberSearch", searchText);
            } else {
                conditions.put("nameSearch", searchText);
            }
        }

        System.out.println("최종 검색 조건: " + conditions);
        try {
            currentStudentListInTable = userService.searchStudents(conditions);
            System.out.println("조회된 학생 수: " + currentStudentListInTable.size());
            for (UserDto userDto : currentStudentListInTable) {
                studentTableModel.addRow(new Object[]{
                        userDto.getStudentNumber(), userDto.getName(),
                        userDto.getGrade() > 0 ? userDto.getGrade() + "학년" : "-",
                        userDto.getDepartmentName() != null ? userDto.getDepartmentName() : "미지정",
                        userDto.getStatus(),
                        userDto.getBirthDate() != null ? userDto.getBirthDate().format(dateFormatter) : "-",
                        userDto.getCreatedAt() != null ? userDto.getCreatedAt().toLocalDate().format(dateFormatter) : "-"
                });
            }
        } catch (ServiceException e) {
            JOptionPane.showMessageDialog(this, "학생 정보 조회 중 오류: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        enableContextualButtons(false);
    }

    private void resetFiltersAndSearch() {
        // 리스너가 중복 호출되지 않도록 임시로 제거 후 다시 추가하는 방식 고려 가능
        ActionListener[] deptListeners = departmentFilterComboBox.getActionListeners();
        for(ActionListener l : deptListeners) departmentFilterComboBox.removeActionListener(l);
        ActionListener[] gradeListeners = gradeFilterComboBox.getActionListeners();
        for(ActionListener l : gradeListeners) gradeFilterComboBox.removeActionListener(l);

        departmentFilterComboBox.setSelectedIndex(0);
        gradeFilterComboBox.setSelectedIndex(0);
        searchTextField.setText("");

        // 리스너 다시 추가
        for(ActionListener l : deptListeners) departmentFilterComboBox.addActionListener(l);
        for(ActionListener l : gradeListeners) gradeFilterComboBox.addActionListener(l);

        loadStudentsForCurrentTab(); // 필터 초기화 후 현재 탭 기준으로 목록 다시 로드
    }

    // --- 학생 관리 버튼 액션 메서드들 ---
    // (openStudentEditor, openBatchStudentProcessor, editSelectedStudent, changeSelectedStudentStatus, resetSelectedStudentPassword 는 이전과 동일)
    private void openStudentEditor(UserDto studentToEdit) {
        StudentEditorDialog editorDialog = new StudentEditorDialog(
                mainFrame, userService, studentToEdit, this::loadStudentsForCurrentTab
        );
        editorDialog.setVisible(true);
    }

    private void openBatchStudentProcessor() {
        BatchStudentProcessingDialog batchDialog = new BatchStudentProcessingDialog(
                mainFrame, userService, this::loadStudentsForCurrentTab
        );
        batchDialog.setVisible(true);
    }

    private void editSelectedStudent() {
        int selectedViewRow = studentTable.getSelectedRow();
        if (selectedViewRow == -1) {
            JOptionPane.showMessageDialog(this, "수정할 학생을 선택해주세요.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = studentTable.convertRowIndexToModel(selectedViewRow);
        if (modelRow >= 0 && modelRow < currentStudentListInTable.size()) {
            UserDto selectedStudent = currentStudentListInTable.get(modelRow);
            openStudentEditor(selectedStudent);
        } else {
            JOptionPane.showMessageDialog(this, "선택된 학생 정보가 유효하지 않습니다 (모델 인덱스 오류).", "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void changeSelectedStudentStatus() {
        int selectedViewRow = studentTable.getSelectedRow();
        if (selectedViewRow == -1) { JOptionPane.showMessageDialog(this, "상태를 변경할 학생을 선택해주세요.","알림",JOptionPane.WARNING_MESSAGE); return; }
        int modelRow = studentTable.convertRowIndexToModel(selectedViewRow);
        if (modelRow >= 0 && modelRow < currentStudentListInTable.size()) {
            UserDto selectedStudent = currentStudentListInTable.get(modelRow);
            String[] possibleStatuses = {"재학", "휴학", "졸업", "자퇴", "퇴학"};
            String newStatus = (String) JOptionPane.showInputDialog( this, "학생 '" + selectedStudent.getName() + "'의 새 상태를 선택하세요:", "학생 상태 변경", JOptionPane.PLAIN_MESSAGE, null, possibleStatuses, selectedStudent.getStatus() );
            if (newStatus != null && !newStatus.equals(selectedStudent.getStatus())) {
                try {
                    userService.changeStudentStatus(selectedStudent.getUserId(), newStatus);
                    JOptionPane.showMessageDialog(this, "학생 상태가 성공적으로 변경되었습니다.", "상태 변경 완료", JOptionPane.INFORMATION_MESSAGE);
                    loadStudentsForCurrentTab();
                } catch (ServiceException ex) { JOptionPane.showMessageDialog(this, "학생 상태 변경 중 오류: " + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE); }
            }
        } else { JOptionPane.showMessageDialog(this, "선택된 학생 정보가 유효하지 않습니다 (모델 인덱스 오류).", "오류", JOptionPane.ERROR_MESSAGE); }
    }

    private void resetSelectedStudentPassword() {
        int selectedViewRow = studentTable.getSelectedRow();
        if (selectedViewRow == -1) { JOptionPane.showMessageDialog(this, "비밀번호를 초기화할 학생을 선택해주세요.","알림",JOptionPane.WARNING_MESSAGE); return; }
        int modelRow = studentTable.convertRowIndexToModel(selectedViewRow);
        if (modelRow >= 0 && modelRow < currentStudentListInTable.size()) {
            UserDto selectedStudent = currentStudentListInTable.get(modelRow);
            if (selectedStudent.getBirthDate() == null) { JOptionPane.showMessageDialog(this, "해당 학생의 생년월일 정보가 없어 비밀번호를 초기화할 수 없습니다.\n먼저 학생 정보를 수정하여 생년월일을 등록해주세요.", "초기화 불가", JOptionPane.WARNING_MESSAGE); return; }
            int confirm = JOptionPane.showConfirmDialog(this, "학생 '" + selectedStudent.getName() + "'의 비밀번호를 초기 비밀번호('a' + 생년월일 8자리)로 초기화하시겠습니까?", "비밀번호 초기화 확인", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    userService.resetStudentPassword(selectedStudent.getUserId());
                    JOptionPane.showMessageDialog(this, "비밀번호가 성공적으로 초기화되었습니다.", "초기화 완료", JOptionPane.INFORMATION_MESSAGE);
                } catch (ServiceException ex) { JOptionPane.showMessageDialog(this, "비밀번호 초기화 중 오류: " + ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE); }
            }
        } else { JOptionPane.showMessageDialog(this, "선택된 학생 정보가 유효하지 않습니다 (모델 인덱스 오류).", "오류", JOptionPane.ERROR_MESSAGE); }
    }

    // --- 헬퍼 클래스들 (DepartmentItem, GradeItem) ---
    private static class DepartmentItem {
        private int id;
        private String name;

        public DepartmentItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        /**
         * 학과 이름을 반환합니다.
         * @return 학과 이름
         */
        public String getName() { // 이 메서드가 없거나 주석 처리되어 있다면 추가/수정합니다.
            return name;
        }

        @Override
        public String toString() {
            return name; // 콤보박스에 표시될 텍스트
        }
    }
    private static class GradeItem {
        private int gradeValue; private String displayName;
        public GradeItem(int gradeValue, String displayName) { this.gradeValue = gradeValue; this.displayName = displayName; }
        public int getGradeValue() { return gradeValue; }
        @Override public String toString() { return displayName; }
    }
}