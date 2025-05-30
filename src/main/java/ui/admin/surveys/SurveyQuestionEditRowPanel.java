package main.java.ui.admin.surveys;

import main.java.dto.SurveyFullDto;
import main.java.model.SurveyQuestion;
import main.java.model.SurveyQuestionOption;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * SurveyEditorFrame 내에서 개별 설문 질문(객관식 또는 주관식)을 편집하기 위한 패널입니다.
 */
public class SurveyQuestionEditRowPanel extends JPanel {

    private boolean isMCQ;
    private Consumer<SurveyQuestionEditRowPanel> onDeleteCallback;

    private JLabel questionNumberLabel;
    private JTextArea questionTextArea;
    private JPanel optionsListPanel; // 선택지 행(singleOptionPanel)들을 담을 패널
    private List<SingleOptionEditPanel> singleOptionPanelsList; // 각 선택지 행 패널을 관리하는 리스트

    private static final int MAX_OPTIONS = 5;

    public SurveyQuestionEditRowPanel(boolean isMCQ, Consumer<SurveyQuestionEditRowPanel> onDeleteCallback) {
        this.isMCQ = isMCQ;
        this.onDeleteCallback = onDeleteCallback;
        this.singleOptionPanelsList = new ArrayList<>();

        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                new EmptyBorder(10, 5, 10, 5)
        ));
        initComponents();
    }

    private void initComponents() {
        JPanel topPanel = new JPanel(new BorderLayout(10,0));
        questionNumberLabel = new JLabel("Q:");
        questionNumberLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));

        JLabel questionTypeLabel = new JLabel(isMCQ ? "[객관식]" : "[주관식]");
        questionTypeLabel.setFont(new Font("맑은 고딕", Font.ITALIC, 12));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5,0));
        titlePanel.add(questionNumberLabel);
        titlePanel.add(questionTypeLabel);
        topPanel.add(titlePanel, BorderLayout.WEST);

        JButton deleteQuestionButton = new JButton("이 질문 삭제");
        deleteQuestionButton.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        deleteQuestionButton.setMargin(new Insets(2,5,2,5));
        deleteQuestionButton.addActionListener(e -> {
            if (onDeleteCallback != null) {
                onDeleteCallback.accept(this);
            }
        });
        topPanel.add(deleteQuestionButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        questionTextArea = new JTextArea(3, 40);
        questionTextArea.setLineWrap(true);
        questionTextArea.setWrapStyleWord(true);
        questionTextArea.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        JScrollPane questionScrollPane = new JScrollPane(questionTextArea);
        questionScrollPane.setBorder(BorderFactory.createTitledBorder("질문 내용"));

        JPanel centerContainer = new JPanel(); // 질문 내용과 옵션 패널을 담을 컨테이너
        centerContainer.setLayout(new BoxLayout(centerContainer, BoxLayout.Y_AXIS));
        centerContainer.add(questionScrollPane);

        if (isMCQ) {
            centerContainer.add(Box.createVerticalStrut(5)); // 질문과 옵션 사이 간격

            JPanel optionsOuterPanel = new JPanel(new BorderLayout()); // 옵션리스트와 추가버튼을 감싸는 패널
            optionsOuterPanel.setBorder(BorderFactory.createTitledBorder("선택지 (최대 " + MAX_OPTIONS + "개)"));

            optionsListPanel = new JPanel(); // 실제 선택지 행들이 들어갈 패널
            optionsListPanel.setLayout(new BoxLayout(optionsListPanel, BoxLayout.Y_AXIS));
            optionsOuterPanel.add(new JScrollPane(optionsListPanel), BorderLayout.CENTER); // 스크롤 가능하도록

            // 기본 선택지 추가
            addOptionRowUI(null);
            addOptionRowUI(null);

            JButton addOptionButton = new JButton("선택지 추가");
            addOptionButton.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
            addOptionButton.setMargin(new Insets(2,5,2,5));
            addOptionButton.addActionListener(e -> {
                if (singleOptionPanelsList.size() < MAX_OPTIONS) {
                    addOptionRowUI(null);
                } else {
                    JOptionPane.showMessageDialog(this, "최대 " + MAX_OPTIONS + "개까지만 선택지를 추가할 수 있습니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
                }
            });
            JPanel addOptionButtonContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
            addOptionButtonContainer.add(addOptionButton);
            optionsOuterPanel.add(addOptionButtonContainer, BorderLayout.SOUTH);

            centerContainer.add(optionsOuterPanel);
        }
        add(centerContainer, BorderLayout.CENTER);
    }

    /**
     * 객관식 선택지 입력 UI 행을 추가합니다.
     * @param optionText 선택지에 미리 채워넣을 텍스트 (null이면 빈 칸)
     */
    private void addOptionRowUI(String optionText) {
        if (!isMCQ || singleOptionPanelsList.size() >= MAX_OPTIONS) return;

        SingleOptionEditPanel newOptionPanel = new SingleOptionEditPanel(optionText, this::removeOptionRowUI);
        singleOptionPanelsList.add(newOptionPanel);
        optionsListPanel.add(newOptionPanel);

        refreshOptionLabelsAndPanel();
    }

    /**
     * 특정 선택지 입력 UI 행을 제거합니다.
     * @param optionPanelToRemove 제거할 SingleOptionEditPanel 객체
     */
    private void removeOptionRowUI(SingleOptionEditPanel optionPanelToRemove) {
        if (singleOptionPanelsList.size() <= 1) { // 최소 1개는 유지
            JOptionPane.showMessageDialog(this, "최소 1개의 선택지는 있어야 합니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        singleOptionPanelsList.remove(optionPanelToRemove);
        optionsListPanel.remove(optionPanelToRemove);

        refreshOptionLabelsAndPanel();
    }

    /**
     * 선택지 라벨(A, B, C...)을 순서대로 업데이트하고 패널을 새로고침합니다.
     */
    private void refreshOptionLabelsAndPanel() {
        for (int i = 0; i < singleOptionPanelsList.size(); i++) {
            singleOptionPanelsList.get(i).setOptionLabelText("  " + (char)('A' + i) + ". ");
        }
        if (optionsListPanel != null) {
            optionsListPanel.revalidate();
            optionsListPanel.repaint();
        }
    }

    public void setQuestionNumber(int number) {
        questionNumberLabel.setText("Q" + number + ".");
    }

    public void setData(SurveyQuestion question, List<SurveyQuestionOption> options) {
        questionTextArea.setText(question.getQuestionText());
        if (isMCQ) {
            // 기존 선택지 UI 모두 제거
            optionsListPanel.removeAll();
            singleOptionPanelsList.clear();

            if (options != null && !options.isEmpty()) {
                for (SurveyQuestionOption option : options) {
                    addOptionRowUI(option.getOptionText()); // UI 추가 및 리스트에 추가
                }
            } else { // 기존 선택지가 없으면 기본 2개 추가
                addOptionRowUI(null);
                addOptionRowUI(null);
            }
            refreshOptionLabelsAndPanel(); // 최종적으로 라벨 및 패널 갱신
        }
    }

    public SurveyFullDto.SurveyQuestionDto getQuestionData() throws IllegalStateException {
        String questionText = questionTextArea.getText().trim();
        if (questionText.isEmpty()) {
            throw new IllegalStateException("질문 내용을 입력해주세요. (" + questionNumberLabel.getText() + ")");
        }

        SurveyQuestion question = new SurveyQuestion();
        question.setQuestionText(questionText);
        question.setQuestionType(isMCQ ? "MCQ" : "TEXT");

        SurveyFullDto.SurveyQuestionDto qDto = new SurveyFullDto.SurveyQuestionDto();
        qDto.setQuestion(question);

        if (isMCQ) {
            if (singleOptionPanelsList.isEmpty()){
                throw new IllegalStateException("객관식 질문에는 최소 1개의 선택지가 필요합니다. (" + questionNumberLabel.getText() + ")");
            }
            List<SurveyQuestionOption> options = new ArrayList<>();
            int optionOrder = 1;
            for (SingleOptionEditPanel optionPanel : singleOptionPanelsList) {
                String optionTextValue = optionPanel.getOptionText();
                if (optionTextValue.isEmpty()) {
                    throw new IllegalStateException("비어 있는 선택지가 있습니다. 내용을 입력해주세요. (" + questionNumberLabel.getText() + " - " + optionPanel.getOptionLabelText().trim() +")");
                }
                SurveyQuestionOption option = new SurveyQuestionOption();
                option.setOptionText(optionTextValue);
                option.setOptionOrder(optionOrder++); // 순서 설정
                options.add(option);
            }
            if (options.isEmpty()){
                throw new IllegalStateException("객관식 질문에는 최소 1개의 선택지가 필요합니다. (" + questionNumberLabel.getText() + ")");
            }
            qDto.setOptions(options);
        }
        return qDto;
    }

    public boolean validateInputs() {
        try {
            getQuestionData();
            return true;
        } catch (IllegalStateException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "입력 오류", JOptionPane.WARNING_MESSAGE);
            return false;
        }
    }

    /**
     * 객관식 선택지 하나의 UI를 담당하는 내부 패널 클래스입니다.
     */
    private static class SingleOptionEditPanel extends JPanel {
        private JLabel optionLabelComponent;
        private JTextField optionTextField;
        private JButton removeButton;

        public SingleOptionEditPanel(String initialText, Consumer<SingleOptionEditPanel> removeCallback) {
            setLayout(new BorderLayout(5, 0));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 35)); // 높이 고정

            optionLabelComponent = new JLabel(); // 라벨은 refreshOptionLabels에서 설정
            optionLabelComponent.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
            add(optionLabelComponent, BorderLayout.WEST);

            optionTextField = new JTextField(30);
            optionTextField.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
            if (initialText != null) {
                optionTextField.setText(initialText);
            }
            add(optionTextField, BorderLayout.CENTER);

            removeButton = new JButton("X");
            removeButton.setFont(new Font("맑은 고딕", Font.BOLD, 10));
            removeButton.setMargin(new Insets(1, 3, 1, 3));
            removeButton.setForeground(Color.RED);
            removeButton.addActionListener(e -> removeCallback.accept(this));
            add(removeButton, BorderLayout.EAST);
        }

        public String getOptionText() {
            return optionTextField.getText().trim();
        }

        public void setOptionLabelText(String labelText) {
            optionLabelComponent.setText(labelText);
        }

        public String getOptionLabelText(){ // 유효성 검사용
            return optionLabelComponent.getText();
        }
    }
}