/*
 * Copyright (C) 2011 asksven
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

/**
 * @author sven
 *
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.asksven.betterbatterystats.data.KbData;
import com.asksven.betterbatterystats.data.SampleKbData;
import com.asksven.betterbatterystats_xdaedition.R;

public class KbReader
{
	private static final String URL = "http://asksven.github.com/BetterBatteryStats-Knowledge-Base/kb_v1.0.json";
    private static KbData m_kb = null;
    private static boolean m_bNoConnection = false;
    
    private static final String TAG = "KbReader";
    
    public static KbData read(Context ctx)
    {
    	if (m_kb != null)
    	{
    		return m_kb;
    	}
    	else
    	{
    		// make sure we don't obcess
    		if (m_bNoConnection)
    		{
    			return null;
    		}
    		
	    	KbData data = null;
	     	SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		  	 
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