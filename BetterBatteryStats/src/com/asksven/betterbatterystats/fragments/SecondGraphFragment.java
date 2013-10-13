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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.androidplot.xy.BarFormatter;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.asksven.android.common.privateapiproxies.Misc;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.android.common.utils.DateUtils;
import com.asksven.betterbatterystats.BatteryGraphSeries;
import com.asksven.betterbatterystats.BbsApplication;
import com.asksven.betterbatterystats.LogSettings;
import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.data.Reference;
import com.asksven.betterbatterystats.data.ReferenceStore;
import com.asksven.betterbatterystats.data.StatsProvider;
import com.asksven.betterbatterystats.widgets.MyPieGraph;
import com.echo.holographlibrary.PieGraph.OnSliceClickedListener;
import com.echo.holographlibrary.PieSlice;

public class SecondGraphFragment extends SherlockFragment
{
	private static String TAG = "AwakeGraphFragment";
    private XYPlot m_plotWakelock;
    public static final String GRAPH_SERIE = "com.asksven.betterbatterystats.GRAPH_SERIE";
    public static final String GRAPH_TITLE = "com.asksven.betterbatterystats.GRAPH_TITLE";

	public static SecondGraphFragment newInstance(Bundle args)
	{
		SecondGraphFragment fragment = new SecondGraphFragment();
		fragment.setArguments(args);
		return fragment;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		final View v = inflater.inflate(R.layout.fragment_second_graph, container, false);
		m_plotWakelock 	= (XYPlot) v.findViewById(R.id.wakelockPlot);
		
		// get the serie that was passed
		int serie = 0;
		Bundle args = getArguments();
		if (args != null)
		{
			serie = args.getInt(SecondGraphFragment.GRAPH_SERIE , 0);
	
		}
		
		if (serie == 0)
		{
			Log.e(TAG, "No valid serie was passed");
		}
		
		
		// Configure Serie
		BatteryGraphSeries mySerie2 = new BatteryGraphSeries(BatteryGraphFragment.m_histList, serie, "");
		BarFormatter formater2 = new BarFormatter(getResources().getColor(R.color.state_yellow),
				getResources().getColor(R.color.state_yellow));
		formater2.getFillPaint().setAlpha(220);
        LineAndPointFormatter formater = new LineAndPointFormatter(
        		getResources().getColor(R.color.state_yellow),
        		getResources().getColor(R.color.state_yellow),
        		getResources().getColor(R.color.state_yellow),
        		new PointLabelFormatter(Color.TRANSPARENT));
        formater.getFillPaint().setAlpha(220);

		m_plotWakelock.addSeries((XYSeries) mySerie2, formater);
		BatteryGraphFragment.configBinPlot(m_plotWakelock);

		BatteryGraphFragment.makePlotPretty(m_plotWakelock);
		m_plotWakelock.redraw();

		return v;
	}
	

}
