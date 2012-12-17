/*
 * Copyright (C) 2011-12 asksven
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

import org.achartengine.chart.TimeChart;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.android.common.privateapiproxies.Misc;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.android.common.utils.DateUtils;
import com.asksven.android.common.utils.GenericLogger;
import com.asksven.android.common.utils.StringUtils;
import com.asksven.betterbatterystats.data.KbData;
import com.asksven.betterbatterystats.data.KbDbHelper;
import com.asksven.betterbatterystats.data.StatsProvider;
import com.asksven.betterbatterystats.widgets.WidgetBars;
import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.Wakelock;
import com.google.gson.Gson;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.method.TimeKeyListener;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RemoteViews;

/**
 * @author sven
 *
 */
public class KbReaderService extends Service
{
	private static final String TAG = "KbReaderService";
	private static final String URL = "http://asksven.github.com/BetterBatteryStats-Knowledge-Base/kb_v1.0.json";
    private static KbData m_kb = null;
    private static boolean m_transactional = false;
    

	@Override
	public void onStart(Intent intent, int startId)
	{
		m_transactional = true;
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		Log.i(TAG, "Called at " + DateUtils.now());
    	KbData data = null;
		KbDbHelper myDB = new KbDbHelper(this);

	  	 
    	String strUrlMod = sharedPrefs.getString("kb_url_appender", "");
	      	
    	try
    	{
    		InputStream source = retrieveStream(URL + strUrlMod);
    		
    		if (source != null)
    		{
	    		Gson gson = new Gson();	
	    		Reader reader = new InputStreamReader(source);
	    		
	    		// Now do the magic.
	    		data = gson.fromJson(reader,
	    				KbData.class);
    		}
	        // testing with static data
	        //data = new Gson().fromJson(SampleKbData.json, KbData.class);
    		
    		// save data and update timestamp
        	m_kb = data;
        	myDB.save(m_kb); 
        	// update cache update timestamp
    		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong("cache_updated", Calendar.getInstance().getTimeInMillis());
            editor.commit();

    	}
    	catch (Exception e)
    	{
    		e.printStackTrace();
    	}


		
		stopSelf();
		m_transactional = false;
		super.onStart(intent, startId);

	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

    public static KbData getCachedKb()
    {
    	return m_kb;
    }

    public static boolean isTransactional()
    {
    	return m_transactional;
    }

    private static InputStream retrieveStream(String url)
    {
        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(url);
        try
        {
           HttpResponse getResponse = client.execute(getRequest);

           final int statusCode = getResponse.getStatusLine().getStatusCode();

           if (statusCode != HttpStatus.SC_OK)
           {
              Log.w(TAG,
                  "Error " + statusCode + " for URL " + url);
              return null;
           }
           HttpEntity getResponseEntity = getResponse.getEntity();
           return getResponseEntity.getContent();
        }
        catch (IOException e)
        {
           getRequest.abort();
           Log.w(TAG, "Error for URL " + url, e);
        }
        return null;
     }

}