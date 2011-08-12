package com.noshufou.android.su.service;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.noshufou.android.su.SuRequestReceiver;
import com.noshufou.android.su.preferences.Preferences;
import com.noshufou.android.su.provider.PermissionsProvider.Apps;
import com.noshufou.android.su.provider.PermissionsProvider.Logs;
import com.noshufou.android.su.util.Util;

public class LogService extends IntentService {
    private static final String TAG = "Su.LogService";

    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_APP_ID = "app_id";
    public static final String EXTRA_APP_UID = SuRequestReceiver.EXTRA_CALLERUID;
    public static final String EXTRA_ALLOW = SuRequestReceiver.EXTRA_ALLOW;
    
    public static final int ADD_LOG = 1;
    public static final int RECYCLE = 2;
    
    ContentResolver mCr = null;
    
    public LogService() {
        super(TAG);
    }
    
    @Override
    public void onStart(Intent intent, int startId) {
        Log.d(TAG, "Service started via onStart()");
        mCr = getContentResolver();
        super.onStart(intent, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getIntExtra(EXTRA_ACTION, 0) == ADD_LOG) {
            if (intent.hasExtra(EXTRA_APP_ID)) {
                addLog(intent.getLongExtra(EXTRA_APP_ID, 0),
                        intent.getIntExtra(EXTRA_ALLOW, 0));
            } else {
                int appUid = intent.getIntExtra(EXTRA_APP_UID, 0);
                Uri appUri = Uri.withAppendedPath(Apps.CONTENT_URI, "uid/" + appUid);
                Log.d(TAG, "Looking for app with UID of " + appUid);
                Log.d(TAG, "Using uri: " + appUri);
                Cursor c = mCr.query(appUri, new String[] { Apps._ID }, null, null, null);
                if (c != null && c.moveToFirst()) {
                    Log.d(TAG, "app_id=" + c.getLong(c.getColumnIndex(Apps._ID)));
                    addLog(c.getLong(c.getColumnIndex(Apps._ID)),
                            intent.getIntExtra(EXTRA_ALLOW, 0));
                    c.close();
                } else {
                    Log.d(TAG, "app_id not found, add it to the database");
                    addAppAndLog(intent);
                }
            }
            recycle();
        } else if (intent.getIntExtra(EXTRA_ACTION, 0) == RECYCLE) {
            recycle();
        } else  {
            throw new IllegalArgumentException();
        }
    }
    
    private void addAppAndLog(Intent intent) {
        ContentValues values = new ContentValues();
        int appUid = intent.getIntExtra(SuRequestReceiver.EXTRA_CALLERUID, 0);
        String appPackage = Util.getAppPackage(this, appUid);
        values.put(Apps.UID, appUid);
        values.put(Apps.PACKAGE, appPackage);
        values.put(Apps.NAME, Util.getAppName(this, appUid, false));
        values.put(Apps.EXEC_UID, intent.getIntExtra("desired_uid", 0));
        values.put(Apps.EXEC_CMD, intent.getStringExtra("desired_cmd"));
        values.put(Apps.ALLOW, Apps.AllowType.ASK);
        long appId = Long.parseLong(mCr.insert(Apps.CONTENT_URI, values).getLastPathSegment());
        Log.d(TAG, "appId = " + appId);
        addLog(appId, intent.getIntExtra("allow", -1));
    }
    
    private void addLog(long appId, int type) {
        Log.d(TAG, "Adding log for app_id " + appId + " for type " + type);
        ContentValues values = new ContentValues();
        values.put(Logs.DATE, System.currentTimeMillis());
        values.put(Logs.TYPE, type);
        mCr.insert(Uri.withAppendedPath(Logs.CONTENT_URI, String.valueOf(appId)), values);
    }
    
    private void recycle() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean(Preferences.DELETE_OLD_LOGS, true)) {
            // Log recycling is disabled, no need to go further
            return;
        }
        
        int limit = prefs.getInt(Preferences.LOG_ENTRY_LIMIT, 200);
        Cursor c = mCr.query(Logs.CONTENT_URI, new String[] { "COUNT() as rows" },
                null, null, null);
        c.moveToFirst();
        int count = c.getInt(0);
        c.close();
        if (count > limit) {
            Log.d(TAG, "Too many logs, " + limit + " allowed, " + count + " found. Deleting oldest logs");
            c = mCr.query(Logs.CONTENT_URI, new String[] { Logs._ID }, null, null, Logs.DATE + " ASC");
            long id = 0;
            while (count > limit && c.moveToNext()) {
                id = c.getLong(0);
                Log.d(TAG, "Deleting log where id=" + id);
                count -= mCr.delete(Logs.CONTENT_URI, Logs._ID + "=?", new String[] { String.valueOf(id) });
                Log.d(TAG, "New count " + count);
            }
        } else {
            Log.d(TAG, "Not too many logs, " + limit + " allowed, " + count + " found.");
        }
    }

}
