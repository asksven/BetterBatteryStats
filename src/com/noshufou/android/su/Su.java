package com.noshufou.android.su; 

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class Su extends ListActivity {
    private static final String TAG = "Su";
    
    private DBHelper db;
    private Cursor cursor;
    private DatabaseAdapter adapter;
    private String helloworld = "Hello, World!";

    private class DatabaseAdapter extends CursorAdapter {
        private LayoutInflater inflater;
        private Drawable drawableAllow, drawableDeny;
    
        public DatabaseAdapter(Context context, Cursor c)
        {
            super(context, c);
            inflater = LayoutInflater.from(context);

            drawableAllow = getResources().getDrawable(android.R.drawable.presence_online);
            drawableDeny = getResources().getDrawable(android.R.drawable.presence_busy);
        }

        @Override
        public void bindView(View view, Context context, Cursor c)
        {
            TextView appNameView = (TextView) view.findViewById(R.id.appName);
            TextView appUidView = (TextView) view.findViewById(R.id.appUid);
            TextView requestView = (TextView) view.findViewById(R.id.request);
            ImageView appIconView = (ImageView) view.findViewById(R.id.appIcon);
            ImageView itemPermission = (ImageView) view.findViewById(R.id.itemPermission);

            final int id = c.getInt(0);
            final int uid = c.getInt(1);
            final int requestUid = c.getInt(2);
            final String requestCommand = c.getString(3);
            final int allow = c.getInt(4);

            String appName = getAppName(context, uid, false);
            Drawable appIcon = getAppIcon(context, uid);
            String requestUser = getUidName(context, requestUid, false);
            
            appNameView.setText(appName);
            appUidView.setText(getString(R.string.uid, uid));
            requestView.setText(getString(R.string.request, requestCommand, requestUser, requestUid));
            appIconView.setImageDrawable(appIcon);
            itemPermission.setImageDrawable((allow!=0) ? drawableAllow : drawableDeny);
            itemPermission.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    db.changeState(id);
                    refreshList();
                }
            });
        }

        @Override
        public View newView(Context context, Cursor c, ViewGroup parent)
        {
            return inflater.inflate(R.layout.item, parent, false);
        }
    }
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        db = new DBHelper(this);

        adapter = new DatabaseAdapter(this, cursor);
        setListAdapter(adapter);

        ListView list = (ListView) findViewById(android.R.id.list);

        list.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                cursor.moveToPosition(position);
                appDetails(cursor.getInt(0));
                refreshList();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();
    }

    private void refreshList() {
        cursor = db.getAllApps();
        adapter.changeCursor(cursor);
    }

    private void appDetails(int id) {
        LayoutInflater inflater = LayoutInflater.from(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog alert;

        View layout = inflater.inflate(R.layout.app_details, (ViewGroup) findViewById(R.id.detailLayout));
        
        TextView packageNameView = (TextView) layout.findViewById(R.id.packageName);
        TextView requestView = (TextView)layout.findViewById(R.id.requestDetail);
        TextView commandView = (TextView) layout.findViewById(R.id.command);
        TextView statusView = (TextView) layout.findViewById(R.id.status);
        TextView createdView = (TextView) layout.findViewById(R.id.created);
        TextView lastAccessedView = (TextView) layout.findViewById(R.id.lastAccessed);

        Cursor app = db.getAppDetails(id);
        app.moveToFirst();
        final int appId = app.getInt(0);
        int appUid = app.getInt(1);

        String appName = getAppName(this, appUid, true);
        String appPackage = getAppPackage(this, appUid);
        Drawable appIcon = getAppIcon(this, appUid);

        packageNameView.setText(appPackage);

        int requestUid = app.getInt(2);
        requestView.setText(getUidName(this, requestUid, true));
        commandView.setText(app.getString(3));
        statusView.setText((app.getInt(4)!=0) ? R.string.allow : R.string.deny);
        createdView.setText(app.getString(5));
        lastAccessedView.setText(app.getString(6));

        builder.setTitle(appName)
               .setIcon(appIcon)
               .setView(layout)
               .setPositiveButton((app.getInt(4)!=0) ? R.string.deny : R.string.allow, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        db.changeState(appId);
                        refreshList();
                    }
                })
               .setNeutralButton(getString(R.string.forget), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        db.deleteById(appId);
                        refreshList();
                        dialog.cancel();
                    }
                })
               .setNegativeButton(getString(R.string.cancel), null);
        alert = builder.create();
        alert.show();
    }

    public static String getAppName(Context c, int uid, boolean withUid) {
        PackageManager pm = c.getPackageManager();
        String appName = "Unknown";
        String[] packages = pm.getPackagesForUid(uid);

        if (packages.length == 1) {
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(packages[0], 0);
                appName = pm.getApplicationLabel(appInfo).toString();
            } catch (NameNotFoundException e) { } // Obligitory catch
        } else if (packages.length > 1) {
            appName = "Multiple Packages";
        }

        if (withUid) {
            appName += " (" + uid + ")";
        }

        return appName;
    }

    public static String getAppPackage(Context c, int uid) {
        PackageManager pm = c.getPackageManager();
        String[] packages = pm.getPackagesForUid(uid);
        String appPackage = "unknown";

        if (packages.length == 1) {
            appPackage = packages[0];
        } else if (packages.length > 1) {
            appPackage = "multiple packages";
        }

        return appPackage;
    }

    public static Drawable getAppIcon(Context c, int uid) {
        PackageManager pm = c.getPackageManager();
        Drawable appIcon = c.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
        String[] packages = pm.getPackagesForUid(uid);

        if (packages.length == 1) {
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(packages[0], 0);
                appIcon = pm.getApplicationIcon(appInfo);
            } catch (NameNotFoundException e) { } // Obligitory catch
        }

        return appIcon;
    }

    public static String getUidName(Context c, int uid, boolean withUid) {
        PackageManager pm = c.getPackageManager();
        String uidName= "";
        if (uid == 0) {
            uidName = "root";
        } else {
            pm.getNameForUid(uid);
        }

        if (withUid) {
            uidName += " (" + uid + ")";
        }
        
        return uidName;
    }
}
