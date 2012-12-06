package org.dyndns.fzoli.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class ConfirmDialog extends AlertDialog.Builder {

	public ConfirmDialog(Context context, String title, String message, String yes, String no, DialogInterface.OnClickListener event) {
		super(context);
    	setTitle(title);
    	setMessage(message);
    	setPositiveButton(yes, event);
    	setNegativeButton(no, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				;
			}
			
		});
	}
	
}