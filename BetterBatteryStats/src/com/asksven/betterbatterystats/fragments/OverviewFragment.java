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
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
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
	TextView m_tvDeepSleep;
	TextView m_tvAwakeScreenOn;
	TextView m_tvAwakeScreenOff;
	MyPieGraph m_pg;

	public static OverviewFragment newInstance(Bundle args)
	{
		OverviewFragment fragment = new OverviewFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		final View v = inflater.inflate(R.layout.overview, container, false);

		return v;
	}

	 @Override
	 public void onViewCreated(View view, Bundle savedInstanceState)
	 {
		super.onViewCreated(view, savedInstanceState);
		m_tvDeepSleep = (TextView) view.findViewById(R.id.textViewDeepSleepValue);
		m_tvAwakeScreenOn = (TextView) view.findViewById(R.id.textViewScreenOnValue);
		m_tvAwakeScreenOff = (TextView) view.findViewById(R.id.textViewAwakeValue);

		m_pg = (MyPieGraph) view.findViewById(R.id.piegraph);

		setHasOptionsMenu(true);
		// setRetainInstance(true);

		doRefresh();

		m_pg.setOnSliceClickedListener(new OnSliceClickedListener()
		{

			@Override
			public void onClick(int index)
			{

			}

		});

	 }
	 
	@Override
	public void onPause()
	{
		super.onPause();

		Toast.makeText(getActivity(), "MyFragment.onPause()", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onResume()
	{
		super.onResume();
		// m_tvDeepSleep = (TextView)
		// getActivity().findViewById(R.id.textViewDeepSleepValue);
		// m_tvAwakeScreenOn = (TextView)
		// getActivity().findViewById(R.id.textViewScreenOnValue);
		// m_tvAwakeScreenOff = (TextView)
		// getActivity().findViewById(R.id.textViewAwakeValue);
		//
		// m_pg = (MyPieGraph) getActivity().findViewById(R.id.piegraph);
		//
		// doRefresh();
		//
		// m_pg.setOnSliceClickedListener(new OnSliceClickedListener()
		// {
		//
		// @Override
		// public void onClick(int index)
		// {
		//
		// }
		//
		// });
		//
		//
		Toast.makeText(getActivity(), "MyFragment.onResume()", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.kernelwakelocks_menu, menu);
	}

	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.refresh:
			// Refresh
			doRefresh();
			break;

		}
		return false;
	}

	void doRefresh()
	{
		// get the data
		StatsProvider stats = StatsProvider.getInstance(getActivity());
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		BbsApplication app = (BbsApplication) getActivity().getApplication();
		// retrieve stats

		String refFrom = app.getRefFromName();
		String refTo = app.getRefToName();

		long timeAwake = 0;
		long timeDeepSleep = 0;
		long timeScreenOn = 0;
		long timeSince = 0;

		try
		{

			Reference toRef = null;
			if (refTo.equals(Reference.CURRENT_REF_FILENAME))
			{
				// Update "current" uncached
				toRef = StatsProvider.getInstance(getActivity()).getUncachedPartialReference(0);
			} else
			{
				toRef = ReferenceStore.getReferenceByName(refTo, getActivity());
			}

			Reference fromRef = ReferenceStore.getReferenceByName(refFrom, getActivity());

			ArrayList<StatElement> otherStats = stats.getOtherUsageStatList(true, fromRef, false, true, toRef);

			if ((otherStats == null) || (otherStats.size() == 1))
			{
				// the desired stat type is unavailable, pick the alternate one
				// and go on with that one
				refFrom = sharedPrefs.getString("widget_fallback_stat_type", Reference.UNPLUGGED_REF_FILENAME);
				fromRef = ReferenceStore.getReferenceByName(refFrom, getActivity());

				otherStats = stats.getOtherUsageStatList(true, fromRef, false, true, toRef);
			}

			if ((otherStats != null) && (otherStats.size() > 1))
			{
				try
				{
					timeAwake = ((Misc) stats.getElementByKey(otherStats, "Awake")).getTimeOn();
					timeScreenOn = ((Misc) stats.getElementByKey(otherStats, "Screen On")).getTimeOn();
				}
				catch (Exception e)
				{
					timeAwake = 0;
					timeScreenOn = 0;
				}

				timeSince = StatsProvider.getInstance(getActivity()).getSince(fromRef, toRef);

				Misc deepSleepStat = ((Misc) stats.getElementByKey(otherStats, "Deep Sleep"));
				if (deepSleepStat != null)
				{
					timeDeepSleep = deepSleepStat.getTimeOn();
				} else
				{
					timeDeepSleep = 0;
				}

			} else
			{
				// no stat available
				timeAwake = -1;
				timeDeepSleep = -1;
				timeScreenOn = -1;
				timeSince = -1;
			}
		}
		catch (Exception e)
		{
			Log.e(TAG, "Exception: " + Log.getStackTraceString(e));
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
		m_pg.removeSlices();
		m_pg.setTitle(DateUtils.formatDurationCompressed(timeSince));
		m_pg.setTextHeightRatio(0.2f);

		PieSlice slice = new PieSlice();
		slice.setColor(getResources().getColor(R.color.state_green));
		slice.setValue(timeDeepSleep);
		slice.setTitle("Deep Sleep");
		m_pg.addSlice(slice);

		slice = new PieSlice();
		slice.setColor(getResources().getColor(R.color.state_red));
		slice.setValue(timeAwake - timeScreenOn);
		slice.setTitle("Awake Screen Off");
		m_pg.addSlice(slice);

		slice = new PieSlice();
		slice.setColor(getResources().getColor(R.color.state_yellow));
		slice.setValue(timeScreenOn);
		slice.setTitle("Awake Screen On");
		m_pg.addSlice(slice);

		// Populate the legend
		m_tvDeepSleep.setText(DateUtils.formatDurationCompressed(timeDeepSleep));
		m_tvAwakeScreenOn.setText(DateUtils.formatDurationCompressed(timeScreenOn));
		m_tvAwakeScreenOff.setText(DateUtils.formatDurationCompressed(timeAwake - timeScreenOn));

	}

}
