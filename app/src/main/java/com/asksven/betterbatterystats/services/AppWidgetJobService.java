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
package com.asksven.betterbatterystats.services;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.asksven.betterbatterystats.handlers.OnBootHandler;
import com.asksven.betterbatterystats.widgetproviders.AppWidget;
import com.asksven.betterbatterystats.widgetproviders.TextAppWidget;


/**
 * JobService to be scheduled by the JobScheduler.
 * Refresh all widgets
 */
@TargetApi(21)
public class AppWidgetJobService extends JobService
{
    private static final String TAG = "AppWidgetJobService";

    @Override
    public boolean onStartJob(JobParameters params)
    {
        Context appContext = this.getApplicationContext();

        // Build the intent to call the services
        Log.i(TAG, "starting AppWidget update job");

        Log.i(TAG, "re-schedule job");
        OnBootHandler.scheduleAppWidgetsJob(getApplicationContext()); // reschedule the job

        Log.i(TAG, "trigger responsive widget update");
        Intent intentWidget = new Intent(AppWidget.WIDGET_UPDATE);
        int idsWidget[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(appContext, AppWidget.class));
        for (int widgetId : idsWidget)
        {
            Log.i(TAG, "Responsive widget to be updated: " + widgetId);
        }

        intentWidget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, idsWidget);

        UpdateWidgetService.enqueueWork(this, intentWidget);

        Log.i(TAG, "trigger text widget update");
        Intent intentTextWidget = new Intent(AppWidget.WIDGET_UPDATE);
        int idsTextWidget[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(appContext, TextAppWidget.class));
        for (int textWidgetId : idsTextWidget)
        {
            Log.i(TAG, "Text widget to be updated: " + textWidgetId);
        }

        intentTextWidget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, idsTextWidget);

        UpdateTextWidgetService.enqueueWork(this, intentTextWidget);

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params)
    {
        return true;
    }

}
