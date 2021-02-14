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

//import net.hockeyapp.android.metrics.MetricsManager;

public class Analytics
{
    private static String TAG = "Analytics";
    private static Analytics singleton = null;
    private static boolean disableAnalytics = false;


    private Analytics() {}

    public static Analytics getInstance(Context ctx)
    {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        disableAnalytics = !sharedPrefs.getBoolean("analytics", true);

        if (singleton == null)
        {
            singleton = new Analytics();
        }

        return singleton;
    }

    public boolean isEnabled()
    {
        return !disableAnalytics;
    }

    public void trackActivity(Activity activity, String name)
    {
//        if (!disableAnalytics) {
//            Log.i(TAG, "Tracked Activity " + activity.getClass().getSimpleName() + " with name " + name);
//            MetricsManager.trackEvent("activity_launched_" + activity.getClass().getSimpleName());
//        }
    }

    public void setRootedDevice(boolean rooted)
    {
//        if (!disableAnalytics)
//        {
//            MetricsManager.trackEvent((rooted) ? Events.EVENT_LAUNCH_ROOTED : Events.EVENT_LAUNCH_UNROOTED);
//        }
    }

    public void setVersion(String value)
    {
        if (!disableAnalytics) {
//            mFirebaseAnalytics.setUserProperty("version", value);
        }
    }

    public void setEdition(String value)
    {
//        if (!disableAnalytics) {
//            MetricsManager.trackEvent((value.equals("xda edition")) ? Events.EVENT_LAUNCH_XDA : Events.EVENT_LAUNCH_GPLAY);
//        }
    }

    public void trackEvent(String value)
    {
//        if (!disableAnalytics) {
//            MetricsManager.trackEvent(value);
//        }
    }

}
