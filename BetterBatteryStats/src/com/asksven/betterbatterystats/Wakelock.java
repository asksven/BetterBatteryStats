package com.asksven.betterbatterystats;

import com.asksven.betterbatterystats.R;
///*
// * Copyright (C) 2012 asksven
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package com.asksven.betterbatterystats;
//
//import android.content.Context;
//import android.os.PowerManager;
//import android.os.PowerManager.WakeLock;
//import android.util.Log;
//
///**
// * An implementation of a global store of static vars
// * @author sven
// *
// */
//public class Globals
//{
//	private static WakeLock screen_on_wakelock;
//	static final String WAKELOCK = "BBS_WAKELOCK_WHILE_SAVING_REF";
//	static final String TAG = "Globals";
//	
//    public static void aquireWakelock(Context ctx)
//    {
//    	PowerManager powerManager = (PowerManager) ctx.getApplicationContext().getSystemService(Context.POWER_SERVICE);
//    	releaseWakelock();
//    	screen_on_wakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK);
//    	screen_on_wakelock.acquire();
//    	Log.d(TAG, "Wakelock " + WAKELOCK + " aquired");
//    }
//    
//    public static void releaseWakelock()
//    {
//    	if ((screen_on_wakelock != null) && (screen_on_wakelock.isHeld()))
//    	{
//    		screen_on_wakelock.release();
//    		Log.d(TAG, "Wakelock " + WAKELOCK + " released");
//    	}
//    }
//
//}
