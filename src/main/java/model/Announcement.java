package main.java.model;

import java.time.LocalDateTime;

/**
 * 공지사항 모델 클래스
 * announcement 테이블과 1:1 매핑되는 DTO
 */
public class Announcement {
    private int announcementId;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int readCount;

    /** 기본 생성자 */
    public Announcement() { }

    /**
     * 전체 필드를 사용하는 생성자
     * @param announcementId 공지사항 고유 ID
     * @param title          제목
     * @param content        내용
     * @param createdAt      생성 일시
     * @param updatedAt      수정 일시
     * @param readCount      조회수
     */
    public Announcement(int announcementId, String title, String content,
                        LocalDateTime createdAt, LocalDateTime updatedAt, int readCount) {
        this.announcementId = announcementId;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.readCount = readCount;
    }

    /**
     * 저장할 때 사용할 생성자 (ID, 날짜, 조회수는 DB에서 자동 생성)
     * @param title   제목
     * @param content 내용
     */
    public Announcement(String title, String content) {
        this.title = title;
        this.content = content;
    }

    // --- getters & setters ---

    public int getAnnouncementId() {
        return announcementId;
    }

    public void setAnnouncementId(int announcementId) {
        this.announcementId = announcementId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getReadCount() {
        return readCount;
    }

    public void setReadCount(int readCount) {
        this.readCount = readCount;
    }

    @Override
    public String toString() {
        return "announcements{" +
                "announcementId=" + announcementId +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", readCount=" + readCount +
                '}';
    }
}