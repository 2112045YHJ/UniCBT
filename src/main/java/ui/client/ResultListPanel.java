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
            // 1. 나에게 배정된 모든 시험 조회 (과거·현재·미래)
            List<Exam> allExams = examService.getAssignedExams(user.getUserId());

            // 2. 사용자의 시험 결과 전체 조회 (Map<examId, ExamResult>)
            Map<Integer, ExamResult> resultMap = examService.getExamResultsByUser(user.getUserId());

            // 3. 연도별로 시험 분류
            Map<Integer, List<Exam>> yearExamMap = new TreeMap<>(Comparator.reverseOrder());
            for (Exam exam : allExams) {
                int year = exam.getStartDate().getYear();
                yearExamMap.computeIfAbsent(year, k -> new ArrayList<>()).add(exam);
            }

            // 4. 연도별 탭 패널 추가
            for (Map.Entry<Integer,List<Exam>> entry : yearExamMap.entrySet()) {
                int year = entry.getKey();
                List<Exam> examsOfYear = entry.getValue();

                JPanel yearPanel = new JPanel(new BorderLayout());
                String[] columns = {"과목명", "점수", "응시일자"};
                DefaultTableModel model = new DefaultTableModel(columns, 0) {
                    @Override public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };

                for (Exam exam : examsOfYear) {
                    ExamResult result = resultMap.get(exam.getExamId());
                    if (result != null) {
                        String scoreStr = result.getScore() + " / 100";
                        String dateStr = result.getCompletedAt() != null
                                ? "응시 날짜 : " + result.getCompletedAt()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                : "응시 날짜 : -";
                        model.addRow(new Object[]{ exam.getSubject(), scoreStr, dateStr });
                    } else {
                        model.addRow(new Object[]{ exam.getSubject(), "미응시", "미응시" });
                    }
                }

                JTable table = new JTable(model);
                table.setRowHeight(28);
                table.setFont(new Font("맑은 고딕", Font.PLAIN, 15));
                table.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 14));
                yearPanel.add(new JScrollPane(table), BorderLayout.CENTER);

                tabbedPane.addTab(String.valueOf(year), yearPanel);
            }

            // 배정된 시험이 하나도 없으면
            if (tabbedPane.getTabCount() == 0) {
                JPanel empty = new JPanel(new BorderLayout());
                JLabel msg = new JLabel("배정된 시험이 없습니다.", SwingConstants.CENTER);
                msg.setFont(new Font("맑은 고딕", Font.BOLD, 16));
                empty.add(msg, BorderLayout.CENTER);
                tabbedPane.addTab("결과 없음", empty);
            }

        } catch (ServiceException e) {
            JPanel error = new JPanel(new BorderLayout());
            JLabel msg = new JLabel("<html>시험 결과 조회 중 오류가 발생했습니다:<br>"
                    + e.getMessage() + "</html>", SwingConstants.CENTER);
            msg.setFont(new Font("맑은 고딕", Font.BOLD, 16));
            error.add(msg, BorderLayout.CENTER);
            tabbedPane.addTab("오류", error);
        } catch (DaoException e) {
            throw new RuntimeException(e);
        }

        add(tabbedPane, BorderLayout.CENTER);
    }
}
