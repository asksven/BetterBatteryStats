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

import java.util.ArrayList;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.android.common.privateapiproxies.HistoryItem;
import com.asksven.android.system.AndroidVersion;
import com.asksven.betterbatterystats.adapters.GraphsAdapter;
import com.asksven.betterbatterystats.data.GraphSerie;
import com.asksven.betterbatterystats.data.GraphSeriesFactory;
import com.asksven.betterbatterystats.widgets.GraphableBarsPlot;

public class GraphActivity extends ActionBarListActivity
{

	private static final String TAG = "GraphActivity";
	//protected static ArrayList<HistoryItem> m_histList;
	GraphsAdapter m_adapter = null;
	//GraphSeriesFactory m_series = null;
	ProgressDialog m_progressDialog;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.graphs);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		toolbar.setTitle(getString(R.string.label_graphs));

		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayUseLogoEnabled(false);

		//m_histList = null; //getHistList();

		m_adapter = new GraphsAdapter(this, null);
		setListAdapter(m_adapter);


		new LoadSerieData().execute();

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
		    			GraphActivity.this.getString(R.string.label_graph_battery),
		    			list.getValues(GraphSeriesFactory.SERIE_CHARGE));


				GraphableBarsPlot bars = (GraphableBarsPlot) GraphActivity.this.findViewById(R.id.Battery);
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
				    	m_progressDialog = new ProgressDialog(GraphActivity.this);
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

}