package com.noshufou.android.su;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.util.Log;

public class DBHelper {
    public static final String TAG = "Su.DBHelper";
    
    private static final String DATABASE_NAME = "permissions.sqlite";
    private static final int DATABASE_VERSION = 5;
    private static final String APPS_TABLE = "apps";
    private static final String LOGS_TABLE = "logs";
    private static final String PREFS_TABLE = "prefs";
    
    public class Apps {
        public static final String ID = "_id";
        public static final String UID = "uid";
        public static final String PACKAGE = "package";
        public static final String NAME = "name";
        public static final String EXEC_UID = "exec_uid";
        public static final String EXEC_CMD = "exec_cmd";
        public static final String ALLOW = "allow";
    }

    public class Logs {
    	public static final String ID = "_id";
    	public static final String APP_ID = "app_id";
    	public static final String DATE = "date";
    	public static final String TYPE = "type";
    }
    
    public class Prefs {
    	public static final String ID = "_id";
    	public static final String KEY = "key";
    	public static final String VALUE = "value";
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
        DBOpenHelper dbOpenHelper = new DBOpenHelper(context);
        this.mDB = dbOpenHelper.getWritableDatabase();
    }

    public AppDetails checkApp(int uid, int execUid, String execCmd) {
        int allow = AppDetails.ASK;
        long dateAccess = 0;
        Cursor c = this.mDB.rawQuery("SELECT apps._id,apps.allow,logs.date FROM apps,logs " +
        		"WHERE (apps.uid=? AND apps.exec_uid=? AND apps.exec_cmd=?) " +
        		"AND (logs.app_id=apps._id AND (logs.type=1 OR logs.type=2)) " +
        		"ORDER BY logs.date LIMIT 1",
        		new String[] { Long.toString(uid), Integer.toString(execUid), execCmd });
        if (c.moveToFirst()) {
            int id = c.getInt(c.getColumnIndex(Apps.ID));
            allow = c.getInt(c.getColumnIndex(Apps.ALLOW));
            dateAccess = c.getLong(c.getColumnIndex(Logs.DATE));

            addLog(id, 0, (allow==AppDetails.ALLOW)?LogType.ALLOW:LogType.DENY);
        }
        c.close();
        return new AppDetails( uid, allow, dateAccess );
    }

    public void insert(int uid, int toUid, String cmd, int allow) {
        ContentValues values = new ContentValues();
        values.put(Apps.UID, uid);
        values.put(Apps.EXEC_UID, toUid);
        values.put(Apps.EXEC_CMD, cmd);
        values.put(Apps.ALLOW, allow);
        values.put(Apps.PACKAGE, Util.getAppPackage(mContext, uid));
        values.put(Apps.NAME, Util.getAppName(mContext, uid, false));
        long id = 0;
        try {
            id = this.mDB.insertOrThrow(APPS_TABLE, null, values);
        } catch (SQLException e) {
            // There was an old, probably stagnant, row in the table
            // Delete it and try again
            deleteByUid(uid);
            id = this.mDB.insert(APPS_TABLE, null, values);
        } finally {
            values.clear();
        
            if (id > 0) {
                addLog(id, System.currentTimeMillis(), LogType.CREATE);
                addLog(id, System.currentTimeMillis(), (allow==AppDetails.ALLOW)?LogType.ALLOW:LogType.DENY);
            }
        }
    }

    public Cursor getAllApps() {
        return this.mDB.query(APPS_TABLE,
                new String[] { Apps.ID, Apps.UID, Apps.PACKAGE, Apps.NAME, Apps.ALLOW },
                null, null, null, null, 
                "allow DESC, name ASC");
    }
    
    public Cursor getAllLogs() {
    	return this.mDB.rawQuery("SELECT logs._id AS _id,logs.date AS date,logs.type AS type," +
    			"apps.uid AS uid,apps.name AS name " +
    			"FROM logs,apps WHERE apps._id=logs.app_id ORDER BY date DESC", null);
    }

