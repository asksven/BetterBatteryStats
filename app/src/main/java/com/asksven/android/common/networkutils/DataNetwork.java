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

package com.asksven.android.common.networkutils;

import android.content.Context;
import android.net.ConnectivityManager;

/**
 * Helper class for data connectivity
 * @author sven
 *
 */
public class DataNetwork
{
	public static boolean hasDataConnection(Context ctx)
	{
		boolean ret = true;
		ConnectivityManager myConnectivity = 
				(ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		// if no network connection is available buffer the update
		// @see android.net.NetworkInfo
		if ( (myConnectivity == null)
				|| (myConnectivity.getActiveNetworkInfo() == null)
				|| (!myConnectivity.getActiveNetworkInfo().isAvailable()) )
		{
			
			ret = false;
		}
		
		return ret;
	}
	
	public static boolean hasWifiConnection(Context ctx)
	{
		boolean ret = false;
		ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
	    if( cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnectedOrConnecting() )
	    {
	        ret = true; 
	    }
	    else
	    {
	    	ret = false;
	    }
	    
	    return ret;
	}
}
