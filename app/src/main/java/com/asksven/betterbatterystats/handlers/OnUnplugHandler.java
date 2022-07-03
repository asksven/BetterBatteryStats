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
 */

package com.asksven.betterbatterystats.handlers;


import com.asksven.betterbatterystats.services.WriteUnpluggedReferenceService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;


/**
 * General broadcast handler: handles event as registered on Manifest
 * @author sven
 *
 */
public class OnUnplugHandler extends BroadcastReceiver
{	
	private static final String TAG = "OnUnplugHandler";
	
	/* (non-Javadoc)
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent)
	{
		Log.i(TAG, "Received Broadcast " + intent.getAction() + ", serializing 'since unplugged'");
		
		// start service to persist reference
		Intent serviceIntent = new Intent(context, WriteUnpluggedReferenceService.class);
        context.startService(serviceIntent);
	}
}
