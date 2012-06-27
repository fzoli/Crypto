package fzoli.view;

import android.app.ProgressDialog;
import android.content.Context;
import android.widget.Toast;

public class ThreadIndicator {

	private final Context CONTEXT;
	
	public ThreadIndicator(Context context, final ThreadIndicatorAction action, String progressMsg, String successMsg, String failMsg) {
		this.CONTEXT = context;
        final ProgressDialog dialog = showProgressDialog(progressMsg);
        final Toast successToast = createToas(successMsg);
        final Toast failToast = createToas(failMsg);
        Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				boolean b = action.run();
				dialog.cancel();
				if (b) successToast.show();
				else failToast.show();
			}
			
		});
        t.start();
	}
	
    private Toast createToas(String msg) {
    	return Toast.makeText(CONTEXT, msg, Toast.LENGTH_SHORT);
    }
    
    private ProgressDialog showProgressDialog(String msg) {
    	return ProgressDialog.show(CONTEXT, "", msg, true);
    }
	
}