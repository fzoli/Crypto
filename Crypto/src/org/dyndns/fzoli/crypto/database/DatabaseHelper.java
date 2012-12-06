package org.dyndns.fzoli.crypto.database;

import java.sql.SQLException;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

public class DatabaseHelper extends OrmLiteSqliteOpenHelper {
	
	private final static int DB_VERSION = 2;
	private final static String DB_NAME = "crypto.db";
	//private final static File SDCARD = Environment.getExternalStorageDirectory();
	//private final static String DB_FILE = SDCARD.getAbsolutePath() + File.separator + DB_NAME;
	private final static String DB_FILE = DB_NAME;
	
	private Dao<ListEntry, Integer> listEntryDao;
	private Dao<LoopbackLog, String> loopbackLogDao;
	
	public DatabaseHelper(Context context) {
		super(context, DB_FILE, null, DB_VERSION);
	}

	@SuppressWarnings("unchecked")
	public Dao<ListEntry, Integer> getListEntryDao() {
		return getCachedDao(ListEntry.class, listEntryDao);
	}
	
	@SuppressWarnings("unchecked")
	public Dao<LoopbackLog, String> getLoopbackLogDao() {
		return getCachedDao(LoopbackLog.class, loopbackLogDao);
	}
	
	@SuppressWarnings("unchecked")
	private Dao getCachedDao(Class c, Dao d) {
		try {
			if (d == null) d = getDao(c);
		}
		catch(SQLException e) {
			throw new RuntimeException(e);
		}
		return d;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
		try {
			TableUtils.createTable(connectionSource, ListEntry.class);
			TableUtils.createTable(connectionSource, LoopbackLog.class);
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
		try {
			TableUtils.dropTable(connectionSource, ListEntry.class, true);
			TableUtils.dropTable(connectionSource, LoopbackLog.class, true);
			onCreate(db, connectionSource);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

}