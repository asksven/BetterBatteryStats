/*
 * Copyright (C) 2013 asksven
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

package com.asksven.betterbatterystats.fragments;

import java.util.ArrayList;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.asksven.android.common.privateapiproxies.Misc;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.android.common.utils.DateUtils;
import com.asksven.betterbatterystats.BbsApplication;
import com.asksven.betterbatterystats.LogSettings;
import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.data.Reference;
import com.asksven.betterbatterystats.data.ReferenceStore;
import com.asksven.betterbatterystats.data.StatsProvider;
import com.asksven.betterbatterystats.widgets.MyPieGraph;
import com.echo.holographlibrary.PieGraph.OnSliceClickedListener;
import com.echo.holographlibrary.PieSlice;

public class OverviewFragment extends SherlockFragment
{
	private static String TAG = "OverviewFragment";
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		final View v = inflater.inflate(R.layout.overview, container, false);
		MyPieGraph pg = (MyPieGraph) v.findViewById(R.id.piegraph);

		// get the data
		StatsProvider stats = StatsProvider.getInstance(getActivity());
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		BbsApplication app = (BbsApplication) getActivity().getApplication();
		// retrieve stats
		
		String refFrom	= app.getRefFromName();
		String refTo	= app.getRefToName();
		
		long timeAwake 		= 0;
		long timeDeepSleep	= 0;
		long timeScreenOn 	= 0;
		long timeSince 		= 0;		

		try
		{
			Reference toRef 	= ReferenceStore.getReferenceByName(refTo, getActivity());
			Reference fromRef 	= ReferenceStore.getReferenceByName(refFrom, getActivity());
			
			ArrayList<StatElement> otherStats = stats.getOtherUsageStatList(true, fromRef, false, true, toRef);

			if ( (otherStats == null) || ( otherStats.size() == 1) )
			{
				// the desired stat type is unavailable, pick the alternate one and go on with that one
				refFrom	= sharedPrefs.getString("widget_fallback_stat_type", Reference.UNPLUGGED_REF_FILENAME);
				fromRef = ReferenceStore.getReferenceByName(refFrom, getActivity());
				
				otherStats = stats.getOtherUsageStatList(true, fromRef, false, true, toRef);
			}
			
			if ( (otherStats != null) && ( otherStats.size() > 1) )
			{
				try
				{
					timeAwake = ((Misc) stats.getElementByKey(otherStats, "Awake")).getTimeOn();
					timeScreenOn = ((Misc) stats.getElementByKey(otherStats, "Screen On")).getTimeOn();
				}
				catch (Exception e)
				{
					timeAwake 		= 0;
					timeScreenOn 	= 0;
				}
				
				timeSince = StatsProvider.getInstance(getActivity()).getSince(fromRef, toRef);

				Misc deepSleepStat = ((Misc) stats.getElementByKey(otherStats, "Deep Sleep"));
				if (deepSleepStat != null)
				{
					timeDeepSleep = deepSleepStat.getTimeOn();
				}
				else
				{
					timeDeepSleep = 0;
				}
				
				
			}
			else
			{
				// no stat available
				timeAwake 		= -1;
				timeDeepSleep	= -1;
				timeScreenOn 	= -1;
				timeSince 		= -1;		
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
				Log.d(TAG, "Since: " + DateUtils.formatDurationShort(timeSince));
				Log.d(TAG, "Awake: " + DateUtils.formatDurationShort(timeAwake));
				Log.d(TAG, "Deep Sleep: " + DateUtils.formatDurationShort(timeDeepSleep));
				Log.d(TAG, "Screen on: " + DateUtils.formatDurationShort(timeScreenOn));
			}
		}

		// Populate the graph
		pg.setTitle(DateUtils.formatDurationCompressed(timeSince));
		pg.setTextHeightRatio(0.2f);
		
		PieSlice slice = new PieSlice();
		slice.setColor(getResources().getColor(R.color.state_green));
		slice.setValue(timeDeepSleep);
		slice.setTitle("Deep Sleep");
		pg.addSlice(slice);

		slice = new PieSlice();
		slice.setColor(getResources().getColor(R.color.state_red));
		slice.setValue(timeAwake - timeScreenOn);
		slice.setTitle("Awake Screen Off");
		pg.addSlice(slice);
		
		slice = new PieSlice();
		slice.setColor(getResources().getColor(R.color.state_yellow));
		slice.setValue(timeScreenOn);
		slice.setTitle("Awake Screen On");
		pg.addSlice(slice);

		// Populate the legend
		TextView tvDeepSleep 		= (TextView) v.findViewById(R.id.textViewDeepSleepValue);
		TextView tvAwakeScreenOn 	= (TextView) v.findViewById(R.id.textViewScreenOnValue);
		TextView tvAwakeScreenOff 	= (TextView) v.findViewById(R.id.textViewAwakeValue);
		
		tvDeepSleep.setText(DateUtils.formatDurationCompressed(timeDeepSleep));
		tvAwakeScreenOn.setText(DateUtils.formatDurationCompressed(timeScreenOn));
		tvAwakeScreenOff.setText(DateUtils.formatDurationCompressed(timeAwake - timeScreenOn));
		

		pg.setOnSliceClickedListener(new OnSliceClickedListener()
		{

			@Override
			public void onClick(int index)
			{

			}

		});

		return v;
	}
}
