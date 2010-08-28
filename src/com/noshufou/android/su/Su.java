package com.noshufou.android.su; 

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TabHost;

public class Su extends TabActivity {
    private static final String TAG = "Su";
    
    private static final int PACKAGE_UNINSTALL = 1;
    private static final int SEND_REPORT = 2;
    
    private Context mContext;
    private String mMaliciousAppPackage = "";
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mContext = this;
		
		setContentView(R.layout.main);
		
		Resources res = getResources();
		TabHost tabHost = getTabHost();
		TabHost.TabSpec spec;
		Intent intent;
		
		intent = new Intent().setClass(this, AppListActivity.class);
		spec = tabHost.newTabSpec("apps").setIndicator(getString(R.string.tab_apps),
				res.getDrawable(R.drawable.ic_tab_permissions))
				.setContent(intent);
		tabHost.addTab(spec);
		
		intent = new Intent().setClass(this, LogActivity.class);
		spec = tabHost.newTabSpec("log").setIndicator(getString(R.string.tab_log),
				res.getDrawable(R.drawable.ic_tab_log))
				.setContent(intent);
		tabHost.addTab(spec);
		
		intent = new Intent().setClass(this, SuPreferences.class);
		spec = tabHost.newTabSpec("settings").setIndicator(getString(R.string.tab_settings),
				res.getDrawable(R.drawable.ic_tab_settings))
				.setContent(intent);
		tabHost.addTab(spec);
		
		tabHost.setCurrentTab(0);
		
		firstRun();
		
		new CheckForMaliciousApps().execute();
	}
	
	private void firstRun() {
		int versionCode = 0;
		try {
			versionCode = getPackageManager()
					.getPackageInfo("com.noshufou.android.su", PackageManager.GET_META_DATA)
					.versionCode;
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Package not found... Odd, since we're in that package...", e);
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		int lastFirstRun = prefs.getInt("last_run", 0);
		
		if (lastFirstRun >= versionCode) {
			Log.d(TAG, "Not first run");
			return;
		}
		Log.d(TAG, "First run for version " + versionCode);
		
		String suVer = getSuVersion();
		Log.d(TAG, "su version: " + suVer);
		new Updater(this, suVer).doUpdate();
		
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt("last_run", versionCode);
		editor.commit();
	}
	
	private void maliciousAppFound(final String packageName) {
	    new AlertDialog.Builder(mContext).setTitle(R.string.warning)
	            .setMessage(getString(R.string.malicious_app_found, packageName))
	            .setPositiveButton(R.string.uninstall, new DialogInterface.OnClickListener() {
                    
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Uri packageUri = Uri.parse("package:" + packageName);
                        Intent intent = new Intent(Intent.ACTION_DELETE, packageUri);
                        startActivityForResult(intent, PACKAGE_UNINSTALL);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new CheckForMaliciousApps().execute();
                    }
                }).show();
	}

	@Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
	    switch (requestCode) {
	    case PACKAGE_UNINSTALL:
	        // We should check to see if the resultCode == 1, but it's always 0
	        // perhaps it's a mistake in PackageInstaller.apk
	        new AlertDialog.Builder(mContext).setTitle(R.string.uninstall_successful)
	        .setMessage(R.string.report_msg)
	        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

	            @Override
	            public void onClick(DialogInterface dialog, int which) {
	                Intent email = new Intent(Intent.ACTION_SEND);
	                email.setType("plain/text");
	                email.putExtra(Intent.EXTRA_EMAIL,
	                        new String[] {"superuser.android@gmail.com"});
	                email.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.report_subject));
	                email.putExtra(Intent.EXTRA_TEXT,
	                        getString(R.string.report_body, mMaliciousAppPackage));
	                startActivityForResult(email, SEND_REPORT);
	            }
	        })
	        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    new CheckForMaliciousApps().execute();
                }
            }).show();
	        break;
	    case SEND_REPORT:
	        new CheckForMaliciousApps().execute();
	        break;
	    }
    }

    public static String getSuVersion()
    {
    	Process process = null;
    	try {
    		process = Runtime.getRuntime().exec("su -v");
    		InputStream processInputStream = process.getInputStream();
    		BufferedReader stdInput = new BufferedReader(new InputStreamReader(processInputStream));
    		Thread.sleep(500);
    		try {
    			if (stdInput.ready()) {
    				String suVersion = stdInput.readLine();
    				return suVersion;
    			} else {
    				return " " + R.string.su_original;
    			}
    		} finally {
    			stdInput.close();
    		}
    	} catch (IOException e) {
    		Log.e(TAG, "Call to su failed. Perhaps the wrong version of su is present", e);
    		return " " + R.string.su_original;
    	} catch (InterruptedException e) {
    		Log.e(TAG, "Call to su failed.", e);
    		return " ...";
		}
    }
	
	public class CheckForMaliciousApps extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            PackageManager pm = mContext.getPackageManager();
            List<ApplicationInfo> apps = pm.getInstalledApplications(0);
            for (int i = 0; i < apps.size(); i++) {
                ApplicationInfo app = apps.get(i);
                if (!app.packageName.equals(mContext.getPackageName()) &&
                        pm.checkPermission("com.noshufou.android.su.RESPOND", app.packageName) ==
                            PackageManager.PERMISSION_GRANTED && 
                            !mMaliciousAppPackage.equals(app.packageName)) {
                    mMaliciousAppPackage = app.packageName;
                    return app.packageName;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                maliciousAppFound(result);
            }
        }
	}
}
