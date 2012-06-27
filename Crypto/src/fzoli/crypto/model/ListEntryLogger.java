package fzoli.crypto.model;

import java.sql.SQLException;
import com.j256.ormlite.dao.Dao;

import fzoli.crypto.database.ListEntry;

public class ListEntryLogger {
	
	private final Dao<ListEntry, Integer> DAO;
	private ListEntryStorage entries;
	
	public ListEntryLogger(Dao<ListEntry, Integer> dao) {
		this.DAO = dao;
		initEntries();
	}
	
	private void initEntries() {
		try {
			entries = new ListEntryStorage(DAO.queryForAll());
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public ListEntryStorage getEntries() {
		return entries;
	}
	
	private boolean addEntry(ListEntry entry) {
		try {
			DAO.create(entry);
			entries.add(entry);
			return true;
		}
		catch (SQLException e) {
			return false;
		}
	}
	
	private boolean updateEntry(ListEntry entry) {
		try {
			DAO.update(entry);
			entries.set(entries.findIndex(entry), entry);
			return true;
		}
		catch(SQLException e) {
			return false;
		}
	}
	
	public boolean addOrUpdateEntry(ListEntry entry) {
		return entries.contains(entry) ? updateEntry(entry) : addEntry(entry);
	}
	
	public boolean removeEntry(ListEntry entry) {
		return removeEntry(entry.getId());
	}
	
	private boolean removeEntry(int id) {
		try {
			ListEntry entry = getEntries().findEntry(id);
			DAO.delete(entry);
			entries.remove(entry);
			return true;
		}
		catch(SQLException e) {
			return false;
		}
	}
	
	public static boolean contains(Dao<ListEntry, Integer> dao, ListEntry entry) {
		return contains(dao, entry.getName());
	}
	
	private static boolean contains(Dao<ListEntry, Integer> dao, String name) {
		try {
			return dao.queryForEq("name", name).size() > 0;
		}
		catch (Exception e) {
			return false;
		}
	}
	
}