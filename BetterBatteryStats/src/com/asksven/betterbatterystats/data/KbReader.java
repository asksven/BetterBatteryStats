/*
 * Copyright (C) 2011-2012 asksven
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
package com.asksven.betterbatterystats.data;

/**
 * @author sven
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.asksven.betterbatterystats.LogSettings;
import com.asksven.betterbatterystats.StatsActivity;
import com.asksven.betterbatterystats.services.KbReaderService;
import com.asksven.betterbatterystats.services.WriteBootReferenceService;
import com.google.gson.Gson;

public class KbReader
{
	private static final String URL = "http://asksven.github.com/BetterBatteryStats-Knowledge-Base/kb_v1.0.json";
    private static boolean m_bNoConnection = false;
    
    private static long MAX_CACHE_AGE_MILLIS = 1000 * 60 * 1440; // 24 hours
    
    private static final String TAG = "KbReader";
    
    private static KbReader m_singleton = null;
    
    private KbData m_cache = null;
    
    private KbReader()
    {
    	
    }
    
    public static KbReader getInstance()
    {
    	if (m_singleton == null)
    	{
    		m_singleton = new KbReader();
    	}
    	
    	return m_singleton;
    }
    
    public KbData read(Context ctx)
    {
    	if (LogSettings.DEBUG)
    	{
    		Log.i(TAG, "read called");
    	}
    	if (m_cache == null)
    	{
    		if (LogSettings.DEBUG)
    		{
    			Log.i(TAG, "Cache is empty");
    		}
	     	SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);

    		// first check if cache is present and not outdated
    		KbDbHelper myDB = new KbDbHelper(ctx);
    		
    		List<KbEntry> myEntries = myDB.fetchAllRows();
    		
			// if cache exists and is not outdaten use it
			long cachedMillis = sharedPrefs.getLong("cache_updated", 0);
			long dateMillis = Calendar.getInstance().getTimeInMillis();
			boolean useCaching = sharedPrefs.getBoolean("cache_kb", true);

			// if cache is not empty, cache not older than 24 hours and caching is on
			if ((myEntries != null) && (myEntries.size() > 0) && (useCaching) && ((dateMillis - cachedMillis) < MAX_CACHE_AGE_MILLIS)) 
			{
				m_cache = new KbData();
				m_cache.setEntries(myEntries);
			}
			else
			{
				if (LogSettings.DEBUG)
				{
					Log.i(TAG, "Starting service to retrieve KB");
				}
				// start async service to retrieve KB if not already running
				if (!KbReaderService.isTransactional())
				{
					Intent serviceIntent = new Intent(ctx, KbReaderService.class);
					ctx.startService(serviceIntent);
				}
			}
    	}
    	else
    	{
    		if (LogSettings.DEBUG)
    		{
    			Log.i(TAG, "returning cached KB");
    		}
    	}
    	return m_cache;
    }
}