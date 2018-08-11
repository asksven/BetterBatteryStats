/*
 * Copyright (C) 2018 asksven
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.betterbatterystats.localeplugin.Constants;
import com.asksven.betterbatterystats.localeplugin.bundle.BundleScrubber;
import com.asksven.betterbatterystats.localeplugin.bundle.PluginBundleManager;
import com.asksven.betterbatterystats.services.WriteCustomReferenceService;
import com.asksven.betterbatterystats.services.WriteDumpfileService;

import java.util.Locale;

/**
 * This is the "fire" BroadcastReceiver for a Locale Plug-in setting.
 */
public final class FireReceiver extends BroadcastReceiver
{

	private static final String TAG = "FireReceiver";

	@Override
	public void onReceive(final Context context, final Intent intent)
	{
        /*
         * Always be strict on input parameters! A malicious third-party app could send a malformed Intent.
         */

        Log.i(TAG, "task plugin onReceive was called");

		if (!com.twofortyfouram.locale.api.Intent.ACTION_FIRE_SETTING.equals(intent.getAction()))
		{
			if (Constants.IS_LOGGABLE)
			{
				Log.e(Constants.LOG_TAG,
						String.format(Locale.US, "Received unexpected Intent action %s", intent.getAction())); //$NON-NLS-1$
			}
			return;
		}

		BundleScrubber.scrub(intent);

		final Bundle bundle = intent.getBundleExtra(com.twofortyfouram.locale.api.Intent.EXTRA_BUNDLE);
		BundleScrubber.scrub(bundle);

		if (PluginBundleManager.isBundleValid(bundle))
		{
            boolean saveRef = bundle.getBoolean(PluginBundleManager.BUNDLE_EXTRA_BOOL_SAVE_REF);
            boolean saveStat = bundle.getBoolean(PluginBundleManager.BUNDLE_EXTRA_BOOL_SAVE_STAT);

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

            if (saveRef)
            {
                Log.d(TAG, "Preparing to save a custom ref");
                Intent serviceIntent = new Intent(context, WriteCustomReferenceService.class);
                context.startService(serviceIntent);
            }

		}
	}
}