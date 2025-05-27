package main.java.model;

import java.time.LocalDateTime;

public class Department {
    private int dpmtId;
    private String dpmtName;
    private String faculty;
    private String contactEmail;
    private String contactPhone;
    private LocalDateTime createdAt;

    public Department() {
    }

    public int getDpmtId() {
        return dpmtId;
    }

    public void setDpmtId(int dpmtId) {
        this.dpmtId = dpmtId;
    }

    public String getDpmtName() {
        return dpmtName;
    }

    public void setDpmtName(String dpmtName) {
        this.dpmtName = dpmtName;
    }

    public String getFaculty() {
        return faculty;
    }

    public void setFaculty(String faculty) {
        this.faculty = faculty;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}