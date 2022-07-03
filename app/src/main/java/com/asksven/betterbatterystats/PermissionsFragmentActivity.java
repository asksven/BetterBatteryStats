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

import java.util.Map;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.ListFragment;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import android.util.Log;

import com.asksven.betterbatterystats.adapters.PermissionsAdapter;
import com.asksven.betterbatterystats.data.Permission;
import com.asksven.betterbatterystats.data.StatsProvider;

/**
 * Demonstration of the use of a CursorLoader to load and display contacts data
 * in a fragment.
 */
public class PermissionsFragmentActivity extends BaseActivity
{
	

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		// we need a layout to inflate the fragment into
	    
		FragmentManager fm = getSupportFragmentManager();

		// Create the list fragment and add it as our sole content.
		if (fm.findFragmentById(android.R.id.content) == null)
		{
			PermissionsListFragment list = new PermissionsListFragment();
			fm.beginTransaction().add(android.R.id.content, list).commit();
		}
	}

	public static class PermissionsListFragment extends ListFragment
	{

		/**
		 * The logging TAG
		 */
		private static final String TAG = "PermissionsListFragment";

		private PermissionsAdapter m_listViewAdapter;
		private Map<String, Permission> m_permDictionary;
		private String m_packageName;

		// This is the Adapter being used to display the list's data.
		SimpleCursorAdapter mAdapter;

		// If non-null, this is the current filter the user has provided.
		String mCurFilter;

		@Override
		public void onActivityCreated(Bundle savedInstanceState)
		{
			super.onActivityCreated(savedInstanceState);
			setHasOptionsMenu(true);
			
			Bundle b = getActivity().getIntent().getExtras();
			m_packageName = b.getString("package");

			if (m_permDictionary == null)
			{
				m_permDictionary = StatsProvider.getInstance().getPermissionMap(getActivity());
			}

			new LoadStatData().execute(getActivity());

		}

	    /** 
	     * Add menu items
	     * 
	     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	     */

		// @see http://code.google.com/p/makemachine/source/browse/trunk/android/examples/async_task/src/makemachine/android/examples/async/AsyncTaskExample.java
		// for more details
		private class LoadStatData extends AsyncTask<Context, Integer, PermissionsAdapter>
		{
			@Override
		    protected PermissionsAdapter doInBackground(Context... params)
		    {

				try
				{
					m_listViewAdapter = new PermissionsAdapter(getActivity(),
							StatsProvider.getInstance().getRequestedPermissionListForPackage(getActivity(), m_packageName), m_permDictionary);

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
		    	setListAdapter(o);
		    }
		}
	}
}
