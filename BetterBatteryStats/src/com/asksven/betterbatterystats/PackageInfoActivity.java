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
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.adapters.PermissionsAdapter;
import com.asksven.betterbatterystats.adapters.ServicesAdapter;
import com.asksven.betterbatterystats.data.Permission;
import com.asksven.betterbatterystats.data.StatsProvider;

public class PackageInfoActivity extends ListActivity
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
	private ServicesAdapter m_serviceListViewAdapter;
	
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
		

		setContentView(R.layout.package_info);
		if (m_permDictionary == null)
		{
			m_permDictionary = StatsProvider.getInstance(PackageInfoActivity.this).getPermissionMap(this);
		}

        TextView pName = (TextView) findViewById(R.id.TextViewPName);
        pName.setText(m_packageName);

        final Button buttonSettings = (Button) findViewById(R.id.buttonSettings);
        buttonSettings.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
            	showInstalledAppDetails(PackageInfoActivity.this, PackageInfoActivity.this.m_packageName);
            }
        });

	}
	
	/* Request updates at startup */
	@Override
	protected void onResume()
	{
		super.onResume();
		if (m_permDictionary == null)
		{
			m_permDictionary = StatsProvider.getInstance(PackageInfoActivity.this).getPermissionMap(this);
		}
		new LoadStatData().execute(this);
		m_serviceListViewAdapter = new ServicesAdapter(PackageInfoActivity.this,
				StatsProvider.getInstance(PackageInfoActivity.this).getServiceListForPackage(PackageInfoActivity.this, m_packageName));
		ListView serviceList = (ListView)findViewById(R.id.list2);
		serviceList.setAdapter(m_serviceListViewAdapter);
	
	}
	 @Override
	 protected void onListItemClick(ListView l, View v, int position, long id)
	{
		 if (l.getId() == android.R.id.list)
		 {
			 // user clicked a list item, make it "selected"
			 m_listViewAdapter.setSelectedPosition(position);
			 m_listViewAdapter.toggleExpand();
		 }
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
				m_listViewAdapter = new PermissionsAdapter(PackageInfoActivity.this,
						StatsProvider.getInstance(PackageInfoActivity.this).getRequestedPermissionListForPackage(PackageInfoActivity.this, m_packageName), m_permDictionary);

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
	    	PackageInfoActivity.this.setListAdapter(o);
	    }
	    @Override
	    protected void onPreExecute()
	    {
	        // update hourglass
	    	// @todo this code is only there because onItemSelected is called twice
	    	if (m_progressDialog == null)
	    	{
		    	m_progressDialog = new ProgressDialog(PackageInfoActivity.this);
		    	m_progressDialog.setMessage("Computing...");
		    	m_progressDialog.setIndeterminate(true);
		    	m_progressDialog.setCancelable(false);
		    	m_progressDialog.show();
	    	}
	    }
	}
	
    private static final String SCHEME = "package";
    private static final String APP_PKG_NAME_21 = "com.android.settings.ApplicationPkgName";
    private static final String APP_PKG_NAME_22 = "pkg";
    private static final String APP_DETAILS_PACKAGE_NAME = "com.android.settings";
    private static final String APP_DETAILS_CLASS_NAME = "com.android.settings.InstalledAppDetails";

	
    public static void showInstalledAppDetails(Context context, String packageName)
    {
        Intent intent = new Intent();
        final int apiLevel = Build.VERSION.SDK_INT;
        if (apiLevel >= 9)
        {
        	// above 2.3
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts(SCHEME, packageName, null);
            intent.setData(uri);
        }
        else
        {
        	// below 2.3
            final String appPkgName = (apiLevel == 8 ? APP_PKG_NAME_22 : APP_PKG_NAME_21);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setClassName(APP_DETAILS_PACKAGE_NAME,
                    APP_DETAILS_CLASS_NAME);
            intent.putExtra(appPkgName, packageName);
        }
        context.startActivity(intent);
    }


}
