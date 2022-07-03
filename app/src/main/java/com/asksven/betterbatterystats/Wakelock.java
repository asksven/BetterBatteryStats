package com.asksven.betterbatterystats;

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


import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

/**
 * An implementation of a global store of static vars
 * @author sven
 *
 */
public class Wakelock
{
	private static WakeLock m_saveWakelock;
	static final String WAKELOCK = "bbs:wakelock_while_saving_ref"; //BBS_WAKELOCK_WHILE_SAVING_REF";
	static final String TAG = "Wakelock";
	static final long TIMEOUT = 120 * 1000; // we should not hold a wakelock for longer that 30s
	
    public static synchronized void aquireWakelock(Context ctx)
    {
    	PowerManager powerManager = (PowerManager) ctx.getApplicationContext().getSystemService(Context.POWER_SERVICE);
    	releaseWakelock();
    	m_saveWakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK);
    	
    	// and android 2.3.x there seems to be a bug where the wakelock references are not kept / released
    	// in a thread safe way. See here for details: https://code.google.com/p/android/issues/detail?id=11622
    	// This is a hack for those versions, avoiding the reference counter to make sure that the under-locking
    	// exception is not getting thrown
    	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
    	{
    		m_saveWakelock.setReferenceCounted(false);
    	}
    	
    	m_saveWakelock.acquire(TIMEOUT);
    	Log.d(TAG, "Wakelock " + WAKELOCK + " aquired");
    }
    
    public static synchronized void releaseWakelock()
    {
    	try
		{
	    	if ((m_saveWakelock != null) && (m_saveWakelock.isHeld()))
	    	{
	    		
	    			m_saveWakelock.release();
	    			Log.d(TAG, "Wakelock " + WAKELOCK + " released");
	    	
	    	}
		}
		catch (Exception e)
		{
			Log.e(TAG, "An error occured releasing wakelock:" + e.getMessage());
		}
    }
}
