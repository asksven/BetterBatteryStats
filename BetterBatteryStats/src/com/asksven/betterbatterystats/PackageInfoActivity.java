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
package com.asksven.betterbatterystats;

/**
 * @author sven
 *
 */

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.asksven.android.common.ReadmeActivity;
import com.asksven.betterbatterystats.adapters.NavigationDrawerAdapter;
import com.asksven.betterbatterystats.data.GoogleAnalytics;
import com.asksven.betterbatterystats.data.NavigationDrawerItem;
import com.asksven.betterbatterystats.fragments.BatteryGraphFragment;
import com.asksven.betterbatterystats.fragments.CreditsFragment;
import com.asksven.betterbatterystats.fragments.OverviewFragment;
import com.asksven.betterbatterystats.fragments.OverviewPagerFragment;
import com.asksven.betterbatterystats.fragments.PackageInfoPagerFragment;
import com.asksven.betterbatterystats.fragments.SelectReferencesFragment;
import com.asksven.betterbatterystats.fragments.RawStatsFragment;
import com.asksven.betterbatterystats.fragments.RawStatsPagerFragment;
import com.asksven.betterbatterystats.fragments.ReadmeFragment;
import com.asksven.betterbatterystats.fragments.StatsFragment;
import com.asksven.betterbatterystats.fragments.StatsPagerFragment;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.support.v4.view.GravityCompat;

public class PackageInfoActivity extends SherlockFragmentActivity
{

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		ActionBar ab = getSupportActionBar();
		ab.setDisplayHomeAsUpEnabled(true);

		FragmentManager fm = getSupportFragmentManager();

		// Create the list fragment and add it as our sole content.
		if (fm.findFragmentById(android.R.id.content) == null)
		{
			PackageInfoPagerFragment frag = new PackageInfoPagerFragment();
			GoogleAnalytics.getInstance(this).trackPage(GoogleAnalytics.ACTIVITY_PERMS);
			getSupportActionBar().setTitle("Package Info");
			fm.beginTransaction().add(android.R.id.content, frag).commit();
			
		}
	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item)
    {  
        switch (item.getItemId())
        {  
	        case android.R.id.home:
    			this.finish();
    			return true;
        }  
        return false;  
    }    

}