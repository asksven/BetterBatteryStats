package com.noshufou.android.su;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;

public class Util {
	private static final String TAG = "Su.Util";
	
    public static String getAppName(Context c, int uid, boolean withUid) {
        PackageManager pm = c.getPackageManager();
        String appName = "Unknown";
        String[] packages = pm.getPackagesForUid(uid);

        if (packages != null) {
            if (packages.length == 1) {
                try {
                    ApplicationInfo appInfo = pm.getApplicationInfo(packages[0], 0);
                    appName = pm.getApplicationLabel(appInfo).toString();
                } catch (NameNotFoundException e) {
                	Log.e(TAG, "No package found matching with the uid " + uid);
                }
            } else if (packages.length > 1) {
                appName = "Multiple Packages";
            }
        } else {
            Log.e(TAG, "Package not found for uid " + uid);
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

        if (packages != null) {
            if (packages.length == 1) {
                appPackage = packages[0];
            } else if (packages.length > 1) {
                appPackage = "Multiple packages";
            }
        } else {
            Log.e(TAG, "Package not found");
        }

        return appPackage;
    }

    public static Drawable getAppIcon(Context c, int uid) {
        PackageManager pm = c.getPackageManager();
        Drawable appIcon = c.getResources().getDrawable(R.drawable.sym_def_app_icon);
        String[] packages = pm.getPackagesForUid(uid);

        if (packages != null) {
            if (packages.length == 1) {
                try {
                    ApplicationInfo appInfo = pm.getApplicationInfo(packages[0], 0);
                    appIcon = pm.getApplicationIcon(appInfo);
                } catch (NameNotFoundException e) {
                	Log.e(TAG, "No package found matching with the uid " + uid);
                }
            }
        } else {
            Log.e(TAG, "Package not found for uid " + uid);
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
    
    public static String formatDate(Context context, long date) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	String format = prefs.getString("pref_date_format", "default");
    	if (format.equals("default")) {
    		return DateFormat.getDateFormat(context).format(date);
    	} else {
    		return (String)DateFormat.format(format, date);
    	}
    }
    
    public static String formatTime(Context context, long time) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	boolean hour24 = prefs.getBoolean("pref_24_hour_format", true);
    	boolean showSeconds = prefs.getBoolean("pref_show_seconds", true);
    	String hour = "kk";
    	String min = "mm";
    	String sec = ":ss";
    	String post = "";
    	
    	if (hour24) {
    		hour = "kk";
    	} else {
    		hour = "hh";
    		post = "aa";
    	}
    	
    	if (showSeconds) {
    		sec = ":ss";
    	} else {
    		sec = "";
    	}
    	
    	String format = String.format("%s:%s%s%s", hour, min, sec, post);
    	 return (String)DateFormat.format(format, time);
    }
    
    public static String formatDateTime(Context context, long date) {
    	return formatDate(context, date) + " " + formatTime(context, date);
    }

}
