/*
 * Copyright (C) 2011 asksven
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
package com.asksven.betterbatterystats.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.asksven.android.common.utils.DateUtils;
import com.asksven.betterbatterystats.R;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;

/**
 * @author sven
 *
 */
public class GoogleAnalytics
{
	private static GoogleAnalytics m_singleton = null;
	private static GoogleAnalyticsTracker m_tracker = null;
	private static boolean m_bActive = false;
	
	public static final String ACTIVITY_ABOUT = "/AboutActivity";
	public static final String ACTIVITY_ALARMS = "/AlarmsActivity";
	public static final String ACTIVITY_BATTERY_GRAPH = "/BatteryGraphActivity";
	public static final String ACTIVITY_BATTERY_GRAPH2 = "/BatteryGraph2Activity";
	public static final String ACTIVITY_BATTERY_GRAPH_SERIES = "/BatteryGraphSeriesActivity";
	public static final String ACTIVITY_HELP = "/HelpActivity";
	public static final String ACTIVITY_HOWTO = "/HowtoActivity";
	public static final String ACTIVITY_README = "/ReadmeActivity";
	public static final String ACTIVITY_HIST = "/HistActivity";
	public static final String ACTIVITY_PREFERENCES = "/PreferencesActivity";
	public static final String ACTIVITY_STATS = "/StatsActivity";
	
	public static final String ACTION_DUMP = "/ActionDump";
	public static final String ACTION_SET_CUSTOM_REF = "/ActionSetCustmRef";
	

	private GoogleAnalytics()
	{
		
	}
	
	public static GoogleAnalytics getInstance(Context context)
	{
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		m_bActive = sharedPrefs.getBoolean("use_analytics", true);

		if (m_singleton == null)
		{
			m_singleton = new GoogleAnalytics();
			if (m_bActive)
			{
				m_tracker = GoogleAnalyticsTracker.getInstance();

				// Start the tracker in manual dispatch mode...
				m_tracker.startNewSession("TRACKING_CODE_HERE", 20, context);
			}
		    
		}
		return m_singleton;
	}
	
	public void trackPage(String page)
	{
		if ((m_tracker != null) && m_bActive )
		{
			m_tracker.trackPageView(page);
		}
	}

	public void trackStats(String page, int stat, int statType, int sort)
	{
		if ((m_tracker != null) && m_bActive )
		{
			m_tracker.trackPageView(page + "Stat" + stat + "Type" + statType + "Sort" + sort);
		}
	}

}
