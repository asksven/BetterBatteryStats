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
package com.noshufou.android.su;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Build.VERSION;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.BufferType;

public class UpdaterActivity extends Activity implements OnClickListener {
    private static final String TAG = "Superuser.UpdaterActivity";

    private static final class DownloadType {
        public static final String MANIFEST = "manifest";
        public static final String BINARY = "binary";
        public static final String FAIL = "fail";
    }

    private String mOkText = null;
    private String mFailText = null;

    private int mProgress = 0;
    private String[] mProgressSteps;

    private String mManifestUrl = null;
    private JSONObject mManifest = null;

    private String mCurrentVersion = null;
    private int mCurrentVersionCode = 0;
    private String mInstalledVersion = null;
    private int mInstalledVersionCode = 0;

    private String mBinaryUrl = null;
    private String mBinaryMd5 = null;

    private ProgressBar mTitleProgressBar = null;
    private ProgressBar mProgressBar = null;
    private ScrollView mConsoleScroll = null;
    private LinearLayout mConsole = null;
    private TextView mStatusText = null;
    private Button mActionButton = null;

    private int mConsoleColorGrey = 0;
    private int mConsoleColorRed = 0;
    private int mConsoleColorGreen = 0;

    private SharedPreferences mPrefs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_updater);

        Resources res = getResources();
        mConsoleColorGrey = res.getColor(R.color.console_grey);
        mConsoleColorGreen = res.getColor(R.color.console_green);
        mConsoleColorRed = res.getColor(R.color.console_red);

        mProgressSteps = res.getStringArray(R.array.updater_steps);

        mOkText = res.getString(R.string.updater_ok);
        mFailText = res.getString(R.string.updater_fail);

        // Set up the titlebar
        ((TextView)findViewById(R.id.title_text)).setText(R.string.updater_title);
        ((ImageButton)findViewById(R.id.home_button)).setOnClickListener(this);
        mTitleProgressBar = (ProgressBar) findViewById(R.id.title_refresh_progress);

        // Other views
        mConsoleScroll = (ScrollView) findViewById(R.id.console_scroll);
        mConsole = (LinearLayout) findViewById(R.id.console);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mStatusText = (TextView) findViewById(R.id.status);
        mActionButton = (Button) findViewById(R.id.action_button);
        mActionButton.setOnClickListener(this);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        int sdkVersion = Integer.parseInt(VERSION.SDK);
        if (sdkVersion < 5) {
            mManifestUrl = getString(R.string.updater_manifest_legacy);
        } else {
            mManifestUrl = getString(R.string.updater_manifest);
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
        case R.id.home_button:
            final Intent intent = new Intent(this, AppListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            break;
        case R.id.action_button:
            handleActionButton();
        }
    }

    private void handleActionButton() {
        mStatusText.setText(R.string.updater_working);
        mActionButton.setText(R.string.updater_working);
        mActionButton.setEnabled(false);
        if (mProgress < 1) {
            incrementProgress();
            new DownloadFileTask().execute(mManifestUrl, "manifest.json", DownloadType.MANIFEST);
        } else if (mProgress < 5) {
            doUpdate();
        }
    }

    private void parseManifest(String manifest) {
        Log.d(TAG, "parsemanifest");
        incrementProgress();

        try {
            mCurrentVersion = mManifest.getString("version");
            mCurrentVersionCode = mManifest.getInt("version-code");
            mBinaryUrl = mManifest.getString("binary");
            mBinaryMd5 = mManifest.getString("binary-md5sum");
        } catch (JSONException e) {
            Log.e(TAG, "Malformed JSON", e);
            addConsoleStatus(mFailText, mConsoleColorRed);
            return;
        }

        addConsoleStatus(mOkText, mConsoleColorGreen);

        incrementProgress();
        addConsoleStatus(mCurrentVersion, mConsoleColorGreen);

        incrementProgress();
        mInstalledVersion = getSuVersion();
        mInstalledVersionCode = getSuVersionCode();
        if (mInstalledVersionCode < mCurrentVersionCode) {
            addConsoleStatus(mInstalledVersion, mConsoleColorRed);
            mStatusText.setText(R.string.updater_new_su_found);
            mActionButton.setText(R.string.updater_update);
            mActionButton.setEnabled(true);
        } else {
            addConsoleStatus(mInstalledVersion, mConsoleColorGreen);
            mStatusText.setText(R.string.updater_current_installed);
            mActionButton.setText(R.string.updater_no_action);
        }
    }

    private static String getSuVersion() {
        Process process = null;
        String inLine = null;

        try {
            process = Runtime.getRuntime().exec("sh");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            BufferedReader is = new BufferedReader(new InputStreamReader(
                    new DataInputStream(process.getInputStream())), 64);
            os.writeBytes("su -v\n");
            Thread.sleep(50);
            if (is.ready()) {
                inLine = is.readLine();
                if (inLine != null) {
                    return inLine;
                }
            } else {
                os.writeBytes("exit\n");
            }
        } catch (IOException e) {
            Log.e(TAG, "Problems reading current version.", e);
            return null;
        } catch (InterruptedException e) {
        } finally {
            if (process != null) {
                process.destroy();
            }
        }

        return null;
    }

    private static int getSuVersionCode() {
        Process process = null;
        String inLine = null;

        try {
            process = Runtime.getRuntime().exec("sh");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            BufferedReader is = new BufferedReader(new InputStreamReader(
                    new DataInputStream(process.getInputStream())), 64);
            os.writeBytes("su -v\n");
            Thread.sleep(50);
            if (is.ready()) {
                inLine = is.readLine();
                if (inLine != null && Integer.parseInt(inLine.substring(0, 1)) > 2) {
                    inLine = null;
                    os.writeBytes("su -V\n");
                    inLine = is.readLine();
                    if (inLine != null) {
                        return Integer.parseInt(inLine);
                    }
                } else {
                    return 0;
                }
            } else {
                os.writeBytes("exit\n");
            }
        } catch (IOException e) {
            Log.e(TAG, "Problems reading current version.", e);
            return 0;
        } catch (InterruptedException e) {
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return 0;
    }

    private void doUpdate() {
        mStatusText.setText(R.string.updater_working);
        mActionButton.setText(R.string.updater_working);
        mActionButton.setEnabled(false);
        incrementProgress();

        new DownloadFileTask().execute(mBinaryUrl, "su", DownloadType.BINARY);
    }

    private void incrementProgress() {
        mProgress++;
        addConsoleEntry(mProgressSteps[mProgress]);
        mProgressBar.setSecondaryProgress(mProgress);
    }

    private void addConsoleEntry(String message) {
        TextView consoleEntry = new TextView(this);
        consoleEntry.setTextColor(mConsoleColorGrey);
        consoleEntry.setTypeface(Typeface.MONOSPACE);
        consoleEntry.setTextSize(10);
        consoleEntry.setText(message, BufferType.SPANNABLE);
        mConsole.addView(consoleEntry);
    }

    private void addConsoleStatus(String status, int color) {
        TextView consoleEntry = (TextView) mConsole.getChildAt(mConsole.getChildCount() - 1);
        consoleEntry.append(" ");
        consoleEntry.append(status);
        Spannable str = (Spannable) consoleEntry.getText();
        str.setSpan(new ForegroundColorSpan(color),
                str.length() - (status.length()), str.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mConsoleScroll.fullScroll(View.FOCUS_DOWN);
        mProgressBar.setProgress(mProgress);
    }

    private class DownloadFileTask extends AsyncTask <String, Integer, String> {
        private String mOutFile = null;

        @Override
        protected void onPreExecute() {
            mTitleProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            Log.d(TAG, "Downloading file: " + params[0]);
            try {
                URL url = new URL(params[0]);
                mOutFile = params[1];

                URLConnection urlCon = url.openConnection();
                BufferedInputStream bis = new BufferedInputStream(urlCon.getInputStream());

                ByteArrayBuffer baf = new ByteArrayBuffer(50);
                int current = 0;
                while ((current = bis.read()) != -1) {
                    baf.append((byte) current);
                }

                if (params[2].equals(DownloadType.MANIFEST)) {
                    try {
                        mManifest = new JSONObject(new String(baf.toByteArray()));
                    } catch (JSONException e) {
                        Log.e(TAG, "Bad JSON file", e);
                        return DownloadType.FAIL;
                    }
                }

                FileOutputStream fos = openFileOutput(mOutFile,
                        Context.MODE_WORLD_READABLE);
                fos.write(baf.toByteArray());
                fos.close();

            } catch (MalformedURLException e) {
                Log.e(TAG, "Bad URL", e);
                return DownloadType.FAIL;
            } catch (IOException e) {
                Log.e(TAG, "Problem downloading file", e);
                return DownloadType.FAIL;
            }

            return params[2];
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "onPostExecute");
            mTitleProgressBar.setVisibility(View.INVISIBLE);
            if (result.equals(DownloadType.FAIL)) {
                addConsoleStatus(mFailText, mConsoleColorRed);
                mStatusText.setText(R.string.updater_download_failed);
                mActionButton.setText(R.string.updater_try_again);
                mActionButton.setEnabled(true);
                mProgress = 0;
                return;
            } else {
                addConsoleStatus(mOkText, mConsoleColorGreen);
            }
            if (result.equals(DownloadType.MANIFEST)) {
                parseManifest(mOutFile);
            } else if (result.equals(DownloadType.BINARY)) {
                new ProcessBinaryTask().execute();
            }
        }
    }

    private class ProcessBinaryTask extends AsyncTask<String, Integer, Boolean> {

        @Override
        protected void onPreExecute() {
            incrementProgress();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            Process process = null;
            DataOutputStream os = null;
            BufferedReader is = null;
            String inLine = null;
            String su = getFilesDir().getPath() + "/su";

            // Check md5sums
            try {
                Log.d(TAG, su);
                process = Runtime.getRuntime().exec("md5sum " + su);
                is = new BufferedReader(new InputStreamReader(
                        new DataInputStream(process.getInputStream())), 64);
                inLine = is.readLine();
                if (!inLine.split(" ")[0].equals(mBinaryMd5)) {
                    Log.e(TAG, "Checksum mismatch");
                    return false;
                }
                publishProgress(1);
            } catch (IOException e) {
                Log.e(TAG, "Problem checking md5sums", e);
            } finally {
                if (process != null) {
                    process.destroy();
                }
            }

            // Gain root access
            publishProgress(0);
            try {
                process = Runtime.getRuntime().exec("su");
                os = new DataOutputStream(process.getOutputStream());
                is = new BufferedReader(new InputStreamReader(
                        new DataInputStream(process.getInputStream())), 64);
                os.writeBytes("whoami\n");
                inLine = is.readLine();
                inLine = "root";
                if (!inLine.equals("root")) {
                    return false;
                } else {
                    publishProgress(1);
                }

                // Remount /system as rw
                publishProgress(0);
                os.writeBytes("mount -o remount,rw /system\n");
                publishProgress(1);

                // chmod su to 06755
                publishProgress(0);
                os.writeBytes("chmod 06755 " + su + "\n");
                publishProgress(1);

                // Copy su to /system/bin
                publishProgress(0);
                os.writeBytes("cp " + su + " /system/bin\n");
                publishProgress(1);

                // Verify successful copy
                publishProgress(0);
                os.writeBytes("md5sum /system/bin/su\n");
                inLine = is.readLine();
                if (!inLine.split(" ")[0].equals(mBinaryMd5)) {
                    Log.e(TAG, "Checksum mismatch");
                    return false;
                }
                publishProgress(1);

                // Remount /system as ro
                publishProgress(0);
                os.writeBytes("mount -o remount,ro /system\n");
                publishProgress(1);

                // Clean up
                publishProgress(0);
                deleteFile("su");
                deleteFile("manifest.json");
                mPrefs.edit().putBoolean("db_enabled", true).commit();
                publishProgress(1);

                // Verify installed version
                publishProgress(0);
                os.writeBytes("su -V\n");
                inLine = is.readLine();
                int installedVersion = Integer.parseInt(inLine);
                if (installedVersion == mCurrentVersionCode) {
                    mInstalledVersionCode = installedVersion;
                    os.writeBytes("su -v\n");
                    mInstalledVersion = is.readLine();
                    return true;
                } else {
                    return false;
                }
            } catch (IOException e) {
                Log.e(TAG, "Problem updating su binary", e);
                return false;
            } finally {
                Log.d(TAG, "finally!");
                if (process != null) {
                    process.destroy();
                }
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (values[0] == 0) {
                incrementProgress();
            } else if (values[0] == 1) {
                addConsoleStatus(mOkText, mConsoleColorGreen);
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                addConsoleStatus(mFailText, mConsoleColorRed);
                mStatusText.setText(R.string.updater_update_failed);
                mActionButton.setText(R.string.updater_try_again);
                mActionButton.setEnabled(true);
                mProgress = 4;
            } else {
                addConsoleStatus(mInstalledVersion, mConsoleColorGreen);
                mStatusText.setText(R.string.updater_su_updated);
                mActionButton.setText(R.string.updater_update);
            }
        }

    }

}
