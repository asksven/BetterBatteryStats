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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;

import com.noshufou.android.su.AppListActivity;
import com.noshufou.android.su.R;
import com.noshufou.android.su.preferences.Preferences;

public class Util {
    private static final String TAG = "Su.Util";

    public static String getAppName(Context c, int uid, String packageName, boolean withUid) {
        PackageManager pm = c.getPackageManager();
        String appName = packageName;
        String[] packages = pm.getPackagesForUid(uid);

        if (packages != null) {
            try {
            	if (appName == null) {
            		if(packages.length == 1) {
            			appName = pm.getApplicationInfo(packages[0], 0).toString();
            		} else {
            			return c.getString(R.string.shared_uid, uid);
            		}
            	} else {
            		for (String s : packages) {
            			if (s.equals(packageName)) {
            				appName = pm.getApplicationLabel(pm.getApplicationInfo(s, 0))
            						.toString();
            				break;
            			}
            		}
            	}
            } catch (NameNotFoundException e) {
            	Log.e(TAG, "Package name not found", e);
            }
        } else {
            Log.e(TAG, "Package not found for uid " + uid);
        }

        if (withUid) {
            appName += " (" + uid + ")";
        }

        return appName;
    }

//    public static String getAppPackage(Context c, int uid) {
//        PackageManager pm = c.getPackageManager();
//        String[] packages = pm.getPackagesForUid(uid);
//        String appPackage = "unknown";
//
//        if (packages != null) {
//            if (packages.length == 1) {
//                appPackage = packages[0];
//            } else if (packages.length > 1) {
//                appPackage = "Multiple packages";
//            }
//        } else {
//            Log.e(TAG, "Package not found");
//        }
//
//        return appPackage;
//    }

    public static Drawable getAppIcon(Context c, int uid) {
        PackageManager pm = c.getPackageManager();
        Drawable appIcon = c.getResources().getDrawable(R.drawable.sym_def_app_icon);
        String[] packages = pm.getPackagesForUid(uid);

        if (packages != null) {
//            if (packages.length == 1) {
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(packages[0], 0);
                appIcon = pm.getApplicationIcon(appInfo);
            } catch (NameNotFoundException e) {
                Log.e(TAG, "No package found matching with the uid " + uid);
            }
//            }
        } else {
            Log.e(TAG, "Package not found for uid " + uid);
        }

