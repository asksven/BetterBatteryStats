package org.zenthought.android.su; 

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class Su extends ListActivity {
    private PermissionDBHelper dbHelper;
    private SQLiteDatabase db;
    private Cursor cursor;
    private DatabaseAdapter adapter;
    
    private Drawable drawableAllow, drawableDeny;

    private class PermissionDBHelper extends SQLiteOpenHelper {
        public PermissionDBHelper(Context context)
        {
            super(context, "permissions.sqlite", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS permissions (_id INTEGER, from_uid INTEGER, from_gid INTEGER, exec_uid INTEGER, exec_gid INTEGER, exec_command TEXT, allow_deny TEXT, PRIMARY KEY (_id), UNIQUE (from_uid,from_gid,exec_uid,exec_gid,exec_command));"
            );
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
        }
    }
    
    private class DatabaseAdapter extends CursorAdapter {
        private LayoutInflater inflater;
    
        public DatabaseAdapter(Context context, Cursor c)
        {
            super(context, c);
            inflater = LayoutInflater.from(context);
        }

        @Override
        public void bindView(View view, Context context, Cursor c)
        {
            TextView fromid = (TextView) view.findViewById(R.id.permission_fromid);
            TextView execid = (TextView) view.findViewById(R.id.permission_execid);
            TextView command = (TextView) view.findViewById(R.id.permission_command);
            ImageView perm = (ImageView) view.findViewById(R.id.permission_perm);

            perm.setImageDrawable(c.getString(6).equals("allow") ? drawableAllow : drawableDeny);
            fromid.setText(c.getString(1) + ":" + c.getString(2));
            execid.setText(c.getString(3) + ":" + c.getString(4));
            command.setText(c.getString(5));
        }

        @Override
        public View newView(Context context, Cursor c, ViewGroup parent)
        {
            return inflater.inflate(R.layout.permission_item, parent, false);
        }
    }
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        drawableAllow = getResources().getDrawable(android.R.drawable.ic_input_add);
        drawableDeny = getResources().getDrawable(android.R.drawable.ic_delete);

        dbHelper = new PermissionDBHelper(this);
        db = dbHelper.getWritableDatabase();
        cursor = db.query("permissions", new String[] { "_id", "from_uid", "from_gid", "exec_uid", "exec_gid", "exec_command", "allow_deny"}, null, null, null, null, "allow_deny");

        adapter = new DatabaseAdapter(this, cursor);
        setListAdapter(adapter);

        ListView list = (ListView) findViewById(android.R.id.list);
        list.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                cursor.moveToPosition(position);
                db.delete("permissions", "_id=?" , new String[] { cursor.getString(0) });
                cursor = db.query("permissions", new String[] { "_id", "from_uid", "from_gid", "exec_uid", "exec_gid", "exec_command", "allow_deny"}, null, null, null, null, "allow_deny");
                adapter.changeCursor(cursor);
            }
        });
    }
}
