/*

 * Copyright (C) 2012 asksven
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
import com.google.firebase.analytics.FirebaseAnalytics;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;
import net.hockeyapp.android.metrics.MetricsManager;

public class Analytics
{
    private static String TAG = "Analytics";
    private static Analytics mSingleton = null;
    private static boolean mDisableFirebase = false;
    private FirebaseAnalytics mFirebaseAnalytics = null;


    private Analytics() {}

    public static Analytics getInstance(Context ctx)
    {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        mDisableFirebase = !sharedPrefs.getBoolean("analytics", true);

        if (mSingleton == null)
        {
            mSingleton = new Analytics();
            if (!mDisableFirebase) {
                mSingleton.mFirebaseAnalytics = FirebaseAnalytics.getInstance(ctx);
            }

        }

        return mSingleton;
    }


    public void trackActivity(Activity activity, String name)
    {
        if (!mDisableFirebase) {
            Log.i(TAG, "Tracked Activity " + activity.getClass().getSimpleName() + " with name " + name);
            mFirebaseAnalytics.setCurrentScreen(activity, name, null);
        }
    }

    public void setRootedDevice(boolean rooted)
    {
        if (!mDisableFirebase) {
            mFirebaseAnalytics.setUserProperty("rooted", (rooted) ? "true" : "false");
        }
    }

    public void setVersion(String value)
    {
        if (!mDisableFirebase) {
            mFirebaseAnalytics.setUserProperty("version", value);
        }
    }

    public void setEdition(String value)
    {
        if (!mDisableFirebase) {
            mFirebaseAnalytics.setUserProperty("edition", value);
        }
    }

    /*
    Bundle params = new Bundle();
            params.putString("image_name", name);
            params.putString("full_text", text);
            mFirebaseAnalytics.logEvent("share_image", params);
    */
}
