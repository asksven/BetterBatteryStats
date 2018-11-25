/*
 * Copyright (C) 2011-2018 asksven
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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import com.asksven.betterbatterystats.appanalytics.Analytics;
import com.asksven.betterbatterystats.appanalytics.Events;

public class BaseActivity extends AppCompatActivity
{
	@Override
	protected void onResume()
	{
		
		this.setTheme(BaseActivity.getTheme(this));
		super.onResume();

		Analytics.getInstance(this).trackActivity(this, this.getClass().getSimpleName());
	}
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// Obtain the shared Tracker instance.
		BbsApplication application = (BbsApplication) getApplication();

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		String theme = sharedPrefs.getString("theme", "0");
		if (theme.equals("0"))
		{
			this.setTheme(R.style.Theme_Bbs);
			Analytics.getInstance(this).trackEvent(Events.EVENT_LAUNCH_LIGHT_THEME);
		} else
		{
			this.setTheme(R.style.Theme_Bbs_Dark);
			Analytics.getInstance(this).trackEvent(Events.EVENT_LAUNCH_DARK_THEME);
		}
		super.onCreate(savedInstanceState);
	}

	public final static int getTheme(Context ctx)
	{
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		String theme = sharedPrefs.getString("theme", "0");
		if (theme.equals("0"))
		{
			return R.style.Theme_Bbs;
		} else
		{
			return R.style.Theme_Bbs_Dark;
		}
	}
}

