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
package com.asksven.betterbatterystats.fragments;

/**
 * @author sven
 *
 */
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.asksven.betterbatterystats.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class StatsPagerFragment extends NestedFragment // we use nested fragment to avoid InvalideStateException: no Activity
{

	public static final String TAG = StatsPagerFragment.class.getSimpleName();

	public static StatsPagerFragment newInstance()
	{
		return new StatsPagerFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.fragment_overview_pager, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		ViewPager mViewPager = (ViewPager) view.findViewById(R.id.viewPager);
		mViewPager.setAdapter(new MyPagerAdapter(getChildFragmentManager()));
	}

	@Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {  
		super.onCreateOptionsMenu(menu, inflater);
    }  

	public class MyPagerAdapter extends FragmentPagerAdapter
	{

		private final String[] TITLES =
		{ "Overview", "Other", "Kernel Wakelocks", "Partial Wakelocks", "Alarms", "Network", "CPU States", "Processes" };

		public MyPagerAdapter(FragmentManager fm)
		{
			super(fm);
		}

		@Override
		public CharSequence getPageTitle(int position)
		{
			return TITLES[position];
		}

		@Override
		public int getCount()
		{
			return TITLES.length;
		}

		@Override
		public Fragment getItem(int position)
		{
			return StatsFragment.newInstance(position);
		}

	}


}