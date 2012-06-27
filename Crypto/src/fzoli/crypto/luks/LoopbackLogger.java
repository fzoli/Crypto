package fzoli.crypto.luks;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.j256.ormlite.dao.Dao;

import fzoli.crypto.database.LoopbackLog;

public class LoopbackLogger {
	
	private final Dao<LoopbackLog, String> DAO;
	private final Map<String, String> devices = new HashMap<String, String>();
	
	public LoopbackLogger(Dao<LoopbackLog, String> dao) {
		this.DAO = dao;
		initDevices();
	}
	
	private void initDevices() {
		try {
			List<LoopbackLog> logs = DAO.queryForAll();
			for (LoopbackLog log : logs) {
				devices.put(log.getMapper(), log.getLoDevice());
			}
		}
		catch (SQLException e) {
			;
		}
	}
	
	public String getLog(String mapper) {
		return devices.get(mapper);
	}
	
	public boolean addLog(String mapper, String loDevice) {
		try {
			LoopbackLog log = new LoopbackLog(mapper, loDevice);
			if (getLog(mapper) == null) DAO.create(log);
			else DAO.update(log);
			devices.put(mapper, loDevice);
			return true;
		}
		catch (SQLException e) {
			return false;
		}
	}
	
	public boolean removeLog(String mapper) {
		try {
			DAO.delete(new LoopbackLog(mapper, null));
			devices.remove(mapper);
			return true;
		}
		catch(SQLException e) {
			return false;
		}
	}
	
}