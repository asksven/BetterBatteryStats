package com.noshufou.android.su;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SuRequestReceiver extends BroadcastReceiver {
//	private static final String TAG = "SuRequest";
	
	public static final String EXTRA_CALLERUID = "caller_uid";
	public static final String EXTRA_UID = "desired_uid";
	public static final String EXTRA_CMD = "desired_cmd";
	public static final String EXTRA_SOCKET = "socket";
	
	@Override
	public void onReceive(Context context, Intent intent) 
	{	
		int callerUid = intent.getIntExtra(EXTRA_CALLERUID, 0);
		int desiredUid = intent.getIntExtra(EXTRA_UID, 0);
		String desiredCmd = intent.getStringExtra(EXTRA_CMD);
		String socketPath = intent.getStringExtra(EXTRA_SOCKET);

		DBHelper db = new DBHelper(context);
		AppDetails appDetails = db.checkApp(callerUid, desiredUid, desiredCmd);

        if (appDetails.getAllow() == AppDetails.ASK) {
            Intent prompt = new Intent(context, SuRequest.class);
            prompt.putExtras(intent);
            prompt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(prompt);
        } else {
            ResponseHelper.sendResult(context, appDetails, socketPath);
        }

		db.close();
	}
}
