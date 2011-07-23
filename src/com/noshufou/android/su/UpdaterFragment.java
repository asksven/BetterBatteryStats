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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.NotificationManager;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Build.VERSION;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.noshufou.android.su.util.Util;

public class UpdaterFragment extends ListFragment implements OnClickListener {
    private static final String TAG = "UpdaterFragment";
    
    private String MANIFEST_URL;
    private int CONSOLE_RED;
    private int CONSOLE_GREEN;

    private enum Step {
        DOWNLOAD_MANIFEST,
        DOWNLOAD_BUSYBOX;
    }

    private class Manifest {
        public String version;
        public int versionCode;
        public String binaryUrl;
        public String binaryMd5;
        public String busyboxUrl;
        public String busyboxMd5;
    }

    private Manifest mManifest;
    private String mBusyboxPath = null;
    private Step mCurrentStep = Step.DOWNLOAD_MANIFEST;
    
    private ProgressBar mTitleProgress;
    private ProgressBar mProgressBar;
    private TextView mStatusText;
    private Button mActionButton;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        MANIFEST_URL = getString(Integer.parseInt(VERSION.SDK) < 5?
                R.string.updater_manifest_legacy:R.string.updater_manifest);
        CONSOLE_RED = getActivity().getResources().getColor(R.color.console_red);
        CONSOLE_GREEN = getActivity().getResources().getColor(R.color.console_green);

        View view = inflater.inflate(R.layout.fragment_updater, container, false);
        
        ((TextView)view.findViewById(R.id.title_text)).setText(R.string.updater_title);
        mTitleProgress = (ProgressBar) view.findViewById(R.id.title_refresh_progress);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        mStatusText = (TextView) view.findViewById(R.id.status);
        mActionButton = (Button) view.findViewById(R.id.action_button);
        mActionButton.setOnClickListener(this);
        
