package com.noshufou.android.su;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

public class SuRequest extends Activity implements OnClickListener {
    private static final String TAG = "SuRequest";

    private int mCallerUid = 0;
    private int mDesiredUid = 0;
    private String mDesiredCmd = "";
    private String mSocketPath;
    private CheckBox mCheckRemember;
    private CountDownTimer mCountDown;

    SharedPreferences prefs;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.request);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
    	if (getCallingPackage() != null) {
            Log.e(TAG, "SuRequest must be started from su");
            finish();
            return;
        }
    	
    	Intent in = getIntent();
    	mCallerUid = in.getIntExtra(SuRequestReceiver.EXTRA_CALLERUID, 0);
    	mDesiredUid = in.getIntExtra(SuRequestReceiver.EXTRA_UID, 0);
    	mDesiredCmd = in.getStringExtra(SuRequestReceiver.EXTRA_CMD);
    	mSocketPath = in.getStringExtra(SuRequestReceiver.EXTRA_SOCKET);
    	
    	TextView appNameView = (TextView) findViewById(R.id.appName);
        appNameView.setText(Util.getAppName(this, mCallerUid, true));

        TextView packageNameView = (TextView) findViewById(R.id.packageName);
        packageNameView.setText(Util.getAppPackage(this, mCallerUid));

        TextView requestDetailView = (TextView) findViewById(R.id.requestDetail);
        requestDetailView.setText(Util.getUidName(this, mDesiredUid, true));

        TextView commandView = (TextView) findViewById(R.id.command);
        commandView.setText(mDesiredCmd);

        mCheckRemember = (CheckBox) findViewById(R.id.checkRemember);
        mCheckRemember.setChecked(prefs.getBoolean("last_remember_value", true));
        
        Button allow = (Button) findViewById(R.id.allow);
        allow.setOnClickListener(this);
        
        Button deny = (Button) findViewById(R.id.deny);
        deny.setOnClickListener(this);
        
        final TextView timer = (TextView) findViewById(R.id.timer);
        mCountDown = new CountDownTimer(11000, 1000) {
        	public void onTick(long millisUntilFinished) {
        		timer.setText(Long.toString(millisUntilFinished / 1000));
        		if (millisUntilFinished < 5001) {
        			timer.setTextColor(Color.RED);
        		}
        	}
        	
        	public void onFinish() {
        		sendDeny();
        		finish();
        	}
        }.start();
    }

    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME) {
    		sendDeny();
    	}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onClick(View v) {
		mCountDown.cancel();
    	AppDetails appDetails = new AppDetails(mCallerUid, 0, System.currentTimeMillis());
    	switch (v.getId()) {
    	case R.id.allow:
    		appDetails.setAllow(AppDetails.ALLOW);
    		break;
    	case R.id.deny:
    		appDetails.setAllow(AppDetails.DENY);
    		break;
    	}
    	if (mCheckRemember.isChecked()) {
            DBHelper db = new DBHelper(this);
           	db.insert(mCallerUid, mDesiredUid, mDesiredCmd, appDetails.getAllow());
           	db.close();
    	}
        ResponseHelper.sendResult(this, appDetails, mSocketPath);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("last_remember_value", mCheckRemember.isChecked());
        editor.commit();
        finish();
	}
	
	private void sendDeny() {
		AppDetails appDetails = new AppDetails(mCallerUid, 0, System.currentTimeMillis());
		appDetails.setAllow(AppDetails.DENY);
		ResponseHelper.sendResult(this, appDetails, mSocketPath);
	}
}
