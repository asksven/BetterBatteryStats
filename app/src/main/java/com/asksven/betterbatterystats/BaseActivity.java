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
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity
{
	@Override
	protected void onResume()
	{
		
		this.setTheme(BaseActivity.getTheme(this));
		super.onResume();

	}
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// Obtain the shared Tracker instance.
		BbsApplication application = (BbsApplication) getApplication();

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		String theme = sharedPrefs.getString("theme", "0");
		super.onCreate(savedInstanceState);
	}

	public final static int getTheme(Context ctx)
	{
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		String theme = sharedPrefs.getString("theme", "0");

		if (theme.equals("0"))
		{
			return R.style.Theme_Bbs_Light;
		} else
		{
			return R.style.Theme_Bbs_Auto;
		}
	}
}

