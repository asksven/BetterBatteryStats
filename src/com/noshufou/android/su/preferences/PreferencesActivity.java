/*******************************************************************************
 * Copyright (c) 2011 Adam Shanks (ChainsDD)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.noshufou.android.su.preferences;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.noshufou.android.su.AppListActivity;
import com.noshufou.android.su.PinActivity;
import com.noshufou.android.su.R;
import com.noshufou.android.su.TagWriterActivity;
import com.noshufou.android.su.UpdaterActivity;
import com.noshufou.android.su.provider.PermissionsProvider.Logs;
import com.noshufou.android.su.service.LogService;
import com.noshufou.android.su.util.Util;
import com.noshufou.android.su.widget.NumberPickerDialog;

public class PreferencesActivity extends PreferenceActivity implements OnClickListener,
        OnSharedPreferenceChangeListener, OnPreferenceChangeListener {
    private static final String TAG = "Su.PreferencesActivity";

    private static final int REQUEST_ENABLE_PIN = 1;
    private static final int REQUEST_DISABLE_PIN = 2;
    private static final int REQUEST_CHANGE_PIN = 3;
    private static final int REQUEST_WRITE_TAG = 4;
    private static final int REQUEST_SECRET_CODE = 5;
    
    SharedPreferences mPrefs = null;

    private Preference mLogLimit = null;
    private Preference mClearLog = null;
    private Preference mToastLocation = null;
    private Preference mApkVersion = null;
    private Preference mBinVersion = null;
    private Preference mTimeoutPreference = null;
    private CheckBoxPreference mPin = null;
    private CheckBoxPreference mGhostMode = null;
    private Preference mSecretCode = null;
    private CheckBoxPreference mAllowTag = null;
    
    private Context mContext;
    private boolean mElite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        addPreferencesFromResource(R.xml.preferences);

        mContext = getApplicationContext();

        // Set up the titlebar
        ((TextView)findViewById(R.id.title_text)).setText(R.string.pref_title);
        ((ImageButton)findViewById(R.id.home_button)).setOnClickListener(this);

        PreferenceScreen prefScreen = getPreferenceScreen();
        mPrefs = prefScreen.getSharedPreferences();

        mElite = Util.elitePresent(mContext, false, 0);
        if (!mElite) {
            Log.d(TAG, "Elite not found, removing Elite preferences");
            for (String s : Preferences.ELITE_PREFS) {
                String[] bits = s.split(":");
                if (bits[1].equals("all")) {
                    prefScreen.removePreference(findPreference(bits[0]));
                } else {
                    ((PreferenceCategory)findPreference(bits[0]))
                            .removePreference(findPreference(bits[1]));
                }
            }
        } else {
            mLogLimit = prefScreen.findPreference(Preferences.LOG_ENTRY_LIMIT);
            mLogLimit.setSummary(getString(R.string.pref_log_entry_limit_summary,
                    mPrefs.getInt(Preferences.LOG_ENTRY_LIMIT, 200)));
            mTimeoutPreference = prefScreen.findPreference(Preferences.TIMEOUT);
            mTimeoutPreference.setSummary(getString(R.string.pref_timeout_summary,
                    mPrefs.getInt(Preferences.TIMEOUT, 0)));
            mPin = (CheckBoxPreference) prefScreen.findPreference(Preferences.PIN);
            mGhostMode = (CheckBoxPreference) prefScreen.findPreference(Preferences.GHOST_MODE);
            mGhostMode.setOnPreferenceChangeListener(this);
            mSecretCode = (Preference) prefScreen.findPreference(Preferences.SECRET_CODE);
            mSecretCode.setSummary(getString(R.string.pref_secret_code_summary,
                    mPrefs.getString(Preferences.SECRET_CODE, "787378737")));
            mSecretCode.setOnPreferenceChangeListener(this);
            mToastLocation = prefScreen.findPreference(Preferences.TOAST_LOCATION);
            mToastLocation.setEnabled(prefScreen.getSharedPreferences()
                    .getString(Preferences.NOTIFICATION_TYPE, "toast").equals("toast"));

            // Remove NFC options if there's no NFC hardware
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
                if (NfcAdapter.getDefaultAdapter(this) == null) {
                    prefScreen.removePreference(findPreference(Preferences.CATEGORY_NFC));
                } else {
                    mAllowTag =
                        (CheckBoxPreference) prefScreen.findPreference(Preferences.USE_ALLOW_TAG);
                }
            }

            ((PreferenceCategory)findPreference(Preferences.CATEGORY_INFO))
                    .removePreference(findPreference(Preferences.GET_ELITE));
        }

        mClearLog = prefScreen.findPreference(Preferences.CLEAR_LOG);
        mApkVersion = prefScreen.findPreference(Preferences.VERSION);
        mBinVersion = prefScreen.findPreference(Preferences.BIN_VERSION);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
        new UpdateVersions().execute();
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
        case R.id.home_button:
            goHome();
            break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            goHome();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void goHome() {
        final Intent intent = new Intent(this, AppListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        String pref = preference.getKey();
        if (pref.equals(Preferences.LOG_ENTRY_LIMIT)) {
            new NumberPickerDialog(this,
                    mLogEntriesSet,
                    mPrefs.getInt(Preferences.LOG_ENTRY_LIMIT, 200),
                    0,
                    500,
                    R.string.pref_log_entry_limit_title).show();
        } else if (pref.equals(Preferences.CLEAR_LOG)) {
            new ClearLog().execute();
        } else if (pref.equals(Preferences.BIN_VERSION)) {
            final Intent intent = new Intent(this, UpdaterActivity.class);
            startActivity(intent);
            return true;
        } else if (pref.equals(Preferences.PIN)) {
            Intent intent = new Intent(this, PinActivity.class);
            if (preferenceScreen.getSharedPreferences().getBoolean(Preferences.PIN, false)) {
                intent.putExtra(PinActivity.EXTRA_MODE, PinActivity.MODE_NEW);
                startActivityForResult(intent, REQUEST_ENABLE_PIN);
            } else {
                intent.putExtra(PinActivity.EXTRA_MODE, PinActivity.MODE_CHECK);
                startActivityForResult(intent, REQUEST_DISABLE_PIN);
            }
            return true;
        } else if (pref.equals(Preferences.CHANGE_PIN)) {
            Intent intent = new Intent(this, PinActivity.class);
            intent.putExtra(PinActivity.EXTRA_MODE, PinActivity.MODE_CHANGE);
            startActivityForResult(intent, REQUEST_CHANGE_PIN);
        } else if (pref.equals(Preferences.GHOST_MODE)) {
            return true;
        } else if (pref.equals(Preferences.SECRET_CODE)) {
            Intent intent = new Intent(this, PinActivity.class);
            intent.putExtra(PinActivity.EXTRA_MODE, PinActivity.MODE_SECRET_CODE);
            startActivityForResult(intent, REQUEST_SECRET_CODE);
        } else if (pref.equals(Preferences.TIMEOUT)) {
            new NumberPickerDialog(this,
                    mTimeoutSet,
                    mPrefs.getInt(Preferences.TIMEOUT, 0),
                    0, 600,
                    R.string.pref_timeout_title).show();
        } else if (pref.equals(Preferences.USE_ALLOW_TAG) ||
                pref.equals(Preferences.WRITE_ALLOW_TAG)) {
            if (!preferenceScreen.getSharedPreferences()
                    .getBoolean(Preferences.USE_ALLOW_TAG, false)) {
                return false;
            } else {
                Intent intent = new Intent(this, PinActivity.class);
                intent.putExtra(PinActivity.EXTRA_MODE, PinActivity.MODE_CHECK);
                startActivityForResult(intent, REQUEST_WRITE_TAG);
                return true;
            }
        } else if (pref.equals(Preferences.GET_ELITE)) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=com.noshufou.android.su.elite"));
            startActivity(intent);
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String pref = preference.getKey();
        if (pref.equals(Preferences.GHOST_MODE)) {
            final boolean ghostMode = (Boolean) newValue;
            if (ghostMode) {
                new AlertDialog.Builder(this).setTitle(R.string.pref_ghost_mode_title)
                .setMessage(R.string.pref_ghost_mode_message)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mGhostMode.setChecked(true);
                        Util.toggleAppIcon(getApplicationContext(), !ghostMode);
                        new AlertDialog.Builder(PreferencesActivity.this)
                        .setTitle(R.string.pref_ghost_mode_title)
                        .setMessage(R.string.pref_ghost_mode_enabled_message)
                        .setPositiveButton(R.string.ok, null)
                        .create().show();
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mGhostMode.setChecked(false);
                    }
                }).create().show();
            } else {
                Util.toggleAppIcon(getApplicationContext(), !ghostMode);
                return true;
            }
            return false;
        } else if (pref.equals(Preferences.SECRET_CODE)) {
            Log.d(TAG, "secret code changed");
            mSecretCode.setSummary(getString(R.string.pref_secret_code_summary,
                    ((String)newValue)));
            return true;
        }
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        if (key.equals(Preferences.NOTIFICATION_TYPE)) {
            mToastLocation.setEnabled(sharedPreferences
                    .getString(Preferences.NOTIFICATION_TYPE, "toast").equals("toast"));
        }
    }

    NumberPickerDialog.OnNumberSetListener mLogEntriesSet =
        new NumberPickerDialog.OnNumberSetListener() {

        @Override
        public void onNumberSet(int number) {
            mLogLimit.setSummary(getString(R.string.pref_log_entry_limit_summary, number));
            mPrefs.edit().putInt(Preferences.LOG_ENTRY_LIMIT, number).commit();
            final Intent intent = new Intent(mContext, LogService.class);
            intent.putExtra(LogService.EXTRA_ACTION, LogService.RECYCLE);
            startService(intent);
        }
    };

    NumberPickerDialog.OnNumberSetListener mTimeoutSet =
        new NumberPickerDialog.OnNumberSetListener() {

        @Override
        public void onNumberSet(int number) {
            mTimeoutPreference.setSummary(getString(R.string.pref_timeout_summary, number));
            mPrefs.edit().putInt(Preferences.TIMEOUT, number).commit();
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_CANCELED) {
            switch (requestCode) {
            case REQUEST_ENABLE_PIN:
                mPin.setChecked(false);
                break;
            case REQUEST_DISABLE_PIN:
                mPin.setChecked(true);
                break;
            }
            return;
        }
        
        switch (requestCode) {
        case REQUEST_ENABLE_PIN:
        case REQUEST_CHANGE_PIN:
            if (data.hasExtra(PinActivity.EXTRA_PIN)) {
                CharSequence newPin = data.getCharSequenceExtra(PinActivity.EXTRA_PIN);
                mPrefs.edit().putString("pin", newPin.toString()).commit();
                mPin.setChecked(true);
            }
            break;
        case REQUEST_DISABLE_PIN:
            mPin.setChecked(false);
            break;
        case REQUEST_WRITE_TAG:
            Intent intent = new Intent(this, TagWriterActivity.class);
            intent.putExtra(TagWriterActivity.EXTRA_TAG, TagWriterActivity.TAG_ALLOW);
            startActivity(intent);
            break;
        case REQUEST_SECRET_CODE:
            CharSequence newSecretCode = data.getCharSequenceExtra(PinActivity.EXTRA_SECRET_CODE);
            mPrefs.edit().putString(Preferences.SECRET_CODE, newSecretCode.toString()).commit();
            mSecretCode.setSummary(getString(R.string.pref_secret_code_summary, newSecretCode));
            break;
        }
    }
    
    private class ClearLog extends AsyncTask<Void, Void, Integer> {

        @Override
        protected void onPreExecute() {
            mClearLog.setTitle(R.string.pref_clearing_log_title);
            mClearLog.setSummary(R.string.pref_clearing_log_summary);
            mClearLog.setEnabled(false);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            return getContentResolver().delete(Logs.CONTENT_URI, null, null);
        }

        @Override
        protected void onPostExecute(Integer result) {
            mClearLog.setTitle(R.string.pref_clear_log_title);
            mClearLog.setSummary("");
            mClearLog.setEnabled(true);
            Toast.makeText(mContext,
                    getResources().getQuantityString(R.plurals.pref_logs_deleted, result, result),
                    Toast.LENGTH_SHORT).show();
        }
        
    }

    private class UpdateVersions extends AsyncTask<Void, Integer, Integer> {
        private String apkVersion;
        private int apkVersionCode;
        private String binVersion;

        @Override
        protected Integer doInBackground(Void... params) {
            try {
                PackageInfo pInfo = getPackageManager()
                .getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
                apkVersion = pInfo.versionName;
                apkVersionCode = pInfo.versionCode;
            } catch (NameNotFoundException e) {
                Log.e(TAG, "Superuser is not installed?", e);
            }

            binVersion = Util.getSuVersion();
            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            mApkVersion.setTitle(getString(R.string.pref_version_title, apkVersion, apkVersionCode));
            mBinVersion.setTitle(getString(R.string.pref_bin_version_title, binVersion));
        }

    }
}
