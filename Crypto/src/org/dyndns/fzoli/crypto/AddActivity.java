package org.dyndns.fzoli.crypto;

import java.io.File;
import java.net.URI;
import org.dyndns.fzoli.crypto.R;
import org.dyndns.fzoli.crypto.database.DatabaseHelper;
import org.dyndns.fzoli.crypto.database.ListEntry;
import org.dyndns.fzoli.crypto.model.ListEntryLogger;
import org.openintents.intents.FileManagerIntents;
import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class AddActivity extends OrmLiteBaseActivity<DatabaseHelper> {
	
	public static final int OK = 1;
	
	private static final int PICKDIR_REQUEST_CODE = 0;
	private static final int PICKFILE_REQUEST_CODE = 2;
	
	private EditText etName, etFilePath, etMountPath;
	
	private boolean old;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add);
		initEditTexts();
		setSaveEvent();
		initSearchEvents();
	}
	
	private void initSearchEvents() {
		findViewById(R.id.btSearchFile).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				browse(PICKFILE_REQUEST_CODE);
			}
			
		});
		
		findViewById(R.id.btSearchDir).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				browse(PICKDIR_REQUEST_CODE);
			}
			
		});
	}
	
	private void browse(int code) {
		try {
			Intent intent = null;
			Editable home = null;
			switch (code) {
				case PICKDIR_REQUEST_CODE:
					intent = new Intent(FileManagerIntents.ACTION_PICK_DIRECTORY);
					intent.putExtra("org.openintents.extra.TITLE", getString(R.string.selectFolder));
					intent.putExtra("org.openintents.extra.BUTTON_TEXT", getString(R.string.useFolder));
					home = etMountPath.getText();
					break;
				case PICKFILE_REQUEST_CODE:
					intent = new Intent(FileManagerIntents.ACTION_PICK_FILE);
					intent.putExtra("org.openintents.extra.TITLE", getString(R.string.selectFile));
					intent.putExtra("org.openintents.extra.BUTTON_TEXT", getString(R.string.ok));
					home = etFilePath.getText();
					break;
			}
			File homeFile = new File(home.toString());
			if (homeFile.exists()) intent.setData(Uri.fromFile(homeFile));
			startActivityForResult(intent, code);
		}
		catch (Exception e) {
			showToast(getString(R.string.installOIFM));
			openMarketOIFM();
		}
	}
	
	private void openMarketOIFM() {
		try {
			Intent goToMarket = new Intent(Intent.ACTION_VIEW)
	        .setData(Uri.parse("market://details?id=org.openintents.filemanager"));
	        startActivity(goToMarket);
		}
		catch(Exception e) {
			;
		}
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == RESULT_OK) {
			fillEditText(requestCode, intent.getData());
		}
	}
	
	private void fillEditText(int code, Uri uri) {
		String path = getPath(uri);
		switch (code) {
			case PICKDIR_REQUEST_CODE:
				etMountPath.setText(path);
				break;
			case PICKFILE_REQUEST_CODE:
				etFilePath.setText(path);
				break;
		}
	}
	
	private String getPath(Uri uri) {
		if (uri != null) {
			String path = uri.toString();
			if (path.toLowerCase().startsWith("file://")) {
		       path = (new File(URI.create(path))).getAbsolutePath();
		    }
		    return path;
		}
		return null;
	}
	
	private void initEditTexts() {
		etName = (EditText) findViewById(R.id.etListname);
		etFilePath = (EditText) findViewById(R.id.etFilePath);
		etMountPath = (EditText) findViewById(R.id.etMountPath);
		fillEditTexts();
	}
	
	private void fillEditTexts() {
		ListEntry e = getEntry();
		if (e != null) {
			old = true;
			etName.setText(e.getName());
			etFilePath.setText(e.getFilePath());
			etMountPath.setText(e.getMountPath());
		}
		else {
			old = false;
		}
	}
	
	private void showToast(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}
	
	private void setSaveEvent() {
		findViewById(R.id.btOk).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				ListEntry entry = createEntry();
				boolean validName = entry.getName().length() > 0;
				if (validName && (old || !contains(entry))) {
					Intent i = new Intent();
					i.putExtra(CryptoActivity.ENTRY, entry);
					setResult(OK, i);
					finish();
				}
				else {
					showToast(getString(validName ? R.string.existsMsg : R.string.emptyNameMsg));
				}
			}
			
		});
		
		findViewById(R.id.btCancel).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				setResult(0);
				finish();
			}
			
		});
	}
	
	private ListEntry createEntry() {
		ListEntry entry = getEntry();
		Integer id = entry == null ? null : entry.getId();
		return new ListEntry(id, etName.getText().toString(), etFilePath.getText().toString(), etMountPath.getText().toString());
	}
	
	private ListEntry getEntry() {
		try {
			return (ListEntry) getIntent().getExtras().getSerializable(CryptoActivity.ENTRY);
		}
		catch(Exception e) {
			return null;
		}
	}
	
	private boolean contains(ListEntry entry) {
		return ListEntryLogger.contains(getHelper().getListEntryDao(), entry);
	}
	
}