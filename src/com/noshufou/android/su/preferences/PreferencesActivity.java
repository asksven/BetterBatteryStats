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

import java.io.IOException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.noshufou.android.su.AppListActivity;
import com.noshufou.android.su.R;
import com.noshufou.android.su.UpdaterActivity;
import com.noshufou.android.su.provider.PermissionsProvider.Logs;
import com.noshufou.android.su.service.LogService;
import com.noshufou.android.su.util.Util;
import com.noshufou.android.su.widget.NumberPickerDialog;

public class PreferencesActivity extends PreferenceActivity implements OnClickListener,
        OnSharedPreferenceChangeListener, OnPreferenceChangeListener {
    private static final String TAG = "Su.Preferences";

    private static final int SET_PIN = 0;
    private static final int OLD_PIN = 1;
    private static final int NEW_PIN = 2;
    private static final int CONFIRM_PIN = 3;
    private static final int WRONG_PIN = 4;
//    private static final int DISABLE_PIN = 5;
//    private static final int CHANGE_TAG = 6;
    
    private static final int TAG_NONE = 0;
    private static final int TAG_ALLOW = 1;
    
    private int mTagToWrite = TAG_NONE;

    SharedPreferences mPrefs = null;

    private Preference mLogLimit = null;
    private Preference mClearLog = null;
    private Preference mToastLocation = null;
    private Preference mApkVersion = null;
    private Preference mBinVersion = null;
    private Preference mTimeoutPreference = null;
    private CheckBoxPreference mPin = null;
    private CheckBoxPreference mGhostMode = null;
    private EditTextPreference mSecretCode = null;
    private CheckBoxPreference mAllowTag = null;
    
    private NfcAdapter mNfcAdapter = null;

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
            mSecretCode = (EditTextPreference) prefScreen.findPreference(Preferences.SECRET_CODE);
            mSecretCode.setSummary(getString(R.string.pref_secret_code_summary,
                    mPrefs.getString(Preferences.SECRET_CODE, "787378737")));
            mSecretCode.setOnPreferenceChangeListener(this);
            mToastLocation = prefScreen.findPreference(Preferences.TOAST_LOCATION);
            mToastLocation.setEnabled(prefScreen.getSharedPreferences()
                    .getString(Preferences.NOTIFICATION_TYPE, "toast").equals("toast"));

            // Remove NFC options if there's no NFC hardware
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
                mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
                if (mNfcAdapter == null) {
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
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);
        }
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
            if (preferenceScreen.getSharedPreferences().getBoolean(Preferences.PIN, false)) {
                changePin(NEW_PIN, "", 0);
            } else {
                changePin(OLD_PIN, "disable", 0);
            }
            return true;
        } else if (pref.equals(Preferences.CHANGE_PIN)) {
            changePin(OLD_PIN, "change", 0);
        } else if (pref.equals(Preferences.GHOST_MODE)) {
            return true;
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
                changePin(OLD_PIN, "allow_tag", 0);
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

    private void changePin(final int action, final String extra, final int attempt) {
        Log.d(TAG, "Showing PIN dialog for action " + action + ", extra = " + extra);
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.pin_layout);
        final TextView pinText = (TextView) dialog.findViewById(R.id.pin);
        switch(action) {
        case SET_PIN:
            pinText.setHint(R.string.pin_new_pin);
            break;
        case OLD_PIN:
//        case DISABLE_PIN:
//        case CHANGE_TAG:
            pinText.setHint(R.string.pin_old_pin);
            break;
        case NEW_PIN:
            if (extra.equals("mismatch")) {
                pinText.setHint(R.string.pin_mismatch);
            } else {
                pinText.setHint(R.string.pin_new_pin);
            }
            break;
        case CONFIRM_PIN:
            pinText.setHint(R.string.pin_confirm_pin);
            break;
        case WRONG_PIN:
            int tryCount = 3 - attempt;
            pinText.setHint(getResources().getQuantityString(
                    R.plurals.pin_incorrect_try, tryCount, tryCount));
            break;
        }
        dialog.setCancelable(true);
        Button okButton = (Button) dialog.findViewById(R.id.pin_ok);
        okButton.setText(R.string.ok);
        okButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                String pinStr = ((EditText)dialog.findViewById(R.id.pin)).getText().toString();
                switch (action) {
                case SET_PIN:
                case NEW_PIN:
                    dialog.dismiss();
                    changePin(CONFIRM_PIN, pinStr, 0);
                    break;
                case OLD_PIN:
                case WRONG_PIN:
                    if (Util.checkPin(getApplicationContext(), pinStr)) {
                        dialog.dismiss();
                        if (extra.equals("change")) {
                            changePin(NEW_PIN, "", 0);
                        } else if (extra.equals("disable")) {
                            mPin.setChecked(false);
                        } else if (extra.equals("allow_tag")) {
                            mAllowTag.setChecked(true);
                            prepareToWriteTag(TAG_ALLOW);
                        }
                    } else {
                        dialog.dismiss();
                        if (attempt < 2) {
                            changePin(WRONG_PIN, extra, attempt + 1);
                        }
                    }
                    break;
                case CONFIRM_PIN:
                    if (pinStr.equals(extra)) {
                        mPrefs.edit().putString("pin", Util.getHash(pinStr)).commit();
                        mPin.setChecked(true);
                        dialog.dismiss();
                    } else {
                        dialog.dismiss();
                        changePin(NEW_PIN, "mismatch", 0);
                    }
                    break;
                }
            }
        });
        Button cancelButton = (Button) dialog.findViewById(R.id.pin_cancel);
        cancelButton.setText(R.string.cancel);
        cancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });
        dialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (action == NEW_PIN) {
                    mPrefs.edit().putBoolean(Preferences.PIN, false).commit();
                    mPin.setChecked(false);
                }
                if (extra.equals("disable")) {
                    mPrefs.edit().putBoolean(Preferences.PIN, true).commit();
                    mPin.setChecked(true);
                } else if (extra.equals("allow_tag")) {
                    mPrefs.edit().putBoolean(Preferences.USE_ALLOW_TAG, false).commit();
                    mAllowTag.setChecked(false);
                }
            }
        });

        OnClickListener onPinButton = new OnClickListener() {
            @Override
            public void onClick(View view) {
                Button button = (Button) view;
                pinText.setText(new StringBuffer(pinText.getText()).append(button.getText()));
            }
        };
        ((Button)dialog.findViewById(R.id.pin_0)).setOnClickListener(onPinButton);
        ((Button)dialog.findViewById(R.id.pin_1)).setOnClickListener(onPinButton);
        ((Button)dialog.findViewById(R.id.pin_2)).setOnClickListener(onPinButton);
        ((Button)dialog.findViewById(R.id.pin_3)).setOnClickListener(onPinButton);
        ((Button)dialog.findViewById(R.id.pin_4)).setOnClickListener(onPinButton);
        ((Button)dialog.findViewById(R.id.pin_5)).setOnClickListener(onPinButton);
        ((Button)dialog.findViewById(R.id.pin_6)).setOnClickListener(onPinButton);
        ((Button)dialog.findViewById(R.id.pin_7)).setOnClickListener(onPinButton);
        ((Button)dialog.findViewById(R.id.pin_8)).setOnClickListener(onPinButton);
        ((Button)dialog.findViewById(R.id.pin_9)).setOnClickListener(onPinButton);

        dialog.show();
    }
    
    private void prepareToWriteTag(int whichTag) {
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndef.addDataType("*/*");
        } catch (MalformedMimeTypeException e) {
            Log.e(TAG, "Bad MIME type declared", e);
            return;
        }
        IntentFilter[] filters = new IntentFilter[] { ndef };
        String[][] techLists = new  String[][] {
                new String[] { Ndef.class.getName() },
                new String[] { NdefFormatable.class.getName() }
        };
        mTagToWrite = whichTag;
        mNfcAdapter.enableForegroundDispatch(this, pendingIntent, filters, techLists);
        Toast.makeText(this, "Ready to write tag", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onNewIntent(Intent intent) {
        switch (mTagToWrite) {
        case TAG_ALLOW:
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            NdefRecord record = new NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE,
                    "com.noshufou:a".getBytes(),
                    new byte[0],
                    mPrefs.getString("pin", "").getBytes());
            NdefMessage message = new NdefMessage(new NdefRecord[] {record });

            Ndef ndef = Ndef.get(tagFromIntent);
            if (ndef != null) {
                if (!ndef.isWritable()) {
                    Toast.makeText(this, "Tag not writeable", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                int maxSize = ndef.getMaxSize();
                Log.d(TAG, "Max tag size = " + maxSize + ", Message size = " + message.toByteArray().length);
                
                if (maxSize < message.toByteArray().length) {
                    Toast.makeText(this, "Tag not big enough", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    ndef.connect();
                    ndef.writeNdefMessage(message);
                } catch (IOException e) {
                    Log.e(TAG, "IOException", e);
                    return;
                } catch (FormatException e) {
                    Log.e(TAG, "FormatException", e);
                    return;
                }
            } else {
                NdefFormatable format = NdefFormatable.get(tagFromIntent);
                if (format != null) {
                    try {
                        format.connect();
                        format.format(message);
                        Log.d(TAG, "formated tag");
                    } catch (IOException e) {
                        Log.e(TAG, "IOException", e);
                        return;
                    } catch (FormatException e) {
                        Log.e(TAG, "FormatException", e);
                        return;
                    }
                }
            }
            Toast.makeText(this, "Tag wrote", Toast.LENGTH_SHORT).show();
            mTagToWrite = TAG_NONE;
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
