/*
 * Copyright (C) 2012-2015 asksven
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

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;

import com.asksven.android.common.CommonLogSettings;
import com.asksven.betterbatterystats.data.StatsProvider;
import com.asksven.betterbatterystats.handlers.OnBootHandler;
import com.asksven.betterbatterystats.services.UpdateTextWidgetService;
import com.asksven.betterbatterystats.services.UpdateWidgetService;

import java.util.UUID;

/**
 * Demonstration of the use of a CursorLoader to load and display contacts data
 * in a fragment.
 */
public class PreferencesFragmentActivity extends BaseActivity
{

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{

		super.onCreate(savedInstanceState);

		// we need a layout to inflate the fragment into
		setContentView(R.layout.preferences_fragment);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		toolbar.setTitle(getString(R.string.label_preferences));

		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayUseLogoEnabled(false);

		getFragmentManager().beginTransaction().replace(R.id.prefs, new PrefsFragment()).commit();

	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class PrefsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener
	{

		/**
		 * @see android.app.Activity#onCreate(Bundle)
		 */
		@Override
		public void onCreate(Bundle savedInstanceState)
		{

			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.preferences);

			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
			prefs.registerOnSharedPreferenceChangeListener(this);

		}

		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data)
		{
			if (requestCode == 1001)
			{
				// get the new value from Intent data
				if (resultCode == RESULT_OK && data != null)
				{
					Uri destinationUri = data.getData();
					SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
					SharedPreferences.Editor editor = preferences.edit();
					editor.putString("storage_path", destinationUri.getPath());
					editor.commit();
				}
			}
		}

		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
		{
			if (key.equals("ref_for_screen_off"))
			{
				boolean serviceShouldBeRunning = sharedPreferences.getBoolean(key, false);

				// enable / disable sliders
				// enable sliders
				findPreference("watchdog_awake_threshold").setEnabled(serviceShouldBeRunning);
				findPreference("watchdog_duration_threshold").setEnabled(serviceShouldBeRunning);

			}

			if (key.equals("debug_logging"))
			{
				boolean enabled = sharedPreferences.getBoolean(key, false);
				if (enabled)
				{
					LogSettings.DEBUG = true;
					CommonLogSettings.DEBUG = true;
				} else
				{
					LogSettings.DEBUG = true;
					CommonLogSettings.DEBUG = true;
				}
			}

			if (key.equals("active_mon_enabled"))
			{
				boolean enabled = sharedPreferences.getBoolean(key, false);

				if (enabled)
				{
					AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
					builder.setMessage(getString(R.string.message_enable_active_mon))
							.setCancelable(false)
							.setPositiveButton(getString(R.string.label_button_yes),
									new DialogInterface.OnClickListener()
									{
										public void onClick(DialogInterface dialog, int id)
										{
											// Fire the alarms
											StatsProvider.scheduleActiveMonAlarm(getActivity());
											dialog.cancel();
										}
									})
							.setNegativeButton(getString(R.string.label_button_no),
									new DialogInterface.OnClickListener()
									{
										public void onClick(DialogInterface dialog, int id)
										{
											SharedPreferences sharedPrefs = PreferenceManager
													.getDefaultSharedPreferences(getActivity());
											SharedPreferences.Editor editor = sharedPrefs.edit();
											editor.putBoolean("active_mon_enabled", false);
											editor.commit();
											dialog.cancel();
										}
									});
					builder.create().show();

				}

				else
				{
					// cancel any existing alarms
					StatsProvider.cancelActiveMonAlarm(BbsApplication.getAppContext());

				}

			}
			if (key.equals("theme"))
			{
				if (getActivity() != null)
				{
					Intent i = getActivity().getBaseContext().getPackageManager()
							.getLaunchIntentForPackage(getActivity().getBaseContext().getPackageName());
					i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(i);
				}
			}

            if (key.equals("flag_time_series"))
            {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

                // get guid value
                String uuid = preferences.getString("uuid", "");
                if (uuid.equals(""))
                {
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("uuid", UUID.randomUUID().toString());
                    editor.commit();
                }

            }

			// if widget settings changed force them to update
			if (key.equals("text_widget_color") || key.equals("widget_show_pct"))
			{
                OnBootHandler.scheduleAppWidgetsJobImmediate(BbsApplication.getAppContext());


			}
		}

	}
}