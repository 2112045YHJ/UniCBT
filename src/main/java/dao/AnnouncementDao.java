package main.java.dao;

import main.java.model.Announcement;
import java.util.List;

public interface AnnouncementDao {
    List<Announcement> findAll() throws DaoException;
    Announcement findById(int id) throws DaoException;
    void save(Announcement ann) throws DaoException;
    void update(Announcement ann) throws DaoException;
    void delete(int id) throws DaoException;
}