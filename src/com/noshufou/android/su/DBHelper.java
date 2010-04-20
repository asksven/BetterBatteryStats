package com.noshufou.android.su;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper {
    private static final String TAG = "DBHelper";
    
    private static final String DATABASE_NAME = "permissions.sqlite";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "permissions";

    private Context context;
    private SQLiteDatabase db;

    public DBHelper(Context context) {
        this.context = context;
        DBOpenHelper dbOpenHelper = new DBOpenHelper(this.context);
        this.db = dbOpenHelper.getWritableDatabase();
    }

    public Cursor getAllApps() {
        return this.db.query(TABLE_NAME, new String[] {"_id", "from_uid", "exec_uid", "exec_command", "allow"}, null, null, null, null, "allow DESC, from_uid ASC");
    }

    public Cursor getAppDetails(int id) {
        return this.db.query(TABLE_NAME, null, "_id=?", new String[] { Integer.toString(id) }, null, null, null);
    }

    public void changeState(int id) {
        Cursor c = this.db.query(TABLE_NAME, new String[] { "allow" }, "_id=?", new String[] { Integer.toString(id) }, null, null, null);
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

        DBOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + 
                " (_id INTEGER, from_uid INTEGER, exec_uid INTEGER, exec_command TEXT, allow INTEGER, date_created TEXT, date_access TEXT, " +
                "PRIMARY KEY (_id), UNIQUE (from_uid,exec_uid,exec_command));"
            );
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }
}
