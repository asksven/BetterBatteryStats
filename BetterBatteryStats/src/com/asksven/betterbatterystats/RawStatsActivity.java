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
import com.asksven.betterbatterystats.data.GoogleAnalytics;
import com.asksven.betterbatterystats.fragments.RawStatsPagerFragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;

public class RawStatsActivity extends SherlockFragmentActivity
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
			RawStatsPagerFragment frag = new RawStatsPagerFragment();
			GoogleAnalytics.getInstance(this).trackPage(GoogleAnalytics.ACTIVITY_RAW);
			getSupportActionBar().setTitle("RAW Stats");
			fm.beginTransaction().add(android.R.id.content, frag).commit();
			
		}
	}

}