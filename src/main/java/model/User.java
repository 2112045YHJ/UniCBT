package main.java.model;

public class User {
    private int userId;
    private int level;               // 0=관리자, 1=학생
    private String name;
    private String studentNumber;
    private String password;
    private int dpmtId;
    private int grade;
    private boolean isActive;

    // 기본 생성자
    public User() {
    }

    // 필요한 필드만 받는 생성자
    public User(int userId, int level, String name, String studentNumber,
                int dpmtId, int grade, boolean isActive) {
        this.userId = userId;
        this.level = level;
        this.name = name;
        this.studentNumber = studentNumber;
        this.dpmtId = dpmtId;
        this.grade = grade;
        this.isActive = isActive;
    }

    // getters & setters...
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getDpmtId() {
        return dpmtId;
    }

    public void setDpmtId(int dpmtId) {
        this.dpmtId = dpmtId;
    }

    public int getGrade() {
        return grade;
    }

    public void setGrade(int grade) {
        this.grade = grade;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}