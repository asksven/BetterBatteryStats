package com.noshufou.android.su; 

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.os.Build.VERSION;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TabHost;

public class Su extends TabActivity {
    private static final String TAG = "Su";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
	}
	
	private void firstRun() {
		int sdkVersion = Integer.parseInt(VERSION.SDK);
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
		
		InputStream fileInput = null;
		FileOutputStream fileOutput = null;
		int zipFile, binFile;
		Log.d(TAG, "sdkVersion = " + sdkVersion);
		if (sdkVersion < 5) {
			Log.d(TAG, "Copying files for cupcake/donut");
			zipFile = R.raw.su_2_3_bin_cd_signed;
			binFile = R.raw.su_cd;
		} else {
			Log.d(TAG, "Copying files for eclair/froyo");
			zipFile = R.raw.su_2_3_bin_ef_signed;
			binFile = R.raw.su_ef;
		}
		try {
			fileInput = getResources().openRawResource(zipFile);
			byte[] zipReader = new byte[fileInput.available()];
			while (fileInput.read(zipReader) != -1);
			fileInput.close();
			fileOutput = openFileOutput("su-2.3-bin.zip", MODE_WORLD_READABLE);
			fileOutput.write(zipReader);
			fileOutput.close();
			
			fileInput = getResources().openRawResource(binFile);
			byte[] binReader = new byte[fileInput.available()];
			while (fileInput.read(binReader) != -1);
			fileInput.close();
			fileOutput = openFileOutput("su", MODE_WORLD_READABLE);
			fileOutput.write(binReader);
			fileOutput.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		}
		
		String suMd5 = getSuMd5();
		String expectedSuMd5 = getString((versionCode < 5)?R.string.su_md5_cd:R.string.su_md5_ef);
		if (suMd5 == null || !suMd5.equals(expectedSuMd5)) {
			Log.d(TAG, "System has outdated su, attempting to copy new version");
			// TODO: find a way to automatically update su, use recovery mode method if that fails
			File sdDir = new File(Environment.getExternalStorageDirectory().getPath());
			if (sdDir.exists() && sdDir.canWrite()) {
				File file = new File(sdDir.getAbsolutePath() + "/su-2.3-bin.zip");
				try {
					file.createNewFile();
				} catch (IOException e) {
					Log.e(TAG, "Couldn't create destination for su-2.3-bin.zip", e);
				}
				if (file.exists() && file.canWrite()) {
					FileInputStream fileIn;
					FileOutputStream fileOut;
					try {
						fileIn = openFileInput("su-2.3-bin.zip");
						fileOut = new FileOutputStream(file);
						byte[] reader = new byte[fileIn.available()];
						while (fileIn.read(reader) != -1);
						fileOut.write(reader);
						fileIn.close();
						fileOut.close();
					} catch (FileNotFoundException e) {
						Log.e(TAG, "Error:", e);
					} catch (IOException e) {
						Log.e(TAG, "Error:", e);
					}
				}
			}
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			AlertDialog dialog = builder.setTitle(R.string.new_su)
					.setMessage(R.string.new_su_msg)
					.setNeutralButton(android.R.string.ok, null).create();
			dialog.show();
		}
		Editor editor = prefs.edit();
		editor.putInt("last_run", versionCode);
		editor.commit();
	}

	private String getSuMd5() {
    	Process process;
    	BufferedReader stdInput;
    	String binSuMd5, xbinSuMd5;
    	
    	try {
    		process = Runtime.getRuntime().exec("md5sum /system/bin/su");
    		stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
    		binSuMd5 = stdInput.readLine().split(" ")[0];
    		stdInput.close();
    		process = Runtime.getRuntime().exec("md5sum /system/xbin/su");
    		stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
    		xbinSuMd5 = stdInput.readLine().split(" ")[0];
    		stdInput.close();
    	} catch (IOException e) {
    		Log.e(TAG, "Failed to gather MD5 sums", e);
    		return null;
    	}
    	
    	if (!binSuMd5.equals(xbinSuMd5)) {
    		Log.e(TAG, "/system/bin/su does not match /system/xbin/su. Possible symlink problem.");
    		return null;
    	} else {
    		return binSuMd5;
    	}
    }
}
