package main.java.ui.client;

import main.java.dao.DaoException;
import main.java.model.User;
import main.java.model.Exam;
import main.java.model.ExamResult;
import main.java.service.ExamService;
import main.java.service.ExamServiceImpl;
import main.java.service.ServiceException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class ResultListPanel extends JPanel {
    private final User user;
    private final ExamService examService = new ExamServiceImpl();

    public ResultListPanel(User user) {
        this.user = user;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // 상단 제목
        JLabel header = new JLabel("시험 결과");
        header.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(header, BorderLayout.NORTH);

        // 연도별 탭 생성
        JTabbedPane tabbedPane = new JTabbedPane();

        try {
            // 1. 내 학과의 모든 학년(1~3학년)의 시험을 합친다 (중복 없이)
            Set<Exam> allExamsSet = new HashSet<>();
            int dpmtId = user.getDpmtId();
            for (int grade = 1; grade <= 3; grade++) {
                allExamsSet.addAll(examService.getAllExams(dpmtId, grade));
            }
            List<Exam> allExams = new ArrayList<>(allExamsSet);
            allExams.sort(Comparator.comparing(Exam::getStartDate).reversed());

            // 2. 사용자의 시험 결과 전체 조회 (Map<examId, ExamResult>)
            Map<Integer, ExamResult> resultMap = examService.getExamResultsByUser(user.getUserId());

            // 3. 연도별로 시험 분류
            Map<Integer, List<Exam>> yearExamMap = new TreeMap<>(Comparator.reverseOrder());
            for (Exam exam : allExams) {
                int year = exam.getStartDate().getYear();
                yearExamMap.computeIfAbsent(year, k -> new ArrayList<>()).add(exam);
            }

            // 4. 연도별 탭 패널 추가
            for (int year : yearExamMap.keySet()) {
                JPanel yearPanel = new JPanel(new BorderLayout());
                String[] columns = {"과목명", "점수", "응시일자"};
                DefaultTableModel model = new DefaultTableModel(columns, 0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };

                for (Exam exam : yearExamMap.get(year)) {
                    ExamResult result = resultMap.get(exam.getExamId());
                    if (result != null) {
                        String scoreStr = result.getScore() + " / 100";
                        String dateStr = (result.getCompletedAt() != null)
                                ? "응시 날짜 : " + result.getCompletedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                : "응시 날짜 : -";
                        model.addRow(new Object[]{
                                exam.getSubject(),
                                scoreStr,
                                dateStr
                        });
                    } else {
                        model.addRow(new Object[]{
                                exam.getSubject(),
                                "미응시",
                                "미응시"
                        });
                    }
                }

                JTable table = new JTable(model);
                table.setRowHeight(28);
                table.setFont(new Font("맑은 고딕", Font.PLAIN, 15));
                table.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 14));
                JScrollPane scrollPane = new JScrollPane(table);
                yearPanel.add(scrollPane, BorderLayout.CENTER);

                tabbedPane.addTab(String.valueOf(year), yearPanel);
            }

            // 시험이 하나도 없을 때 기본 메시지 패널 추가
            if (tabbedPane.getTabCount() == 0) {
                JPanel emptyPanel = new JPanel(new BorderLayout());
                JLabel msg = new JLabel("응시 가능한 시험이 없습니다.", SwingConstants.CENTER);
                msg.setFont(new Font("맑은 고딕", Font.BOLD, 16));
                emptyPanel.add(msg, BorderLayout.CENTER);
                tabbedPane.addTab("결과 없음", emptyPanel);
            }

        } catch (ServiceException | DaoException e) {
            JPanel errorPanel = new JPanel(new BorderLayout());
            JLabel msg = new JLabel("시험 결과 조회 중 오류가 발생했습니다:\n" + e.getMessage(), SwingConstants.CENTER);
            msg.setFont(new Font("맑은 고딕", Font.BOLD, 16));
            errorPanel.add(msg, BorderLayout.CENTER);
            tabbedPane.addTab("오류", errorPanel);
        }

        add(tabbedPane, BorderLayout.CENTER);
    }
}
