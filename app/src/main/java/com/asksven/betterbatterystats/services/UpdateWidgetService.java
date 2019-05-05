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

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.JobIntentService;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.RemoteViews;

import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.android.common.privateapiproxies.Misc;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.android.common.utils.DateUtils;
import com.asksven.android.common.utils.StringUtils;
import com.asksven.betterbatterystats.LogSettings;
import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.StatsActivity;
import com.asksven.betterbatterystats.data.Reference;
import com.asksven.betterbatterystats.data.ReferenceStore;
import com.asksven.betterbatterystats.data.StatsProvider;
import com.asksven.betterbatterystats.widgetproviders.AppWidget;
import com.asksven.betterbatterystats.widgets.WidgetSummary;

import java.util.ArrayList;

/**
 * @author sven
 *
 */
public class UpdateWidgetService extends JobIntentService
{
	private static final String TAG = "UpdateWidgetService";
	/** must be unique for each widget */
	private static final int PI_CODE = 1;

    static final int JOB_ID = 1000;

    /**
     * Convenience method for enqueuing work in to this service.
     * see https://stackoverflow.com/questions/46445265/android-8-0-java-lang-illegalstateexception-not-allowed-to-start-service-inten
     */
    public static void enqueueWork(Context context, Intent work)
    {
        enqueueWork(context, UpdateWidgetService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(Intent intent)
    {
        // We have received work to do.  The system or framework is already
        // holding a wake lock for us at this point, so we can just go.
        Log.i(TAG, "onHandleWork: " + intent);
        if (LogSettings.DEBUG)
        {
            Log.d(TAG, "Service started");
        }
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this
                .getApplicationContext());

        int[] allWidgetIds = intent
                .getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);

        StatsProvider stats = StatsProvider.getInstance();
        // make sure to flush cache
        BatteryStatsProxy proxy = BatteryStatsProxy.getInstance(this);
        if ( proxy != null)
        {
            proxy.invalidate();
        }

        if (allWidgetIds != null)
        {
            if (allWidgetIds.length == 0)
            {
                Log.i(TAG, "allWidgetIds was empty");

            }

            for (int widgetId : allWidgetIds)
            {

                Log.i(TAG, "Update widget " + widgetId);
                RemoteViews remoteViews = new RemoteViews(this
                        .getApplicationContext().getPackageName(),
                        R.layout.widget);

                final int cellSize = 40;
                int width = 3;
                int height = 2;
                int widthDim = 0;
                int heightDim = 0;

                if (Build.VERSION.SDK_INT >= 16)
                {
                    Bundle widgetOptions = appWidgetManager.getAppWidgetOptions(widgetId);
                    width = AppWidget.sizeToCells(widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) - 10);
                    height = AppWidget.sizeToCells(widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) + 10);
                    widthDim = widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
                    heightDim = widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);

                    Log.i(TAG, "[" + widgetId + "] height=" + height + " (" + widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) + ")");
                    Log.i(TAG, "[" + widgetId + "] width=" + width + "(" + widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) + ")");
                    remoteViews = new RemoteViews(this.getPackageName(), R.layout.widget_horz);
                    if ((height <= 2) && (width <= 2))
                    {
                        // switch to image only
                        Log.i(TAG, "[" + widgetId + "] using image-only layout");
                        remoteViews = new RemoteViews(this.getPackageName(), R.layout.widget);
                    } else if (height < width)
                    {
                        // switch to horizontal
                        Log.i(TAG, "[" + widgetId + "] using horizontal layout");
                        remoteViews = new RemoteViews(this.getPackageName(), R.layout.widget_horz);
                    } else
                    {
                        // switch to vertical
                        Log.i(TAG, "[" + widgetId + "] using vertical layout");
                        remoteViews = new RemoteViews(this.getPackageName(), R.layout.widget_vert);
                    }
                }


                // we change the bg color of the layout based on alpha from prefs
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
                int opacity = sharedPrefs.getInt("new_widget_bg_opacity", 20);
                opacity = (255 * opacity) / 100;
                remoteViews.setInt(R.id.background, "setBackgroundColor", (opacity << 24) & android.graphics.Color.BLACK);
                //remoteViews.setInt(R.id.layoutBackground, "setImageAlpha", opacity);

                long timeAwake = 0;
                long timeSince = 0;
                long timeScreenOn = 0;
                long timeDeepSleep = 0;
                long timePWL = 0;
                long timeKWL = 0;

                String refFrom = sharedPrefs.getString("new_widget_default_stat_type", Reference.UNPLUGGED_REF_FILENAME);
                try
                {
                    // retrieve stats
                    Reference currentRef = StatsProvider.getInstance().getUncachedPartialReference(0);
                    Reference fromRef = ReferenceStore.getReferenceByName(refFrom, this);

                    ArrayList<StatElement> otherStats = stats.getOtherUsageStatList(true, fromRef, false, true, currentRef);

                    if ((otherStats == null) || (otherStats.size() == 1))
                    {
                        // the desired stat type is unavailable, pick the alternate one and go on with that one
                        refFrom = sharedPrefs.getString("widget_fallback_stat_type", Reference.UNPLUGGED_REF_FILENAME);
                        fromRef = ReferenceStore.getReferenceByName(refFrom, this);
                        otherStats = stats.getOtherUsageStatList(true, fromRef, false, true, currentRef);
                    }

                    timeSince = StatsProvider.getInstance().getSince(fromRef, currentRef);

                    if ((otherStats != null) && (otherStats.size() > 1))
                    {

                        Misc timeAwakeStat = (Misc) stats.getElementByKey(otherStats, StatsProvider.LABEL_MISC_AWAKE);
                        if (timeAwakeStat != null)
                        {
                            timeAwake = timeAwakeStat.getTimeOn();
                        } else
                        {
                            timeAwake = 0;
                        }

                        Misc timeScreenOnStat = (Misc) stats.getElementByKey(otherStats, "Screen On");
                        if (timeScreenOnStat != null)
                        {
                            timeScreenOn = timeScreenOnStat.getTimeOn();
                        } else
                        {
                            timeScreenOn = 0;
                        }

                        Misc deepSleepStat = ((Misc) stats.getElementByKey(otherStats, "Deep Sleep"));
                        if (deepSleepStat != null)
                        {
                            timeDeepSleep = deepSleepStat.getTimeOn();
                        } else
                        {
                            timeDeepSleep = 0;
                        }

                        ArrayList<StatElement> pWakelockStats = stats.getWakelockStatList(true, fromRef, 0, 0, currentRef);
                        timePWL = stats.sum(pWakelockStats);

                        ArrayList<StatElement> kWakelockStats = stats.getKernelWakelockStatList(true, fromRef, 0, 0, currentRef);
                        timeKWL = stats.sum(kWakelockStats);
                    }
                    else
                    {
                    }
                } catch (Exception e)
                {
                    Log.e(TAG, "Exception: " + Log.getStackTraceString(e));
                } finally
                {
                    if (LogSettings.DEBUG)
                    {
                        Log.d(TAG, "Reference: " + refFrom);
                        Log.d(TAG, "Since: " + DateUtils.formatShort(timeSince) + " " + AppWidget.formatDuration(timeSince) + " " + timeSince);
                        Log.d(TAG, "Awake: " + DateUtils.formatShort(timeAwake) + " " + AppWidget.formatDuration(timeAwake) + " " + timeAwake);
                        Log.d(TAG, "Screen on: " + DateUtils.formatShort(timeScreenOn) + " " + AppWidget.formatDuration(timeScreenOn) + " " + timeScreenOn);
                        Log.d(TAG, "Deep sleep: " + DateUtils.formatShort(timeDeepSleep) + " " + AppWidget.formatDuration(timeDeepSleep) + " " + timeDeepSleep);
                        Log.d(TAG, "KWL: " + DateUtils.formatShort(timeKWL) + " " + AppWidget.formatDuration(timeKWL) + " " + timeKWL);
                        Log.d(TAG, "PWL: " + DateUtils.formatShort(timePWL) + " " + AppWidget.formatDuration(timePWL) + " " + timePWL);
                    }
                    WidgetSummary graph = new WidgetSummary();

                    graph.setAwake(timeAwake);
                    graph.setScreenOn(timeScreenOn);
                    graph.setDeepSleep(timeDeepSleep);
                    graph.setDuration(timeSince);
                    graph.setKWL(timeKWL);
                    graph.setPWL(timePWL);


                    DisplayMetrics metrics = this.getResources().getDisplayMetrics();
                    Log.i(TAG, "Widget Dimensions: height=" + heightDim + " width=" + widthDim);
                    Float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, Math.min(Math.max(Math.min(widthDim, heightDim), 80), 160), metrics);
                    Log.i(TAG, "BitmapDip=" + Math.min(Math.max(Math.min(widthDim, heightDim), 80), 160) + ", BitmapPx=" + px.intValue());
                    graph.setBitmapSizePx(px.intValue());

                    remoteViews.setImageViewBitmap(R.id.imageView1, graph.getBitmap(this));

                    boolean show_pwc_only = sharedPrefs.getBoolean("widget_show_pct", false);
                    if (show_pwc_only)
                    {
                        UpdateWidgetService.setValuesToPct(remoteViews, timeAwake, timeSince, timeScreenOn, timeDeepSleep, timePWL, timeKWL);
                    }
                    else
                    {
                        if ((width > height) && (width <= 4))
                        {

                            UpdateWidgetService.setValuesToDuration(remoteViews, timeAwake, timeSince, timeScreenOn, timeDeepSleep, timePWL, timeKWL);
                        }
                        else
                        {

                            UpdateWidgetService.setValuesToDurationAndPct(remoteViews, timeAwake, timeSince, timeScreenOn, timeDeepSleep, timePWL, timeKWL);
                        }
                    }

                    boolean showColor = sharedPrefs.getBoolean("text_widget_color", true);
                    UpdateWidgetService.setTextColor(remoteViews, showColor, this);

                    // tap zones

                    // Register an onClickListener for the graph -> refresh
                    Intent clickIntentRefresh = new Intent(this.getApplicationContext(),
                            AppWidget.class);

                    clickIntentRefresh.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    clickIntentRefresh.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
                            allWidgetIds);

                    PendingIntent pendingIntentRefresh = PendingIntent.getBroadcast(
                            getApplicationContext(), 0, clickIntentRefresh,
                            PendingIntent.FLAG_UPDATE_CURRENT);
                    remoteViews.setOnClickPendingIntent(R.id.imageViewRefresh, pendingIntentRefresh);

                    // Register an onClickListener for the widget -> call main activity
                    Intent i = new Intent(Intent.ACTION_MAIN);
                    PackageManager manager = getPackageManager();
                    i = manager.getLaunchIntentForPackage(getPackageName());
                    i.addCategory(Intent.CATEGORY_LAUNCHER);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    int stat = Integer.valueOf(sharedPrefs.getString("widget_default_stat", "0"));
                    i.putExtra(StatsActivity.STAT, stat);
                    i.putExtra(StatsActivity.STAT_TYPE_FROM, refFrom);
                    i.putExtra(StatsActivity.STAT_TYPE_TO, Reference.CURRENT_REF_FILENAME);

                    PendingIntent clickPI = PendingIntent.getActivity(
                            this.getApplicationContext(), PI_CODE,
                            i, PendingIntent.FLAG_UPDATE_CURRENT);
                    remoteViews.setOnClickPendingIntent(R.id.imageView1, clickPI);

                    appWidgetManager.updateAppWidget(widgetId, remoteViews);
                }
            }
        }
        else
        {
            Log.i(TAG, "allWidgetIds was null");
        }
        Log.i(TAG, "Completed service @ " + DateUtils.formatDurationLong(SystemClock.elapsedRealtime()));
    }

    static void setValuesAlignmentLeft(RemoteViews remoteViews, boolean leftGravity)
    {
        if (leftGravity)
        {
            remoteViews.setInt(R.id.layoutLegendValues, "setGravity", Gravity.LEFT);
        }
        else
        {
            remoteViews.setInt(R.id.layoutLegendValues, "setGravity", Gravity.RIGHT);
        }
    }

    static void setValuesToDuration(RemoteViews remoteViews, long timeAwake, long timeSince,long  timeScreenOn, long timeDeepSleep, long timePWL, long timeKWL)
    {
        remoteViews.setTextViewText(R.id.textViewAwakeVal, AppWidget.formatDuration(timeAwake - timeScreenOn));
        remoteViews.setTextViewText(R.id.textViewDeepSleepVal, AppWidget.formatDuration(timeDeepSleep));
        remoteViews.setTextViewText(R.id.textViewScreenOnVal, AppWidget.formatDuration(timeScreenOn));
        remoteViews.setTextViewText(R.id.textViewKWLVal, AppWidget.formatDuration(timeKWL));
        remoteViews.setTextViewText(R.id.textViewPWLVal, AppWidget.formatDuration(timePWL));

        UpdateWidgetService.setValuesAlignmentLeft(remoteViews, false);

    }

    static void setValuesToDurationAndPct(RemoteViews remoteViews, long timeAwake, long timeSince,long  timeScreenOn, long timeDeepSleep, long timePWL, long timeKWL)
    {
        remoteViews.setTextViewText(R.id.textViewAwakeVal, AppWidget.formatDuration(timeAwake - timeScreenOn)
                + " (" + StringUtils.formatRatio(timeAwake - timeScreenOn, timeSince) + ")");
        remoteViews.setTextViewText(R.id.textViewDeepSleepVal, AppWidget.formatDuration(timeDeepSleep)
                + " (" + StringUtils.formatRatio(timeDeepSleep, timeSince) + ")");
        remoteViews.setTextViewText(R.id.textViewScreenOnVal, AppWidget.formatDuration(timeScreenOn)
                + " (" + StringUtils.formatRatio(timeScreenOn, timeSince) + ")");
        remoteViews.setTextViewText(R.id.textViewKWLVal, AppWidget.formatDuration(timeKWL)
                + " (" + StringUtils.formatRatio(timeKWL, timeSince) + ")");
        remoteViews.setTextViewText(R.id.textViewPWLVal, AppWidget.formatDuration(timePWL)
                + " (" + StringUtils.formatRatio(timePWL, timeSince) + ")");
        UpdateWidgetService.setValuesAlignmentLeft(remoteViews, false);
    }

    static void setValuesToPct(RemoteViews remoteViews, long timeAwake, long timeSince,long  timeScreenOn, long timeDeepSleep, long timePWL, long timeKWL)
    {
        remoteViews.setTextViewText(R.id.textViewAwakeVal, StringUtils.formatRatio(timeAwake - timeScreenOn, timeSince));
        remoteViews.setTextViewText(R.id.textViewDeepSleepVal, StringUtils.formatRatio(timeDeepSleep, timeSince));
        remoteViews.setTextViewText(R.id.textViewScreenOnVal, StringUtils.formatRatio(timeScreenOn, timeSince));
        remoteViews.setTextViewText(R.id.textViewKWLVal, StringUtils.formatRatio(timeKWL, timeSince));
        remoteViews.setTextViewText(R.id.textViewPWLVal, StringUtils.formatRatio(timePWL, timeSince));

        UpdateWidgetService.setValuesAlignmentLeft(remoteViews, false);
    }

    public static void setTextColor(RemoteViews remoteViews, boolean color, Context context)
    {
        if (!color)
        {
            Log.i(TAG, "removing text color");
            remoteViews.setTextColor(R.id.textViewAwake, context.getResources().getColor(R.color.primary_text_default_material_dark));
            remoteViews.setTextColor(R.id.textViewDeepSleep, context.getResources().getColor(R.color.primary_text_default_material_dark));
            remoteViews.setTextColor(R.id.textViewScreenOn, context.getResources().getColor(R.color.primary_text_default_material_dark));
            remoteViews.setTextColor(R.id.textViewKWL, context.getResources().getColor(R.color.primary_text_default_material_dark));
            remoteViews.setTextColor(R.id.textViewPWL, context.getResources().getColor(R.color.primary_text_default_material_dark));
        }
        else
        {
            Log.i(TAG, "adding text color");
            remoteViews.setTextColor(R.id.textViewAwake, context.getResources().getColor(R.color.awake));
            remoteViews.setTextColor(R.id.textViewDeepSleep, context.getResources().getColor(R.color.deep_sleep));
            remoteViews.setTextColor(R.id.textViewScreenOn, context.getResources().getColor(R.color.screen_on));
            remoteViews.setTextColor(R.id.textViewKWL, context.getResources().getColor(R.color.kwl));
            remoteViews.setTextColor(R.id.textViewPWL, context.getResources().getColor(R.color.pwl));

        }

    }

    public static void setShortLabels(RemoteViews remoteViews, Context context, boolean shortLabels)
    {
        if (shortLabels)
        {
            remoteViews.setTextViewText(R.id.textViewAwake, context.getResources().getString(R.string.label_widget_awake_short));
            remoteViews.setTextViewText(R.id.textViewDeepSleep, context.getResources().getString(R.string.label_widget_deep_sleep_short));
            remoteViews.setTextViewText(R.id.textViewScreenOn, context.getResources().getString(R.string.label_widget_screen_on_short));
            remoteViews.setTextViewText(R.id.textViewKWL, context.getResources().getString(R.string.label_widget_kernel_wakelock_short));
            remoteViews.setTextViewText(R.id.textViewPWL, context.getResources().getString(R.string.label_widget_partial_wakelock_short));
        }
        else
        {
            remoteViews.setTextViewText(R.id.textViewAwake, context.getResources().getString(R.string.label_widget_awake));
            remoteViews.setTextViewText(R.id.textViewDeepSleep, context.getResources().getString(R.string.label_widget_deep_sleep));
            remoteViews.setTextViewText(R.id.textViewScreenOn, context.getResources().getString(R.string.label_widget_screen_on));
            remoteViews.setTextViewText(R.id.textViewKWL, context.getResources().getString(R.string.label_widget_kernel_wakelock));
            remoteViews.setTextViewText(R.id.textViewPWL, context.getResources().getString(R.string.label_widget_partial_wakelock));
        }

    }


}