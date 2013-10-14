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
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockListFragment;
import com.asksven.betterbatterystats.adapters.ServicesAdapter;
import com.asksven.betterbatterystats.data.StatsProvider;

public class ServicesFragment extends SherlockListFragment
{
	private static String TAG = "ServicesFragment";
	String m_packageName;
	private ServicesAdapter m_listViewAdapter;
	ProgressDialog m_progressDialog;

	public static ServicesFragment newInstance(Bundle args)
	{
		ServicesFragment fragment = new ServicesFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		Bundle b = getActivity().getIntent().getExtras();
		m_packageName = b.getString("package");

		new LoadStatData().execute(getActivity());

	}

	// @see
	// http://code.google.com/p/makemachine/source/browse/trunk/android/examples/async_task/src/makemachine/android/examples/async/AsyncTaskExample.java
	// for more details
	private class LoadStatData extends AsyncTask<Context, Integer, ServicesAdapter>
	{
		@Override
		protected ServicesAdapter doInBackground(Context... params)
		{

			try
			{
				m_listViewAdapter = new ServicesAdapter(getActivity(), StatsProvider.getInstance(getActivity())
						.getServiceListForPackage(getActivity(), m_packageName));

			}
			catch (Exception e)
			{
				Log.e(TAG, "Loading of alarm stats failed");
				m_listViewAdapter = null;
			}
			// StatsActivity.this.setListAdapter(m_listViewAdapter);
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
			// @todo this code is only there because onItemSelected is called
			// twice
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
}
