package com.noshufou.android.su;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class SuPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
//	private static final String TAG = "Su.SuPreferences";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        Preference versionPreference = (Preference)getPreferenceScreen().findPreference("pref_version");
        versionPreference.setTitle(getString(R.string.pref_version_title, getSuperuserVersion()));
        DBHelper db = new DBHelper(this);
        versionPreference.setSummary(getString(R.string.pref_version_summary, Su.getSuVersion(), db.getDBVersion()));
        db.close();
    }
    
	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals("pref_notifications")) {
			DBHelper db = new DBHelper(this);
			db.setNotifications(sharedPreferences.getBoolean("pref_notifications", true));
			db.close();
		}
	}

	private String getSuperuserVersion()
    {
    	String versionName = "";
    	
    	try
        {
        	PackageInfo pInfo = getPackageManager().getPackageInfo("com.noshufou.android.su", PackageManager.GET_META_DATA);
        	versionName = pInfo.versionName;
        } catch (NameNotFoundException e)
        {
        	e.printStackTrace();
        }
        
        return versionName;
    }
}
