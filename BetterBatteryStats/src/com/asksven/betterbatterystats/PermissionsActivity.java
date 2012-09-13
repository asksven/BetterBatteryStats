/*
 * Copyright (C) 2011-2012 asksven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *et
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.asksven.betterbatterystats;

/**
 * Shows alarms in a list
 * @author sven
 */

import java.util.Map;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.data.Permission;
import com.asksven.betterbatterystats.data.StatsProvider;

public class PermissionsActivity extends ListActivity
{
	/**
	 * The logging TAG
	 */
	private static final String TAG = "PermissionsActivity";
	
	/**
	 * a progess dialog to be used for long running tasks
	 */
	ProgressDialog m_progressDialog;
	
	/**
	 * The ArrayAdpater for rendering the ListView
	 */
	private PermissionsAdapter m_listViewAdapter;
	
	private Map<String, Permission> m_permDictionary;
	
	private String m_packageName;// = "com.asksven.betterbatterystats";
	/**
	 * @see android.app.Activity#onCreate(Bundle@SuppressWarnings("rawtypes")
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Bundle b = getIntent().getExtras();
		m_packageName = b.getString("package");
		setContentView(R.layout.alarms);
		if (m_permDictionary == null)
		{
			m_permDictionary = StatsProvider.getInstance(PermissionsActivity.this).getPermissionMap(this);
		}
	}
	
	/* Request updates at startup */
	@Override
	protected void onResume()
	{
		super.onResume();
		if (m_permDictionary == null)
		{
			m_permDictionary = StatsProvider.getInstance(PermissionsActivity.this).getPermissionMap(this);
		}
		new LoadStatData().execute(this);
	
	}
	 @Override
	 protected void onListItemClick(ListView l, View v, int position, long id)
	{
		// user clicked a list item, make it "selected"
		 m_listViewAdapter.setSelectedPosition(position);
		 m_listViewAdapter.toggleExpand();
	}



	// @see http://code.google.com/p/makemachine/source/browse/trunk/android/examples/async_task/src/makemachine/android/examples/async/AsyncTaskExample.java
	// for more details
	private class LoadStatData extends AsyncTask<Context, Integer, PermissionsAdapter>
	{
		@Override
	    protected PermissionsAdapter doInBackground(Context... params)
	    {

			try
			{
				m_listViewAdapter = new PermissionsAdapter(PermissionsActivity.this,
						StatsProvider.getInstance(PermissionsActivity.this).getRequestedPermissionListForPackage(PermissionsActivity.this, m_packageName), m_permDictionary);
			}
			catch (Exception e)
			{
				Log.e(TAG, "Loading of alarm stats failed");
				m_listViewAdapter = null;
			}
	    	//StatsActivity.this.setListAdapter(m_listViewAdapter);
	        // getStatList();
	        return m_listViewAdapter;
	    }
		
		@Override
		protected void onPostExecute(PermissionsAdapter o)
	    {
			super.onPostExecute(o);
	        // update hourglass
	    	if (m_progressDialog != null)
	    	{
	    		m_progressDialog.hide();
	    		m_progressDialog = null;
	    	}
	    	PermissionsActivity.this.setListAdapter(o);
	    }
	    @Override
	    protected void onPreExecute()
	    {
	        // update hourglass
	    	// @todo this code is only there because onItemSelected is called twice
	    	if (m_progressDialog == null)
	    	{
		    	m_progressDialog = new ProgressDialog(PermissionsActivity.this);
		    	m_progressDialog.setMessage("Computing...");
		    	m_progressDialog.setIndeterminate(true);
		    	m_progressDialog.setCancelable(false);
		    	m_progressDialog.show();
	    	}
	    }
	}

}
