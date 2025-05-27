// AnnouncementServiceImpl.java
package main.java.service;

import main.java.dao.AnnouncementDao;
import main.java.dao.AnnouncementDaoImpl;
import main.java.dao.DaoException;
import main.java.model.Announcement;
import java.util.List;

public class AnnouncementServiceImpl implements AnnouncementService {
    private final AnnouncementDao dao = new AnnouncementDaoImpl();

    @Override
    public List<Announcement> getAllAnnouncements() throws DaoException {
        return dao.findAll();
    }
    @Override
    public Announcement getAnnouncement(int id) throws DaoException {
        return dao.findById(id);
    }
    @Override
    public void createAnnouncement(Announcement ann) throws DaoException {
        dao.save(ann);
    }
    @Override
    public void modifyAnnouncement(Announcement ann) throws DaoException {
        dao.update(ann);
    }
    @Override
    public void removeAnnouncement(int id) throws DaoException {
        dao.delete(id);
    }
}