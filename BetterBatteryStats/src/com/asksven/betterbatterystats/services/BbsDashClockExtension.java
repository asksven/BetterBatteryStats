/*
 * Copyright (C) 2011-2013 asksven
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

import java.util.ArrayList;

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
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class BbsDashClockExtension extends DashClockExtension
{
	private static final String TAG = "BbsDashClockExtension";

	public static final String PREF_NAME = "pref_name";

	@Override
	protected void onUpdateData(int reason)
	{
		// Get preference value.
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		// we want to refresh out stats each time the screen goes on
		setUpdateWhenScreenOn(true);
		
		// collect some data
		String refFrom	= sharedPrefs.getString("dashclock_default_stat_type", Reference.UNPLUGGED_REF_FILENAME);

		long timeAwake 		= 0;
		long timeSince 		= 0;
		long drain			= 0;
		
		String strAwake = "";
		String strDrain = "";
		
		StatsProvider stats = StatsProvider.getInstance(this);
		// make sure to flush cache
		BatteryStatsProxy.getInstance(this).invalidate();

		try
		{
			
			Reference toRef = StatsProvider.getInstance(this).getUncachedPartialReference(0);
			Reference fromRef = ReferenceStore.getReferenceByName(refFrom, this);

			ArrayList<StatElement> otherStats = stats.getOtherUsageStatList(true, fromRef, false, true, toRef);
			timeSince = stats.getSince(fromRef, toRef);
			drain = stats.getBatteryLevelStat(fromRef, toRef);
			
			if ( (otherStats == null) || ( otherStats.size() == 1) )
			{
				// the desired stat type is unavailable, pick the alternate one and go on with that one
				refFrom	= sharedPrefs.getString("dashclock_fallback_stat_type", Reference.BOOT_REF_FILENAME);
				fromRef = ReferenceStore.getReferenceByName(refFrom, this);
				otherStats = stats.getOtherUsageStatList(true, fromRef, false, true, toRef);
			}

			if ( (otherStats != null) && ( otherStats.size() > 1) )
			{
				Misc timeAwakeStat = (Misc) stats.getElementByKey(otherStats, StatsProvider.LABEL_MISC_AWAKE);
				if (timeAwakeStat != null)
				{
					timeAwake = timeAwakeStat.getTimeOn();
				}
				else
				{
					timeAwake = 0;
				}				
			}
		}
		catch (Exception e)
		{
			Log.e(TAG, "Exception: "+Log.getStackTraceString(e));
		}
		finally
		{
			if (LogSettings.DEBUG)
			{
				Log.d(TAG, "Awake: " + DateUtils.formatDuration(timeAwake));
				Log.d(TAG, "Since: " + DateUtils.formatDuration(timeSince));
				Log.d(TAG, "Drain: " + drain);
				if (timeSince != 0)
				{
					Log.d(TAG, "Drain %/h: " + drain/timeSince);
				}
				else
				{
					Log.d(TAG, "Drain %/h: 0");
				}
				
			}

			//strAwake = DateUtils.formatDurationCompressed(timeAwake);
			strAwake = StringUtils.formatRatio(timeAwake, timeSince);
			if (timeSince != 0)
			{
				float pct = drain / ((float)timeSince / 1000F / 60F / 60F);
				strDrain = String.format("%.1f", pct) + "%/h";
			}
			else
			{
				strDrain = "0 %/h";
			}
		}

		String refs = getString(R.string.label_since) + " " + Reference.getLabel(refFrom);
		// Publish the extension data update.
		publishUpdate(new ExtensionData().visible(true).icon(R.drawable.ic_dashclock).status(strDrain + ", " + strAwake)
				.expandedTitle(strAwake + " " + getString(R.string.label_awake_abbrev) + ", " + strDrain).expandedBody(refs)
				.clickIntent(new Intent(this, StatsActivity.class)));
	}
}
