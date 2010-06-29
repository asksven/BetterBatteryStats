package com.noshufou.android.su;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper {
    public static final String TAG = "DBHelper";
    
    private static final String DATABASE_NAME = "permissions.sqlite";
    private static final int DATABASE_VERSION = 2;
    private static final String TABLE_NAME = "permissions";

    public static final int ASK = 0;
    public static final int ALLOW = 1;
    public static final int DENY = 2;

    private Context context;
    private SQLiteDatabase db;

    public DBHelper(Context context) {
        this.context = context;
        DBOpenHelper dbOpenHelper = new DBOpenHelper(this.context);
        this.db = dbOpenHelper.getWritableDatabase();
    }

    public class AppStatus {
        public int permission;
        public long dateAccess;

        AppStatus(int permissionIn, long dateAccessIn) {
            permission = permissionIn;
            dateAccess = dateAccessIn;
        }
    }

    public AppStatus checkApp(int fromUid, int toUid, String cmd) {
        int allow = ASK;
        long dateAccess = 0;
        Cursor c = this.db.query(TABLE_NAME,
                                 new String[] { "_id", "allow", "date_access" },
                                 "from_uid=? AND exec_uid=? AND exec_command=?",
                                 new String[] { Integer.toString(fromUid), Integer.toString(toUid), cmd },
                                 null,
                                 null,
                                 null);
        if (c.moveToFirst()) {
            int id = c.getInt(0);
            allow = c.getInt(1);
            dateAccess = c.getLong(2);

            ContentValues values = new ContentValues();
            values.put("date_access", System.currentTimeMillis());
            this.db.update(TABLE_NAME, values, "_id=?", new String[] { Integer.toString(id) });
            
            allow = (allow != 0) ? ALLOW : DENY;
        }
        c.close();
        return new AppStatus( allow, dateAccess );
    }

    public void insert(int fromUid, int toUid, String cmd, int allow) {
        ContentValues values = new ContentValues();
        values.put("from_uid", fromUid);
        values.put("exec_uid", toUid);
        values.put("exec_command", cmd);
        values.put("allow", allow);
        long time = System.currentTimeMillis();
        values.put("date_created", time);
        values.put("date_access", time);
        this.db.insert(TABLE_NAME, null, values);
    }

    public Cursor getAllApps() {
        return this.db.query(TABLE_NAME,
                             new String[] {"_id", "from_uid", "exec_uid", "exec_command", "allow"},
                             null,
                             null,
                             null,
                             null,
                             "allow DESC, from_uid ASC");
    }

    public Cursor getAppDetails(int id) {
        return this.db.query(TABLE_NAME,
                             null,
                             "_id=?",
                             new String[] { Integer.toString(id) },
                             null,
                             null,
                             null);
    }

    public void changeState(int id) {
        Cursor c = this.db.query(TABLE_NAME,
                                 new String[] { "allow" },
                                 "_id=?",
                                 new String[] { Integer.toString(id) },
                                 null,
                                 null,
                                 null);
        if (c.moveToFirst()) {
            int allow = c.getInt(0);
            ContentValues values = new ContentValues();
            values.put("allow", (allow!=0) ? 0 : 1);
            this.db.update(TABLE_NAME, values, "_id=?", new String[] { Integer.toString(id) });
        }
    }

    public void deleteById(int id) {
        this.db.delete(TABLE_NAME, "_id=?", new String[] { Integer.toString(id) });
    }

    public void deleteByUid(int uid) {
        this.db.delete(TABLE_NAME, "from_uid=?", new String[] { Integer.toString(uid) });
    }

    public void close() {
        if (this.db.isOpen()) {
            this.db.close();
        }
    }

    private static class DBOpenHelper extends SQLiteOpenHelper {
        private static final String CREATE_STATEMENT = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME +
            " (_id INTEGER, from_uid INTEGER, exec_uid INTEGER, exec_command TEXT, allow INTEGER, date_created INTEGER, date_access INTEGER, " +
            "PRIMARY KEY (_id), UNIQUE (from_uid,exec_uid,exec_command));";

        DBOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_STATEMENT);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion == 1 && newVersion == 2) {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                ContentValues values = new ContentValues();
                Date dateCreated = new Date();
                Date dateAccess = new Date();
                
                db.execSQL("ALTER TABLE " + TABLE_NAME + " RENAME TO hold");
                db.execSQL(CREATE_STATEMENT);
                
                Cursor c = db.query("hold", null, null, null, null, null, null);
                while (c.moveToNext()) {
                    values.put("_id", c.getInt(0));
                    values.put("from_uid", c.getInt(1));
                    values.put("exec_uid", c.getInt(2));
                    values.put("exec_command", c.getString(3));
                    values.put("allow", c.getInt(4));
                    try {
                        dateCreated = formatter.parse(c.getString(5));
                        dateAccess = formatter.parse(c.getString(6));
                    } catch (java.text.ParseException e) { }
                    values.put("date_created", dateCreated.getTime());
                    values.put("date_access", dateAccess.getTime());
                    db.insert(TABLE_NAME, null, values);
                    values.clear();
                }
                db.execSQL("DROP TABLE IF EXISTS hold");
                c.close();
            }
        }
    }
}
