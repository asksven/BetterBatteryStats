package com.asksven.betterbatterystats.todelete;
///*
// * Copyright (C) 2012 asksven
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.asksven.betterbatterystats.fragments;
//
//import java.util.ArrayList;
//
//import android.content.Context;
//import android.os.Bundle;
//import android.os.Handler;
//import android.support.v4.app.Fragment;
//import android.support.v4.app.FragmentActivity;
//import android.support.v4.app.FragmentPagerAdapter;
//import android.support.v4.view.ViewPager;
//import android.util.TypedValue;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TabHost;
//import android.widget.TabWidget;
//
//import com.actionbarsherlock.app.SherlockFragment;
//import com.actionbarsherlock.app.SherlockFragmentActivity;
//import com.asksven.betterbatterystats.R;
//import com.astuetz.viewpager.extensions.PagerSlidingTabStrip;
//import android.support.v4.app.FragmentManager;
//
///**
// * Demonstrates combining a TabHost with a ViewPager to implement a tab UI that
// * switches between tabs and also allows the user to perform horizontal flicks
// * to move between the tabs.
// */
//public class OverviewPagerFragment extends SherlockFragment
//{
//	private PagerSlidingTabStrip tabs;
//	private ViewPager pager;
//	private MyPagerAdapter adapter;
//
//	@Override
//	public void onCreate(Bundle savedInstanceState)
//	{
//		super.onCreate(savedInstanceState);
//		setRetainInstance(true);
//	}
//
//	// @Override
//	// public void onActivityCreated(Bundle savedInstanceState)
//	// {
//	// super.onActivityCreated(savedInstanceState);
//	//
//	// tabs = (PagerSlidingTabStrip) getView().findViewById(R.id.tabs);
//	// tabs.setIndicatorColor(getResources().getColor(R.color.holoblue));
//	// pager = (ViewPager) getView().findViewById(R.id.pager);
//	// adapter = new MyPagerAdapter(getChildFragmentManager());
//	// //getActivity().getSupportFragmentManager());
//	//
//	// pager.setAdapter(adapter);
//	//
//	// final int pageMargin = (int)
//	// TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources()
//	// .getDisplayMetrics());
//	// pager.setPageMargin(pageMargin);
//	//
//	// tabs.setViewPager(pager);
//	//
//	// }
//
//	@Override
//	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
//	{
//		View rootView = inflater.inflate(R.layout.ovewview_pager, container, false);
//
//		tabs = (PagerSlidingTabStrip) rootView.findViewById(R.id.tabs);
//		tabs.setIndicatorColor(getResources().getColor(R.color.holoblue));
//		pager = (ViewPager) rootView.findViewById(R.id.pager);
//		adapter = new MyPagerAdapter(getChildFragmentManager());//getActivity().getSupportFragmentManager());
//
//		pager.setAdapter(adapter);
//
//		tabs.setViewPager(pager);
//
//		return rootView;
//	}
//
//	public class MyPagerAdapter extends FragmentPagerAdapter
//	{
//
//		private final String[] TITLES =
//		{ "Overview", "Details", "Hint" };
//
//		public MyPagerAdapter(FragmentManager fm)
//		{
//			super(fm);
//		}
//
//		@Override
//		public CharSequence getPageTitle(int position)
//		{
//			return TITLES[position];
//		}
//
//		@Override
//		public int getCount()
//		{
//			return TITLES.length;
//		}
//
//		@Override
//		public Fragment getItem(int position)
//		{
//			switch (position)
//			{
//			case 0:
//				return new OverviewFragment();
//			default:
//				return new EmptyFragment();
//			}
//		}
//
//	}
//
//}
