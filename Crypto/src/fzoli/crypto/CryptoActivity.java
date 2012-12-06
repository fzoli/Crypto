package fzoli.crypto;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.j256.ormlite.android.apptools.OrmLiteBaseListActivity;

import fzoli.crypto.database.DatabaseHelper;
import fzoli.crypto.database.ListEntry;
import fzoli.crypto.luks.LUKSManager;
import fzoli.crypto.luks.LoopbackLogger;
import fzoli.crypto.model.ListEntryLogger;
import fzoli.view.ConfirmDialog;
import fzoli.view.PasswordInputDialog;
import fzoli.view.ThreadIndicator;
import fzoli.view.ThreadIndicatorAction;

public class CryptoActivity extends OrmLiteBaseListActivity<DatabaseHelper> {
	
	public static final String ENTRY = "entry";
	public static final String CRYPTSETUP = "cryptsetup";
	public static final String ENTRY_STORAGE = "entry_storage";
	public static final String ENTRY_MOUNTED = "entry_mounted";
	public static final String ENTRY_UMOUNTED = "entry_umounted";
	
	private static final int ADD = 0;
	
	private LUKSManager luksManager;
	private ListEntryLogger listLogger;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        initDbObjects();
        showInitMessages();
        initService();
        initList();
    }
    
    private Menu menu;

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	this.menu = menu;
    	setMenu();
        return true;
	}
    
    private void setMenu() {
    	menu.clear();
    	int menuId = isMountedEntry() ? R.menu.umount_menu : R.menu.main_menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(menuId, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    		case R.id.addEntry:
    			startAddActivity();
    			return true;
    		case R.id.umountAllEntry:
    			showUmountAllConfirmDialog();
    			return true;
    		default:
    			return super.onOptionsItemSelected(item); 
    	}
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	if (requestCode == ADD && resultCode == AddActivity.OK) {
    		listLogger.addOrUpdateEntry((ListEntry) data.getExtras().getSerializable(ENTRY));
    		initList();
    	}
    }
    
    private boolean isMountedEntry() {
    	List<ListEntry> entries = listLogger.getEntries();
    	for (ListEntry e : entries) {
    		if (luksManager.isUsed(e)) return true;
    	}
    	return false;
    }
    
    private void startAddActivity() {
    	startAddActivity(null);
    }
    
    private void startAddActivity(ListEntry entry) {
    	Intent intent = new Intent(CryptoActivity.this, AddActivity.class);
		intent.putExtra(ENTRY, entry);
        startActivityForResult(intent, ADD);
    }
    
    private CharSequence[] createListItemOptions(ListEntry entry) {
    	List<CharSequence> l = new ArrayList<CharSequence>();
    	boolean used = luksManager.isUsed(entry);
    	if (luksManager.isBinaryExists() && luksManager.isPathsExists(entry)) {
    		l.add(used ? getString(R.string.umount) : getString(R.string.mount));
    	}
    	if (!used) {
    		l.add(getString(R.string.edit));
    		l.add(getString(R.string.delete));
    	}
    	CharSequence[] csa = new CharSequence[l.size()];
    	for (int i = 0; i < l.size(); i++) {
    		csa[i] = l.get(i);
    	}
    	return csa;
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	super.onListItemClick(l, v, position, id);
    	final ListEntry selected = listLogger.getEntries().get(position);
    	showChooseDialog(selected);
    }
    
    private void umountAll() {
    	umountIndicator(new ThreadIndicatorAction() {
			
			@Override
			public boolean run() {
				boolean success = true;
				for (ListEntry entry : listLogger.getEntries()) {
					luksManager.umount(entry);
					success &= !luksManager.isUsed(entry);
				}
				if (success) {
					setMenu();
					stopService(new Intent(CryptoActivity.this, CryptoService.class));
				}
				return success;
			}
			
		});
    }
    
    private void umount(final ListEntry entry) {
    	umountIndicator(new ThreadIndicatorAction() {
			
			@Override
			public boolean run() {
				boolean success = luksManager.umount(entry);
				if (success) {
					startService(ENTRY_UMOUNTED, entry);
				}
				return success;
			}
			
		});
    }
    
    private void umountIndicator(ThreadIndicatorAction action) {
    	new ThreadIndicator(CryptoActivity.this, action, getString(R.string.umounting), getString(R.string.umountSuccess), getString(R.string.umountFail));
    }
    
    private void mount(final ListEntry entry, final String password) {
        new ThreadIndicator(CryptoActivity.this, new ThreadIndicatorAction() {
			
			@Override
			public boolean run() {
				boolean success = luksManager.mount(entry, password);
				if (success) {
					startService(ENTRY_MOUNTED, entry);
				}
				return success;
			}
			
		}, getString(R.string.mounting), getString(R.string.mountSuccess), getString(R.string.mountFail));
    }
    
    private void setBinaryFile() {
    	showToas(getString(R.string.needCryptsetup));
    	try {
    		int length;
    		byte[] buffer = new byte[1024];
    		InputStream in = getAssets().open(CRYPTSETUP);
    		OutputStream out = openFileOutput(CRYPTSETUP, MODE_PRIVATE);
    		getFileStreamPath(CRYPTSETUP).setExecutable(true);
    		while ((length = in.read(buffer)) != -1) {
    			out.write(buffer, 0, length);
    		}
    		out.flush();
    		out.close();
    		in.close();
    		showToas(getString(R.string.installedCryptsetup));
    	}
    	catch (Exception ex) {
    		showToas(getString(R.string.notInstalledCryptsetup));
    	}
    }
    
    private void showSdSharedMsgIfNeed() {
    	String state = Environment.getExternalStorageState();
		if(state.equals(Environment.MEDIA_SHARED)) {
			showToas(getString(R.string.sdShared));
		}
    }
    
    private void showChooseDialog(final ListEntry entry) {
    	if (!luksManager.isBinaryExists() && luksManager.isPathsExists(entry)) setBinaryFile();
    	final CharSequence[] items = createListItemOptions(entry);
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle(entry.getName());
    	builder.setItems(items, new DialogInterface.OnClickListener() {
    	    
    		public void onClick(DialogInterface dialog, int item) {
    			if (!luksManager.isBinaryExists() || !luksManager.isPathsExists(entry)) item++;
    			switch (item) {
    				case 0:
    					if (!luksManager.isUsed(entry)) createListPasswordInputDialog(entry).show();
    					else showUmountEntryConfirmDialog(entry);
    					break;
    				case 1:
    					startAddActivity(entry);
    					break;
    				case 2:
    					showDeleteConfirmDialog(entry);
    					break;
    			}
    	    }
    		
    	});
    	builder.create().show();
    }
    
    private void showDeleteConfirmDialog(final ListEntry entry) {
    	showConfirmDialog(getString(R.string.deleting_) + " " + entry.getName(), getString(R.string.deleteConfirmMsg), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				listLogger.removeEntry(entry);
				initList();
			}
			
		});
    }
    
    private void showUmountAllConfirmDialog() {
    	showUmountConfirmDialog(getString(R.string.umountAll), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				umountAll();
			}
			
		});
    }
    
    private void showUmountEntryConfirmDialog(final ListEntry entry) {
    	showUmountConfirmDialog(getString(R.string.umounting_) + " " + entry.getName(), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				umount(entry);
			}
			
		});
    }
    
    private void showUmountConfirmDialog(String title, DialogInterface.OnClickListener action) {
    	showConfirmDialog(title, getString(R.string.umountConfirmMsg), action);
    }
    
    private void showConfirmDialog(String title, String message, DialogInterface.OnClickListener event) {  	
    	new ConfirmDialog(this, title, message, getString(R.string.yes), getString(R.string.no), event).show();
    }
    
    private PasswordInputDialog createListPasswordInputDialog(final ListEntry entry) {
    	final PasswordInputDialog alert = new PasswordInputDialog(this, getString(R.string.password), getString(R.string.ok), getString(R.string.cancel));

		alert.setPositiveEvent(new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int whichButton) {
				String passwd = alert.getPassword();
				if (!passwd.isEmpty()) mount(entry, passwd);
				else showToas(getString(R.string.needPassword));
			}
			
		});
		
		return alert;
    }
    
    private void showToas(String msg) {
    	Toast.makeText(CryptoActivity.this, msg, Toast.LENGTH_SHORT).show();
    }
    
    private void initDbObjects() {
		DatabaseHelper helper = getHelper();
        luksManager = new LUKSManager(new LoopbackLogger(helper.getLoopbackLogDao()), getFileStreamPath(CRYPTSETUP).getAbsolutePath());
        listLogger = new ListEntryLogger(helper.getListEntryDao());
	}
	
    private void showInitMessages() {
    	showSdSharedMsgIfNeed();
    	if (!luksManager.isBinaryExists()) setBinaryFile();
    }
    
	private void initList() {
		List<String> names = listLogger.getEntries().getNames();
		setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, names));
		if (names.isEmpty()) showToas(getString(R.string.emptyList));
	}
	
	private void initService() {
		startService(ENTRY_STORAGE, listLogger.getEntries());
	}
	
	private void startService(String key, Serializable obj) {
		Intent i = new Intent(this, CryptoService.class);
		i.putExtra(key, obj);
		startService(i);
	}
	
}