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
import com.asksven.betterbatterystats.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class OverviewPagerFragment extends NestedFragment // we use nested fragment to avoid InvalideStateException: no Activity
{

	public static final String TAG = OverviewPagerFragment.class.getSimpleName();

	public static OverviewPagerFragment newInstance()
	{
		return new OverviewPagerFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.fragment_parent, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		ViewPager mViewPager = (ViewPager) view.findViewById(R.id.viewPager);
		mViewPager.setAdapter(new MyPagerAdapter(getChildFragmentManager()));
	}

	private class MyPagerAdapter extends FragmentPagerAdapter
	{

		private final String[] TITLES =
		{ "Summary", "Details", "Hint" };

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
			switch (position)
			{
			case 0:
				return new OverviewFragment();
			default:
				return new EmptyFragment();
			}
		}

	}

	private static class MyAdapter extends FragmentPagerAdapter
	{
		public MyAdapter(FragmentManager fm)
		{
			super(fm);
		}

		@Override
		public int getCount()
		{
			return 2;
		}

		@Override
		public Fragment getItem(int position)
		{
			Bundle args = new Bundle();
			args.putInt(TabHostOverviewPagerFragment.POSITION_KEY, position);
			switch (position)
			{
			case 0:
				return OverviewFragment.newInstance(args);
			case 1:
				return EmptyFragment.newInstance(args);
			default:
				return EmptyFragment.newInstance(args);
			}
		}

		@Override
		public CharSequence getPageTitle(int position)
		{
			return "Fragment # " + position;
		}

	}

}