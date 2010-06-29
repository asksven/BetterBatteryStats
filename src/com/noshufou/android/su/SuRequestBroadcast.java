package com.noshufou.android.su;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class SuRequestBroadcast extends BroadcastReceiver 
{
	private static final String TAG = "SuRequest";
	public static final String REQUEST = "SuRequest";
	public static final String ALLOW = "ALLOW";
	public static final String DENY = "DENY";
	
	public static final String EXTRA_CALLERUID = "caller_uid";
	public static final String EXTRA_UID = "desired_uid";
	public static final String EXTRA_CMD = "desired_cmd";
	public static final String EXTRA_SOCKET = "socket";
	
    private int callerUid = 0;
    private int desiredUid = 0;
    private String desiredCmd = "";
    private String socketPath;
    
    private DBHelper db;
    private DBHelper.AppStatus app_status;
    private SendResponseHelper ResponseHelper;
    
    Context context;
    SharedPreferences prefs;
	
	@Override
	public void onReceive(Context context, Intent intent) 
	{	
		if (intent.getAction().equals(REQUEST))
		{
			this.context = context;
			
			callerUid = intent.getIntExtra(EXTRA_CALLERUID, 0);
			desiredUid = intent.getIntExtra(EXTRA_UID, 0);
			desiredCmd = intent.getStringExtra(EXTRA_CMD);
			socketPath = intent.getStringExtra(EXTRA_SOCKET);
			
			db = new DBHelper(context);
			app_status = db.checkApp(callerUid, desiredUid, desiredCmd);
			
			ResponseHelper = new SendResponseHelper(context);
			
			switch (app_status.permission) {
            	case DBHelper.ALLOW: sendResult(ALLOW); break;
            	case DBHelper.DENY:  sendResult(DENY); break;
            	case DBHelper.ASK:   askUser(); break;
            	default: Log.e(TAG, "Bad response from database"); break;
			}
			
			db.close();
		}
	}
	
	private void askUser()
	{
		Intent PrompterActivity = new Intent(context, SuRequest.class);
		PrompterActivity.putExtra(EXTRA_CALLERUID, callerUid);
		PrompterActivity.putExtra(EXTRA_UID, desiredUid);
		PrompterActivity.putExtra(EXTRA_CMD, desiredCmd);
		PrompterActivity.putExtra(EXTRA_SOCKET, socketPath);
		
		context.startActivity(PrompterActivity);
	}
	
	private void sendResult(String resultCode)
	{
		ResponseHelper.sendResult(resultCode, false, callerUid, desiredUid, desiredCmd, socketPath);
	}
}