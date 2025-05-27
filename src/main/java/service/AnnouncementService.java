package main.java.service;

import main.java.dao.DaoException;
import main.java.model.Announcement;
import java.util.List;

public interface AnnouncementService {
    List<Announcement> getAllAnnouncements() throws DaoException;
    Announcement getAnnouncement(int id) throws DaoException;
    void createAnnouncement(Announcement ann) throws DaoException;
    void modifyAnnouncement(Announcement ann) throws DaoException;
    void removeAnnouncement(int id) throws DaoException;
}
