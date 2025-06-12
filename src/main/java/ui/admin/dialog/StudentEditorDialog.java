package main.java.ui.admin.dialog; // 다이얼로그를 위한 패키지 (예시)

import main.java.dao.DaoException;
import main.java.dto.UserDto;
import main.java.model.Department;
import main.java.service.DepartmentService;
import main.java.service.DepartmentServiceImpl;
import main.java.service.UserService;
import main.java.service.ServiceException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector; // JComboBox 모델용
import java.util.function.Consumer;

/**
 * 학생 정보를 추가하거나 수정하기 위한 JDialog 입니다.
 */
public class StudentEditorDialog extends JDialog {

    private final UserService userService;
    private final DepartmentService departmentService = new DepartmentServiceImpl();
    private final UserDto studentToEdit; // 수정 대상 학생 정보 (신규 등록 시 null)
    private final Runnable onSaveSuccessCallback; // 저장 성공 시 호출될 콜백
    private final Consumer<UserDto> onUpdateCallback; // 미리보기 수정용 콜백

    private boolean isPreviewMode = false;

    // UI 컴포넌트
    private JTextField studentNumberField;
    private JTextField nameField;
    private JSpinner birthDateSpinner;
    private JComboBox<DepartmentItem> departmentComboBox;
    private JComboBox<String> gradeComboBox;
    private JComboBox<String> statusComboBox;

    private boolean isSaved = false; // 저장 성공 여부 플래그

    /**
     * 학생 정보 편집 다이얼로그 생성자입니다.
     * @param owner 부모 프레임
     * @param userService 사용자 서비스 객체
     * @param studentToEdit 수정할 학생 DTO (새로 추가 시 null)
     * @param onSaveSuccessCallback 저장 성공 시 실행될 콜백 (예: 목록 새로고침)
     */
    public StudentEditorDialog(Frame owner, UserService userService, UserDto studentToEdit, Runnable onSaveSuccessCallback) {
        super(owner, (studentToEdit == null ? "새 학생 등록" : "학생 정보 수정: " + studentToEdit.getName()), true);
        this.userService = userService;
        this.studentToEdit = studentToEdit;
        this.onSaveSuccessCallback = onSaveSuccessCallback;
        this.onUpdateCallback = null;

        setSize(480, 420); // 다이얼로그 크기 조정
        setResizable(false); // 크기 조절 불가
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        // JDialog의 contentPane에 직접 접근하여 여백 설정
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        initComponents();

        if (studentToEdit != null) {
            populateFieldsForEdit(); // 수정 모드일 경우 기존 데이터로 필드 채우기
            studentNumberField.setEditable(false); // 학번은 수정 불가로 설정 (정책에 따라 변경 가능)
        } else {
            studentNumberField.setEditable(true); // 새 학생 등록 시 학번 입력 가능
            statusComboBox.setSelectedItem("재학"); // 신규 등록 시 기본 상태 "재학"
        }
    }

    public StudentEditorDialog(Dialog owner, UserDto studentToEdit, Consumer<UserDto> onUpdateCallback) {
        super(owner, "미리보기 정보 수정: " + studentToEdit.getName(), true);
        this.userService = null; // 미리보기 모드에서는 DB 서비스 직접 사용 안 함
        this.studentToEdit = studentToEdit;
        this.onSaveSuccessCallback = null;
        this.onUpdateCallback = onUpdateCallback; // 콜백 저장
        this.isPreviewMode = true; // 미리보기 모드 활성화

        setSize(480, 420);
        setResizable(false);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        initComponents(); // UI 초기화는 공통 로직 사용
        populateFieldsForEdit(); // 전달받은 DTO로 필드 채우기
        // 학번은 미리보기에서도 수정하지 않는 것을 원칙으로 함
        studentNumberField.setEditable(false);
    }

