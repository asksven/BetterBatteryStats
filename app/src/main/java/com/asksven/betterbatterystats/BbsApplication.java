/*
 * Copyright (C) 2014 asksven
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
package com.asksven.betterbatterystats;

import java.util.Locale;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TextView;

import com.asksven.android.common.RootShell;
import com.asksven.betterbatterystats.appanalytics.Analytics;
import com.asksven.betterbatterystats.appanalytics.Events;


/**
 * @author android
 */
public class BbsApplication extends Application
{

    private Locale localeEN = Locale.ENGLISH;
    private static String TAG = "BbsApplication";




    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        boolean forceEN = settings.getBoolean("force_en", false);

        Configuration config = getBaseContext().getResources().getConfiguration();

        Locale appLocale = null;
        if (forceEN)
        {
            appLocale = localeEN;
        } else
        {
            appLocale = getResources().getConfiguration().locale;
        }
        Locale.setDefault(appLocale);
        config.locale = appLocale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        boolean forceEN = settings.getBoolean("force_en", false);

        boolean showBars = settings.getBoolean("show_gauge", false);
        if (showBars)
        {
            Analytics.getInstance(this).trackEvent(Events.EVENT_LAUNCH_LINEAR_GAUGES);
        }
        else
        {
            Analytics.getInstance(this).trackEvent(Events.EVENT_LAUNCH_ROUND_GAUGES);
        }

        Configuration config = getBaseContext().getResources().getConfiguration();

        Locale appLocale = null;
        if (forceEN)
        {
            appLocale = localeEN;
        } else
        {
            appLocale = getResources().getConfiguration().locale;
        }
        Locale.setDefault(appLocale);
        config.locale = appLocale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());

        // set a few analytics user properties

        // device rooted
        Analytics.getInstance(this).setRootedDevice(RootShell.getInstance().hasRootPermissions());

        try
        {
            PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pinfo.versionName;
            Analytics.getInstance(this).setVersion(version);

            String edition = "";

            if (pinfo.packageName.endsWith("_xdaedition"))
            {
                edition = "xda edition";
            } else
            {
                edition = "google play edition";
            }

            Analytics.getInstance(this).setEdition(edition);

        } catch (Exception e)
        {
            Log.e(TAG, "An error occured retrieveing the version info: " + e.getMessage());

        }
    }
}
