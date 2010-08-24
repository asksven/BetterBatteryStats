package com.noshufou.android.su;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Build.VERSION;
import android.util.Log;
import android.widget.Toast;

public class Updater {
    private static final String TAG = "Su.Updater";

    private JSONObject mManifest;
    private JSONObject mBinaries;
    private Context mContext;
    private String mSuVersion;

    public Updater(Context context, String suVersion) {
        mContext = context;
        mSuVersion = suVersion;
    }
    
    public void doUpdate() {
        ConnectivityManager cm =
                (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm.getActiveNetworkInfo() == null
                || cm.getActiveNetworkInfo().getState() == NetworkInfo.State.DISCONNECTED) {
            Toast.makeText(mContext, R.string.no_connection, Toast.LENGTH_SHORT).show();
        } else {
            // Get the process started, it all goes from here.
            new DownloadFileTask().execute("http://dl.dropbox.com/u/6408470/Superuser/manifest.json");
        }
    }

    private void postManifest() {
        String expectedSuVersion = "";
        try {
            expectedSuVersion = mManifest.getString("version");
            int sdkVersion = Integer.parseInt(VERSION.SDK);
            if (sdkVersion < 5) {
                expectedSuVersion += "-cd";
                mBinaries = mManifest.getJSONObject("cd");
            } else {
                expectedSuVersion += "-ef";
                mBinaries = mManifest.getJSONObject("ef");
            }
            if (mSuVersion == null || !mSuVersion.equals(expectedSuVersion)) {
                Log.d(TAG, "System has outdated su, attempting to copy new version");
                new AlertDialog.Builder(mContext).setTitle(R.string.new_su)
                .setMessage(R.string.new_su_msg)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            new DownloadFileTask()
                                    .execute(mBinaries.getString("binary"), mBinaries.getString("update"));
                        } catch (JSONException e) {
                            Log.e(TAG, "Malformed JSON", e);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create().show();
            } else {
                Toast.makeText(mContext, R.string.su_up_to_date, Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            Log.e(TAG, "Malformed JSON", e);
            return;
        }
    }
    
    private void copyUpdateZip(boolean fromError) {
        final String updateFilename = mContext.getString(R.string.update_filename);
        if (fromError) {
            new AlertDialog.Builder(mContext).setTitle(R.string.su_not_updated_title)
                    .setMessage(R.string.su_not_updated)
                    .setNeutralButton(android.R.string.ok, null)
                    .create().show();
        }
        File sdDir = new File(Environment.getExternalStorageDirectory().getPath());
        if (sdDir.exists() && sdDir.canWrite()) {
            File file = new File(sdDir.getAbsolutePath() + "/" + updateFilename);
            try {
                file.createNewFile();
            } catch (IOException e) {
                Log.e(TAG, "Couldn't create destination for " + updateFilename, e);
            }
            if (file.exists() && file.canWrite()) {
                FileInputStream fileIn;
                FileOutputStream fileOut;
                try {
                    fileIn = mContext.openFileInput(updateFilename);
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
    }

    private class DownloadFileTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            for (String s : params) {
                try {
                    URL url = new URL(s);
                    String file = s.substring(s.lastIndexOf("/") + 1);

                    URLConnection urlCon = url.openConnection();
                    BufferedInputStream bis = new BufferedInputStream(urlCon.getInputStream());

                    ByteArrayBuffer baf = new ByteArrayBuffer(50);
                    int current = 0;
                    while ((current = bis.read()) != -1) {
                        baf.append((byte) current);
                    }
                    if (file.equals("manifest.json")) {
                        try {
                            mManifest = new JSONObject(new String(baf.toByteArray()));
                        } catch (JSONException e) {
                            Log.e(TAG, "Bad JSON file", e);
                        }
                    } else {
                        FileOutputStream fos = mContext.openFileOutput(file,
                                Context.MODE_WORLD_READABLE);
                        fos.write(baf.toByteArray());
                        fos.close();
                    }
                    return file;
                } catch (MalformedURLException e) {
                    Log.e(TAG, "Bad URL", e);
                } catch (IOException e) {
                    Log.e(TAG, "Problem downloading file", e);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result.equals("manifest.json")) {
                postManifest();
            } else if (result.equals("su")) {
                new AutoUpdateTask().execute();
            }
        }
    }
    
    public class AutoUpdateTask extends AsyncTask<String, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            String device = null;
            boolean foundSystem = false;
            try {
                Process process = Runtime.getRuntime().exec("mount");
                BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = stdInput.readLine()) != null) {
                    String[] array = line.split(" ");
                    device = array[0];
                    if ((array[1].equals("on") && array[2].equals("/system")) || 
                            array[1].equals("/system")) {
                        foundSystem = true;
                        break;
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Problem remounting /system", e);
                return false;
            }

            if (foundSystem && device != null) {
                final String mountDev = device;
                Process process;
                try {
                    process = Runtime.getRuntime().exec("su");
                    DataOutputStream os = new DataOutputStream(process.getOutputStream());
                    os.writeBytes("mount -o remount,rw " + mountDev + " /system\n");
                    os.writeBytes("cat /data/data/com.noshufou.android.su/files/su > /system/bin/su\n");
                    os.writeBytes("chmod 06755 /system/bin/su\n");
                    os.writeBytes("cat /data/data/com.noshufou.android.su/files/su > /system/sbin/su\n");
                    os.writeBytes("chmod 06755 /system/sbin/su\n");
                    os.writeBytes("mount -o remount,ro " + mountDev + " /system\n");
                    os.writeBytes("exit\n");
                    try {
                        process.waitFor();
                        if (process.exitValue() != 255) {
                            return true;
                        } else {
                            return false;
                        }
                    } catch (InterruptedException e) {
                        return false;
                    }
                } catch (IOException e) {
                    return false;
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                Toast.makeText(mContext, R.string.su_updated, Toast.LENGTH_SHORT).show();
            } else {
                copyUpdateZip(true);
            }
        }
    }
}
