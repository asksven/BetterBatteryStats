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

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import com.asksven.betterbatterystats.R;

public class DashClockSettingsActivity extends PreferenceActivity
{
	@SuppressLint("NewApi")
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		//getActionBar().setIcon(R.drawable.icon_notext);
		//getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref_dashboard);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == android.R.id.home)
		{
			// TODO: if the previous activity on the stack isn't a
			// ConfigurationActivity,
			// launch it.
			finish();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}


}
