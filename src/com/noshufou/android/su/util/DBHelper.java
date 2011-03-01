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
package com.noshufou.android.su.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;

public class DBHelper {
    private static final String TAG = "Su.DbHelper";

    private static final String DATABASE_NAME = "permissions.sqlite";
    private static final int DATABASE_VERSION = 6;
    private static final String APPS_TABLE = "apps";
    private static final String LOGS_TABLE = "logs";
    private static final String PREFS_TABLE = "prefs";

    private static final String ADD_LOG_TRIGGER_NAME = "add_log";
//    private static final String ADD_LOG_TRIGGER = "CREATE TRIGGER IF NOT EXISTS add_log " +
//            "AFTER UPDATE OF last_access ON apps\n" +
//            "BEGIN\n" +
//            "  INSERT INTO logs(name,date,type) VALUES (NEW.name,NEW.last_access,NEW.last_access_type);\n" +
//            "END;";

    public class Apps {
        public static final String ID = "_id";
        public static final String UID = "uid";
        public static final String PACKAGE = "package";
        public static final String NAME = "name";
        public static final String EXEC_UID = "exec_uid";
        public static final String EXEC_CMD = "exec_cmd";
        public static final String ALLOW = "allow";
        public static final String NOTIFICATIONS = "notifications";
        public static final String LOGGING = "logging";
    }

    public class Logs {
        public static final String ID = "_id";
        public static final String APP_ID = "app_id";
        public static final String NAME = "name";
        public static final String UID = "uid";
        public static final String DATE = "date";
        public static final String TYPE = "type";
    }

    public class Prefs {
        public static final String ID = "_id";
        public static final String KEY = "key";
        public static final String VALUE = "value";
    }
    
    public class PrefsKeys {
        public static final String NOTIFICATIONS = "notifications";
        public static final String LOGGING = "logging";
    }

    public class LogType {
        public static final int DENY = 0;
        public static final int ALLOW = 1;
        public static final int CREATE = 2;
        public static final int TOGGLE = 3;
    }

    private Context mContext;
    private SQLiteDatabase mDB;

    public DBHelper(Context context) {
        this.mContext = context;
        this.mDB = new DBOpenHelper(context).getWritableDatabase();
    }
    
    public void close() {
        if (this.mDB.isOpen()) {
            this.mDB.close();
        }
    }


