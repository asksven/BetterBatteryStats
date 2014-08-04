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
package com.asksven.betterbatterystats;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.FloatMath;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.LineRegion;
import com.androidplot.ui.*;
import com.androidplot.xy.*;
import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.android.common.privateapiproxies.HistoryItem;
import com.asksven.android.common.utils.DataStorage;
import com.asksven.android.common.utils.DateUtils;
import com.asksven.android.common.utils.SysUtils;
import com.asksven.android.system.AndroidVersion;
import com.asksven.betterbatterystats.ZoomScrollGraphActivity.Viewport;
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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


/**
 * The battery graph rendered by AndroidPlot
 * @author sven
 *
 */
public class BatteryGraphActivity extends Activity
{
	/**
	 * a progess dialog to be used for long running tasks
	 */
	ProgressDialog m_progressDialog;
	
	private static final String TAG = "BatteryGraphActivity";
    private static final int FONT_LABEL_SIZE = 13;
    private int GRAPH_COLOR = 0;
    private XYPlot m_plotCharge;
    private XYPlot m_plotWakelock;
    private XYPlot m_plotScreenOn;
    private XYPlot m_plotWifi;

    protected static ArrayList<HistoryItem> m_histList;
	    
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        GRAPH_COLOR = getResources().getColor(R.color.peterriver);
        
		if ( (Build.VERSION.SDK_INT >= 19) && !SysUtils.hasBatteryStatsPermission(this) )
		{
			// show message that data is not available
			// prepare the alert box
            AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
 
            // set the message to display
            alertbox.setMessage("On kitkat Google has removed the possibility for apps to access BATTERY_STATS. For that reason the history and graphs are not available anymore");
 
            // add a neutral button to the alert box and assign a click listener
            alertbox.setNeutralButton("Ok", new DialogInterface.OnClickListener()
            {
 
                // click listener on the alert box
                public void onClick(DialogInterface arg0, int arg1)
                {
                	setResult(RESULT_OK);
                	finish();
                }
            });
 
            // show it
            alertbox.show();

		}

        setContentView(R.layout.batterygraph);
        m_plotCharge 	= (XYPlot) findViewById(R.id.myBatteryXYPlot);
        m_plotWakelock 	= (XYPlot) findViewById(R.id.wakelockPlot);
        m_plotScreenOn 	= (XYPlot) findViewById(R.id.screenOnPlot);
        m_plotWifi	 	= (XYPlot) findViewById(R.id.wifiPlot);
        
		//new LoadStatData().execute(this);
		m_histList = this.getHistList();
        seriesSetup();
      
        makePlotPretty(m_plotCharge);
        makePlotPretty(m_plotWakelock);
        makePlotPretty(m_plotScreenOn);
        makePlotPretty(m_plotWifi);
        
        refreshPlot(m_plotCharge);
        refreshPlot(m_plotWakelock);
        refreshPlot(m_plotScreenOn);
        refreshPlot(m_plotWifi);

