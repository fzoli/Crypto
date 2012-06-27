package fzoli.crypto;

import java.util.List;
import com.j256.ormlite.android.apptools.OrmLiteBaseService;
import fzoli.crypto.database.DatabaseHelper;
import fzoli.crypto.database.ListEntry;
import fzoli.crypto.luks.LUKSManager;
import fzoli.crypto.luks.LoopbackLogger;
import fzoli.crypto.model.ListEntryLogger;
import fzoli.crypto.model.ListEntryStorage;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

public class CryptoService extends OrmLiteBaseService<DatabaseHelper> {
	
	private static final int NOTIFICATION_ID = 1;
	private static final String CMD_KEY = "command";
	private static final String UMOUNT_ALL_CMD = "umount all";
	
	private ServiceHandler serviceHandler;
	private LUKSManager luksManager;
	private ListEntryStorage entries;
	
	private final class ServiceHandler extends Handler {
		
		public ServiceHandler(Looper looper) {
			super(looper);
		}
		
		private void umountAll() {
			for (ListEntry entry : entries) {
				luksManager.umount(entry);
			}
			removeUmountedEntries();
			stopServiceIfNeed();
		}
		
		@Override
		public void handleMessage(Message msg) {
			synchronized (this) {
				Bundle data = msg.getData();
				String cmd = data.getString(CMD_KEY);
				if (cmd.equals(UMOUNT_ALL_CMD)) {
					umountAll();
				}
			}
		}
		
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		initServiceHandler();
		initMountEventHandler();
	}
	
	private void initMountEventHandler() {
		IntentFilter ejectFilter = new IntentFilter(Intent.ACTION_MEDIA_EJECT);
		ejectFilter.addDataScheme("file");
		registerReceiver(new BroadcastReceiver(){

			@Override
			public void onReceive(Context arg0, Intent arg1) {
				sendServiceHandlerMessage(UMOUNT_ALL_CMD);
			}
	
		 }, ejectFilter);
	}

	private void initServiceHandler() {
		HandlerThread thread = new HandlerThread("ServiceHandlerThread",
	            Process.THREAD_PRIORITY_BACKGROUND);
	    thread.start();
	    serviceHandler = new ServiceHandler(thread.getLooper());
	}
	
	private void createNotification() {
		String notify = getString(R.string.serviceNotify);
		Notification notification = new Notification(android.R.drawable.stat_notify_sdcard_prepare, notify,
		        System.currentTimeMillis());
		Intent notificationIntent = new Intent(this, CryptoActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(this, getString(R.string.app_name),
		        notify, pendingIntent);
		startForeground(NOTIFICATION_ID, notification);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		initServiceIfNeed(intent, startId);
		handleStartCommand(intent);
		stopServiceIfNeed(startId);
		return START_STICKY;
	}

	private void handleStartCommand(Intent intent) {
		Bundle extras = intent.getExtras();
		if (extras != null) {
			if (extras.containsKey(CryptoActivity.ENTRY_MOUNTED)) {
				entries.add(getEntry(intent, CryptoActivity.ENTRY_MOUNTED));
				createNotification();
			}
			if (extras.containsKey(CryptoActivity.ENTRY_UMOUNTED)) {
				entries.removeEntry(getEntry(intent, CryptoActivity.ENTRY_UMOUNTED));
			}
		}
	}
	
	private ListEntry getEntry(Intent intent, String key) {
		try {
			return (ListEntry) intent.getExtras().get(key);
		}
		catch(Exception e) {
			return null;
		}
	}
	
	private void sendServiceHandlerMessage(String command) {
		Message msg = serviceHandler.obtainMessage();
		Bundle b = new Bundle();
		b.putString(CMD_KEY, command);
		msg.setData(b);
		serviceHandler.sendMessage(msg);
	}
	
	private void stopServiceIfNeed(Integer startId) {
		if (entries.isEmpty()) {
			if (startId != null) stopSelf(startId);
			else stopSelf();
		}
	}

	private void stopServiceIfNeed() {
		stopServiceIfNeed(null);
	}
	
	@SuppressWarnings("unchecked")
	private void initServiceIfNeed(Intent intent, int startId) {
		if (startId == 1) {
			if (intent != null) {
				Bundle extras = intent.getExtras();
				if (extras != null) {
					boolean listInit = extras.containsKey(CryptoActivity.ENTRY_STORAGE);
					boolean entryInit = extras.containsKey(CryptoActivity.ENTRY_MOUNTED);
					if (listInit || entryInit) createLUKSManager();
					if (listInit) {
						List<ListEntry> list = (List<ListEntry>) extras.get(CryptoActivity.ENTRY_STORAGE);
						entries = new ListEntryStorage(list);
						removeUmountedEntries();
					}
					else {
						entries = new ListEntryStorage();
					}
				}
				else {
					ListEntryLogger logger = new ListEntryLogger(getHelper().getListEntryDao());
					entries = logger.getEntries();
					removeUmountedEntries();
					if (!entries.isEmpty()) createLUKSManager();
				}
			}
		}
		if (!entries.isEmpty()) createNotification();
	}

	private void createLUKSManager() {
		luksManager = new LUKSManager(new LoopbackLogger(getHelper().getLoopbackLogDao()));
	}
	
	private void removeUmountedEntries() {
		for (ListEntry entry : entries) {
			if (!luksManager.isUsed(entry)) {
				entries.remove(entry);
				removeUmountedEntries();
				return;
			}
		}
	}
	
}