    public AppDetails getAppDetails(long id) {
        Cursor cursor = this.mDB.rawQuery("SELECT apps._id AS _id,apps.uid AS uid,apps.package AS package," +
        		"apps.name AS name,apps.exec_uid AS exec_uid,apps.exec_cmd AS exec_cmd,apps.allow AS allow," +
        		"logs.date AS date,logs.type AS type " +
        		"FROM apps,logs " + 
        		"WHERE apps._id=? AND logs.app_id=apps._id AND (logs.type=0 OR logs.type=1 OR logs.type=2)" +
        		"ORDER BY logs.date DESC ",
        		new String[] { Long.toString(id) });
        AppDetails appDetails = new AppDetails();
        if (cursor.moveToFirst()) {
        	appDetails.setUid(cursor.getInt(cursor.getColumnIndex(Apps.UID)));
        	appDetails.setPackageName(cursor.getString(cursor.getColumnIndex(Apps.PACKAGE)));
        	appDetails.setName(cursor.getString(cursor.getColumnIndex(Apps.NAME)));
        	appDetails.setAllow(cursor.getInt(cursor.getColumnIndex(Apps.ALLOW)));
        	appDetails.setExecUid(cursor.getInt(cursor.getColumnIndex(Apps.EXEC_UID)));
        	appDetails.setCommand(cursor.getString(cursor.getColumnIndex(Apps.EXEC_CMD)));
        	boolean accessFound = false;
        	boolean createdFound = false;
        	do {
        		int logType = cursor.getInt(cursor.getColumnIndex(Logs.TYPE));
        		if (logType == LogType.CREATE) {
        			appDetails.setDateCreated(cursor.getLong(cursor.getColumnIndex(Logs.DATE)));
        			createdFound = true;
        		} else if (logType == LogType.ALLOW || logType == LogType.DENY) {
        			appDetails.setAccessType(logType);
        			appDetails.setDateAccess(cursor.getLong(cursor.getColumnIndex(Logs.DATE)));
        			accessFound = true;
        		}
        		if (accessFound && createdFound) {
        			break;
        		}
        	} while (cursor.moveToNext());
        }
        cursor.close();
        return appDetails;
    }
    
