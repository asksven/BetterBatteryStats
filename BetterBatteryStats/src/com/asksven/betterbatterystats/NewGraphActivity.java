/*
 * Copyright (C) 2014-2015 asksven
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.android.common.privateapiproxies.HistoryItem;
import com.asksven.android.common.utils.DataStorage;
import com.asksven.android.common.utils.DateUtils;
import com.asksven.android.system.AndroidVersion;
import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.adapters.GraphsAdapter;
import com.asksven.betterbatterystats.data.GraphSerie;
import com.asksven.betterbatterystats.data.GraphSeriesFactory;
import com.asksven.betterbatterystats.widgets.GraphableBarsPlot;

public class NewGraphActivity extends ActionBarListActivity
{

	private static final String TAG = "NewGraphActivity";
	//protected static ArrayList<HistoryItem> m_histList;
	GraphsAdapter m_adapter = null;
	//GraphSeriesFactory m_series = null;
	ProgressDialog m_progressDialog;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.newgraphs);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		toolbar.setTitle(getString(R.string.label_graphs));

		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayUseLogoEnabled(false);

		//m_histList = null; //getHistList();

		m_adapter = new GraphsAdapter(this, null); //m_histList);
		setListAdapter(m_adapter);

		//BatteryGraphSeries mySerie1 = new BatteryGraphSeries(m_histList, BatteryGraphSeries.SERIE_CHARGE, "Battery");

		//GraphableBarsPlot bars = (GraphableBarsPlot) this.findViewById(R.id.Battery);
		//bars.setValues(mySerie1.getValues());
		
		new LoadSerieData().execute();

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
		
        switch (item.getItemId())
        {  

			case R.id.view_serie:
				Intent intentHist = new Intent(this, HistActivity.class);
			    this.startActivity(intentHist);
			    break;
			case R.id.dump:
            	// Dump to File
            	new WriteDumpFile().execute("");
            	break;

        }        
        return super.onOptionsItemSelected(item);
    }
	/**
	 * Get the Stat to be displayed
	 * 
	 * @return a List of StatElements sorted (descending)
	 */
	protected ArrayList<HistoryItem> getHistList()
	{
		if (AndroidVersion.isFroyo())
		{
			Snackbar
			  .make(findViewById(android.R.id.content), R.string.message_no_hist_froyo, Snackbar.LENGTH_LONG)
			  .show();
//			Toast.makeText(this, getString(R.string.message_no_hist_froyo), Toast.LENGTH_SHORT).show();
		}
		ArrayList<HistoryItem> myRet = new ArrayList<HistoryItem>();

		BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(this);
		try
		{
			myRet = mStats.getHistory(this);
			//mStats.dumpHistory(this);
		}
		catch (Exception e)
		{
			Log.e(TAG, "An error occured while retrieving history. No result");
		}
		return myRet;
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
	
	// @see http://code.google.com/p/makemachine/source/browse/trunk/android/examples/async_task/src/makemachine/android/examples/async/AsyncTaskExample.java
		// for more details
		private class LoadSerieData extends AsyncTask<Void, Void, GraphSeriesFactory>
		{
			@Override
		    protected GraphSeriesFactory doInBackground(Void... params)
		    {

				//ArrayList<HistoryItem> list = null;
				GraphSeriesFactory store = null; 
				try
				{
					Log.i(TAG, "LoadSerieData: refreshing series");
					store = new GraphSeriesFactory(getHistList());
				}
				catch (Exception e)
				{
					Log.e(TAG, "Exception: "+Log.getStackTraceString(e));
				}

		        return store;
		    }
			
//			@Override
			protected void onPostExecute(GraphSeriesFactory list)
		    {
//				super.onPostExecute(o);
		        // update hourglass
				try
				{
			    	if (m_progressDialog != null)
			    	{
			    		m_progressDialog.dismiss();
			    		m_progressDialog = null;
			    	}
				}
				catch (Exception e)
				{
					// nop
				}
				finally 
				{
					m_progressDialog = null;
				}

		    	
		    	m_adapter.setSeries(list);
		    	m_adapter.notifyDataSetChanged();
		    	GraphSerie mySerie1 = new GraphSerie(
		    			NewGraphActivity.this.getString(R.string.label_graph_battery),
		    			list.getValues(GraphSeriesFactory.SERIE_CHARGE));
		   

				GraphableBarsPlot bars = (GraphableBarsPlot) NewGraphActivity.this.findViewById(R.id.Battery);
				bars.setValues(mySerie1.getValues());

		    }
//		    @Override
		    protected void onPreExecute()
		    {
		        // update hourglass
		    	// @todo this code is only there because onItemSelected is called twice
		    	if (m_progressDialog == null)
		    	{
		    		try
		    		{
				    	m_progressDialog = new ProgressDialog(NewGraphActivity.this);
				    	m_progressDialog.setMessage(getString(R.string.message_computing));
				    	m_progressDialog.setIndeterminate(true);
				    	m_progressDialog.setCancelable(false);
				    	m_progressDialog.show();
		    		}
		    		catch (Exception e)
		    		{
		    			m_progressDialog = null;
		    		}
		    	}
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
    		Toast.makeText(this, getString(R.string.message_external_storage_write_error), Toast.LENGTH_SHORT).show();
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
				ArrayList<HistoryItem> histList = getHistList();
				for (int i=0; i < histList.size(); i++)
				{
			    	HistoryItem entry = histList.get(i);
			    	
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
    		Toast.makeText(this, getString(R.string.message_error_writing_dumpfile), Toast.LENGTH_SHORT).show();
    	}	
	}

}