        return view;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(new ConsoleAdapter(getActivity()));
        getListAdapter().setNotifyOnChange(false);
        mProgressBar.setInterpolator(getActivity(), android.R.anim.accelerate_decelerate_interpolator);
        new UpdateTask().execute();
    }

    @Override
    public void onClick(View view) {
            new UpdateTask().execute();
    }

    @Override
    public ConsoleAdapter getListAdapter() {
        return (ConsoleAdapter) super.getListAdapter();
    }
    
    private class UpdateTask extends AsyncTask<Void, Object, Integer> {
        
        public static final int STATUS_AWAITING_ACTION = 1;
        public static final int STATUS_FINISHED_SUCCESSFUL = 2;
        public static final int STATUS_FINISHED_FAIL = 3;
        public static final int STATUS_FINISHED_NO_NEED = 4;

        @Override
        protected void onPreExecute() {
            mTitleProgress.setVisibility(View.VISIBLE);
            mStatusText.setText(R.string.updater_working);
            mActionButton.setText(R.string.updater_working);
            mActionButton.setEnabled(false);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            int progressTotal = 0;
            int progressStep = 0;

            switch (mCurrentStep) {
            case DOWNLOAD_MANIFEST:
                progressTotal = 4;
                progressStep++;
                publishProgress(progressTotal, progressStep - 1, progressStep,
                        R.string.updater_step_download_manifest);
                if (downloadFile(MANIFEST_URL, "manifest")) {
                    publishProgress(progressTotal, progressStep, progressStep,
                            R.string.updater_ok, CONSOLE_GREEN);
                } else {
                    publishProgress(progressTotal, progressStep - 1, progressStep,
                            R.string.updater_fail, CONSOLE_RED);
                    return STATUS_FINISHED_FAIL;
                }
                // Parse manifest
                // TODO: Actually parse the manifest here, as of now it's being
                //       done at download time.
                progressStep++;
                publishProgress(progressTotal, progressStep - 1, progressStep,
                        R.string.updater_step_parse_manifest);
                if (mManifest == null) {
                    publishProgress(progressTotal, progressStep - 1, progressStep,
                            R.string.updater_fail, CONSOLE_RED);
                    return STATUS_FINISHED_FAIL;
                }
                publishProgress(progressTotal, progressStep, progressStep,
                        R.string.updater_ok, CONSOLE_GREEN);
                
                // Display the latest version
                progressStep++;
                publishProgress(progressTotal, progressStep - 1, progressStep,
                        R.string.updater_step_latest_version);
                publishProgress(progressTotal, progressStep, progressStep,
                        mManifest.version, CONSOLE_GREEN);
                
                // Check the currently installed version
                progressStep++;
                publishProgress(progressTotal, progressStep - 1, progressStep,
                        R.string.updater_step_check_installed_version);
                int installedVersionCode = Util.getSuVersionCode();
                String installedVersion = Util.getSuVersion();
                if (installedVersionCode < mManifest.versionCode) {
                    publishProgress(progressTotal, progressStep, progressStep,
                            installedVersion, CONSOLE_RED);
                    mCurrentStep = Step.DOWNLOAD_BUSYBOX;
                    return STATUS_AWAITING_ACTION;
                } else {
                    publishProgress(progressTotal, progressStep, progressStep,
                            installedVersion, CONSOLE_GREEN);
                    mCurrentStep = Step.DOWNLOAD_BUSYBOX;
                    return STATUS_FINISHED_NO_NEED;
                }
            case DOWNLOAD_BUSYBOX:
                // Fix the db if necessary. A bug in the 2.3x binary could cause
                // the su binary to fail if there is no prefs table, which would
                // happen if the user never opened the app. Fringe case, but
                // needs to be addressed.
                boolean fixDb = (Util.getSuVersionCode() == 0);
                if (fixDb) {
                    progressTotal = 15;
                    progressStep = 1;
                    publishProgress(progressTotal, progressStep - 1, progressStep,
                            R.string.updater_step_fix_db);
                    SQLiteDatabase db = null;
                    try {
                        db = getActivity().openOrCreateDatabase(
                                "permissions.sqlite", Context.MODE_PRIVATE, null);
                        // We just need to make sure all the tables exist and have the proper
                        // columns. We'll delete this DB at the end of the update.
                        db.execSQL("CREATE TABLE IF NOT EXISTS apps (_id INTEGER, uid INTEGER, " +
                                "package TEXT, name TEXT, exec_uid INTEGER, exec_cmd TEXT, " +
                        " allow INTEGER, PRIMARY KEY (_id), UNIQUE (uid,exec_uid,exec_cmd))");
                        db.execSQL("CREATE TABLE IF NOT EXISTS logs (_id INTEGER, app_id INTEGER, " +
                        "date INTEGER, type INTEGER, PRIMARY KEY (_id))");
                        db.execSQL("CREATE TABLE IF NOT EXISTS prefs (_id INTEGER, key TEXT, " + 
                        "value TEXT, PRIMARY KEY (_id))");
                    } catch (SQLException e) {
                        Log.e(TAG, "Failed to fix database", e);
                        publishProgress(progressTotal, progressStep - 1, progressStep,
                                R.string.updater_fail, CONSOLE_RED);
                        return STATUS_FINISHED_FAIL;
                    } finally {
                        // Make sure we close the DB here, or the su call will also fail.
                        if (db != null) {
                            db.close();
                        }
                    }
                    publishProgress(progressTotal, progressStep, progressStep,
                            R.string.updater_ok, CONSOLE_GREEN);
                } else {
                    progressTotal = 14;
                    progressStep = 0;
                }

                // Download custom tiny busybox
                progressStep++;
                publishProgress(progressTotal, progressStep - 1, progressStep,
                        R.string.updater_step_download_busybox);
                if (downloadFile(mManifest.busyboxUrl, "busybox")) {
                    try {
//                        Process process = Runtime.getRuntime().exec(new String[] { "chmod", "755", mBusyboxPath });
                        Process process = new ProcessBuilder()
                                .command("chmod", "755", mBusyboxPath)
                                .redirectErrorStream(true).start();
                        Log.d(TAG, "chmod 755 " + mBusyboxPath);
                        process.waitFor();
                        process.destroy();
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to set busybox to executable", e);
                        publishProgress(progressTotal, progressStep - 1, progressStep,
                                R.string.updater_fail, CONSOLE_RED);
                        return STATUS_FINISHED_FAIL;
                    } catch (InterruptedException e) {
                        Log.w(TAG, "Process interrupted", e);
                    }
                    publishProgress(progressTotal, progressStep, progressStep,
                            R.string.updater_ok, CONSOLE_GREEN);
                } else {
                    Log.e(TAG, "Failed to download busybox");
                    publishProgress(progressTotal, progressStep - 1, progressStep,
                            R.string.updater_fail);
                    return STATUS_FINISHED_FAIL;
                }

                // Verify md5sum of busybox
                progressStep++;
                publishProgress(progressTotal, progressStep - 1, progressStep,
                        R.string.updater_step_check_md5sum);
                if (verifyFile(mBusyboxPath, mManifest.busyboxMd5)) {
                    publishProgress(progressTotal, progressStep, progressStep,
                            R.string.updater_ok, CONSOLE_GREEN);
                } else {
                    publishProgress(progressTotal, progressStep - 1, progressStep,
                            R.string.updater_fail, CONSOLE_RED);
                    return STATUS_FINISHED_FAIL;
                }
                
                // Download new su binary
                progressStep++;
                publishProgress(progressTotal, progressStep - 1, progressStep,
                        R.string.updater_step_download_su);
                String suPath;
                if (downloadFile(mManifest.binaryUrl, "su")) {
                    suPath = getActivity().getFileStreamPath("su").toString();
                    publishProgress(progressTotal, progressStep, progressStep,
                            R.string.updater_ok, CONSOLE_GREEN);
                } else {
                    publishProgress(progressTotal, progressStep - 1, progressStep,
                            R.string.updater_fail, CONSOLE_RED);
                    return STATUS_FINISHED_FAIL;
                }
                
                // Verify md5sum of su
                progressStep++;
                publishProgress(progressTotal, progressStep - 1, progressStep,
                        R.string.updater_step_check_md5sum);
                if (verifyFile(suPath, mManifest.binaryMd5)) {
                    publishProgress(progressTotal, progressStep, progressStep,
                            R.string.updater_ok, CONSOLE_GREEN);
                } else {
                    publishProgress(progressTotal, progressStep - 1, progressStep,
                            R.string.updater_fail, CONSOLE_RED);
                    return STATUS_FINISHED_FAIL;
                }
                
                Process process = null;
                try { // Just use one try/catch for all the root commands
                    // Get root access
                    progressStep++;
                    publishProgress(progressTotal, progressStep - 1, progressStep,
                            R.string.updater_step_get_root);
                    process = Runtime.getRuntime().exec("su");
                    DataOutputStream os = new DataOutputStream(process.getOutputStream());
                    BufferedReader is = new BufferedReader(new InputStreamReader(
                            new DataInputStream(process.getInputStream())), 64);
                    os.writeBytes("id\n");
                    String inLine = is.readLine();
                    if (inLine == null) {
                        publishProgress(progressTotal, progressStep - 1, progressStep,
                                R.string.updater_fail, CONSOLE_RED);
                        return STATUS_FINISHED_FAIL;
                    }
                    Pattern pattern = Pattern.compile("uid=([\\d]+)");
                    Matcher matcher = pattern.matcher(inLine);
                    if (!matcher.find() || !matcher.group(1).equals("0")) {
                        publishProgress(progressTotal, progressStep - 1, progressStep,
                                R.string.updater_fail, CONSOLE_RED);
                        return STATUS_FINISHED_FAIL;
                    }
                    publishProgress(progressTotal, progressStep, progressStep,
                            R.string.updater_ok, CONSOLE_GREEN);
                    
                    // Remount system partition
                    progressStep++;
                    publishProgress(progressTotal, progressStep - 1, progressStep,
                            R.string.updater_step_remount_rw);
                    executeCommand(os, null, mBusyboxPath + " mount -o remount,rw /system");
                    inLine = executeCommand(os, is, mBusyboxPath + " touch /system/su && " +
                            mBusyboxPath + " echo YEAH");
                    if (!inLine.equals("YEAH")) {
                        publishProgress(progressTotal, progressStep - 1, progressStep,
                                R.string.updater_ok, CONSOLE_RED);
                        return STATUS_FINISHED_FAIL;
                    }
                    publishProgress(progressTotal, progressStep, progressStep,
                            R.string.updater_ok, CONSOLE_GREEN);
                    
                    // Copy su to /system. Put it in here first so it's on the system
                    // partition then use an atomic move to make sure we don't get
                    // corrupted
                    progressStep++;
                    publishProgress(progressTotal, progressStep - 1, progressStep,
                            R.string.updater_step_cp);
                    inLine = executeCommand(os, is, mBusyboxPath, "cp", suPath, "/system &&",
                            mBusyboxPath, "echo YEAH");
                    if (!inLine.equals("YEAH")) {
                        publishProgress(progressTotal, progressStep - 1, progressStep,
                                R.string.updater_fail, CONSOLE_RED);
                        return STATUS_FINISHED_FAIL;
                    }
                    publishProgress(progressTotal, progressStep, progressStep,
                            R.string.updater_ok, CONSOLE_GREEN);
                    
                    // Check su md5sum again. Do it often to make sure everything is
                    // going good.
                    progressStep++;
                    publishProgress(progressTotal, progressStep - 1, progressStep,
                            R.string.updater_step_check_md5sum);
                    inLine = executeCommand(os, is, mBusyboxPath, "md5sum /system/su");
                    if (inLine == null || !inLine.split(" ")[0].equals(mManifest.binaryMd5)) {
                        publishProgress(progressTotal, progressStep - 1, progressStep,
                                R.string.updater_fail, CONSOLE_RED);
                        return STATUS_FINISHED_FAIL;
                    }
                    publishProgress(progressTotal, progressStep, progressStep,
                            R.string.updater_ok, CONSOLE_GREEN);
                    
                    // Move /system/su to /system/bin/su
                    progressStep++;
                    publishProgress(progressTotal, progressStep - 1, progressStep,
                            R.string.updater_step_mv);
                    inLine = executeCommand(os, is, mBusyboxPath, "mv /system/su /system/bin/su &&",
                            mBusyboxPath, "echo YEAH");
                    if (!inLine.equals("YEAH")) {
                        publishProgress(progressTotal, progressStep - 1, progressStep,
                                R.string.updater_fail, CONSOLE_RED);
                        return STATUS_FINISHED_FAIL;
                    }
                    publishProgress(progressTotal, progressStep, progressStep,
                            R.string.updater_ok, CONSOLE_GREEN);
                    
                    // Check su md5sum again. Last time, I promise
                    progressStep++;
                    publishProgress(progressTotal, progressStep - 1, progressStep,
                            R.string.updater_step_check_md5sum);
                    // Can't use the verifyFile method here since we need to be root
                    inLine = executeCommand(os, is, mBusyboxPath, "md5sum /system/bin/su");
                    if (inLine == null || !inLine.split(" ")[0].equals(mManifest.binaryMd5)) {
                        publishProgress(progressTotal, progressStep - 1, progressStep,
                                R.string.updater_fail, CONSOLE_RED);
                        return STATUS_FINISHED_FAIL;
                    }
                    publishProgress(progressTotal, progressStep, progressStep,
                            R.string.updater_ok, CONSOLE_GREEN);
                    
                    // Change su file mode
                    progressStep++;
                    publishProgress(progressTotal, progressStep - 1, progressStep,
                            R.string.updater_step_chmod);
                    inLine = executeCommand(os, is, mBusyboxPath, "chmod 06755 /system/bin/su &&",
                            mBusyboxPath, "echo YEAH");
                    if (!inLine.equals("YEAH")) {
                        publishProgress(progressTotal, progressStep - 1, progressStep,
                                R.string.updater_fail, CONSOLE_RED);
                    }
                    publishProgress(progressTotal, progressStep, progressStep,
                            R.string.updater_ok, CONSOLE_GREEN);

                    // Remount system partition
                    progressStep++;
                    publishProgress(progressTotal, progressStep - 1, progressStep,
                            R.string.updater_step_remount_ro);
                    executeCommand(os, null, mBusyboxPath, "mount -o remount,ro /system");
                    inLine = executeCommand(os, is, mBusyboxPath, "touch /system/su ||",
                            mBusyboxPath, "echo YEAH");
                    if (inLine == null || !inLine.equals("YEAH")) {
                        publishProgress(progressTotal, progressStep - 1, progressStep,
                                R.string.updater_fail, CONSOLE_RED);
                        publishProgress(progressTotal, progressStep, progressStep,
                                R.string.updater_remount_ro_failed);
                    }
                    publishProgress(progressTotal, progressStep, progressStep,
                            R.string.updater_ok, CONSOLE_GREEN);

                    os.writeBytes("exit\n");
                } catch (IOException e) {
                    Log.e(TAG, "Failed to execute root commands", e);
                } finally {
                    process.destroy();
                }
                
                // Verify the proper version is installed.
                progressStep++;
                publishProgress(progressTotal, progressStep - 1, progressStep,
                        R.string.updater_step_check_installed_version);
                installedVersionCode = Util.getSuVersionCode();
                installedVersion = Util.getSuVersion();
                if (installedVersionCode == mManifest.versionCode) {
                    publishProgress(progressTotal, progressStep, progressStep,
                            installedVersion, CONSOLE_GREEN);
                } else {
                    publishProgress(progressTotal, progressStep, progressStep,
                            installedVersion, CONSOLE_RED);
                    mCurrentStep = Step.DOWNLOAD_BUSYBOX;
                    return STATUS_FINISHED_FAIL;
                }
                
                // Clean up
                progressStep++;
                publishProgress(progressTotal, progressStep - 1, progressStep,
                        R.string.updater_step_clean_up);
                getActivity().deleteFile("busybox");
                getActivity().deleteFile("su");
                if (fixDb) {
                    getActivity().deleteDatabase("permissions.sqlite");
                }
                publishProgress(progressTotal, progressStep, progressStep,
                        R.string.updater_ok, CONSOLE_GREEN);
                return STATUS_FINISHED_SUCCESSFUL;
            }
            
            return -1;
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            getListAdapter().notifyDataSetChanged();
            mProgressBar.setMax((Integer)values[0]);
            mProgressBar.setProgress((Integer)values[1]);
            mProgressBar.setSecondaryProgress((Integer)values[2]);
            if (values.length == 4) {
                addConsoleEntry((Integer)values[3]);
            } else if (values.length == 5) {
                if (values[3] instanceof String) {
                    addStatusToEntry((String)values[3], (Integer)values[4]);
                } else  {
                    addStatusToEntry((Integer)values[3], (Integer)values[4]);
                }
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            mTitleProgress.setVisibility(View.GONE);
            mActionButton.setEnabled(true);
            switch (result) {
            case STATUS_AWAITING_ACTION:
                mActionButton.setText(R.string.updater_update);
                mStatusText.setText(R.string.updater_new_su_found);
                break;
            case STATUS_FINISHED_NO_NEED:
                mActionButton.setText(R.string.updater_update_anyway);
                mStatusText.setText(R.string.updater_current_installed);
                break;
            case STATUS_FINISHED_SUCCESSFUL:
                mActionButton.setText(R.string.updater_cool);
                mStatusText.setText(R.string.updater_su_updated);
                NotificationManager nm = (NotificationManager) getActivity()
                        .getSystemService(Context.NOTIFICATION_SERVICE);
                nm.cancel(1);
                break;
            case STATUS_FINISHED_FAIL:
                mActionButton.setText(R.string.updater_try_again);
                mStatusText.setText(R.string.updater_update_failed);
            }
        }

    }
    
    private void addConsoleEntry(int res) {
        ConsoleEntry entry = new ConsoleEntry(res);
        getListAdapter().add(entry);
    }
    
    private void addStatusToEntry(int res, int color) {
        addStatusToEntry(getActivity().getString(res), color);
    }
    
    private void addStatusToEntry(String status, int color) {
        ConsoleEntry entry = getListAdapter().getItem(getListAdapter().getCount() - 1);
        entry.status = status;
        entry.statusColor = color;
    }

    private boolean downloadFile(String urlStr, String localName) {
        BufferedInputStream bis = null;
        
        try {
            URL url = new URL(urlStr);
            
            URLConnection urlCon = url.openConnection();
            bis = new BufferedInputStream(urlCon.getInputStream());

            ByteArrayBuffer baf = new ByteArrayBuffer(50);
            int current = 0;
            while ((current = bis.read()) != -1) {
                baf.append((byte) current);
            }
            bis.close();
            
            if (localName.equals("manifest")) {
                try {
                    JSONObject manifest = new JSONObject(new String(baf.toByteArray()));
                    mManifest = new Manifest();
                    mManifest.version = manifest.getString("version");
                    mManifest.versionCode = manifest.getInt("version-code");
                    mManifest.binaryUrl = manifest.getString("binary");
                    mManifest.binaryMd5 = manifest.getString("binary-md5sum");
                    mManifest.busyboxUrl = manifest.getString("busybox");
                    mManifest.busyboxMd5 = manifest.getString("busybox-md5sum");
                } catch (JSONException e) {
                    Log.e(TAG, "Malformed manifest file", e);
                }
                return true;
            } else {
                FileOutputStream outFileStream = getActivity().openFileOutput(localName, 0);
                outFileStream.write(baf.toByteArray());
                outFileStream.close();
                if (localName.equals("busybox")) {
                    mBusyboxPath = getActivity().getFilesDir().getAbsolutePath().concat("/busybox");
                }
            }

        } catch (MalformedURLException e) {
            Log.e(TAG, "Bad URL: " + urlStr, e);
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Problem downloading file: " + localName, e);
            return false;
        }
        return true;
    }
    
    private boolean verifyFile(String path, String md5sum) {
        if (mBusyboxPath == null) {
            Log.e(TAG, "Busybox not present");
            return false;
        }
        
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(
                    new String[] { mBusyboxPath, "md5sum", path});
            BufferedReader is = new BufferedReader(new InputStreamReader(
                    new DataInputStream(process.getInputStream())), 64);
            BufferedReader es = new BufferedReader(new InputStreamReader(
                    new DataInputStream(process.getErrorStream())), 64);
            for (int i = 0; i < 100; i++) {
                if (is.ready()) break;
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Log.w(TAG, "Sleep timer got interrupted...");
                }
            }
            String inLine = null;
            if (es.ready()) {
                inLine = es.readLine();
                Log.d(TAG, inLine);
            }
            if (is.ready()) {
                inLine = is.readLine();
            } else {
                Log.e(TAG, "Could not check md5sum");
                return false;
            }
            process.destroy();
            if (!inLine.split(" ")[0].equals(md5sum)) {
                Log.e(TAG, "Checksum mismatch");
                return false;
            }
        } catch (IOException e) {
            Log.e(TAG, "Checking of md5sum failed", e);
            return false;
        }
        return true;
    }
    
    private String executeCommand(DataOutputStream os, BufferedReader is, String... commands)
            throws IOException {
        if (commands.length == 0) return null;
        StringBuilder command = new StringBuilder();
        for (String s : commands) {
            command.append(s).append(" ");
        }
        command.append("\n");
        Log.d(TAG, command.toString());
        os.writeBytes(command.toString());
        if (is != null) {
            for (int i = 0; i < 100; i++) {
                if (is.ready()) break;
                try {
                    Thread.sleep(5);
                    Log.d(TAG, "Slept " + i);
                } catch (InterruptedException e) {
                    Log.w(TAG, "Sleep timer interrupted", e);
                }
            }
            if (is.ready()) {
                return is.readLine();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private class ConsoleAdapter extends ArrayAdapter<ConsoleEntry> {
        
        ConsoleAdapter(Context context) {
            super(context, R.layout.console_item, R.id.console_step);
        }
        
        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            this.setNotifyOnChange(false);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            
            TextView status = (TextView) view.findViewById(R.id.console_status);
            status.setText(getItem(position).status);
            status.setTextColor(getItem(position).statusColor);
            
            return view;
        }
    }
    
    private class ConsoleEntry {
        public String entry;
        public String status;
        public int statusColor;
        
        public ConsoleEntry(int res) {
            entry = getActivity().getString(res);
        }

        @Override
        public String toString() {
            return entry;
        }
    }

}
