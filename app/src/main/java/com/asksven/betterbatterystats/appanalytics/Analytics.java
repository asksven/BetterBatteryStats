/*
 * Copyright (C) 2017 asksven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.asksven.betterbatterystats.appanalytics;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.data.Reference;
//import com.google.firebase.analytics.FirebaseAnalytics;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;
import net.hockeyapp.android.metrics.MetricsManager;

public class Analytics
{
    private static String TAG = "Analytics";
    private static Analytics mSingleton = null;
    private static boolean mDisableAnalytics = false;
//    private FirebaseAnalytics mFirebaseAnalytics = null;


    private Analytics() {}

    public static Analytics getInstance(Context ctx)
    {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        mDisableAnalytics = !sharedPrefs.getBoolean("analytics", true);

        if (mSingleton == null)
        {
            mSingleton = new Analytics();
            if (!mDisableAnalytics) {
//                mSingleton.mFirebaseAnalytics = FirebaseAnalytics.getInstance(ctx);
            }

        }

        return mSingleton;
    }


    public void trackActivity(Activity activity, String name)
    {
        if (!mDisableAnalytics) {
            Log.i(TAG, "Tracked Activity " + activity.getClass().getSimpleName() + " with name " + name);
//            mFirebaseAnalytics.setCurrentScreen(activity, name, null);
            MetricsManager.trackEvent("activity_launched_" + activity.getClass().getSimpleName());
        }
    }

    public void setRootedDevice(boolean rooted)
    {
        if (!mDisableAnalytics)
        {
//            mFirebaseAnalytics.setUserProperty("rooted", (rooted) ? "true" : "false");
            MetricsManager.trackEvent((rooted) ? Events.EVENT_LAUNCH_ROOTED : Events.EVENT_LAUNCH_UNROOTED);
        }
    }

    public void setVersion(String value)
    {
        if (!mDisableAnalytics) {
//            mFirebaseAnalytics.setUserProperty("version", value);
        }
    }

    public void setEdition(String value)
    {
        if (!mDisableAnalytics) {
//            mFirebaseAnalytics.setUserProperty("edition", value);
            MetricsManager.trackEvent((value.equals("xda edition")) ? Events.EVENT_LAUNCH_XDA : Events.EVENT_LAUNCH_GPLAY);
        }
    }

    public void trackEvent(String value)
    {
        if (!mDisableAnalytics) {
            MetricsManager.trackEvent(value);
        }
    }

    /*
    Bundle params = new Bundle();
            params.putString("image_name", name);
            params.putString("full_text", text);
            mFirebaseAnalytics.logEvent("share_image", params);
    */
}
