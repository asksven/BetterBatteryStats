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
import android.content.Intent;
import android.util.Log;

import com.asksven.betterbatterystats.LogSettings;
import com.asksven.betterbatterystats.StatsActivity;
import com.asksven.betterbatterystats.handlers.OnBootHandler;
import com.asksven.betterbatterystats.widgetproviders.BbsWidgetProvider;


/**
 * JobService to be scheduled by the JobScheduler.
 * Refresh all widgets
 */
@TargetApi(23)
public class AppWidgetJobService extends JobService
{
    private static final String TAG = "AppWidgetJobService";

    @Override
    public boolean onStartJob(JobParameters params)
    {
        // we will refresh all widget types
//        Intent serviceLargeWidget = new Intent(getApplicationContext(), UpdateLargeWidgetService.class);
//        Intent serviceMediumWidget = new Intent(getApplicationContext(), UpdateMediumWidgetService.class);
//        Intent serviceSmallWidget = new Intent(getApplicationContext(), UpdateSmallWidgetService.class);
//        Intent serviceWidget = new Intent(getApplicationContext(), UpdateWidgetService.class);

//        getApplicationContext().startService(serviceLargeWidget);
//        getApplicationContext().startService(serviceMediumWidget);
//        getApplicationContext().startService(serviceSmallWidget);
//        getApplicationContext().startService(serviceWidget);


        // Build the intent to call the services
        Log.i(TAG, "starting AppWidget update job");

        Intent intentWidget = new Intent(BbsWidgetProvider.WIDGET_UPDATE);
        int idsWidget[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(this, UpdateWidgetService.class));
        intentWidget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, idsWidget);

        UpdateWidgetService.enqueueWork(this, intentWidget);

        Intent intentSmallWidget = new Intent(BbsWidgetProvider.WIDGET_UPDATE);
        int idsSmallWidget[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(this, UpdateSmallWidgetService.class));
        intentSmallWidget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, idsSmallWidget);

        UpdateWidgetService.enqueueWork(this, intentSmallWidget);

        Intent intentMediumWidget = new Intent(BbsWidgetProvider.WIDGET_UPDATE);
        int idsMediumWidget[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(this, UpdateMediumWidgetService.class));
        intentMediumWidget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, idsMediumWidget);

        UpdateWidgetService.enqueueWork(this, intentMediumWidget);

        Intent intentLargeWidget = new Intent(BbsWidgetProvider.WIDGET_UPDATE);
        int idsLargeWidget[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(this, UpdateLargeWidgetService.class));
        intentMediumWidget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, idsLargeWidget);

        UpdateWidgetService.enqueueWork(this, intentMediumWidget);

        OnBootHandler.scheduleAppWidgetsJob(getApplicationContext()); // reschedule the job
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params)
    {
        return true;
    }

}
