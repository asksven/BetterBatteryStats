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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.androidplot.xy.*;
import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.android.common.privateapiproxies.HistoryItem;
import com.asksven.android.common.utils.DataStorage;
import com.asksven.android.common.utils.DateUtils;
import com.asksven.android.system.AndroidVersion;
import com.asksven.betterbatterystats.BatteryGraphSeries;
import com.asksven.betterbatterystats.HistActivity;
import com.asksven.betterbatterystats.R;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Date;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;


/**
 * The battery graph rendered by AndroidPlot
 * @author sven
 *
 */
public class BatteryGraphFragment extends NestedFragment // we use nested fragment to avoid InvalideStateException: no Activity
{
	/**
	 * a progess dialog to be used for long running tasks
	 */
	ProgressDialog m_progressDialog;
	
	private static final String TAG = "BatteryGraphActivity";
    private static final int FONT_LABEL_SIZE = 25;
    private XYPlot m_plotCharge;

    public static ArrayList<HistoryItem> m_histList;
	    
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }
    
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		ViewPager mViewPager = (ViewPager) view.findViewById(R.id.viewPager);
		mViewPager.setAdapter(new MyPagerAdapter(getChildFragmentManager()));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.fragment_battery_graph, container, false);
		
        
		m_plotCharge 	= (XYPlot) rootView.findViewById(R.id.myBatteryXYPlot);
        
		//new LoadStatData().execute(this);
		m_histList = this.getHistList();
        seriesSetup();
      
        makePlotPretty(m_plotCharge);
        
        refreshPlot(m_plotCharge);

		//Set of internal variables for keeping track of the boundaries
		m_plotCharge.calculateMinMaxVals();
		
		return rootView;
    }
	 
	@Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {  
		super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.history_menu, menu);
    }  

	@Override
    public boolean onOptionsItemSelected(MenuItem item)
    {  
		
        switch (item.getItemId())
        {  

			case R.id.view_serie:
				Intent intentHist = new Intent(getActivity(), HistActivity.class);
			    this.startActivity(intentHist);
			    break;
			case R.id.dump:
            	// Dump to File
            	new WriteDumpFile().execute("");
            	break;
        }        
        return true;
    }
			    
     /**
     * Cleans up the plot's general layout and color scheme
     */
    protected static void makePlotPretty(XYPlot plot)
    {	 
    	if (plot != null)
    	{
	        // hide legend
	        plot.getLegendWidget().setVisible(false);
	        // make our domain and range labels invisible:
	        plot.getDomainLabelWidget().setVisible(false);
	        plot.getRangeLabelWidget().setVisible(false);
	 
	//	        plot.getGraphWidget().setRangeLabelMargin(-1);
	        plot.getGraphWidget().setRangeLabelWidth(25);
	        plot.getGraphWidget().setDomainLabelWidth(10);
	//	        plot.getGraphWidget().setDomainLabelMargin(-6);
	        plot.setBackgroundPaint(null);
	        plot.getGraphWidget().setBackgroundPaint(null);
	        plot.setBorderPaint(null);
	        
	        plot.getGraphWidget().getGridBackgroundPaint().setColor(Color.BLACK);
	        plot.getGraphWidget().getDomainLabelPaint().setTextSize(FONT_LABEL_SIZE);
	        plot.getGraphWidget().getDomainOriginLabelPaint().setTextSize(FONT_LABEL_SIZE);
	        plot.getGraphWidget().getRangeLabelPaint().setTextSize(FONT_LABEL_SIZE);
	        plot.getGraphWidget().getRangeOriginLabelPaint().setTextSize(FONT_LABEL_SIZE);
//	        plot.getGraphWidget().getGridLinePaint().setPathEffect(new DashPathEffect(new float[]{1, 2, 1, 2}, 0));
	        plot.getTitleWidget().getLabelPaint().setTextSize(FONT_LABEL_SIZE);
	        plot.getTitleWidget().pack();
//	        plot.disableAllMarkup();
    	}
    }
	 
    private void seriesSetup()
    {
        // SERIES #1:
        BatteryGraphSeries mySerie = new BatteryGraphSeries(
        		m_histList,
        		BatteryGraphSeries.SERIE_CHARGE,
        		"Charge");
        
        LineAndPointFormatter formater = new LineAndPointFormatter(
        		getResources().getColor(R.color.state_green), // Color.rgb(0, 0, 200),
        		null,
        		getResources().getColor(R.color.state_green), //Color.rgb(0, 0, 80),
        		new PointLabelFormatter(Color.TRANSPARENT));
        formater.getFillPaint().setAlpha(220);
        
        m_plotCharge.addSeries((XYSeries) mySerie, formater);
        
        m_plotCharge.setTicksPerDomainLabel(2);
        m_plotCharge.setTicksPerRangeLabel(1);
//        m_plotCharge.disableAllMarkup();
        m_plotCharge.setDomainLabel("Time");
        m_plotCharge.setRangeLabel("%");
        
        m_plotCharge.setRangeBoundaries(0, 100, BoundaryMode.FIXED);
        
        m_plotCharge.setRangeValueFormat(new DecimalFormat("0"));
        m_plotCharge.setDomainValueFormat(new MyDateFormat());
        
    }

    /**
     * Set common attributes for binary (0/1) plots
     * @param plot the plot to be configured
     */
    protected static void configBinPlot(XYPlot plot)
    {
        plot.setTicksPerDomainLabel(2);
        plot.setTicksPerRangeLabel(1);
        plot.setRangeBoundaries(0, 1, BoundaryMode.FIXED);
//        plot.disableAllMarkup();
        plot.setDomainLabel("Time");
        plot.setRangeLabel("");
        
        plot.setRangeValueFormat(new DecimalFormat("0"));
        plot.setDomainValueFormat(new SimpleDateFormat("HH:mm:ss"));
        
        // remove ticks
        plot.getGraphWidget().getDomainLabelPaint().setAlpha(0);
        plot.getGraphWidget().getDomainOriginLabelPaint().setAlpha(0);
        plot.getGraphWidget().getRangeLabelPaint().setAlpha(0);
        plot.getGraphWidget().getRangeOriginLabelPaint().setAlpha(0);
    }
    
    /**
     * refresh a plot
     * @param plot the plot to be refreshed
     */
    void refreshPlot(XYPlot plot)
    {
    	plot.redraw();
    }
	/**
	 * Get the Stat to be displayed
	 * @return a List of StatElements sorted (descending)
	 */
	protected ArrayList<HistoryItem> getHistList()
	{
		if (AndroidVersion.isFroyo())
		{
			Toast.makeText(getActivity(), "Unfortunately Froyo does not support history data.", Toast.LENGTH_SHORT).show();
		}
		ArrayList<HistoryItem> myRet = new ArrayList<HistoryItem>();
		
		
		BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(getActivity());
		try
		{
			myRet = mStats.getHistory(getActivity());
		}
		catch (Exception e)
		{
			Log.e(TAG, "An error occured while retrieving history. No result");
		}
		return myRet;
	}

		
	class MyDateFormat extends Format 
	{
        // create a simple date format that draws on the year portion of our timestamp.
        // see http://download.oracle.com/javase/1.4.2/docs/api/java/text/SimpleDateFormat.html
        // for a full description of SimpleDateFormat.
        private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
 
 
        @Override
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos)
        {
            long timestamp = ((Number) obj).longValue();
            Date date = new Date(timestamp);
            return dateFormat.format(date, toAppendTo, pos);
        }
 
		@Override
		public Object parseObject(String source, ParsePosition pos)
		{
			return null;
		}
 
	}
		
	// @see http://code.google.com/p/makemachine/source/browse/trunk/android/examples/async_task/src/makemachine/android/examples/async/AsyncTaskExample.java
	// for more details
	private class LoadStatData extends AsyncTask<Context, Integer, Integer>
	{
		@Override
	    protected Integer doInBackground(Context... params)
	    {
			//super.doInBackground(params);
			m_histList = getHistList();
	    	//StatsActivity.this.setListAdapter(m_listViewAdapter);
	        // getStatList();
	        return 1;
	    }
		
		@Override
		protected void onPostExecute(Integer i)
	    {
			super.onPostExecute(i);
	        // update hourglass
	    	if (m_progressDialog != null)
	    	{
	    		m_progressDialog.hide();
	    		m_progressDialog = null;
	    	}
	    }
	    @Override
	    protected void onPreExecute()
	    {
	        // update hourglass
	    	// @todo this code is only there because onItemSelected is called twice
	    	if (m_progressDialog == null)
	    	{
		    	m_progressDialog = new ProgressDialog(getActivity());
		    	m_progressDialog.setMessage("Computing...");
		    	m_progressDialog.setIndeterminate(true);
		    	m_progressDialog.setCancelable(false);
		    	m_progressDialog.show();
	    	}
	    }
	}


	private class WriteDumpFile extends AsyncTask
	{
		@Override
	    protected Object doInBackground(Object... params)
	    {
			writeDumpToFile();
	    	return true;
	    }

		@Override
		protected void onPostExecute(Object o)
	    {
			super.onPostExecute(o);
	        // update hourglass
	    }
	}
	/** 
	 * Dumps relevant data to an output file
	 * 
	 */
	void writeDumpToFile()
	{
		
		if (!DataStorage.isExternalStorageWritable())
		{
			Log.e(TAG, "External storage can not be written");
    		Toast.makeText(getActivity(), "External Storage can not be written", Toast.LENGTH_SHORT).show();
		}
		try
    	{		
			// open file for writing
			File root = Environment.getExternalStorageDirectory();
		    if (root.canWrite())
		    {
		    	String strFilename = "BetterBatteryStats_History-" + DateUtils.now("yyyy-MM-dd_HHmmssSSS") + ".txt";
		    	File dumpFile = new File(root, strFilename);
		        FileWriter fw = new FileWriter(dumpFile);
		        BufferedWriter out = new BufferedWriter(fw);
			  
				// write header
		        out.write("===================\n");
				out.write("History\n");
				out.write("===================\n");
				PackageInfo pinfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
				out.write("BetterBatteryStats version: " + pinfo.versionName + "\n");
				out.write("Creation Date: " + DateUtils.now() + "\n");
				out.write("\n");
				out.write("\n");
				out.write("Time;Battery Level;Charging;"
						+ "Screen On;GPS On;Wifi Running;"
						+ "Wakelock;BT On;In Call;"
						+ "Phone Scanning"
						+ "\n");
				
				for (int i=0; i < m_histList.size(); i++)
				{
			    	HistoryItem entry = m_histList.get(i);
			    	
			       	out.write(
			       			entry.getNormalizedTime() + ";"
			       			+ entry.getBatteryLevel() + ";"
			       			+ entry.getCharging() + ";"
			       			+ entry.getScreenOn() + ";"
			       			+ entry.getGpsOn() + ";"
			       			+ entry.getWifiRunning() + ";"
			       			+ entry.getWakelock() + ";"
			       			+ entry.getBluetoothOn() + ";"
			       			+ entry.getPhoneInCall() + ";"
			       			+ entry.getPhoneScanning() 
			       			+ "\n");	
				}
				
				// close file
				out.close();
		    }
    	}
    	catch (Exception e)
    	{
    		Log.e(TAG, "Exception: " + e.getMessage());
    		Toast.makeText(getActivity(), "an error occured while dumping the history", Toast.LENGTH_SHORT).show();
    	}		
	}
	
	private class MyPagerAdapter extends FragmentPagerAdapter
	{

		private final String[] TITLES =
		{ "Awake", "Screen On",
				"Wifi On", "GPS On",
				"Bluetooth On" };

		private final int[] GRAPH_SERIES =
		{ BatteryGraphSeries.SERIE_WAKELOCK, BatteryGraphSeries.SERIE_SCREENON,
				BatteryGraphSeries.SERIE_WIFI, BatteryGraphSeries.SERIE_GPS,
				BatteryGraphSeries.SERIE_BT };

		public MyPagerAdapter(FragmentManager fm)
		{
			super(fm);
		}

		@Override
		public CharSequence getPageTitle(int position)
		{
			return TITLES[position];
		}

		@Override
		public int getCount()
		{
			return TITLES.length;
		}

		@Override
		public Fragment getItem(int position)
		{
			Bundle args = new Bundle();
			args.putInt(TabHostOverviewPagerFragment.POSITION_KEY, position);
			args.putInt(SecondGraphFragment.GRAPH_SERIE, GRAPH_SERIES[position]);
			args.putString(SecondGraphFragment.GRAPH_TITLE, TITLES[position]);
			
			return SecondGraphFragment.newInstance(args);
		}

	}
}