/*
 * Copyright (C) 2012 asksven
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
 *
 * This file was contributed by two forty four a.m. LLC <http://www.twofortyfouram.com>
 * unter the terms of the Apache License, Version 2.0
 */

package com.asksven.betterbatterystats.localeplugin.receiver;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.betterbatterystats.localeplugin.bundle.PluginBundleManager;
import com.asksven.betterbatterystats.services.WriteCustomReferenceService;
import com.asksven.betterbatterystats.services.WriteDumpfileService;
import com.twofortyfouram.locale.sdk.client.receiver.AbstractPluginConditionReceiver;

/**
 * This is the "fire" BroadcastReceiver for a Locale Plug-in setting.
 */
public final class FireReceiver extends AbstractPluginConditionReceiver
{

	private static final String TAG = "FireReceiver";


	@Override
	protected int getPluginConditionResult(@NonNull final Context context,
										   @NonNull final Bundle bundle)
	{


		boolean saveRef = bundle.getBoolean(PluginBundleManager.BUNDLE_EXTRA_BOOL_SAVE_REF);
		boolean saveStat = bundle.getBoolean(PluginBundleManager.BUNDLE_EXTRA_BOOL_SAVE_STAT);
		boolean saveJson = bundle.getBoolean(PluginBundleManager.BUNDLE_EXTRA_BOOL_SAVE_JSON);

		String refFrom = bundle.getString(PluginBundleManager.BUNDLE_EXTRA_STRING_REF_NAME);

		Log.i(TAG, "Retrieved Bundle: " + bundle.toString());

		// make sure to flush cache
		BatteryStatsProxy.getInstance(context).invalidate();

		if (saveStat)
		{
			Log.d(TAG, "Preparing to save a dumpfile");

			Intent serviceIntent = new Intent(context, WriteDumpfileService.class);
			serviceIntent.putExtra(WriteDumpfileService.STAT_TYPE_FROM, refFrom);
			context.startService(serviceIntent);

		}

		if (saveJson)
		{
			Log.d(TAG, "Preparing to save a json dumpfile");

			Intent serviceIntent = new Intent(context, WriteDumpfileService.class);
			serviceIntent.putExtra(WriteDumpfileService.STAT_TYPE_FROM, refFrom);
			serviceIntent.putExtra(WriteDumpfileService.OUTPUT, "JSON");

			context.startService(serviceIntent);

		}
		if (saveRef)
		{
			Log.d(TAG, "Preparing to save a custom ref");
			Intent serviceIntent = new Intent(context, WriteCustomReferenceService.class);
			context.startService(serviceIntent);

		}

		return com.twofortyfouram.locale.api.Intent.RESULT_CONDITION_SATISFIED;

	}

	@Override
	protected boolean isBundleValid(@NonNull final Bundle bundle)
	{
		return PluginBundleManager.isBundleValid(bundle);
	}

	@Override
	protected boolean isAsync()
	{
		return true;
	}
}