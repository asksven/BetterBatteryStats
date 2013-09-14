/*
 * Copyright (C) 2011-2013 asksven
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

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.asksven.android.common.ReadmeActivity;
import com.asksven.betterbatterystats.adapters.NavigationDrawerAdapter;
import com.asksven.betterbatterystats.data.GoogleAnalytics;
import com.asksven.betterbatterystats.data.NavigationDrawerItem;
import com.asksven.betterbatterystats.fragments.BatteryGraphFragment;
import com.asksven.betterbatterystats.fragments.CreditsFragment;
import com.asksven.betterbatterystats.fragments.OverviewFragment;
import com.asksven.betterbatterystats.fragments.OverviewPagerFragment;
import com.asksven.betterbatterystats.fragments.RawStatsFragment;
import com.asksven.betterbatterystats.fragments.ReadmeFragment;
import com.asksven.betterbatterystats.fragments.StatsFragment;

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

public class MainActivity extends SherlockFragmentActivity
{
	public static String STAT 				= "STAT";
	public static String STAT_TYPE_FROM		= "STAT_TYPE_FROM";
	public static String STAT_TYPE_TO		= "STAT_TYPE_TO";
	public static String FROM_NOTIFICATION 	= "FROM_NOTIFICATION";

	// Declare Variables
	DrawerLayout mDrawerLayout;
	ListView mDrawerList;
	ActionBarDrawerToggle mDrawerToggle;
//	MenuListAdapter mMenuAdapter;
	NavigationDrawerAdapter mDrawerAdapter;
	String[] title;
	// int[] icon;
	Fragment m_OverviewPagerFragment = new OverviewPagerFragment();//OverviewPagerFragment();
	Fragment m_statsFragment = new StatsFragment();
	Fragment m_creditsFragment = new CreditsFragment();
	Fragment m_readmeFragment = new ReadmeFragment();
	Fragment m_helpFragment = new ReadmeFragment();
	Fragment m_howtoFragment = new ReadmeFragment();
	Fragment m_batteryGraphFragment = new BatteryGraphFragment();
	Fragment m_rawStatsFragment = new RawStatsFragment();

	private CharSequence mDrawerTitle;
	private CharSequence mTitle;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		// Get the view from drawer_main.xml
		setContentView(R.layout.drawer_main);

		// Get the Title
		mTitle = mDrawerTitle = getTitle();

		// Generate title
		title = new String[]
		{ "Stats", "Settings", "Graphs", "RAW Stats", "About", "Getting Started", "How To", "Release Notes", "Credits" };

		// Generate icon
		// icon = new int[]
		// { R.drawable.action_about, R.drawable.action_settings,
		// R.drawable.collections_cloud, 0,
		// R.drawable.action_about, 0,
		// 0, 0,
		// 0 };

		// Locate DrawerLayout in drawer_main.xml
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

		// Locate ListView in drawer_main.xml
		mDrawerList = (ListView) findViewById(R.id.listview_drawer);

		// Set a custom shadow that overlays the main content when the drawer
		// opens
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

		// Pass string arrays to MenuListAdapter

		// mMenuAdapter = new MenuListAdapter(MainActivity.this, title, icon);
		mDrawerAdapter = new NavigationDrawerAdapter(this);
		// Add First Header
		mDrawerAdapter.addHeader(R.string.drawer_header1);
		String[] menuItems = getResources().getStringArray(R.array.drawer_group1_items);
		String[] menuItemsIcon = getResources().getStringArray(R.array.drawer_group1_icons);

		int res = 0;
		for (String item : menuItems)
		{

			int id_title = getResources().getIdentifier(item, "string", this.getPackageName());
			int id_icon = 0;
			// we may have no icon
			if (res < menuItemsIcon.length)
			{
				id_icon = getResources().getIdentifier(menuItemsIcon[res], "drawable", this.getPackageName());
			}

			NavigationDrawerItem entry = new NavigationDrawerItem(id_title, id_icon);
			mDrawerAdapter.addItem(entry);
			res++;
		}

		mDrawerAdapter.addHeader(R.string.drawer_header2);

		menuItems = getResources().getStringArray(R.array.drawer_group2_items);
		// menuItemsIcon =
		// getResources().getStringArray(R.array.drawer_group2_icons);

		for (String item : menuItems)
		{

			int id_title = getResources().getIdentifier(item, "string", this.getPackageName());
			int id_icon = 0;
			// we may have no icon
			if (res < menuItemsIcon.length)
			{
				id_icon = getResources().getIdentifier(menuItemsIcon[res], "drawable", this.getPackageName());
			}

			NavigationDrawerItem entry = new NavigationDrawerItem(id_title, id_icon);
			mDrawerAdapter.addItem(entry);
			res++;
		}

		mDrawerList.setAdapter(mDrawerAdapter);
		// Set the MenuListAdapter to the ListView
		// mDrawerList.setAdapter(mMenuAdapter);

		// Capture listview menu item click
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

		// Enable ActionBar app icon to behave as action to toggle nav drawer
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// ActionBarDrawerToggle ties together the the proper interactions
		// between the sliding drawer and the action bar app icon
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open,
				R.string.drawer_close)
		{

			public void onDrawerClosed(View view)
			{
				// TODO Auto-generated method stub
				super.onDrawerClosed(view);
			}

			public void onDrawerOpened(View drawerView)
			{
				// TODO Auto-generated method stub
				// Set the title on the action when drawer open
				getSupportActionBar().setTitle(mDrawerTitle);
				super.onDrawerOpened(drawerView);
			}
		};

		mDrawerLayout.setDrawerListener(mDrawerToggle);

		if (savedInstanceState == null)
		{
			selectItem(1);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{

		if (item.getItemId() == android.R.id.home)
		{

			if (mDrawerLayout.isDrawerOpen(mDrawerList))
			{
				mDrawerLayout.closeDrawer(mDrawerList);
			} else
			{
				mDrawerLayout.openDrawer(mDrawerList);
			}
		}

		return super.onOptionsItemSelected(item);
	}

	// ListView click listener in the navigation drawer
	private class DrawerItemClickListener implements ListView.OnItemClickListener
	{
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
			selectItem(position);
		}
	}

	private void selectItem(int position)
	{
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		Bundle b = new Bundle();

		// Locate constant at selected position
		switch (mDrawerAdapter.getItem(position).title)
		{
			case R.string.drawer_item_overview:
				ft.replace(R.id.content_frame, m_OverviewPagerFragment);

				break;

			case R.string.drawer_item_stats:
				ft.replace(R.id.content_frame, m_statsFragment);
				break;
				
			case R.string.drawer_item_settings:
				Intent intentPrefs = new Intent(this, PreferencesActivity.class);
				GoogleAnalytics.getInstance(this).trackPage(GoogleAnalytics.ACTIVITY_PREFERENCES);
				this.startActivity(intentPrefs);
				break;
	
			case R.string.drawer_item_graphs:
				GoogleAnalytics.getInstance(this).trackPage(GoogleAnalytics.ACTIVITY_BATTERY_GRAPH);
				ft.replace(R.id.content_frame, m_batteryGraphFragment);
				break;
	
			case R.string.drawer_item_raw:
				GoogleAnalytics.getInstance(this).trackPage(GoogleAnalytics.ACTIVITY_RAW);
				ft.replace(R.id.content_frame, m_rawStatsFragment);
				break;
	
			case R.string.drawer_item_about:
				// About
				Intent intentAbout = new Intent(this, AboutActivity.class);
				GoogleAnalytics.getInstance(this).trackPage(GoogleAnalytics.ACTIVITY_ABOUT);
				this.startActivity(intentAbout);
				break;
			case R.string.drawer_item_getting_started:
				// Help
				b.clear();
				b.putString("filename", "help.html");
				m_helpFragment.setArguments(b);
				ft.replace(R.id.content_frame, m_helpFragment);
				break;
			case R.string.drawer_item_howto:
				// How To
				b.clear();
				b.putString("filename", "howto.html");
				m_howtoFragment.setArguments(b);
				ft.replace(R.id.content_frame, m_howtoFragment);
				break;
	
			case R.string.drawer_item_release_notes:
				// Release notes
				b.clear();
				b.putString("filename", "readme.html");
				m_readmeFragment.setArguments(b);
				ft.replace(R.id.content_frame, m_readmeFragment);
				break;

			case R.string.drawer_item_credits:
				ft.replace(R.id.content_frame, m_creditsFragment);
				break;
		}
		
		ft.commit();
		mDrawerList.setItemChecked(position, true);

		// Get the title followed by the position
		setTitle(getString(mDrawerAdapter.getItem(position).title));
		// Close drawer
		mDrawerLayout.closeDrawer(mDrawerList);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggles
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public void setTitle(CharSequence title)
	{
		mTitle = title;
		getSupportActionBar().setTitle(mTitle);
	}
}