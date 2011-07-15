// @todo: remove / relocate
///*
// * Copyright (C) 2011 asksven
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package com.asksven.betterbatterystats;
//
///**
// * @author sven
// *
// */
//
//import android.content.Context;
//import android.content.pm.ApplicationInfo;
//import android.content.pm.PackageInfo;
//import android.content.pm.PackageManager;
//import android.content.pm.PackageManager.NameNotFoundException;
//import android.graphics.drawable.Drawable;
//import android.os.Handler;
//import com.asksven.android.common.privateapiproxies.BatteryStatsTypes.Uid;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//
//class BatterySipper implements Comparable<BatterySipper> {
//	
//    enum DrainType {
//        IDLE,
//        CELL,
//        PHONE,
//        WIFI,
//        BLUETOOTH,
//        SCREEN,
//        APP
//    }
//    
//    final Context mContext;
//    final HashMap<String,UidToDetail> mUidCache = new HashMap<String,UidToDetail>();
//    final ArrayList<BatterySipper> mRequestQueue;
//    final Handler mHandler;
//    String name;
//    Drawable icon;
//    int iconId; // For passing to the detail screen.
//    Uid uidObj;
//    double value;
//    double[] values;
//    DrainType drainType;
//    long usageTime;
//    long cpuTime;
//    long gpsTime;
//    long wifiRunningTime;
//    long cpuFgTime;
//    long wakeLockTime;
//    long tcpBytesReceived;
//    long tcpBytesSent;
//    double percent;
//    double noCoveragePercent;
//    String defaultPackageName;
//
//    static class UidToDetail {
//        String name;
//        String packageName;
//        Drawable icon;
//    }
//
//    BatterySipper(Context context, ArrayList<BatterySipper> requestQueue,
//            Handler handler, String label, DrainType drainType,
//            int iconId, Uid uid, double[] values) {
//        mContext = context;
//        mRequestQueue = requestQueue;
//        mHandler = handler;
//        this.values = values;
//        name = label;
//        this.drainType = drainType;
//        if (iconId > 0) {
//            icon = mContext.getResources().getDrawable(iconId);
//        }
//        if (values != null) value = values[0];
//        if ((label == null || iconId == 0) && uid != null) {
//            getQuickNameIconForUid(uid);
//        }
//        uidObj = uid;
//    }
//
//    double getSortValue() {
//        return value;
//    }
//
//    double[] getValues() {
//        return values;
//    }
//
//    Drawable getIcon() {
//        return icon;
//    }
//
//    public int compareTo(BatterySipper other) {
//        // Return the flipped value because we want the items in descending order
//        return (int) (other.getSortValue() - getSortValue());
//    }
//
//    void getQuickNameIconForUid(Uid uidObj) {
//        final int uid = uidObj.getUid();
//        final String uidString = Integer.toString(uid);
//        if (mUidCache.containsKey(uidString)) {
//            UidToDetail utd = mUidCache.get(uidString);
//            defaultPackageName = utd.packageName;
//            name = utd.name;
//            icon = utd.icon;
//            return;
//        }
//        PackageManager pm = mContext.getPackageManager();
//        final Drawable defaultActivityIcon = pm.getDefaultActivityIcon();
//        String[] packages = pm.getPackagesForUid(uid);
//        icon = pm.getDefaultActivityIcon();
//        if (packages == null) {
//            //name = Integer.toString(uid);
//            if (uid == 0) {
//                name = mContext.getResources().getString(R.string.process_kernel_label);
//            } else if ("mediaserver".equals(name)) {
//                name = mContext.getResources().getString(R.string.process_mediaserver_label);
//            }
//            return;
//        } else {
//            //name = packages[0];
//        }
//        synchronized (mRequestQueue) {
//            mRequestQueue.add(this);
//        }
//    }
//
//    /**
//     * Sets name and icon
//     * @param uid Uid of the application
//     */
//    void getNameIcon() {
//        PackageManager pm = mContext.getPackageManager();
//        final int uid = uidObj.getUid();
//        final Drawable defaultActivityIcon = pm.getDefaultActivityIcon();
//        String[] packages = pm.getPackagesForUid(uid);
//        if (packages == null) {
//            name = Integer.toString(uid);
//            return;
//        }
//
//        String[] packageLabels = new String[packages.length];
//        System.arraycopy(packages, 0, packageLabels, 0, packages.length);
//
//        int preferredIndex = -1;
//        // Convert package names to user-facing labels where possible
//        for (int i = 0; i < packageLabels.length; i++) {
//            // Check if package matches preferred package
//            if (packageLabels[i].equals(name)) preferredIndex = i;
//            try {
//                ApplicationInfo ai = pm.getApplicationInfo(packageLabels[i], 0);
//                CharSequence label = ai.loadLabel(pm);
//                if (label != null) {
//                    packageLabels[i] = label.toString();
//                }
//                if (ai.icon != 0) {
//                    defaultPackageName = packages[i];
//                    icon = ai.loadIcon(pm);
//                    break;
//                }
//            } catch (NameNotFoundException e) {
//            }
//        }
//        if (icon == null) icon = defaultActivityIcon;
//
//        if (packageLabels.length == 1) {
//            name = packageLabels[0];
//        } else {
//            // Look for an official name for this UID.
//            for (String pkgName : packages) {
//                try {
//                    final PackageInfo pi = pm.getPackageInfo(pkgName, 0);
//                    if (pi.sharedUserLabel != 0) {
//                        final CharSequence nm = pm.getText(pkgName,
//                                pi.sharedUserLabel, pi.applicationInfo);
//                        if (nm != null) {
//                            name = nm.toString();
//                            if (pi.applicationInfo.icon != 0) {
//                                defaultPackageName = pkgName;
//                                icon = pi.applicationInfo.loadIcon(pm);
//                            }
//                            break;
//                        }
//                    }
//                } catch (PackageManager.NameNotFoundException e) {
//                }
//            }
//        }
//        final String uidString = Integer.toString(uidObj.getUid());
//        UidToDetail utd = new UidToDetail();
//        utd.name = name;
//        utd.icon = icon;
//        utd.packageName = defaultPackageName;
//        mUidCache.put(uidString, utd);
//
//    }
//}