        return appIcon;
    }

    public static Drawable getStatusIconDrawable(Context context, int allow) {
        int[][] statusButtons = {
                { R.drawable.perm_deny_dot, R.drawable.perm_allow_dot },
                { R.drawable.perm_deny_emo, R.drawable.perm_allow_emo }
        };
        
        String iconTypeString = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(Preferences.STATUS_ICON_TYPE, "emote");
        int statusIconType = 1;
        if (iconTypeString.equals("dot")) {
            statusIconType = 0;
        } else if (iconTypeString.equals("emote")) {
            statusIconType = 1;
        }

        if (allow < 0 || allow > 1) {
            Log.e(TAG, "Bad value given to getStatusButtonDrawable(int). Expecting 0 or 1, got " + allow);
            return null;
        }

        Drawable drawable = context.getResources().getDrawable(statusButtons[statusIconType][allow]);
        return drawable;
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
    
    public static void goHome(Context context) {
        // Be clever here, otherwise the home button doesn't work as advertised.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Intent intent;
        if (prefs.getBoolean(Preferences.GHOST_MODE, false)) {
            intent = new Intent(context, AppListActivity.class);
        } else {
            intent = new Intent();
            intent.setComponent(new ComponentName("com.noshufou.android.su",
                    "com.noshufou.android.su.Su"));
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    public static String getSuVersion() {
        Process process = null;
        String inLine = null;

        try {
            process = Runtime.getRuntime().exec("sh");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            BufferedReader is = new BufferedReader(new InputStreamReader(
                    new DataInputStream(process.getInputStream())), 64);
            os.writeBytes("su -v\n");

            // We have to hold up the thread to make sure that we're ready to read
            // the stream, using increments of 5ms makes it return as quick as
            // possible, and limiting to 500ms makes sure that it doesn't hang for
            // too long if there's a problem.
            for (int i = 0; i < 100; i++) {
                if (is.ready()) {
                    Log.d(TAG, "slept " + i + " times");
                    break;
                }
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Log.w(TAG, "Sleep timer got interrupted...");
                }
            }
            if (is.ready()) {
                inLine = is.readLine();
                if (inLine != null) {
                    return inLine;
                }
            } else {
                os.writeBytes("exit\n");
            }
        } catch (IOException e) {
            Log.e(TAG, "Problems reading current version.", e);
            return null;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }

        return null;
    }

    public static int getSuVersionCode() {
        Process process = null;
        String inLine = null;

        try {
            process = Runtime.getRuntime().exec("sh");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            BufferedReader is = new BufferedReader(new InputStreamReader(
                    new DataInputStream(process.getInputStream())), 64);
            os.writeBytes("su -v\n");

            // We have to hold up the thread to make sure that we're ready to read
            // the stream, using increments of 5ms makes it return as quick as
            // possible, and limiting to 500ms makes sure that it doesn't hang for
            // too long if there's a problem.
            for (int i = 0; i < 100; i++) {
                if (is.ready()) {
                    Log.d(TAG, "slept " + i + " times");
                    break;
                }
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Log.w(TAG, "Sleep timer got interrupted...");
                }
            }
            if (is.ready()) {
                inLine = is.readLine();
                if (inLine != null && Integer.parseInt(inLine.substring(0, 1)) > 2) {
                    inLine = null;
                    os.writeBytes("su -V\n");
                    inLine = is.readLine();
                    if (inLine != null) {
                        return Integer.parseInt(inLine);
                    }
                } else {
                    return 0;
                }
            } else {
                os.writeBytes("exit\n");
            }
        } catch (IOException e) {
            Log.e(TAG, "Problems reading current version.", e);
            return 0;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return 0;
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
    
    public static boolean elitePresent(Context context, boolean versionCheck, int minVersion) {
        PackageManager pm = context.getPackageManager();
        int sigs = pm.checkSignatures("com.noshufou.android.su", "com.noshufou.android.su.elite");
        if (sigs != PackageManager.SIGNATURE_MATCH) {
            return false;
        } else {
            if (versionCheck) {
                PackageInfo pi;
                try {
                    pi = pm.getPackageInfo("com.noshufou.android.su.elite", 0);
                    Log.d(TAG, pi.applicationInfo.sourceDir);
                    if (pi.versionCode >= minVersion) {
                        return true;
                    } else {
                        return false;
                    }
                } catch (NameNotFoundException e) {
                    return false;
                }
            } else {
                return true;
            }
        }
    }
    
    public static String getHash(String pin) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            Log.e("Utils", "NoSuchAlgorithm, storing in plain text...", e);
            return pin;
        }
        digest.reset();
        try {
            byte[] input = digest.digest(pin.getBytes("UTF-8"));
            String base64 = Base64.encode(input);
            Log.d("Utils", "hashed pin = " + base64);
            return base64;
        } catch (UnsupportedEncodingException e) {
            Log.e("Utils", "UnsupportedEncoding, storing in plain text...", e);
            return pin;
        }
    }
    
    public static boolean checkPin(Context context, String pin) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String setPin = prefs.getString("pin", "");
        return getHash(pin).equals(setPin);
    }
    
    public static void toggleAppIcon(Context context, boolean newState) {
        ComponentName componentName = new ComponentName("com.noshufou.android.su",
                "com.noshufou.android.su.Su");
        context.getPackageManager().setComponentEnabledSetting(componentName, 
                newState ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED:
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
}