    public long getLastLog(int appId, int type) {
        Cursor c = this.mDB.query(LOGS_TABLE, 
                new String[] { Logs.ID, Logs.DATE, Logs.TYPE },
                "app_id=? AND type=?",
                new String[] { Integer.toString(appId), Integer.toString(type) },
                null,
                null,
                "date DESC",
                "1");
        long date = 0;
        if (c.moveToFirst()) {
            date = c.getLong(c.getColumnIndex(Logs.DATE));
        }
        c.close();
        return date;
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

    public void changeState(long id) {
    	Cursor c = this.mDB.query(APPS_TABLE,
    			new String[] { Apps.ALLOW },
    			"_id=?",
    			new String[] { Long.toString(id) },
    			null, null, null);
        if (c.moveToFirst()) {
            int allow = c.getInt(0);
            ContentValues values = new ContentValues();
            values.put(Apps.ALLOW, (allow!=0) ? 0 : 1);
            this.mDB.update(APPS_TABLE, values, "_id=?", new String[] { Long.toString(id) });
            values.clear();
            
            addLog(id, 0, LogType.TOGGLE);
        } else {
        	Log.d(TAG, "app matching uid " + id + " not found in database");
        }
        c.close();
    }

    public void deleteById(long id) {
    	Log.d(TAG, "Deleting from logs table where app_id=" + id);
    	this.mDB.delete(LOGS_TABLE, "app_id=?", new String[] { Long.toString(id) });
    	Log.d(TAG, "Deleting from apps table where _id=" + id);
        this.mDB.delete(APPS_TABLE, "_id=?", new String[] { Long.toString(id) });
    }

    public void deleteByUid(int uid) {
    	Cursor cursor = this.mDB.query(APPS_TABLE, new String[] { Apps.ID },
    			"uid=?", new String[] { Integer.toString(uid) },
    			null, null, null);
    	if (cursor.moveToFirst()) {
    		Log.d(TAG, "_id found, deleting logs");
    		long id = cursor.getLong(cursor.getColumnIndex(Apps.ID));
    		this.mDB.delete(LOGS_TABLE, "_id=?", new String[] { Long.toString(id) });
    	}
        this.mDB.delete(APPS_TABLE, "uid=?", new String[] { Integer.toString(uid) });
        cursor.close();
    }
    
    private void addLog(long id, long time, int logType) {
    	ContentValues values = new ContentValues();
    	values.put(Logs.APP_ID, id);
    	values.put(Logs.DATE, (time==0)?System.currentTimeMillis():time);
    	values.put(Logs.TYPE, logType);
    	this.mDB.insert(LOGS_TABLE, null, values);
    }
    
    public void clearLog() {
        this.mDB.delete(LOGS_TABLE, null, null);
    }
    
    public void setNotifications(boolean notifications) {
    	ContentValues values = new ContentValues();
    	values.put("value", notifications?1:0);
    	this.mDB.update(PREFS_TABLE, values, "key=?", new String[] { "notifications" });
    }
    
    public int getDBVersion() {
    	return this.mDB.getVersion();
    }

    public void close() {
        if (this.mDB.isOpen()) {
            this.mDB.close();
        }
    }

    private static class DBOpenHelper extends SQLiteOpenHelper {
    	private static final String CREATE_APPS = "CREATE TABLE IF NOT EXISTS " + APPS_TABLE +
    			" (_id INTEGER, uid INTEGER, package TEXT, name TEXT, exec_uid INTEGER, " +
    			"exec_cmd TEXT, allow INTEGER," +
    			" PRIMARY KEY (_id), UNIQUE (uid,exec_uid,exec_cmd));";
        
        private static final String CREATE_LOGS = "CREATE TABLE IF NOT EXISTS " + LOGS_TABLE +
        		" (_id INTEGER, app_id INTEGER, date INTEGER, type INTEGER, " + 
        		"PRIMARY KEY (_id));";
        
        private static final String CREATE_PREFS = "CREATE TABLE IF NOT EXISTS " + PREFS_TABLE +
        		" (_id INTEGER, key TEXT, value TEXT, PRIMARY KEY (_id));";
        
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
            ContentValues values = new ContentValues();

            if (oldVersion == 1) {
                db.execSQL("DROP TABLE IF EXISTS permissions");
                db.execSQL(CREATE_APPS);
                db.execSQL(CREATE_LOGS);
                createPrefs(db);
            } else if (oldVersion == 2 && newVersion == 5) {
            	db.execSQL(CREATE_APPS);
            	db.execSQL(CREATE_LOGS);
            	createPrefs(db);

            	long id;
            	int uid;
            	String packageName;
            	String appName;
            	int allow;
            	Cursor c = db.query("permissions", null, null, null, null, null, null);
            	while (c.moveToNext()) {
            		uid = c.getInt(c.getColumnIndex("from_uid"));
            		packageName = Util.getAppPackage(mContext, uid);
            		appName = Util.getAppName(mContext, uid, false);
            		allow = c.getInt(c.getColumnIndex("allow"));

            		values.put(Apps.UID, uid);
            		values.put(Apps.PACKAGE, packageName);
            		values.put(Apps.NAME, appName);
            		values.put(Apps.EXEC_UID, c.getInt(c.getColumnIndex("exec_uid")));
            		values.put(Apps.EXEC_CMD, c.getString(c.getColumnIndex("exec_command")));
            		values.put(Apps.ALLOW, allow);
            		id = db.insert(APPS_TABLE, null, values);
            		values.clear();

                	values.put(Logs.APP_ID, id);
                	values.put(Logs.DATE, c.getLong(c.getColumnIndex("date_created")));
                	values.put(Logs.TYPE, LogType.CREATE);
                	db.insert(LOGS_TABLE, null, values);
                	values.clear();

                	values.put(Logs.APP_ID, id);
                	values.put(Logs.DATE, c.getLong(c.getColumnIndex("date_access")));
                	values.put(Logs.TYPE, (allow == AppDetails.ALLOW)?LogType.ALLOW:LogType.DENY);
                	db.insert(LOGS_TABLE, null, values);
                	values.clear();
            	}
            	c.close();
            	db.execSQL("DROP TABLE IF EXISTS permissions");
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
    }
}
