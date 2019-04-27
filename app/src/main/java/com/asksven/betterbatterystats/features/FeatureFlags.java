/*
 * Copyright (C) 2011-2018 asksven
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

package com.asksven.betterbatterystats.features;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.asksven.betterbatterystats.appanalytics.Analytics;
import com.asksven.betterbatterystats.appanalytics.Events;

import net.hockeyapp.android.metrics.MetricsManager;

/**
 * Singleton provider for all the statistics
 *
 *
 * @author sven
 *
 */
public class FeatureFlags
{
    private static String TAG = "FeatureFlags";
    private static FeatureFlags singleton = null;
    private static SharedPreferences prefs;

    private FeatureFlags() {}

    public static FeatureFlags getInstance(Context ctx)
    {
        prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        if (singleton == null)
        {
            singleton = new FeatureFlags();
        }

        return singleton;
    }

    public boolean isTimeSeriesEnabled()
    {
        boolean enabled = prefs.getBoolean("flag_time_series", false);
        return enabled;
    }


}
