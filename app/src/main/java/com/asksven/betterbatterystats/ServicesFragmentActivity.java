/*
 * Copyright (C) 2012-2014 asksven
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

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.util.Log;

import com.asksven.betterbatterystats.adapters.ServicesAdapter;
import com.asksven.betterbatterystats.data.StatsProvider;
import com.asksven.betterbatterystats.R;

/**
 * Demonstration of the use of a CursorLoader to load and display contacts data
 * in a fragment.
 */
public class ServicesFragmentActivity extends BaseActivity
{
	

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		FragmentManager fm = getSupportFragmentManager();

		// Create the list fragment and add it as our sole content.
		if (fm.findFragmentById(android.R.id.content) == null)
		{
			ServicesListFragment list = new ServicesListFragment();
			fm.beginTransaction().add(android.R.id.content, list).commit();
		}
	}

	public static class ServicesListFragment extends ListFragment
	{

		/**
		 * The logging TAG
		 */
		private static final String TAG = "ServicesListFragment";

		private ServicesAdapter m_listViewAdapter;
		private String m_packageName;
		ProgressDialog m_progressDialog;
		



		@Override
		public void onActivityCreated(Bundle savedInstanceState)
		{
			super.onActivityCreated(savedInstanceState);

			Bundle b = getActivity().getIntent().getExtras();
			m_packageName = b.getString("package");

			new LoadStatData().execute(getActivity());

		}


		// @see http://code.google.com/p/makemachine/source/browse/trunk/android/examples/async_task/src/makemachine/android/examples/async/AsyncTaskExample.java
		// for more details
		private class LoadStatData extends AsyncTask<Context, Integer, ServicesAdapter>
		{
			@Override
		    protected ServicesAdapter doInBackground(Context... params)
		    {

				try
				{
					m_listViewAdapter = new ServicesAdapter(getActivity(),
							StatsProvider.getInstance(getActivity()).getServiceListForPackage(getActivity(), m_packageName));

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
			protected void onPostExecute(ServicesAdapter o)
		    {
				super.onPostExecute(o);
		        // update hourglass
		    	if (m_progressDialog != null)
		    	{
		    		m_progressDialog.hide();
		    		m_progressDialog = null;
		    	}
		    	setListAdapter(o);
		    }
		    @Override
		    protected void onPreExecute()
		    {
		        // update hourglass
		    	// @todo this code is only there because onItemSelected is called twice
		    	if (m_progressDialog == null)
		    	{
			    	m_progressDialog = new ProgressDialog(getActivity());
			    	m_progressDialog.setMessage(getString(R.string.message_computing));
			    	m_progressDialog.setIndeterminate(true);
			    	m_progressDialog.setCancelable(false);
			    	m_progressDialog.show();
		    	}
		    }
		}		
	}
}