		//Set of internal variables for keeping track of the boundaries
		m_plotCharge.calculateMinMaxVals();
    }
	 
	/* Request updates at startup */
	@Override
	protected void onResume()
	{
		super.onResume();

	}

	/** 
     * Add menu items
     * 
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    public boolean onCreateOptionsMenu(Menu menu)
    {  
    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.history_menu, menu);
        return true;
    }  
	
    /** 
     * Define menu action
     * 
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    public boolean onOptionsItemSelected(MenuItem item)
    {  
    	BatteryGraphSeries myZoomSerie = null;
		ZoomOnSerieChart myZoomChart = null;
		Intent myZoomIntent = null;
		
        switch (item.getItemId())
        {  

			case R.id.view_serie:
				Intent intentHist = new Intent(this, HistActivity.class);
			    this.startActivity(intentHist);
			    break;
			case R.id.more:
				Intent intentMore = new Intent(this, BatteryGraph2Activity.class);
			    this.startActivity(intentMore);
			    break;

			case R.id.dump:
            	// Dump to File
            	new WriteDumpFile().execute("");
            	break;
			case R.id.refresh:
				BatteryStatsProxy.getInstance(this).invalidate();
				m_histList = this.getHistList();
		        refreshPlot(m_plotCharge);
		        refreshPlot(m_plotWakelock);
		        refreshPlot(m_plotScreenOn);
		        refreshPlot(m_plotWifi);
		        break;
			case R.id.zoomWakelock:
				myZoomSerie = new BatteryGraphSeries(
		        		m_histList,
		        		BatteryGraphSeries.SERIE_WAKELOCK,
		        		"Wakelock");
				myZoomChart = new ZoomOnSerieChart(myZoomSerie);
				myZoomIntent = myZoomChart.execute(this);
				startActivity(myZoomIntent);
				break;
			case R.id.zoomScreenOn:
				myZoomSerie = new BatteryGraphSeries(
		        		m_histList,
		        		BatteryGraphSeries.SERIE_SCREENON,
		        		"Screen On");
				myZoomChart = new ZoomOnSerieChart(myZoomSerie);
				myZoomIntent = myZoomChart.execute(this);
				startActivity(myZoomIntent);
				break;
			case R.id.zoomWifi:
				myZoomSerie = new BatteryGraphSeries(
		        		m_histList,
		        		BatteryGraphSeries.SERIE_WIFI,
		        		"Wifi");
				myZoomChart = new ZoomOnSerieChart(myZoomSerie);
				myZoomIntent = myZoomChart.execute(this);
				startActivity(myZoomIntent);
				break;
			case R.id.zoomGps:
				myZoomSerie = new BatteryGraphSeries(
		        		m_histList,
		        		BatteryGraphSeries.SERIE_GPS,
		        		"GPS");
				myZoomChart = new ZoomOnSerieChart(myZoomSerie);
				myZoomIntent = myZoomChart.execute(this);
				startActivity(myZoomIntent);
				break;
			case R.id.zoomBluetooth:
				myZoomSerie = new BatteryGraphSeries(
		        		m_histList,
		        		BatteryGraphSeries.SERIE_BT,
		        		"Bluetooth");
				myZoomChart = new ZoomOnSerieChart(myZoomSerie);
				myZoomIntent = myZoomChart.execute(this);
				startActivity(myZoomIntent);
				break;
        }        
        return true;
    }
			    
     /**
     * Cleans up the plot's general layout and color scheme
     */
    protected void makePlotPretty(XYPlot plot)
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
	        plot.getGraphWidget().getGridLinePaint().setPathEffect(new DashPathEffect(new float[]{1, 2, 1, 2}, 0));
	        plot.getTitleWidget().getLabelPaint().setTextSize(FONT_LABEL_SIZE);
	        plot.getTitleWidget().pack();
	        plot.disableAllMarkup();
    	}
    }
	 
    private void seriesSetup()
    {
        // SERIES #1:
        // will not contain points with the same battery level value
        ArrayList<HistoryItem> shortHistory = new ArrayList<HistoryItem>(m_histList.size());
        Iterator<HistoryItem> historyIterator = m_histList.iterator();
        shortHistory.add(historyIterator.next());
        HistoryItem lastShortHistoryItem = shortHistory.get(0);
        while (historyIterator.hasNext()) {
            HistoryItem item = historyIterator.next();
            if (item.getBatteryLevelInt() != lastShortHistoryItem.getBatteryLevelInt()) {
                shortHistory.add(item);
                lastShortHistoryItem = shortHistory.get(shortHistory.size() - 1);
            }
        }
        BatteryGraphSeries mySerie = new BatteryGraphSeries(
        		shortHistory,
        		BatteryGraphSeries.SERIE_CHARGE,
        		"Charge");
        
        LineAndPointFormatter formater = new LineAndPointFormatter(
        		GRAPH_COLOR,
        		null,
        		GRAPH_COLOR);
        formater.getFillPaint().setAlpha(220);
        
        m_plotCharge.addSeries(mySerie, formater);
        
        m_plotCharge.setTicksPerDomainLabel(2);
        m_plotCharge.setTicksPerRangeLabel(1);
        m_plotCharge.disableAllMarkup();
        m_plotCharge.setDomainLabel("Time");
        m_plotCharge.setRangeLabel("%");
        
        m_plotCharge.setRangeBoundaries(0, 100, BoundaryMode.FIXED);
        
        m_plotCharge.setRangeValueFormat(new DecimalFormat("0"));
        m_plotCharge.setDomainValueFormat(new MyDateFormat());
        
        // SERIES #2:
        BatteryGraphSeries mySerie2 = new BatteryGraphSeries(
        		m_histList,
        		BatteryGraphSeries.SERIE_WAKELOCK,
        		"Wakelock");
        BarFormatter formater2 = new BarFormatter(
        		GRAPH_COLOR,
        		GRAPH_COLOR);
        formater2.getFillPaint().setAlpha(220);

        m_plotWakelock.addSeries(mySerie2, formater2);	        
        configBinPlot(m_plotWakelock);
        
        // SERIES #3:
		BatteryGraphSeries mySerie3 = new BatteryGraphSeries(
				m_histList,
				BatteryGraphSeries.SERIE_SCREENON,
				"Screen On");
		m_plotScreenOn.addSeries(mySerie3, formater2);	        
		configBinPlot(m_plotScreenOn);

        // SERIES #4:
		BatteryGraphSeries mySerie4 = new BatteryGraphSeries(
				m_histList,
				BatteryGraphSeries.SERIE_WIFI,
				"Wifi");
		m_plotWifi.addSeries(mySerie4, formater2);	        
		configBinPlot(m_plotWifi);

    }

    /**
     * Set common attributes for binary (0/1) plots
     * @param plot the plot to be configured
     */
    protected void configBinPlot(XYPlot plot)
    {
        plot.setTicksPerDomainLabel(2);
        plot.setTicksPerRangeLabel(1);
        plot.setRangeBoundaries(0, 1, BoundaryMode.FIXED);
        plot.disableAllMarkup();
        plot.setDomainLabel("Time");
        plot.setRangeLabel("");
        
        plot.setRangeValueFormat(new DecimalFormat("0"));
        plot.setDomainValueFormat(new MyDateFormat());
        
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
			Toast.makeText(this, "Unfortunately Froyo does not support history data.", Toast.LENGTH_SHORT).show();
		}
		ArrayList<HistoryItem> myRet = new ArrayList<HistoryItem>();
		
		
		BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(this);
		try
		{
			myRet = mStats.getHistory(this);
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
		    	m_progressDialog = new ProgressDialog(BatteryGraphActivity.this);
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
    		Toast.makeText(this, "External Storage can not be written", Toast.LENGTH_SHORT).show();
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
				PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
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
    		Toast.makeText(this, "an error occured while dumping the history", Toast.LENGTH_SHORT).show();
    	}		
	}
	
	

}