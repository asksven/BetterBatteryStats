package com.noshufou.android.su;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;

public class SuPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener,
        OnPreferenceClickListener {
//	private static final String TAG = "Su.SuPreferences";
    
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        
        mContext = this;

        Preference versionPreference = getPreferenceScreen().findPreference("pref_version");
        versionPreference.setTitle(getString(R.string.pref_version_title, getSuperuserVersion()));
        DBHelper db = new DBHelper(this);
        versionPreference.setSummary(getString(R.string.pref_version_summary, db.getDBVersion()));
        db.close();

        Preference binVersionPreference = getPreferenceScreen().findPreference("pref_bin_version");
        updateSuVersionInformation();
        binVersionPreference.setOnPreferenceClickListener(this);

        Preference clearLogPreference = getPreferenceScreen().findPreference("pref_clear_log");
        clearLogPreference.setOnPreferenceClickListener(this);
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

	@Override
    public boolean onPreferenceClick(Preference preference) {
	    if (preference.getKey().equals("pref_bin_version")) {
	        Toast.makeText(this, R.string.checking, Toast.LENGTH_SHORT).show();
	        new Updater(this, Su.getSuVersion(mContext)).doUpdate();
	        return true;
	    } else if (preference.getKey().equals("pref_clear_log")) {
	        DBHelper db = new DBHelper(this);
	        db.clearLog();
	        db.close();
	        return true;
	    } else {
	        return false;
	    }
    }
	
	public void updateSuVersionInformation() {
		new ShowBinVersion().execute();
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
    
    private class ShowBinVersion extends AsyncTask<String, Integer, Boolean> {
        private String suVersion;

        @Override
        protected Boolean doInBackground(String... params) {
            suVersion = Su.getSuVersion(mContext);
            return null;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            getPreferenceScreen().findPreference("pref_bin_version")
                    .setTitle(getString(R.string.pref_bin_version_title, suVersion));
        }
        
    }
}
