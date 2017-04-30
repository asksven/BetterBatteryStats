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
package com.asksven.android.common.wifi;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * A proxy for accessing WifiManager's private API
 * @author sven
 *
 */
public class WifiManagerProxy
{

	private static final String TAG = "WifiManagerProxy";
	private static WifiManager m_manager = null;
	
	private WifiManagerProxy(Context ctx)
	{
		
	}
	
	private static void init(Context ctx)
	{
		if (m_manager == null)
		{
			m_manager = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		}
	}
	public static boolean hasWifiLock(Context ctx)
	{
		init(ctx);
		return (getWifiLocks(ctx) > 0);
	}
	
	
	/**
	 * returns the number of help WifiLocks
	 * @return
	 */
	public static int getWifiLocks(Context ctx)
	{
		init(ctx);
		
		int ret = 0;
		try
		{
			Field privateStringField = WifiManager.class.getDeclaredField("mActiveLockCount");
	
			privateStringField.setAccessible(true);
	
			Integer fieldValue = (Integer) privateStringField.get(m_manager);
			Log.d(TAG, "mActiveLockCount is " + fieldValue);
			ret = fieldValue;
		}
		catch (Exception e)
		{
			Log.e(TAG, "An exception occured in getWifiLocks(): " + e.getMessage());
			ret = -1;
		}
		
		Log.d(TAG, ret + " Wifilocks detected");
		return ret;
	}
}
