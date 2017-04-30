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

package com.asksven.android.common.privateapiproxies;


import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

import com.asksven.android.common.nameutils.UidInfo;
import com.asksven.android.common.nameutils.UidNameResolver;
import com.asksven.android.common.utils.DateUtils;



/**
 * A proxy to the non-public API BatteryStats
 * http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android/2.3.3_r1/android/os/BatteryStats.java/?v=source
 * @author sven
 *
 */
public class NetworkQueryProxy
{
	/*
	 * Instance of the BatteryStatsImpl
	 */
	private Object m_Instance = null;
	@SuppressWarnings("rawtypes")
	private Class m_ClassDefinition = null;
	
	private static final String TAG = "NetworkQueryProxy";
	/*
	 * The UID stats are kept here as their methods / data can not be accessed
	 * outside of this class due to non-public types (Uid, Proc, etc.)
	 */
	private SparseArray<? extends Object> m_uidStats = null;
	
	/** 
	 * An instance to the UidNameResolver 
	 */
	private UidNameResolver m_nameResolver;

    /**
	 * Default cctor
	 */
	public NetworkQueryProxy(Context context)
	{
				
		try
		{
	          ClassLoader cl = context.getClassLoader();
	          
//	          ActivityManager am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
//	          am.restartPackage("com.android.phone");
	          Class networkQueryServiceClassDefinition = cl.loadClass("com.android.phone.NetworkQueryService");
	          
	          // get the IBinder to the "batteryinfo" service
	          @SuppressWarnings("rawtypes")
			  Class serviceManagerClass = cl.loadClass("android.os.ServiceManager");
	          
	          // parameter types
	          @SuppressWarnings("rawtypes")
			  Class[] paramTypesGetService= new Class[1];
	          paramTypesGetService[0]= String.class;
	          
	          @SuppressWarnings("unchecked")
			  Method methodGetService = serviceManagerClass.getMethod("getService", paramTypesGetService);
	          
	          // parameters
	          Object[] paramsGetService= new Object[1];
	          paramsGetService[0] = "networkquery";
	     
	          IBinder serviceBinder = (IBinder) methodGetService.invoke(serviceManagerClass, paramsGetService); 

	          context.startService(new Intent(context, networkQueryServiceClassDefinition));
	          if (serviceBinder == null)
	          {
	        	  Log.e(TAG, "no binder to networkquery found");
	        	  
	          }
	          else
	          {
	        	  Log.e(TAG, "binder to networkquery acquired");
	          }
//	          // now we have a binder. Let's us that on IBatteryStats.Stub.asInterface
//	          // to get an IBatteryStats
//	          // Note the $-syntax here as Stub is a nested class
//	          @SuppressWarnings("rawtypes")
//			  Class iBatteryStatsStub = cl.loadClass("com.android.internal.app.IBatteryStats$Stub");
//
//	          //Parameters Types
//	          @SuppressWarnings("rawtypes")
//			  Class[] paramTypesAsInterface= new Class[1];
//	          paramTypesAsInterface[0]= IBinder.class;
//
//	          @SuppressWarnings("unchecked")
//			  Method methodAsInterface = iBatteryStatsStub.getMethod("asInterface", paramTypesAsInterface);
//
//	          // Parameters
//	          Object[] paramsAsInterface= new Object[1];
//	          paramsAsInterface[0] = serviceBinder;
//	          	          
//	          Object iBatteryStatsInstance = methodAsInterface.invoke(iBatteryStatsStub, paramsAsInterface);
//	          
//	          // and finally we call getStatistics from that IBatteryStats to obtain a Parcel
//	          @SuppressWarnings("rawtypes")
//			  Class iBatteryStats = cl.loadClass("com.android.internal.app.IBatteryStats");
//	          
//	          @SuppressWarnings("unchecked")
//	          Method methodGetStatistics = iBatteryStats.getMethod("getStatistics");
//	          byte[] data = (byte[]) methodGetStatistics.invoke(iBatteryStatsInstance);
//	          
//	          Parcel parcel = Parcel.obtain();
//	          parcel.unmarshall(data, 0, data.length);
//	          parcel.setDataPosition(0);
//	          
//	          @SuppressWarnings("rawtypes")
//			  Class batteryStatsImpl = cl.loadClass("com.android.internal.os.BatteryStatsImpl");
//	          Field creatorField = batteryStatsImpl.getField("CREATOR");
//	          
//	          // From here on we don't need reflection anymore
//	          @SuppressWarnings("rawtypes")
//			  Parcelable.Creator batteryStatsImpl_CREATOR = (Parcelable.Creator) creatorField.get(batteryStatsImpl); 
//	          
//	          m_Instance = batteryStatsImpl_CREATOR.createFromParcel(parcel);        
	    }
		catch( Exception e )
		{
			Log.e("TAG", "An exception occured in NetworkQueryProxy(). Message: " + e.getMessage() + ", cause: " + e.getCause().getMessage());
	    	m_Instance = null;
	    }    
	}
	

}