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
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.util.Log;

import com.asksven.betterbatterystats.appanalytics.Analytics;
import com.asksven.betterbatterystats.appanalytics.Events;


/**
 * @author android
 */
public class BbsApplication extends Application
{

    private Locale localeEN = Locale.ENGLISH;
    private static String TAG = "BbsApplication";
    private static Context context;



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

        BbsApplication.context = getApplicationContext();

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

        // set a few analytics user properties
    }

    public static Context getAppContext()
    {
        return BbsApplication.context;
    }
}
