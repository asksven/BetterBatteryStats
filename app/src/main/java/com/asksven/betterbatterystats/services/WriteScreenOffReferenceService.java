/*
 * Copyright (C) 2011-14 asksven
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
package com.asksven.betterbatterystats.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.asksven.android.common.utils.DateUtils;
import com.asksven.android.common.utils.SysUtils;
import com.asksven.betterbatterystats.Wakelock;
import com.asksven.betterbatterystats.data.Reference;
import com.asksven.betterbatterystats.data.ReferenceStore;
import com.asksven.betterbatterystats.data.StatsProvider;
import com.asksven.betterbatterystats.widgetproviders.AppWidget;

/**
 * @author sven
 *
 */
public class WriteScreenOffReferenceService extends IntentService
{
	private static final String TAG = "WriteScreenOffRefServ";

	public WriteScreenOffReferenceService()
	{
	    super("WriteScreenOffReferenceService");
	}
	
	@Override
	public void onHandleIntent(Intent intent)
	{
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		Log.i(TAG, "Called at " + DateUtils.now());
		try
		{
			

			// Store the "since screen off" ref
			Wakelock.aquireWakelock(this);
			StatsProvider.getInstance().setReferenceSinceScreenOff(0);
			Intent i = new Intent(ReferenceStore.REF_UPDATED).putExtra(Reference.EXTRA_REF_NAME, Reference.SCREEN_OFF_REF_FILENAME);
			i.setPackage(SysUtils.getPackageName(this));
			this.sendBroadcast(i);

			long now = System.currentTimeMillis();
			// Save current time to prefs
			SharedPreferences.Editor editor = sharedPrefs.edit();
			editor.putLong("screen_went_off_at", now);
			editor.commit();

			// Build the intent to update the widget
			Intent intentRefreshWidgets = new Intent(AppWidget.WIDGET_UPDATE);
			this.sendBroadcast(intentRefreshWidgets);

		}
		catch (Exception e)
		{
			Log.e(TAG, "An error occured: " + e.getMessage());
		}
		finally
		{
			Wakelock.releaseWakelock();
		}
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}
	
	@Override
	public void onDestroy()
	{
		Log.i(TAG, "Destroyed at" + DateUtils.now());
		Wakelock.releaseWakelock();
	}

}