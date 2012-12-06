package org.dyndns.fzoli.view;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils.TruncateAt;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;

public class PasswordInputDialog extends AlertDialog.Builder {
	
	private String ok, cancel;
	private EditText input;
	
	public PasswordInputDialog(Context context, String title, String ok, String cancel) {
		this(context, title);
		this.ok = ok;
		this.cancel = cancel;
		setPositiveEvent(createEmptyEvent());
		setNegativeEvent(createEmptyEvent());
	}
	
	private PasswordInputDialog(Context context, String title) {
		this(context);
		setTitle(title);
	}
	
	private PasswordInputDialog(Context context) {
		super(context);
		input = new EditText(context);
		input.setEllipsize(TruncateAt.END);
    	input.setSingleLine();
    	input.setTransformationMethod(PasswordTransformationMethod.getInstance());
    	this.setView(input);
	}
	
	public String getPassword() {
		return input.getText().toString();
	}
	
	public Builder setPositiveEvent(DialogInterface.OnClickListener listener) {
		return setPositiveButton(ok, listener);
	}

	public Builder setNegativeEvent(DialogInterface.OnClickListener listener) {
		return setNegativeButton(cancel, listener);
	}
	
	private DialogInterface.OnClickListener createEmptyEvent() {
		return new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int whichButton) {
				;
			}
			
		};
	}
	
}