    /**
     * 다이얼로그의 UI 컴포넌트들을 초기화하고 배치합니다.
     */
    private void initComponents() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6); // 컴포넌트 간 간격 조정
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        Font labelFont = new Font("맑은 고딕", Font.BOLD, 13);
        Font fieldFont = new Font("맑은 고딕", Font.PLAIN, 13);

        // 학번
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel studentNumberLabel = new JLabel("학번:");
        studentNumberLabel.setFont(labelFont);
        formPanel.add(studentNumberLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        studentNumberField = new JTextField(15); // 필드 너비 조정
        studentNumberField.setFont(fieldFont);
        formPanel.add(studentNumberField, gbc);
        gbc.weightx = 0;

        // 이름
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel nameLabel = new JLabel("이름:");
        nameLabel.setFont(labelFont);
        formPanel.add(nameLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        nameField = new JTextField(15);
        nameField.setFont(fieldFont);
        formPanel.add(nameField, gbc);

        // 생년월일
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel birthDateLabel = new JLabel("생년월일:");
        birthDateLabel.setFont(labelFont);
        formPanel.add(birthDateLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -20); // 기본값을 오늘로부터 20년 전으로 설정
        Date defaultBirthDate = cal.getTime();
        SpinnerDateModel birthDateModel = new SpinnerDateModel(defaultBirthDate, null, new Date(), Calendar.DAY_OF_MONTH);
        birthDateSpinner = new JSpinner(birthDateModel);
        birthDateSpinner.setEditor(new JSpinner.DateEditor(birthDateSpinner, "yyyy-MM-dd"));
        birthDateSpinner.setFont(fieldFont);
        formPanel.add(birthDateSpinner, gbc);

        // 학과
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel departmentLabel = new JLabel("학과:");
        departmentLabel.setFont(labelFont);
        formPanel.add(departmentLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 3;
        departmentComboBox = new JComboBox<>();
        departmentComboBox.setFont(fieldFont);
        loadDepartmentsIntoComboBox(); // 학과 목록 로드
        formPanel.add(departmentComboBox, gbc);

        // 학년
        gbc.gridx = 0; gbc.gridy = 4;
        JLabel gradeLabel = new JLabel("학년:");
        gradeLabel.setFont(labelFont);
        formPanel.add(gradeLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 4;
        String[] grades = {"1학년", "2학년", "3학년", "4학년"}; // 최대 학년은 실제에 맞게 조정
        gradeComboBox = new JComboBox<>(grades);
        gradeComboBox.setFont(fieldFont);
        formPanel.add(gradeComboBox, gbc);

        // 상태
        gbc.gridx = 0; gbc.gridy = 5;
        JLabel statusLabel = new JLabel("상태:");
        statusLabel.setFont(labelFont);
        formPanel.add(statusLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 5;
        String[] statuses = {"재학", "휴학", "졸업", "자퇴", "퇴학"};
        statusComboBox = new JComboBox<>(statuses);
        statusComboBox.setFont(fieldFont);
        formPanel.add(statusComboBox, gbc);

        add(formPanel, BorderLayout.CENTER);

        // 하단 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton saveButton = new JButton("저장");
        saveButton.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        JButton cancelButton = new JButton("취소");
        cancelButton.setFont(new Font("맑은 고딕", Font.PLAIN, 13));

        saveButton.addActionListener(e -> saveStudent());
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
     * 학과 목록을 DepartmentService를 통해 가져와 콤보박스에 채웁니다.
     */
    private void loadDepartmentsIntoComboBox() {
        try {
            List<Department> departments = departmentService.getAllDepartments();
            departmentComboBox.addItem(new DepartmentItem(0, "-- 학과 선택 --")); // 안내 메시지
            if (departments != null) {
                for (Department dept : departments) {
                    departmentComboBox.addItem(new DepartmentItem(dept.getDpmtId(), dept.getDpmtName()));
                }
            }
        } catch (ServiceException e) {
            JOptionPane.showMessageDialog(this, "학과 목록 로드 중 오류 발생: " + e.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 수정 모드일 때, 전달받은 studentToEdit DTO의 정보로 UI 필드를 채웁니다.
     */
    private void populateFieldsForEdit() {
        if (studentToEdit == null) return;

        studentNumberField.setText(studentToEdit.getStudentNumber());
        nameField.setText(studentToEdit.getName());

        if (studentToEdit.getBirthDate() != null) {
            birthDateSpinner.setValue(Date.from(studentToEdit.getBirthDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }

        // 학과 선택
        if (studentToEdit.getDpmtId() > 0) {
            for (int i = 0; i < departmentComboBox.getItemCount(); i++) {
                if (departmentComboBox.getItemAt(i) instanceof DepartmentItem &&
                        ((DepartmentItem) departmentComboBox.getItemAt(i)).getId() == studentToEdit.getDpmtId()) {
                    departmentComboBox.setSelectedIndex(i);
                    break;
                }
            }
        } else {
            departmentComboBox.setSelectedIndex(0); // "-- 학과 선택 --"
        }

        // 학년 선택
        if (studentToEdit.getGrade() > 0) {
            String gradeToSelect = studentToEdit.getGrade() + "학년";
            for (int i = 0; i < gradeComboBox.getItemCount(); i++) {
                if (gradeToSelect.equals(gradeComboBox.getItemAt(i))) {
                    gradeComboBox.setSelectedIndex(i);
                    break;
                }
            }
        } else {
            gradeComboBox.setSelectedIndex(0); // 기본값 (예: 1학년)
        }

        // 상태 선택
        if (studentToEdit.getStatus() != null) {
            statusComboBox.setSelectedItem(studentToEdit.getStatus());
        } else {
            statusComboBox.setSelectedItem("재학"); // 기본값
        }
    }

    /**
     * UI 필드에서 입력된 정보로 UserDto를 생성하고, UserService를 통해 저장/수정합니다.
     */
    private void saveStudent() {
        // ... (유효성 검사 로직 - 이전과 동일)
        String studentNumber = studentNumberField.getText().trim();
        String name = nameField.getText().trim();
        Date birthDateUtil = (Date) birthDateSpinner.getValue();
        DepartmentItem selectedDeptItem = (DepartmentItem) departmentComboBox.getSelectedItem();
        String selectedGradeStr = (String) gradeComboBox.getSelectedItem();
        String selectedStatus = (String) statusComboBox.getSelectedItem();

        if (studentNumber.isEmpty() || name.isEmpty() || birthDateUtil == null || (selectedDeptItem == null || selectedDeptItem.getId() == 0)) {
            JOptionPane.showMessageDialog(this, "학번, 이름, 생년월일, 학과는 필수 입력 항목입니다.", "입력 오류", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate birthDate = birthDateUtil.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        int grade = Integer.parseInt(selectedGradeStr.replace("학년", ""));

        // DTO에 현재 UI의 값을 채움
        UserDto userDto = (studentToEdit != null) ? studentToEdit : new UserDto();
        userDto.setStudentNumber(studentNumber);
        userDto.setName(name);
        userDto.setBirthDate(birthDate);
        userDto.setDpmtId(selectedDeptItem.getId());
        userDto.setDepartmentName(selectedDeptItem.getName());
        userDto.setGrade(grade);
        userDto.setStatus(selectedStatus);
        userDto.setLevel(1);

        if (isPreviewMode) {
            // 미리보기 모드: DB 저장 없이 콜백만 호출
            if (onUpdateCallback != null) {
                onUpdateCallback.accept(userDto);
            }
            isSaved = true;
            dispose();
        } else {
            // 기존 모드: DB에 저장
            try {
                if (studentToEdit == null) {
                    userService.registerStudent(userDto);
                    JOptionPane.showMessageDialog(this, "학생이 성공적으로 등록되었습니다.\n초기 비밀번호는 'a' + 생년월일 8자리입니다.", "등록 완료", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    userService.updateStudentInfo(studentToEdit.getUserId(), userDto);
                    JOptionPane.showMessageDialog(this, "학생 정보가 성공적으로 수정되었습니다.", "수정 완료", JOptionPane.INFORMATION_MESSAGE);
                }
                isSaved = true;
                if (onSaveSuccessCallback != null) {
                    onSaveSuccessCallback.run();
                }
                dispose();
            } catch (ServiceException | SQLException | DaoException ex) {
                JOptionPane.showMessageDialog(this, "학생 정보 저장 중 오류 발생:\n" + ex.getMessage(), "저장 오류", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 다이얼로그가 성공적으로 저장되었는지 여부를 반환합니다. (선택적 사용)
     * @return 저장 성공 시 true
     */
    public boolean isSaved() {
        return isSaved;
    }

    /**
     * 학과 JComboBox에 사용될 아이템 내부 클래스입니다.
     */
    private static class DepartmentItem {
        private int id;
        private String name;
        public DepartmentItem(int id, String name) { this.id = id; this.name = name; }
        public int getId() { return id; }
        public String getName() { return name; }
        @Override public String toString() { return name; }
    }
}