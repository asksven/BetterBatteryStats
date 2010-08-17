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
		
		copyNewFiles();
		
		String suVer = getSuVersion();
		String expectedSuVer = (versionCode < 5)?"2.3-cd":"2.3-ef";
		Log.d(TAG, "Actual su version: " + suVer);
		Log.d(TAG, "Expected su version: " + expectedSuVer);
		if (suVer == null || !suVer.equals(expectedSuVer)) {
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
						if (fileIn != null) {
							fileIn.close();
						}
						if (fileOut != null) {
							fileOut.close();
						}
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

    private String getSuVersion()
    {
    	String suVersion = "";
    	Process process = null;
    	
	    try {
			process = Runtime.getRuntime().exec("su -v");
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
			suVersion = stdInput.readLine();
		} catch (IOException e) {
			Log.e(TAG, "Call to su failed. Perhaps the wrong version of su is present", e);
			return null;
		}
    	
    	return suVersion;
    }
	
	private void copyNewFiles() {
		int sdkVersion = Integer.parseInt(VERSION.SDK);
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
		
	}
	
//	private boolean remount(boolean writeable) {
//		String device = null;
//		String type = null;
//		try {
//			Process process = Runtime.getRuntime().exec("mount");
//			BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
//			String line;
//			while ((line = stdInput.readLine()) != null) {
//				String[] array = line.split(" ");
//				device = array[0];
//				if (array[1].equals("on") && array[2].equals("/system")) {
//					type = array[4];
//					break;
//				} else if (array[1].equals("/system")) {
//					type = array[2];
//					break;
//				}
//			}
//			if (type != null) {
//				String mode = writeable?"rw ":"ro ";
//				String mountStr = "su -c mount -o remount," + mode + device + " /system";
//				Log.d(TAG, mountStr);
//				process = Runtime.getRuntime().exec(mountStr);
//			}
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			return false;
//		}
//		return true;
//	}
}