    public static void setPreference(Context context, String key, boolean value) {
        SQLiteDatabase db = new DBOpenHelper(context).getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Prefs.VALUE, value?1:0);
        int rows = db.update(PREFS_TABLE, values, "key=?", new String[] { key });
        if (rows == 0) {
            db.insert(PREFS_TABLE, null, values);
        } else if (rows > 1) {
            db.delete(PREFS_TABLE, "key=", new String[] { key });
            db.insert(PREFS_TABLE, null, values);
        }
    }
    
    public int countPermissions(int permission) {
        int count = 0;
        Cursor cursor = this.mDB.query(APPS_TABLE,
                new String[] { "count(*)" },
                "allow=?",
                new String[] { Integer.toString(permission) },
                null, null, null);
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }
    
    public Cursor getAllApps() {
        return this.mDB.query(APPS_TABLE, null, null, null, null, null, "allow DESC, name ASC");
    }
    
    public AppDetails getAppDetails(long id) {
        AppDetails appDetails = new AppDetails();
        Cursor cursor = this.mDB.query(APPS_TABLE, 
                null, "_id=?", new String[] { Long.toString(id) },
                null, null, null, "1");
        if (cursor.moveToFirst()) {
            appDetails.setId((int)id);
            appDetails.setUid(cursor.getInt(cursor.getColumnIndex(Apps.UID)));
            appDetails.setPackageName(cursor.getString(cursor.getColumnIndex(Apps.PACKAGE)));
            String appName = cursor.getString(cursor.getColumnIndex(Apps.NAME));
            appDetails.setName(appName);
            appDetails.setExecUid(cursor.getInt(cursor.getColumnIndex(Apps.EXEC_UID)));
            appDetails.setCommand(cursor.getString(cursor.getColumnIndex(Apps.EXEC_CMD)));
            appDetails.setAllow(cursor.getInt(cursor.getColumnIndex(Apps.ALLOW)));
            Cursor logCursor = this.mDB.query(LOGS_TABLE, null, "name=?",
                    new String[] { appName }, null, null, "date DESC" );
            appDetails.setRecentUsage(logCursor);
            return appDetails;
        } else {
            return null;
        }
    }
    
    public void insert(int uid, int toUid, String cmd, boolean allow, long time) {
        String appName = Util.getAppName(mContext, uid, false);
        ContentValues values = new ContentValues();
        values.put(Apps.UID, uid);
        values.put(Apps.EXEC_UID, toUid);
        values.put(Apps.EXEC_CMD, cmd);
        values.put(Apps.ALLOW, allow ? AppDetails.ALLOW : AppDetails.DENY);
        values.put(Apps.PACKAGE, Util.getAppPackage(mContext, uid));
        values.put(Apps.NAME, appName);
        try {
            this.mDB.insertOrThrow(APPS_TABLE, null, values);
        } catch (SQLiteException e) {
            this.mDB.delete(APPS_TABLE, Apps.UID + " = ?", new String[] { Integer.toString(uid) });
            this.mDB.insert(APPS_TABLE, null, values);
        }
        if (PreferenceManager.getDefaultSharedPreferences(mContext)
                .getBoolean("pref_log_enabled", true)) {
            addLog(appName, LogType.CREATE, time);
            addLog(appName, allow ? LogType.ALLOW : LogType.DENY, time);
        }
    }
    
    public void addLog(String appName, int logType, long time) {
        ContentValues values = new ContentValues();
        values.put(Logs.NAME, appName);
        values.put(Logs.TYPE, logType);
        values.put(Logs.DATE, time);
        this.mDB.insert(LOGS_TABLE, null, values);
    }

    private static class DBOpenHelper extends SQLiteOpenHelper {
        private static final String CREATE_APPS = "CREATE TABLE IF NOT EXISTS " + APPS_TABLE +
                " (_id INTEGER, uid INTEGER, package TEXT, name TEXT, exec_uid INTEGER, " +
                "exec_cmd TEXT, allow INTEGER, " +
                "PRIMARY KEY (_id) UNIQUE (uid,exec_uid,exec_cmd));";

        private static final String CREATE_LOGS = "CREATE TABLE IF NOT EXISTS " + LOGS_TABLE +
                " (_id INTEGER, uid INTEGER, name TEXT, app_id INTEGER, date INTEGER, type INTEGER, " +
                "PRIMARY KEY (_id));";

        private static final String CREATE_PREFS = "CREATE TABLE IF NOT EXISTS " + PREFS_TABLE +
                " (_id INTEGER, key TEXT, value TEXT, PRIMARY KEY (_id) UNIQUE (key));";

        private Context mContext;

        DBOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_APPS);
            db.execSQL(CREATE_LOGS);
            createPrefs(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            int upgradeVersion = oldVersion;

            // Pattern for upgrade blocks
            //
            //    if (upgradeVersion == [the DATABASE_VERSION you set] - 1) {
            //        .. your upgrade logic ..
            //        upgradeVersion = [the DATABASE_VERSION you set]
            //    }
            if (upgradeVersion < 5) {
                // Really lazy here... Plus I can't really remember the structure
                // of the apps table before version 5...
                db.execSQL("DROP TABLE IF EXISTS permissions;");
                onCreate(db);
                return;
            }

            if (upgradeVersion == 5) {
                convertLog(db);
                db.execSQL("ALTER TABLE apps ADD COLUMN notifications INTEGER");
                db.execSQL("ALTER TABLE apps ADD COLUMN logging INTEGER");
                upgradeVersion = 6;
            }
        }

        private void createPrefs(SQLiteDatabase db) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            boolean notifications = prefs.getBoolean("pref_notifications", true);

            db.execSQL(CREATE_PREFS);

            ContentValues values = new ContentValues();
            values.put(Prefs.KEY, "notifications");
            values.put(Prefs.VALUE, notifications?"1":"0");
            db.insert(PREFS_TABLE, null, values);
        }
        
        private void convertLog(SQLiteDatabase db) {
            db.execSQL("ALTER TABLE logs ADD COLUMN name TEXT;");
            db.execSQL("ALTER TABLE logs ADD COLUMN uid INTEGER;");
            
            Cursor apps = db.query(APPS_TABLE,
                    new String[] { Apps.ID, Apps.UID, Apps.NAME },
                    null, null, null, null, "_id ASC");
            int idIndex = apps.getColumnIndex(Apps.ID);
            int uidIndex = apps.getColumnIndex(Apps.UID);
            int nameIndex = apps.getColumnIndex(Apps.NAME);
            
            ContentValues values = new ContentValues();
            while (apps.moveToNext()) {
                values.clear();
                values.put(Logs.NAME, apps.getString(nameIndex));
                values.put(Logs.UID, apps.getInt(uidIndex));
                db.update(LOGS_TABLE, values, Logs.APP_ID + "=?",
                        new String[] { apps.getString(idIndex) });
            }
            apps.close();
            
        }
    }
}
