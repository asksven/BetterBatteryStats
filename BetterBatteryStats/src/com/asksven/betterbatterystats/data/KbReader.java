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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.asksven.betterbatterystats.StatsActivity;
import com.google.gson.Gson;

public class KbReader
{
	private static final String URL = "http://asksven.github.com/BetterBatteryStats-Knowledge-Base/kb_v1.0.json";
    private static KbData m_kb = null;
    private static boolean m_bNoConnection = false;
    
    private static long MAX_CACHE_AGE_MILLIS = 1000 * 60 * 1440; // 24 hours
    
    private static final String TAG = "KbReader";
    
    public static KbData read(Context ctx)
    {
    	if (m_kb != null)
    	{
    		return m_kb;
    	}
    	else
    	{
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
				m_kb = new KbData();
				m_kb.setEntries(myEntries);
			}
			// retrieve data and update cache
			else
			{
	    		// make sure we don't obcess
	    		if (m_bNoConnection)
	    		{
	    			return null;
	    		}
	    		
		    	KbData data = null;
			  	 
		    	String strUrlMod = sharedPrefs.getString("kb_url_appender", "");
	  	      	
		    	try
		    	{
		    		InputStream source = retrieveStream(URL + strUrlMod);
		    		
		    		if (source == null)
		    		{
		    			m_bNoConnection = true;
		    			return null;
		    		}
		    		Gson gson = new Gson();

		    		Reader reader = new InputStreamReader(source);
		    		
		    		// Now do the magic.
		    		data = gson.fromJson(reader,
		    				KbData.class);
		    		
			        // testing with static data
			        //data = new Gson().fromJson(SampleKbData.json, KbData.class);
			
		    	}
		    	catch (Exception e)
		    	{
		    		e.printStackTrace();
		    	}
		    	m_kb = data;
		    	myDB.save(m_kb); 
		    	// update cache update timestamp
        		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    	        SharedPreferences.Editor editor = prefs.edit();
    	        editor.putLong("cache_updated", dateMillis);
    	        editor.commit();


			}
    	}
    	return m_kb;
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