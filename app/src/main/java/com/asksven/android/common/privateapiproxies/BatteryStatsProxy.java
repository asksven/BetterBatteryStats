/*
 * Copyright (C) 2011-2018 asksven
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


import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.MemoryFile;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;

import com.asksven.android.common.CommonLogSettings;
import com.asksven.android.common.nameutils.UidInfo;
import com.asksven.android.common.nameutils.UidNameResolver;
import com.asksven.android.common.utils.DateUtils;


/**
 * A proxy to the non-public API BatteryStats
 * @author sven
 * P preview 2:         https://android.googlesource.com/platform/frameworks/base/+/android-p-preview-2/core/java/com/android/internal/app/IBatteryStats.aidl
 *                      https://android.googlesource.com/platform/frameworks/base/+/android-p-preview-2/core/java/com/android/internal/os/BatteryStatsImpl.java
 *                      https://android.googlesource.com/platform/frameworks/base/+/android-p-preview-2/core/java/android/os/BatteryStats.java
 * Oreo (SDK26-27):     http://androidxref.com/8.0.0_r4/xref/frameworks/base/core/java/com/android/internal/os/BatteryStatsImpl.java#106
 * Nougat (SDK25-26):   http://androidxref.com/7.1.2_r36/xref/frameworks/base/core/java/com/android/internal/os/BatteryStatsImpl.java
 * Marshmallow (DSK23): http://androidxref.com/6.0.1_r10/xref/frameworks/base/core/java/com/android/internal/os/BatteryStatsImpl.java#94
 * Lolipop (SDK21-22):  http://androidxref.com/5.1.1_r6/xref/frameworks/base/core/java/com/android/internal/os/BatteryStatsImpl.java#85
 * Kitkat: (SDK19):     http://androidxref.com/4.4.4_r1/xref/frameworks/base/core/java/com/android/internal/os/BatteryStatsImpl.java#75
 *
 */
public class BatteryStatsProxy
{
	/*
	 * Instance of the BatteryStatsImpl
	 */
	private Object m_Instance = null;

	private static String m_lastError = "";
	private static boolean m_fallbackStats = false;

	@SuppressWarnings("rawtypes")
	private Class m_ClassDefinition = null;
	
	private static final String TAG = "BatteryStatsProxy";

    /**
     * Type to be passed to getNetworkActivityCount for different
     * stats.
     */
    private static final int NETWORK_MOBILE_RX_BYTES = 0;   // received bytes using mobile data

    private static final int NETWORK_MOBILE_TX_BYTES = 1;   // transmitted bytes using mobile data

    private static final int NETWORK_WIFI_RX_BYTES = 2;     // received bytes using wifi

    private static final int NETWORK_WIFI_TX_BYTES = 3;     // transmitted bytes using wifi

	/*
	 * The UID stats are kept here as their methods / data can not be accessed
	 * outside of this class due to non-public types (Uid, Proc, etc.)
	 */
	private SparseArray<? extends Object> m_uidStats = null;
	
	/** 
	 * An instance to the UidNameResolver 
	 */
	private static BatteryStatsProxy m_proxy = null;
	
	synchronized public static BatteryStatsProxy getInstance(Context ctx)
	{

	    try {
            if ((m_proxy == null) || (m_proxy.m_Instance == null)) {
                m_fallbackStats = false;
                m_lastError = "";

                if (Build.VERSION.SDK_INT >= 22) {
                    m_proxy = new BatteryStatsProxy(ctx, true);
                    // some devices, e.g. Samsung Galaxy S10 throw a Permission denied when reading the FileInputStream
                    // if the instance could not be created try the old way
                    if (m_proxy.m_Instance == null) {
                        m_fallbackStats = true;
                        m_proxy = new BatteryStatsProxy(ctx);
                    }
                } else {
                    m_fallbackStats = false;
                    m_proxy = new BatteryStatsProxy(ctx);
                }
            }
        }
        catch (NullPointerException e)
        {
            if (Build.VERSION.SDK_INT >= 22) {
                m_proxy = new BatteryStatsProxy(ctx, true);
                // some devices, e.g. Samsung Galaxy S10 throw a Permission denied when reading the FileInputStream
                // if the instance could not be created try the old way
                if (m_proxy.m_Instance == null) {
                    m_fallbackStats = true;
                    m_proxy = new BatteryStatsProxy(ctx);
                }
            } else {
                m_fallbackStats = false;
                m_proxy = new BatteryStatsProxy(ctx);
            }

        }
		return m_proxy;
	}

    public String getError()
    {
        return m_lastError;
    }

    public boolean isFallback()
    {
        return m_fallbackStats;
    }

    public void invalidate()
	{
//	    // if using fallback mode we try to not call batterinfo too often
//	    if (!isFallback())
//	    {
            m_proxy = null;
//        }
	}
	
    /**
	 * Default cctor
	 */
	protected BatteryStatsProxy(Context context)
	{
		/*
		 * As BatteryStats is a service we need to get a binding using the IBatteryStats.Stub.getStatistics()
		 * method (using reflection).
		 * If we would be using a public API the code would look like:
		 * @see com.android.settings.fuelgauge.PowerUsageSummary.java 
		 * protected void onCreate(Bundle icicle) {
         *  super.onCreate(icicle);
		 *	
         *  mStats = (BatteryStatsImpl)getLastNonConfigurationInstance();
		 *
         *  addPreferencesFromResource(R.xml.power_usage_summary);
         *  mBatteryInfo = IBatteryStats.Stub.asInterface(
         *       ServiceManager.getService("batteryinfo"));
         *  mAppListGroup = (PreferenceGroup) findPreference("app_list");
         *  mPowerProfile = new PowerProfile(this);
    	 * }
		 *
		 * followed by
		 * private void load() {
         *	try {
         *   byte[] data = mBatteryInfo.getStatistics();
         *   Parcel parcel = Parcel.obtain();
         *   parcel.unmarshall(data, 0, data.length);
         *   parcel.setDataPosition(0);
         *   mStats = com.android.internal.os.BatteryStatsImpl.CREATOR
         *           .createFromParcel(parcel);
         *   mStats.distributeWorkLocked(BatteryStats.STATS_SINCE_CHARGED);
         *  } catch (RemoteException e) {
         *   Log.e(TAG, "RemoteException:", e);
         *  }
         * }
		 */
				
		try
		{
            ClassLoader cl = context.getClassLoader();

            m_ClassDefinition = cl.loadClass("com.android.internal.os.BatteryStatsImpl");

            // enumerate some data
//            dumpClass(m_ClassDefinition);
//            Class iBatteryStatsUid = cl.loadClass("com.android.internal.os.BatteryStatsImpl$Uid");
//            dumpClass(iBatteryStatsUid);

            // get the IBinder to the "batteryinfo" service
            @SuppressWarnings("rawtypes")
            Class serviceManagerClass = cl.loadClass("android.os.ServiceManager");

            // parameter types
            @SuppressWarnings("rawtypes")
            Class[] paramTypesGetService= new Class[1];
            paramTypesGetService[0]= String.class;

            @SuppressWarnings("unchecked")
            Method methodGetService = serviceManagerClass.getMethod("getService", paramTypesGetService);

            String service = "";
            if (Build.VERSION.SDK_INT >= 19)
            {
              // kitkat and following
              service = "batterystats";
            }
            else
            {
              service = "batteryinfo";
            }
            // parameters
            Object[] paramsGetService= new Object[1];
            paramsGetService[0] = service;

            if (CommonLogSettings.DEBUG)
            {
              Log.i(TAG, "invoking android.os.ServiceManager.getService(\"" + service + "\")");
            }
            IBinder serviceBinder = (IBinder) methodGetService.invoke(serviceManagerClass, paramsGetService);

            if (CommonLogSettings.DEBUG)
            {
              Log.i(TAG, "android.os.ServiceManager.getService(\"" + service + "\") returned a service binder");
            }

            // now we have a binder. Let's us that on IBatteryStats.Stub.asInterface
            // to get an IBatteryStats
            // Note the $-syntax here as Stub is a nested class
            @SuppressWarnings("rawtypes")
            Class iBatteryStatsStub = cl.loadClass("com.android.internal.app.IBatteryStats$Stub");

            //Parameters Types
            @SuppressWarnings("rawtypes")
            Class[] paramTypesAsInterface= new Class[1];
            paramTypesAsInterface[0]= IBinder.class;

            @SuppressWarnings("unchecked")
            Method methodAsInterface = iBatteryStatsStub.getMethod("asInterface", paramTypesAsInterface);

            // Parameters
            Object[] paramsAsInterface= new Object[1];
            paramsAsInterface[0] = serviceBinder;

            if (CommonLogSettings.DEBUG)
            {
              Log.i(TAG, "invoking com.android.internal.app.IBatteryStats$Stub.asInterface");
            }
            Object iBatteryStatsInstance = methodAsInterface.invoke(iBatteryStatsStub, paramsAsInterface);

            // and finally we call getStatistics from that IBatteryStats to obtain a Parcel
            @SuppressWarnings("rawtypes")
            Class iBatteryStats = cl.loadClass("com.android.internal.app.IBatteryStats");

            @SuppressWarnings("unchecked")
            Method methodGetStatistics = iBatteryStats.getMethod("getStatistics");

            if (CommonLogSettings.DEBUG)
            {
              Log.i(TAG, "invoking getStatistics");
            }
            byte[] data = (byte[]) methodGetStatistics.invoke(iBatteryStatsInstance);

            if (CommonLogSettings.DEBUG)
            {
              Log.i(TAG, "retrieving parcel");
            }

            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(data, 0, data.length);
            parcel.setDataPosition(0);

            @SuppressWarnings("rawtypes")
            Class batteryStatsImpl = cl.loadClass("com.android.internal.os.BatteryStatsImpl");

            if (CommonLogSettings.DEBUG)
            {
              Log.i(TAG, "reading CREATOR field");
            }
            Field creatorField = batteryStatsImpl.getField("CREATOR");

            // From here on we don't need reflection anymore
            @SuppressWarnings("rawtypes")
            Parcelable.Creator batteryStatsImpl_CREATOR = (Parcelable.Creator) creatorField.get(batteryStatsImpl);

            m_Instance = batteryStatsImpl_CREATOR.createFromParcel(parcel);
            m_lastError = "";
	    }
		catch( Exception e )
		{
			if (e instanceof InvocationTargetException && e.getCause() != null)
			{
   				Log.e(TAG, "An exception occured in BatteryStatsProxy(). Message: " + e.getCause().getMessage());
   				m_lastError = e.getCause().getMessage();
			}
			else
			{
				Log.e(TAG, "An exception occured in BatteryStatsProxy(). Message: " + e.getMessage());
                m_lastError = e.getMessage();
			}
	    	m_Instance = null;
	    	
	    }    
	}

	protected void dumpClass(Class someClass)
    {
        List<Method> result = new ArrayList<Method>();

        Class clazz = someClass;
        while (clazz != null) {
            for (Method method : clazz.getDeclaredMethods()) {
                int modifiers = method.getModifiers();
                if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
                    result.add(method);
                }
            }
            clazz = clazz.getSuperclass();
        }

        Log.i(TAG, "Attributes of " + someClass.getName());

        for (int i=0; i < result.size(); i++)
        {
            Method method = result.get(i);
            Log.i(TAG, someClass.getName() + "." + method.getName());
        }

    }

	protected BatteryStatsProxy(Context context, boolean dummy) // just need a different signature
	{
		/*
		 * Same as default cctor but reading the parcel as stream and not as byte[]
		 * see here for details: https://android.googlesource.com/platform/frameworks/base.git/+/nougat-mr2.1-release/core/java/com/android/internal/os/BatteryStatsHelper.java
		 */

		try
		{
			ClassLoader cl = context.getClassLoader();

			m_ClassDefinition = cl.loadClass("com.android.internal.os.BatteryStatsImpl");

			// get the IBinder to the "batteryinfo" service
			@SuppressWarnings("rawtypes")
			Class serviceManagerClass = cl.loadClass("android.os.ServiceManager");

			// parameter types
			@SuppressWarnings("rawtypes")
			Class[] paramTypesGetService= new Class[1];
			paramTypesGetService[0]= String.class;

			@SuppressWarnings("unchecked")
			Method methodGetService = serviceManagerClass.getMethod("getService", paramTypesGetService);

			String service = "batterystats";

			// parameters
			Object[] paramsGetService= new Object[1];
			paramsGetService[0] = service;

			if (CommonLogSettings.DEBUG)
			{
				Log.i(TAG, "invoking android.os.ServiceManager.getService(\"" + service + "\")");
			}
			IBinder serviceBinder = (IBinder) methodGetService.invoke(serviceManagerClass, paramsGetService);

			if (CommonLogSettings.DEBUG)
			{
				Log.i(TAG, "android.os.ServiceManager.getService(\"" + service + "\") returned a service binder");
			}

			// now we have a binder. Let's us that on IBatteryStats.Stub.asInterface
			// to get an IBatteryStats
			// Note the $-syntax here as Stub is a nested class
			@SuppressWarnings("rawtypes")
			Class iBatteryStatsStub = cl.loadClass("com.android.internal.app.IBatteryStats$Stub");

			//Parameters Types
			@SuppressWarnings("rawtypes")
			Class[] paramTypesAsInterface= new Class[1];
			paramTypesAsInterface[0]= IBinder.class;

			@SuppressWarnings("unchecked")
			Method methodAsInterface = iBatteryStatsStub.getMethod("asInterface", paramTypesAsInterface);

			// Parameters
			Object[] paramsAsInterface= new Object[1];
			paramsAsInterface[0] = serviceBinder;

			if (CommonLogSettings.DEBUG)
			{
				Log.i(TAG, "invoking com.android.internal.app.IBatteryStats$Stub.asInterface");
			}
			Object iBatteryStatsInstance = methodAsInterface.invoke(iBatteryStatsStub, paramsAsInterface);

			// and finally we call getStatistics from that IBatteryStats to obtain a Parcel
			@SuppressWarnings("rawtypes")
			Class iBatteryStats = cl.loadClass("com.android.internal.app.IBatteryStats");

            @SuppressWarnings("unchecked")
            Method methodGetStatisticsStream = iBatteryStats.getMethod("getStatisticsStream");
            // returns a ParcelFileDescriptor

            if (CommonLogSettings.DEBUG)
			{
				Log.i(TAG, "invoking getStatisticsStream");
			}
            ParcelFileDescriptor pfd = (ParcelFileDescriptor) methodGetStatisticsStream.invoke(iBatteryStatsInstance);


            if (pfd != null)
            {
                FileInputStream fis = new ParcelFileDescriptor.AutoCloseInputStream(pfd);

                try
                {
                    if (CommonLogSettings.DEBUG) { Log.i(TAG, "retrieving parcel"); }

                    @SuppressWarnings("rawtypes")
                    Class[] paramTypes= new Class[1];
                    paramTypes[0]= FileDescriptor.class;


                    // we want to access MemoryFile.getSize(pfd.getFileDescriptor()) but this methos id hidden
                    Method methodGetSize = MemoryFile.class.getMethod("getSize", paramTypes);
                    methodGetSize.setAccessible(true);

                    int size = (int) methodGetSize.invoke(null, /* null = static method */ pfd.getFileDescriptor());

                    byte[] data = readFully(fis, size);

                    Parcel parcel = Parcel.obtain();
                    parcel.unmarshall(data, 0, data.length);
                    parcel.setDataPosition(0);

                    @SuppressWarnings("rawtypes")
                    Class batteryStatsImpl = cl.loadClass("com.android.internal.os.BatteryStatsImpl");

                    if (CommonLogSettings.DEBUG) { Log.i(TAG, "reading CREATOR field"); }
                    Field creatorField = batteryStatsImpl.getField("CREATOR");

                    // From here on we don't need reflection anymore
                    @SuppressWarnings("rawtypes")
                    Parcelable.Creator batteryStatsImpl_CREATOR = (Parcelable.Creator) creatorField.get(batteryStatsImpl);

                    m_Instance = batteryStatsImpl_CREATOR.createFromParcel(parcel);
                    m_lastError = "";

                }
                catch (IOException e)
                {
                    Log.w(TAG, "Unable to read statistics stream", e);
                    m_lastError = "Unable to read statistics stream: " + e.getMessage();
                }
            }

		}
		catch( Exception e )
		{
			if (e instanceof InvocationTargetException && e.getCause() != null)
			{
				Log.e(TAG, "An exception occured in BatteryStatsProxy(). Message: " + e.getCause().getMessage());
                m_lastError = e.getCause().getMessage();
			}
			else
			{
				Log.e(TAG, "An exception occured in BatteryStatsProxy(). Message: " + e.getMessage());
                m_lastError = e.getMessage();
			}
			m_Instance = null;

		}
	}

	// both these methods come from com/android/internal/os/BatteryStatsHelper.java
    protected static byte[] readFully(FileInputStream stream) throws java.io.IOException {
        return readFully(stream, stream.available());
    }
    protected static byte[] readFully(FileInputStream stream, int avail) throws java.io.IOException {
        int pos = 0;
        byte[] data = new byte[avail];
        while (true) {
            int amt = stream.read(data, pos, data.length-pos);
            Log.i(TAG, "Read " + amt + " bytes at " + pos
                    + " of avail " + data.length);
            if (amt <= 0) {
                Log.i(TAG, "**** FINISHED READING: pos=" + pos
                        + " len=" + data.length);
                return data;
            }
            pos += amt;
            avail = stream.available();
            if (avail > data.length-pos) {
                byte[] newData = new byte[pos+avail];
                System.arraycopy(data, 0, newData, 0, pos);
                data = newData;
            }
        }
    }
	/**
	 * Returns true if the proxy could not be initialized properly
	 * @return true if the proxy wasn't initialized
	 */
	public boolean initFailed()
	{
		return m_Instance == null;
	}
	
	/**
     * Returns the total, last, or current battery realtime in microseconds.
     *
     * @param curTime the current elapsed realtime in microseconds.
     * @param iStatsType one of STATS_TOTAL, STATS_LAST, or STATS_CURRENT.
     */
    public Long computeBatteryRealtime(long curTime, int iStatsType) throws BatteryInfoUnavailableException
	{
    	Long ret = new Long(0);

        try
        {
          //Parameters Types
          @SuppressWarnings("rawtypes")
          Class[] paramTypes= new Class[2];
          paramTypes[0]= long.class;
          paramTypes[1]= int.class;          

          @SuppressWarnings("unchecked")
		  Method method = m_ClassDefinition.getMethod("computeBatteryRealtime", paramTypes);

          //Parameters
          Object[] params= new Object[2];
          params[0]= new Long(curTime);
          params[1]= new Integer(iStatsType);

			ret = (Long) method.invoke(m_Instance, params);

        }
        catch( IllegalArgumentException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            ret = new Long(0);
            throw new BatteryInfoUnavailableException();
        }

        return ret;

	
	}

	/**
     * Returns the total, last, or current battery realtime in microseconds.
     *
     * @param curTime the current elapsed realtime in microseconds.
     */
    public Long getBatteryRealtime(long curTime) throws BatteryInfoUnavailableException
	{
    	Long ret = new Long(0);

        try
        {
          //Parameters Types
          @SuppressWarnings("rawtypes")
          Class[] paramTypes= new Class[1];
          paramTypes[0]= long.class;
         

          @SuppressWarnings("unchecked")
		  Method method = m_ClassDefinition.getMethod("getBatteryRealtime", paramTypes);

          //Parameters
          Object[] params= new Object[1];
          params[0]= new Long(curTime);


          ret= (Long) method.invoke(m_Instance, params);

        }
        catch( IllegalArgumentException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            ret = new Long(0);
            throw new BatteryInfoUnavailableException();
        }

        return ret;

	
	}

	/**
     * Returns the total, last, or current battery uptime in microseconds.
     *
     * @param curTime the current elapsed realtime in microseconds.
     * @param iStatsType one of STATS_TOTAL, STATS_LAST, or STATS_CURRENT.
     */
    public Long computeBatteryUptime(long curTime, int iStatsType) throws BatteryInfoUnavailableException
	{
    	Long ret = new Long(0);

        try
        {
          //Parameters Types
          @SuppressWarnings("rawtypes")
          Class[] paramTypes= new Class[2];
          paramTypes[0]= long.class;
          paramTypes[1]= int.class;          

          @SuppressWarnings("unchecked")
		  Method method = m_ClassDefinition.getMethod("computeBatteryUptime", paramTypes);

          //Parameters
          Object[] params= new Object[2];
          params[0]= new Long(curTime);
          params[1]= new Integer(iStatsType);

          ret= (Long) method.invoke(m_Instance, params);

        }
        catch( IllegalArgumentException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            ret = new Long(0);
            throw new BatteryInfoUnavailableException();
        }

        return ret;

	
	}

	/**
     * Returns the total, last, or current screen on time in microseconds.
     *
     * @param batteryRealtime the battery realtime in microseconds (@see computeBatteryRealtime).
     * @param iStatsType one of STATS_TOTAL, STATS_LAST, or STATS_CURRENT.
     */
    public Long getScreenOnTime(long batteryRealtime, int iStatsType) throws BatteryInfoUnavailableException
	{
    	Long ret = new Long(0);

        try
        {
          //Parameters Types
          @SuppressWarnings("rawtypes")
          Class[] paramTypes= new Class[2];
          paramTypes[0]= long.class;
          paramTypes[1]= int.class;          

          @SuppressWarnings("unchecked")
		  Method method = m_ClassDefinition.getMethod("getScreenOnTime", paramTypes);

          //Parameters
          Object[] params= new Object[2];
          params[0]= new Long(batteryRealtime);
          params[1]= new Integer(iStatsType);

          ret= (Long) method.invoke(m_Instance, params);

        }
        catch( IllegalArgumentException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            ret = new Long(0);
            throw new BatteryInfoUnavailableException();
        }

        return ret;

	
	}

	/**
     * Returns if phone is on battery.
     *
     */
    public boolean getIsOnBattery() throws BatteryInfoUnavailableException
	{
    	boolean ret = true;

        try
        {
          //Parameters Types
          @SuppressWarnings("rawtypes")
          Class[] paramTypes= new Class[2];
          paramTypes[0]= long.class;
          paramTypes[1]= int.class;          

          @SuppressWarnings("unchecked")
		  Method method = m_ClassDefinition.getMethod("getIsOnBattery", paramTypes);


          ret= (Boolean) method.invoke(m_Instance);

        }
        catch( IllegalArgumentException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            ret = true;
            throw new BatteryInfoUnavailableException();
        }

        return ret;

	
	}

	/**
     * Returns the total, last, or current phone on time in microseconds.
     *
     * @param batteryRealtime the battery realtime in microseconds (@see computeBatteryRealtime).
     * @param iStatsType one of STATS_TOTAL, STATS_LAST, or STATS_CURRENT.
     */
    public Long getPhoneOnTime(long batteryRealtime, int iStatsType) throws BatteryInfoUnavailableException
	{
    	Long ret = new Long(0);

        try
        {
          //Parameters Types
          @SuppressWarnings("rawtypes")
          Class[] paramTypes= new Class[2];
          paramTypes[0]= long.class;
          paramTypes[1]= int.class;          

          @SuppressWarnings("unchecked")
		  Method method = m_ClassDefinition.getMethod("getPhoneOnTime", paramTypes);

          //Parameters
          Object[] params= new Object[2];
          params[0]= new Long(batteryRealtime);
          params[1]= new Integer(iStatsType);

          ret= (Long) method.invoke(m_Instance, params);

        }
        catch( IllegalArgumentException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            ret = new Long(0);
            throw new BatteryInfoUnavailableException();
        }

        return ret;

	
	}

	/**
     * Returns the total, last, or current wifi on time in microseconds.
     *
     * @param batteryRealtime the battery realtime in microseconds (@see computeBatteryRealtime).
     * @param iStatsType one of STATS_TOTAL, STATS_LAST, or STATS_CURRENT.
     */
    public Long getWifiOnTime(long batteryRealtime, int iStatsType) throws BatteryInfoUnavailableException
	{
    	Long ret = new Long(0);

        try
        {
          //Parameters Types
          @SuppressWarnings("rawtypes")
          Class[] paramTypes= new Class[2];
          paramTypes[0]= long.class;
          paramTypes[1]= int.class;          

          @SuppressWarnings("unchecked")
		  Method method = m_ClassDefinition.getMethod("getWifiOnTime", paramTypes);

          //Parameters
          Object[] params= new Object[2];
          params[0]= new Long(batteryRealtime);
          params[1]= new Integer(iStatsType);

          ret= (Long) method.invoke(m_Instance, params);
          
          if (CommonLogSettings.DEBUG)
          {
        	  Log.i(TAG, "getWifiOnTime with params " + params[0] + " and " + params[1] +  " returned " + ret);
          }

        }
        catch( IllegalArgumentException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            ret = new Long(0);
            throw new BatteryInfoUnavailableException();
        }

        return ret;

	
	}

	/**
     * Returns the total, last, or current wifi on time in microseconds.
     *
     * @param batteryRealtime the battery realtime in microseconds (@see computeBatteryRealtime).
     * @param iStatsType one of STATS_TOTAL, STATS_LAST, or STATS_CURRENT.
     */
    public Long getGlobalWifiRunningTime(long batteryRealtime, int iStatsType) throws BatteryInfoUnavailableException
	{
    	Long ret = new Long(0);

        try
        {
          //Parameters Types
          @SuppressWarnings("rawtypes")
          Class[] paramTypes= new Class[2];
          paramTypes[0]= long.class;
          paramTypes[1]= int.class;          

          @SuppressWarnings("unchecked")
		  Method method = m_ClassDefinition.getMethod("getGlobalWifiRunningTime", paramTypes);

          //Parameters
          Object[] params= new Object[2];
          params[0]= new Long(batteryRealtime);
          params[1]= new Integer(iStatsType);

          ret= (Long) method.invoke(m_Instance, params);
          
          if (CommonLogSettings.DEBUG)
          {
        	  Log.i(TAG, "getGlobalWifiRunningTime with params " + params[0] + " and " + params[1] +  " returned " + ret);
          }

        }
        catch( IllegalArgumentException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            ret = new Long(0);
            throw new BatteryInfoUnavailableException();
        }

        return ret;

	
	}

	/**
     * Returns the total, last, or current wifi running time in microseconds.
     *
     * @param batteryRealtime the battery realtime in microseconds (@see computeBatteryRealtime).
     * @param iStatsType one of STATS_TOTAL, STATS_LAST, or STATS_CURRENT.
     */
    public Long getWifiRunningTime(Context context, long batteryRealtime, int iStatsType) throws BatteryInfoUnavailableException
	{
    	Long ret = new Long(0);

    	this.collectUidStats();
		if (m_uidStats != null)
		{
	        try
	        {
				
				ClassLoader cl = context.getClassLoader();
				@SuppressWarnings("rawtypes")
				Class iBatteryStatsUid = cl.loadClass("com.android.internal.os.BatteryStatsImpl$Uid");

				int NU = m_uidStats.size();
		        for (int iu = 0; iu < NU; iu++)
		        {
		        	// Object is an instance of BatteryStats.Uid
		            Object myUid = m_uidStats.valueAt(iu);
	            
					@SuppressWarnings("rawtypes")
		        	Class[] paramTypes= new Class[2];
		        	paramTypes[0]= long.class;
		        	paramTypes[1]= int.class;          
	
		        	@SuppressWarnings("unchecked")
		        	Method method = iBatteryStatsUid.getMethod("getWifiRunningTime", paramTypes);
	
		        	//Parameters
		        	Object[] params= new Object[2];
		        	params[0]= new Long(batteryRealtime);
		        	params[1]= new Integer(iStatsType);
		        	
		        	ret += (Long) method.invoke(myUid, params);
		   
		        	if (CommonLogSettings.DEBUG)
		        	{
		        		Log.i(TAG, "getWifiRunningTime with params " + params[0] + " and " + params[1] + " returned " + ret);
		        	}
		        	
	    	
		        }
	        }
	        catch( IllegalArgumentException e )
	        {
	        	Log.e(TAG, "getWifiRunning threw an IllegalArgumentException: " + e.getMessage());
	            throw e;
	        }
	        catch( Exception e )
	        {
	        	Log.e(TAG, "getWifiRunning threw an Exception: " + e.getMessage());
	            ret = new Long(0);
	            throw new BatteryInfoUnavailableException();
	        }
		}
        return ret;
	}

	/**
     * Returns the total, last, or current wifi lock time in microseconds.
     *
     * @param batteryRealtime the battery realtime in microseconds (@see computeBatteryRealtime).
     * @param iStatsType one of STATS_TOTAL, STATS_LAST, or STATS_CURRENT.
     */
    public Long getFullWifiLockTime(Context context, long batteryRealtime, int iStatsType) throws BatteryInfoUnavailableException
	{
    	Long ret = new Long(0);

    	this.collectUidStats();
		if (m_uidStats != null)
		{
	        try
	        {
				
				ClassLoader cl = context.getClassLoader();
				@SuppressWarnings("rawtypes")
				Class iBatteryStatsUid = cl.loadClass("com.android.internal.os.BatteryStatsImpl$Uid");

				int NU = m_uidStats.size();
		        for (int iu = 0; iu < NU; iu++)
		        {
		        	// Object is an instance of BatteryStats.Uid
		            Object myUid = m_uidStats.valueAt(iu);
	            
					@SuppressWarnings("rawtypes")
		        	Class[] paramTypes= new Class[2];
		        	paramTypes[0]= long.class;
		        	paramTypes[1]= int.class;          
	
		        	@SuppressWarnings("unchecked")
		        	Method method = iBatteryStatsUid.getMethod("getFullWifiLockTime", paramTypes);
	
		        	//Parameters
		        	Object[] params= new Object[2];
		        	params[0]= new Long(batteryRealtime);
		        	params[1]= new Integer(iStatsType);
	
		        	ret += (Long) method.invoke(myUid, params);
	    	
		        }
	        }
	        catch( IllegalArgumentException e )
	        {
	            throw e;
	        }
	        catch( Exception e )
	        {
	            ret = new Long(0);
	            throw new BatteryInfoUnavailableException();
	        }
		}
        return ret;
	}

	/**
     * Returns the total, last, or current wifi scanning time in microseconds.
     *
     * @param batteryRealtime the battery realtime in microseconds (@see computeBatteryRealtime).
     * @param iStatsType one of STATS_TOTAL, STATS_LAST, or STATS_CURRENT.
     */
    public Long getScanWifiLockTime(Context context, long batteryRealtime, int iStatsType) throws BatteryInfoUnavailableException
	{
    	Long ret = new Long(0);

    	this.collectUidStats();
		if (m_uidStats != null)
		{
	        try
	        {
				
				ClassLoader cl = context.getClassLoader();
				@SuppressWarnings("rawtypes")
				Class iBatteryStatsUid = cl.loadClass("com.android.internal.os.BatteryStatsImpl$Uid");

				int NU = m_uidStats.size();
		        for (int iu = 0; iu < NU; iu++)
		        {
		        	// Object is an instance of BatteryStats.Uid
		            Object myUid = m_uidStats.valueAt(iu);
	            
					@SuppressWarnings("rawtypes")
		        	Class[] paramTypes= new Class[2];
		        	paramTypes[0]= long.class;
		        	paramTypes[1]= int.class;          
	
		        	@SuppressWarnings("unchecked")
		        	Method method = iBatteryStatsUid.getMethod("getScanWifiLockTime", paramTypes);
	
		        	//Parameters
		        	Object[] params= new Object[2];
		        	params[0]= new Long(batteryRealtime);
		        	params[1]= new Integer(iStatsType);
	
		        	ret += (Long) method.invoke(myUid, params);
	    	
		        }
	        }
	        catch( IllegalArgumentException e )
	        {
	            throw e;
	        }
	        catch( Exception e )
	        {
	            ret = new Long(0);
	            throw new BatteryInfoUnavailableException();
	        }
		}
        return ret;
	}

	/**
     * Returns the total, last, or current wifi multicast time in microseconds.
     *
     * @param batteryRealtime the battery realtime in microseconds (@see computeBatteryRealtime).
     * @param iStatsType one of STATS_TOTAL, STATS_LAST, or STATS_CURRENT.
     */
    public Long getWifiMulticastTime(Context context, long batteryRealtime, int iStatsType) throws BatteryInfoUnavailableException
	{
    	Long ret = new Long(0);

    	this.collectUidStats();
		if (m_uidStats != null)
		{
	        try
	        {
				
				ClassLoader cl = context.getClassLoader();
				@SuppressWarnings("rawtypes")
				Class iBatteryStatsUid = cl.loadClass("com.android.internal.os.BatteryStatsImpl$Uid");

				int NU = m_uidStats.size();
		        for (int iu = 0; iu < NU; iu++)
		        {
		        	// Object is an instance of BatteryStats.Uid
		            Object myUid = m_uidStats.valueAt(iu);
	            
					@SuppressWarnings("rawtypes")
		        	Class[] paramTypes= new Class[2];
		        	paramTypes[0]= long.class;
		        	paramTypes[1]= int.class;          
	
		        	@SuppressWarnings("unchecked")
		        	Method method = iBatteryStatsUid.getMethod("getWifiMulticastTime", paramTypes);
	
		        	//Parameters
		        	Object[] params= new Object[2];
		        	params[0]= new Long(batteryRealtime);
		        	params[1]= new Integer(iStatsType);
	
		        	ret += (Long) method.invoke(myUid, params);
	    	
		        }
	        }
	        catch( IllegalArgumentException e )
	        {
	            throw e;
	        }
	        catch( Exception e )
	        {
	            ret = new Long(0);
	            throw new BatteryInfoUnavailableException();
	        }
		}
        return ret;
	}

	/**
     * Returns the time in microseconds the phone has been running with the given data connection type.
     *
     * @params dataType the given data connection type (@see http://www.netmite.com/android/mydroid/donut/frameworks/base/core/java/android/os/BatteryStats.java)
     * @param batteryRealtime the battery realtime in microseconds (@see computeBatteryRealtime).
     * @param iStatsType one of STATS_TOTAL, STATS_LAST, or STATS_CURRENT.
     */
    public Long getPhoneDataConnectionTime(int dataType, long batteryRealtime, int iStatsType) throws BatteryInfoUnavailableException
	{
    	Long ret = new Long(0);

        try
        {
          //Parameters Types
          @SuppressWarnings("rawtypes")
          Class[] paramTypes= new Class[3];
          paramTypes[0]= int.class;
          paramTypes[1]= long.class;
          paramTypes[2]= int.class;          

          @SuppressWarnings("unchecked")
		  Method method = m_ClassDefinition.getMethod("getPhoneDataConnectionTime", paramTypes);

          //Parameters
          Object[] params= new Object[3];
          params[0]= new Integer(dataType);
          params[1]= new Long(batteryRealtime);
          params[2]= new Integer(iStatsType);

          ret= (Long) method.invoke(m_Instance, params);
	      if (CommonLogSettings.DEBUG)
	      {
	    	  Log.i(TAG, "getPhoneDataConnectionTime with params " + params[0] + ", " + params[1] + "and " + params[2] + " returned " + ret);
	      }
        }
        catch( IllegalArgumentException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            ret = new Long(0);
            throw new BatteryInfoUnavailableException();
        }

        return ret;

	
	}

	/**
     * Returns the time in microseconds the phone has been running with the given signal strength.
     *
     * @params signalStrength the given data connection type (@see http://www.netmite.com/android/mydroid/donut/frameworks/base/core/java/android/os/BatteryStats.java)
     * @param batteryRealtime the battery realtime in microseconds (@see computeBatteryRealtime).
     * @param iStatsType one of STATS_TOTAL, STATS_LAST, or STATS_CURRENT.
     */
    public Long getPhoneSignalStrengthTime(int signalStrength, long batteryRealtime, int iStatsType) throws BatteryInfoUnavailableException
	{
    	Long ret = new Long(0);

        try
        {
          //Parameters Types
          @SuppressWarnings("rawtypes")
          Class[] paramTypes= new Class[3];
          paramTypes[0]= int.class;
          paramTypes[1]= long.class;
          paramTypes[2]= int.class;          

          @SuppressWarnings("unchecked")
		  Method method = m_ClassDefinition.getMethod("getPhoneSignalStrengthTime", paramTypes);

          //Parameters
          Object[] params= new Object[3];
          params[0]= new Integer(signalStrength);
          params[1]= new Long(batteryRealtime);
          params[2]= new Integer(iStatsType);

          ret= (Long) method.invoke(m_Instance, params);
	      if (CommonLogSettings.DEBUG)
	      {
	    	  Log.i(TAG, "getPhoneSignalStrengthTime with params " + params[0] + ", " + params[1] + "and " + params[2] + " returned " + ret);
	      }

        }
        catch( IllegalArgumentException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            ret = new Long(0);
            throw new BatteryInfoUnavailableException();
        }

        return ret;

	
	}

	/**
     * Returns the time in microseconds the screen has been running with the given brightness
     */
    public Long getScreenBrightnessTime(int brightness, long batteryRealtime, int iStatsType) throws BatteryInfoUnavailableException
	{
    	Long ret = new Long(0);

        try
        {
          //Parameters Types
          @SuppressWarnings("rawtypes")
          Class[] paramTypes= new Class[3];
          paramTypes[0]= int.class;
          paramTypes[1]= long.class;
          paramTypes[2]= int.class;          

          @SuppressWarnings("unchecked")
		  Method method = m_ClassDefinition.getMethod("getScreenBrightnessTime", paramTypes);

          //Parameters
          Object[] params= new Object[3];
          params[0]= new Integer(brightness);
          params[1]= new Long(batteryRealtime);
          params[2]= new Integer(iStatsType);

          ret= (Long) method.invoke(m_Instance, params);
	      if (CommonLogSettings.DEBUG)
	      {
	    	  Log.i(TAG, "getScreenBrightnessTime with params " + params[0] + ", " + params[1] + "and " + params[2] + " returned " + ret);
	      }

        }
        catch( IllegalArgumentException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            ret = new Long(0);
            throw new BatteryInfoUnavailableException();
        }

        return ret;

	
	}

    /**
     * Returns the total, last, or current audio on time in microseconds.
     *
     * @param batteryRealtime the battery realtime in microseconds (@see computeBatteryRealtime).
     * @param iStatsType one of STATS_TOTAL, STATS_LAST, or STATS_CURRENT.
     */
    public Long getAudioTurnedOnTime(Context context, long batteryRealtime, int iStatsType) throws BatteryInfoUnavailableException
	{
    	Long ret = new Long(0);

    	this.collectUidStats();
		if (m_uidStats != null)
		{
	        try
	        {
				
				ClassLoader cl = context.getClassLoader();
				@SuppressWarnings("rawtypes")
				Class iBatteryStatsUid = cl.loadClass("com.android.internal.os.BatteryStatsImpl$Uid");

				int NU = m_uidStats.size();
		        for (int iu = 0; iu < NU; iu++)
		        {
		        	// Object is an instance of BatteryStats.Uid
		            Object myUid = m_uidStats.valueAt(iu);
	            
					@SuppressWarnings("rawtypes")
		        	Class[] paramTypes= new Class[2];
		        	paramTypes[0]= long.class;
		        	paramTypes[1]= int.class;          
	
		        	@SuppressWarnings("unchecked")
		        	Method method = iBatteryStatsUid.getMethod("getAudioTurnedOnTime", paramTypes);
	
		        	//Parameters
		        	Object[] params= new Object[2];
		        	params[0]= new Long(batteryRealtime);
		        	params[1]= new Integer(iStatsType);
	
		        	ret += (Long) method.invoke(myUid, params);
	    	
		        }
	        }
	        catch( IllegalArgumentException e )
	        {
	            throw e;
	        }
	        catch( Exception e )
	        {
	            ret = new Long(0);
	            throw new BatteryInfoUnavailableException();
	        }
		}
        return ret;
	}

	/**
     * Returns the total, last, or current video on time in microseconds.
     *
     * @param batteryRealtime the battery realtime in microseconds (@see computeBatteryRealtime).
     * @param iStatsType one of STATS_TOTAL, STATS_LAST, or STATS_CURRENT.
     */
    public Long getVideoTurnedOnTime(Context context, long batteryRealtime, int iStatsType) throws BatteryInfoUnavailableException
	{
    	Long ret = new Long(0);

    	this.collectUidStats();
		if (m_uidStats != null)
		{
	        try
	        {
				
				ClassLoader cl = context.getClassLoader();
				@SuppressWarnings("rawtypes")
				Class iBatteryStatsUid = cl.loadClass("com.android.internal.os.BatteryStatsImpl$Uid");

				int NU = m_uidStats.size();
		        for (int iu = 0; iu < NU; iu++)
		        {
		        	// Object is an instance of BatteryStats.Uid
		            Object myUid = m_uidStats.valueAt(iu);
	            
					@SuppressWarnings("rawtypes")
		        	Class[] paramTypes= new Class[2];
		        	paramTypes[0]= long.class;
		        	paramTypes[1]= int.class;          
	
		        	@SuppressWarnings("unchecked")
		        	Method method = iBatteryStatsUid.getMethod("getVideoTurnedTime", paramTypes);
	
		        	//Parameters
		        	Object[] params= new Object[2];
		        	params[0]= new Long(batteryRealtime);
		        	params[1]= new Integer(iStatsType);
	
		        	ret += (Long) method.invoke(myUid, params);
	    	
		        }
	        }
	        catch( IllegalArgumentException e )
	        {
	            throw e;
	        }
	        catch( Exception e )
	        {
	            ret = new Long(0);
	            throw new BatteryInfoUnavailableException();
	        }
		}
        return ret;
	}


    /**
     * Returns the totalsensor time in microseconds.

     * @param context
     * @param batteryRealtime
     * @param iStatsType
     * @return
     * @throws BatteryInfoUnavailableException
     */
    public Long getSensorOnTime(Context context, long batteryRealtime, int iStatsType) throws BatteryInfoUnavailableException
	{
    	Long ret = new Long(0);
    	
    	this.collectUidStats();
		if (m_uidStats != null)
		{
	        try
	        {
				
				ClassLoader cl = context.getClassLoader();
				@SuppressWarnings("rawtypes")
				Class iBatteryStatsUid = cl.loadClass("com.android.internal.os.BatteryStatsImpl$Uid");

				int NU = m_uidStats.size();
		        for (int iu = 0; iu < NU; iu++)
		        {
		        	// Object is an instance of BatteryStats.Uid
		            Object myUid = m_uidStats.valueAt(iu);
		            
		            Method methodGetUid	= iBatteryStatsUid.getMethod("getUid");
					Integer uid 		= (Integer) methodGetUid.invoke(myUid);
	            	
		        	@SuppressWarnings("unchecked")
		        	Method methodGetSensorStats = iBatteryStatsUid.getMethod("getSensorStats");

	        		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
	        		{
						// call public SparseArray<? extends BatteryStats.Uid.Sensor> getSensorStats()
						SparseArray<? extends Object> sensorStats = (SparseArray<? extends Object>)  methodGetSensorStats.invoke(myUid);
						
						if (sensorStats.size() > 0)
						{
						    for (int i = 0; i < sensorStats.size(); i++)
						    {
							    // Object is a BatteryStatsTypes.Uid.Proc
							    Object sensor = sensorStats.valueAt(i);
								@SuppressWarnings("rawtypes")
								Class batteryStatsUidSensor = cl.loadClass("com.android.internal.os.BatteryStatsImpl$Uid$Sensor");
	
								Method methodGetSensorTime = batteryStatsUidSensor.getMethod("getSensorTime");
								Object timer = methodGetSensorTime.invoke(sensor);
								
								Method methodGetHandle = batteryStatsUidSensor.getMethod("getHandle");
								Integer handle = (Integer) methodGetHandle.invoke(sensor);
								
								Class batteryStatsUidTimer = cl.loadClass("com.android.internal.os.BatteryStatsImpl$StopwatchTimer");
	
								//Parameters Types
								@SuppressWarnings("rawtypes")
								Class[] paramsTypesGetTotalTimeLocked= new Class[1];
								paramsTypesGetTotalTimeLocked[0]= long.class;
	
								// method is protected so we must make it accessible
								Method computeRunTimeLocked = batteryStatsUidTimer.getDeclaredMethod("computeRunTimeLocked", paramsTypesGetTotalTimeLocked);
								computeRunTimeLocked.setAccessible(true);
	
								
					        	//Parameters
					        	Object[] params= new Object[1];
					        	params[0]= new Long(batteryRealtime);
	
								// call public long getTotalTimeLocked(long elapsedRealtimeUs, int which)
					        	Long value = (Long) computeRunTimeLocked.invoke(timer, params);
					        	ret += value;
                                if (CommonLogSettings.DEBUG)
                                {
                                    Log.i("BBS.Sensors",
                                            "UID=" + uid
                                                    + ", Sensor=" + decodeSensor(handle) + " (" + handle + ") "
                                                    + ", time=" + DateUtils.formatDuration((long) value / 1000) + " (" + value + ")");
                                }
						    }
						}
	        		}
	        		else
	        		{
						// call public Map<Integer, ? extends BatteryStats.Uid.Sensor> getSensorStats()
						Map<Integer, ? extends Object> sensorStats = (Map<Integer, ? extends Object>)  methodGetSensorStats.invoke(myUid);
						
						if (sensorStats.size() > 0)
						{
					        // Map of String, BatteryStats.Uid.Wakelock
				            for (Map.Entry<Integer, ? extends Object> sensorEntry : sensorStats.entrySet())
				            {
				            	Object sensor = sensorEntry.getValue();

								@SuppressWarnings("rawtypes")
								Class batteryStatsUidSensor = cl.loadClass("com.android.internal.os.BatteryStatsImpl$Uid$Sensor");
	
								Method methodGetSensorTime = batteryStatsUidSensor.getMethod("getSensorTime");
								Object timer = methodGetSensorTime.invoke(sensor);
								
								Method methodGetHandle = batteryStatsUidSensor.getMethod("getHandle");
								Integer handle = (Integer) methodGetHandle.invoke(sensor);
								
								Class batteryStatsUidTimer = cl.loadClass("com.android.internal.os.BatteryStatsImpl$StopwatchTimer");
	
								//Parameters Types
								@SuppressWarnings("rawtypes")
								Class[] paramsTypesGetTotalTimeLocked= new Class[1];
								paramsTypesGetTotalTimeLocked[0]= long.class;
	
								// method is protected so we must make it accessible
								Method computeRunTimeLocked = batteryStatsUidTimer.getDeclaredMethod("computeRunTimeLocked", paramsTypesGetTotalTimeLocked);
								computeRunTimeLocked.setAccessible(true);
	
								
					        	//Parameters
					        	Object[] params= new Object[1];
					        	params[0]= new Long(batteryRealtime);
	
								// call public long getTotalTimeLocked(long elapsedRealtimeUs, int which)
					        	Long value = (Long) computeRunTimeLocked.invoke(timer, params);
					        	ret += value;
                                if (CommonLogSettings.DEBUG)
                                {
                                    Log.i("BBS.Sensors",
                                            "UID=" + uid
                                                    + ", Sensor=" + decodeSensor(handle) + " (" + handle + ") "
                                                    + ", time=" + DateUtils.formatDuration((long) value / 1000) + " (" + value + ")");
                                }
						    }
						}
	        			
	        		}
		        }
	        }
	        catch( IllegalArgumentException e )
	        {
	            throw e;
	        }
	        catch( Exception e )
	        {
	            ret = new Long(0);
	            throw new BatteryInfoUnavailableException();
	        }
		}
        return ret;
	}
    
    /**
	 * Returns the sensor stats.
	
	 * @param context
	 * @param batteryRealtime
	 * @param iStatsType
	 * @return
	 * @throws BatteryInfoUnavailableException
	 */
	@SuppressLint("NewApi")
	public ArrayList<SensorUsage> getSensorStats(Context context, long batteryRealtime, int iStatsType) throws BatteryInfoUnavailableException
	{
		ArrayList<SensorUsage> myRet = new ArrayList<SensorUsage>(); 
	
		this.collectUidStats();
		if (m_uidStats != null)
		{
	        try
	        {
				
				ClassLoader cl = context.getClassLoader();
				@SuppressWarnings("rawtypes")
				Class iBatteryStatsUid = cl.loadClass("com.android.internal.os.BatteryStatsImpl$Uid");
	
				int NU = m_uidStats.size();
		        for (int iu = 0; iu < NU; iu++)
		        {
		        	// Object is an instance of BatteryStats.Uid
		            Object myUid = m_uidStats.valueAt(iu);
		            
		            Method methodGetUid	= iBatteryStatsUid.getMethod("getUid");
					Integer uid 		= (Integer) methodGetUid.invoke(myUid);
	            	
		        	@SuppressWarnings("unchecked")
		        	Method methodGetSensorStats = iBatteryStatsUid.getMethod("getSensorStats");
					
		        	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		        	{
						// call public SparseArray<? extends BatteryStats.Uid.Sensor> getSensorStats()
						SparseArray<? extends Object> sensorStats = (SparseArray<? extends Object>)  methodGetSensorStats.invoke(myUid);
						Long uidTotalSensorTime = 0L;
						
						if (sensorStats.size() > 0)
						{
							ArrayList<SensorUsageItem> myItems = new ArrayList<SensorUsageItem>(); 
							
						    for (int i = 0; i < sensorStats.size(); i++)
						    {
							    // Object is a BatteryStatsTypes.Uid.Proc
							    Object sensor = sensorStats.valueAt(i);
								@SuppressWarnings("rawtypes")
								Class batteryStatsUidSensor = cl.loadClass("com.android.internal.os.BatteryStatsImpl$Uid$Sensor");
		
								Method methodGetSensorTime = batteryStatsUidSensor.getMethod("getSensorTime");
								Object timer = methodGetSensorTime.invoke(sensor);
								
								Method methodGetHandle = batteryStatsUidSensor.getMethod("getHandle");
								Integer handle = (Integer) methodGetHandle.invoke(sensor);
								
								Class batteryStatsUidTimer = cl.loadClass("com.android.internal.os.BatteryStatsImpl$StopwatchTimer");
		
								//Parameters Types
								@SuppressWarnings("rawtypes")
								Class[] paramsTypesGetTotalTimeLocked= new Class[1];
								paramsTypesGetTotalTimeLocked[0]= long.class;
		
								// method is protected so we must make it accessible
								Method computeRunTimeLocked = batteryStatsUidTimer.getDeclaredMethod("computeRunTimeLocked", paramsTypesGetTotalTimeLocked);
								computeRunTimeLocked.setAccessible(true);
		
								
					        	//Parameters
					        	Object[] params= new Object[1];
					        	params[0]= new Long(batteryRealtime);
		
								// call public long getTotalTimeLocked(long elapsedRealtimeUs, int which)
					        	Long value = (Long) computeRunTimeLocked.invoke(timer, params);
					        	uidTotalSensorTime += value;
                                if (CommonLogSettings.DEBUG)
                                {
                                    Log.i("BBS.Sensors",
                                            "UID=" + uid
                                                    + ", Sensor=" + decodeSensor(handle) + " (" + handle + ") "
                                                    + ", time=" + DateUtils.formatDuration((long) value / 1000) + " (" + value + ")");
                                }
					        	Sensor lookup = findSensor(context, handle);
					        	
					        	String sensorText = "";
					        	
					        	if (lookup == null)
					        	{
					        		sensorText = "Unknown";
					        	}
					        	else
					        	{
					        		// we try to get the most info out of the API
					        		if (Build.VERSION.SDK_INT >= 21)
					        		{
					        			sensorText = lookup.getName() + "(" + handle+ "), wakeup=" + lookup.isWakeUpSensor();
					        		}
					        		else
					        		{
					        			sensorText = lookup.getName();
					        		}
		
					        		
					        	}
					        	SensorUsageItem myItem = new SensorUsageItem(value/1000, sensorText, handle);
					        	myItems.add(myItem);
						    }
						    SensorUsage myData = new SensorUsage(uidTotalSensorTime.longValue()/1000);
							// try resolving names
							UidInfo myInfo = UidNameResolver.getInstance().getNameForUid(uid);
							myData.setUidInfo(myInfo);
							myData.setItems(myItems);
							myRet.add(myData);
							
		
						}
		        	}
		        	else
		        	{
		        		Long uidTotalSensorTime = 0L;
						// call public Map<Integer, ? extends BatteryStats.Uid.Sensor> getSensorStats()
						Map<Integer, ? extends Object> sensorStats = (Map<Integer, ? extends Object>)  methodGetSensorStats.invoke(myUid);
						
						if (sensorStats.size() > 0)
						{
							ArrayList<SensorUsageItem> myItems = new ArrayList<SensorUsageItem>(); 

							// Map of String, BatteryStats.Uid.Wakelock
				            for (Map.Entry<Integer, ? extends Object> sensorEntry : sensorStats.entrySet())
				            {
				            	Object sensor = sensorEntry.getValue();
						
								@SuppressWarnings("rawtypes")
								Class batteryStatsUidSensor = cl.loadClass("com.android.internal.os.BatteryStatsImpl$Uid$Sensor");
		
								Method methodGetSensorTime = batteryStatsUidSensor.getMethod("getSensorTime");
								Object timer = methodGetSensorTime.invoke(sensor);
								
								Method methodGetHandle = batteryStatsUidSensor.getMethod("getHandle");
								Integer handle = (Integer) methodGetHandle.invoke(sensor);
								
								Class batteryStatsUidTimer = cl.loadClass("com.android.internal.os.BatteryStatsImpl$StopwatchTimer");
		
								//Parameters Types
								@SuppressWarnings("rawtypes")
								Class[] paramsTypesGetTotalTimeLocked= new Class[1];
								paramsTypesGetTotalTimeLocked[0]= long.class;
		
								// method is protected so we must make it accessible
								Method computeRunTimeLocked = batteryStatsUidTimer.getDeclaredMethod("computeRunTimeLocked", paramsTypesGetTotalTimeLocked);
								computeRunTimeLocked.setAccessible(true);
		
								
					        	//Parameters
					        	Object[] params= new Object[1];
					        	params[0]= new Long(batteryRealtime);
		
								// call public long getTotalTimeLocked(long elapsedRealtimeUs, int which)
					        	Long value = (Long) computeRunTimeLocked.invoke(timer, params);
					        	uidTotalSensorTime += value;
                                if (CommonLogSettings.DEBUG)
                                {
                                    Log.i("BBS.Sensors",
                                            "UID=" + uid
                                                    + ", Sensor=" + decodeSensor(handle) + " (" + handle + ") "
                                                    + ", time=" + DateUtils.formatDuration((long) value / 1000) + " (" + value + ")");
                                }
					        	Sensor lookup = findSensor(context, handle);
					        	
					        	String sensorText = "";
					        	
					        	if (lookup == null)
					        	{
					        		sensorText = "Unknown";
					        	}
					        	else
					        	{
					        		// we try to get the most info out of the API
					        		if (Build.VERSION.SDK_INT >= 21)
					        		{
					        			sensorText = lookup.getName() + "(" + handle+ "), wakeup=" + lookup.isWakeUpSensor();
					        		}
					        		else
					        		{
					        			sensorText = lookup.getName();
					        		}
		
					        		
					        	}
					        	SensorUsageItem myItem = new SensorUsageItem(value/1000, sensorText, handle);
					        	myItems.add(myItem);
						    }
						    SensorUsage myData = new SensorUsage(uidTotalSensorTime.longValue()/1000);
							// try resolving names
							UidInfo myInfo = UidNameResolver.getInstance().getNameForUid(uid);
							myData.setUidInfo(myInfo);
							myData.setItems(myItems);
							myRet.add(myData);
							
		
						}
		        		
		        	}
		        }
	        }
	        catch( IllegalArgumentException e )
	        {
	            throw e;
	        }
	        catch( Exception e )
	        {
	            myRet = new ArrayList<SensorUsage>();
	            throw new BatteryInfoUnavailableException();
	        }
		}
	    return myRet;
	}
	
	/**
     * Decodes the sensor handle using the constants from https://developer.android.com/reference/android/hardware/Sensor.html#STRING_TYPE_ACCELEROMETER
     * @param handle the sensor handle number
     * @return a string describing the sensor
     */
    String decodeSensor(int handle)
    {
    	switch (handle)
    	{
    		case Sensor.TYPE_ACCELEROMETER: return "Accelerometer";
    		case Sensor.TYPE_AMBIENT_TEMPERATURE: return "Ambient Temperatur";
    		case Sensor.TYPE_GAME_ROTATION_VECTOR: return "Game Rotation Vector";
    		case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR: return "Geomagnetic rotation Vector";
    		case Sensor.TYPE_GRAVITY: return "Gravity";
    		case Sensor.TYPE_GYROSCOPE: return "Gyroscope";
    		case Sensor.TYPE_GYROSCOPE_UNCALIBRATED: return "Gyroscope Uncalibrated";
    		case Sensor.TYPE_HEART_RATE: return "Heart Rate";
    		case Sensor.TYPE_LIGHT: return "Light";
    		case Sensor.TYPE_LINEAR_ACCELERATION: return "Linear Acceleration";
    		case Sensor.TYPE_MAGNETIC_FIELD: return "Megnetic Field";
    		case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED: return "Magnetic Field Uncalibrated";
    		case Sensor.TYPE_ORIENTATION: return "Orientation";
    		case Sensor.TYPE_PRESSURE: return "Pressure";
    		case Sensor.TYPE_PROXIMITY: return "Proximity";
    		case Sensor.TYPE_RELATIVE_HUMIDITY: return "Relative Humidity";
    		case Sensor.TYPE_ROTATION_VECTOR: return "Rotation Vector";
    		case Sensor.TYPE_SIGNIFICANT_MOTION: return "Significant Motion";
    		case Sensor.TYPE_STEP_COUNTER: return "Step Counter";
    		case Sensor.TYPE_STEP_DETECTOR: return "Step Detection";
    		case Sensor.TYPE_TEMPERATURE: return "Temperature";
    		case -10000: return "GPS";
    		
    		default: return "Unknown";
    		
    		
    	}
    }
    
    @SuppressLint("NewApi")
	Sensor findSensor(Context ctx, int handle)
    {
    	String TAG = "BBS.Sensors";
    	Sensor retVal = null;
    	
    	// Enumerate all sensors
    	final SensorManager sensorManager = (SensorManager)ctx.getSystemService(Context.SENSOR_SERVICE);
    	List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);

    	if ( (sensors == null) || (sensors.size() == 0)) return null;
    	for (int i=0; i < sensors.size(); i++)
    	{
    		Sensor sensor = sensors.get(i);
    		Method methodGetHandle;
    		
    		int hhandle = -1;
			try
			{
				methodGetHandle = sensor.getClass().getDeclaredMethod("getHandle");
	    		methodGetHandle.setAccessible(true);
	    		hhandle = ((Integer) methodGetHandle.invoke(sensor)).intValue();

			} catch (Exception e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		
			if (hhandle == handle)
			{
				retVal = sensor;
				return retVal;
			}

			if (CommonLogSettings.DEBUG)
            {
                if (Build.VERSION.SDK_INT >= 21)
                {
                    Log.i(TAG, "name=" + sensor.getName() + ", handle=" + handle + ", wakeup=" + sensor.isWakeUpSensor() + ", type=" + sensor.getStringType());
                } else
                {
                    Log.i(TAG, "name=" + sensor.getName() + ", handle=" + handle);
                }
            }
    	}
    	
    	return null;
    }

    /**
     * Returns the total GPS time in microseconds.

     * @param context
     * @param batteryRealtime
     * @param iStatsType
     * @return
     * @throws BatteryInfoUnavailableException
     */
    public Long getGpsOnTime(Context context, long batteryRealtime, int iStatsType) throws BatteryInfoUnavailableException
	{
    	Long ret = new Long(0);

    	this.collectUidStats();
		if (m_uidStats != null)
		{
	        try
	        {
				
				ClassLoader cl = context.getClassLoader();
				@SuppressWarnings("rawtypes")
				Class iBatteryStatsUid = cl.loadClass("com.android.internal.os.BatteryStatsImpl$Uid");

				int NU = m_uidStats.size();
		        for (int iu = 0; iu < NU; iu++)
		        {
		        	// Object is an instance of BatteryStats.Uid
		            Object myUid = m_uidStats.valueAt(iu);
		            
		            Method methodGetUid	= iBatteryStatsUid.getMethod("getUid");
					Integer uid 		= (Integer) methodGetUid.invoke(myUid);
	            	
		        	@SuppressWarnings("unchecked")
		        	Method methodGetSensorStats = iBatteryStatsUid.getMethod("getSensorStats");
					
					// call public SparseArray<? extends BatteryStats.Uid.Sensor> getSensorStats()
					SparseArray<? extends Object> sensorStats = (SparseArray<? extends Object>)  methodGetSensorStats.invoke(myUid);
					
					if (sensorStats.size() > 0)
					{
					    for (int i = 0; i < sensorStats.size(); i++)
					    {
						    // Object is a BatteryStatsTypes.Uid.Proc
						    Object sensor = sensorStats.valueAt(i);
							@SuppressWarnings("rawtypes")
							Class batteryStatsUidSensor = cl.loadClass("com.android.internal.os.BatteryStatsImpl$Uid$Sensor");

							Method methodGetSensorTime = batteryStatsUidSensor.getMethod("getSensorTime");
							Object timer = methodGetSensorTime.invoke(sensor);
							
							Method methodGetHandle = batteryStatsUidSensor.getMethod("getHandle");
							Integer handle = (Integer) methodGetHandle.invoke(sensor);
							
							// hack -> see https://developer.android.com/reference/android/hardware/Sensor.html
							// GPS is not defined in the HAL but has a constant value of -10000
							if (handle == -10000)
							{
								Class batteryStatsUidTimer = cl.loadClass("com.android.internal.os.BatteryStatsImpl$StopwatchTimer");
	
								//Parameters Types
								@SuppressWarnings("rawtypes")
								Class[] paramsTypesGetTotalTimeLocked= new Class[1];
								paramsTypesGetTotalTimeLocked[0]= long.class;
	
								// method is protected so we must make it accessible
								Method computeRunTimeLocked = batteryStatsUidTimer.getDeclaredMethod("computeRunTimeLocked", paramsTypesGetTotalTimeLocked);
								computeRunTimeLocked.setAccessible(true);
	
								
					        	//Parameters
					        	Object[] params= new Object[1];
					        	params[0]= new Long(batteryRealtime);
	
								// call public long getTotalTimeLocked(long elapsedRealtimeUs, int which)
					        	Long value = (Long) computeRunTimeLocked.invoke(timer, params);
					        	ret += value;
					        	
					        	Log.i("Sensors", "UID=" + uid + ", Sensor=" + handle + ", time=" + value);
							}
					    }
					}
		        }
	        }
	        catch( IllegalArgumentException e )
	        {
	            throw e;
	        }
	        catch( Exception e )
	        {
	            ret = new Long(0);
	            throw new BatteryInfoUnavailableException();
	        }
		}
        return ret;
	}

    /**
     * Returns the total, last, or current bluetooth on time in microseconds.
     *
     * @param batteryRealtime the battery realtime in microseconds (@see computeBatteryRealtime).
     * @param iStatsType one of STATS_TOTAL, STATS_LAST, or STATS_CURRENT.
     */
    public Long getBluetoothOnTime(long batteryRealtime, int iStatsType) throws BatteryInfoUnavailableException
	{
    	Long ret = new Long(0);

        try
        {
          //Parameters Types
          @SuppressWarnings("rawtypes")
          Class[] paramTypes= new Class[2];
          paramTypes[0]= long.class;
          paramTypes[1]= int.class;          

          @SuppressWarnings("unchecked")
		  Method method = m_ClassDefinition.getMethod("getBluetoothOnTime", paramTypes);

          //Parameters
          Object[] params= new Object[2];
          params[0]= new Long(batteryRealtime);
          params[1]= new Integer(iStatsType);

          ret= (Long) method.invoke(m_Instance, params);

        }
        catch( IllegalArgumentException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            ret = new Long(0);
            throw new BatteryInfoUnavailableException();
        }

        return ret;

	
	}

    /**
     * Returns the total, last, or current bluetooth on time in microseconds.
     *
     */
    public Long getBluetoothInStateTime(Context ctx, int iStatsType) throws BatteryInfoUnavailableException
	{
    	Long ret = new Long(0);

    	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
    	{
    		Log.e(TAG, "Bluetooth in state time is supported only from Marshmallow");
    		throw new BatteryInfoUnavailableException("Bluetooth in state time is supported only from Marshmallow");
    	}
    	
        try
        {
            if (Build.VERSION.SDK_INT < 24)
            {
                //Parameters Types
                @SuppressWarnings("rawtypes")
                Class[] paramTypes = new Class[2];
                paramTypes[0] = int.class;
                paramTypes[1] = int.class;

                @SuppressWarnings("unchecked")
                Method method = m_ClassDefinition.getMethod("getBluetoothControllerActivity", paramTypes);

                //Parameters
                Object[] paramsIdle = new Object[2];
                paramsIdle[0] = new Integer(BatteryStatsTypes.CONTROLLER_IDLE_TIME);
                paramsIdle[1] = new Integer(iStatsType);

                Object[] paramsRx = new Object[2];
                paramsRx[0] = new Integer(BatteryStatsTypes.CONTROLLER_IDLE_TIME);
                paramsRx[1] = new Integer(iStatsType);

                Object[] paramsTx = new Object[2];
                paramsTx[0] = new Integer(BatteryStatsTypes.CONTROLLER_IDLE_TIME);
                paramsTx[1] = new Integer(iStatsType);

                Long idleTimeMs     = (Long) method.invoke(m_Instance, paramsIdle);
                Long rxTimeMs       = (Long) method.invoke(m_Instance, paramsRx);
                Long txTimeMs       = (Long) method.invoke(m_Instance, paramsTx);

                ret                 = idleTimeMs + txTimeMs + rxTimeMs;

            }
            else
            {
                // we need to sum-up the time. See http://androidxref.com/7.1.2_r36/xref/frameworks/base/core/java/com/android/internal/os/BluetoothPowerCalculator.java#67.
                //         final long idleTimeMs = counter.getIdleTimeCounter().getCountLocked(statsType);
                //        final long rxTimeMs = counter.getRxTimeCounter().getCountLocked(statsType);
                //        final long txTimeMs = counter.getTxTimeCounters()[0].getCountLocked(statsType);
                //        final long totalTimeMs = idleTimeMs + txTimeMs + rxTimeMs;
                //Parameters Types
                Method method = m_ClassDefinition.getMethod("getBluetoothControllerActivity");

                Object counter = (Object) method.invoke(m_Instance);
                // counter is of type BatteryStats.ControllerActivityCounter
                ClassLoader cl = ctx.getClassLoader();
                @SuppressWarnings("rawtypes")
                Class iBatteryStatsControllerActivityCounter = cl.loadClass("com.android.internal.os.BatteryStatsImpl$ControllerActivityCounterImpl");
                Class iBatteryStatsLongSamplingCounter = cl.loadClass("com.android.internal.os.BatteryStatsImpl$LongSamplingCounter");

                Method getIdleTimeCounter = iBatteryStatsControllerActivityCounter.getMethod("getIdleTimeCounter");
                Method getRxTimeCounter = iBatteryStatsControllerActivityCounter.getMethod("getRxTimeCounter");
                Method getTxTimeCounters = iBatteryStatsControllerActivityCounter.getMethod("getTxTimeCounters");

                //Parameters Types
                @SuppressWarnings("rawtypes")
                Class[] paramTypes = new Class[1];
                paramTypes[0] = int.class;

                @SuppressWarnings("unchecked")
                Method getCountLocked = iBatteryStatsLongSamplingCounter.getMethod("getCountLocked", paramTypes);

                //Parameters
                Object[] params = new Object[1];
                params[0] = new Integer(iStatsType);

                Long idleTimeMs = (Long) getCountLocked.invoke(getIdleTimeCounter.invoke(counter), params);
                Long rxTimeMs = (Long) getCountLocked.invoke(getRxTimeCounter.invoke(counter), params);
                Long txTimeMs = (Long) getCountLocked.invoke(((Object[])getTxTimeCounters.invoke(counter))[0], params);
                ret = idleTimeMs + txTimeMs + rxTimeMs;
            }
        }
        catch( IllegalArgumentException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            ret = new Long(0);
            throw new BatteryInfoUnavailableException();
        }

        return ret;

	
	}

    /**
     * Returns the time in doze mode POWERSAVE
     * @param batteryRealtime
     * @param iStatsType
     * @return
     */
    public long getPowerSaveModeEnabledTime(long batteryRealtime, int iStatsType)  throws BatteryInfoUnavailableException
	{
    	Long ret = new Long(0);

        try
        {
          //Parameters Types
          @SuppressWarnings("rawtypes")
          Class[] paramTypes= new Class[2];
          paramTypes[0]= long.class;
          paramTypes[1]= int.class;          

          @SuppressWarnings("unchecked")
		  Method method = m_ClassDefinition.getMethod("getPowerSaveModeEnabledTime", paramTypes);

          //Parameters
          Object[] params= new Object[2];
          params[0]= new Long(batteryRealtime);
          params[1]= new Integer(iStatsType);

          ret= (Long) method.invoke(m_Instance, params);

        }
        catch( IllegalArgumentException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            ret = new Long(0);
            throw new BatteryInfoUnavailableException();
        }

        return ret;

	
	}

    /**
     * Returns the time in doze mode IDLE
     * @param batteryRealtime
     * @param iStatsType
     * @return
     */
    public long getDeviceIdleModeEnabledTime(long batteryRealtime, int iStatsType)  throws BatteryInfoUnavailableException
	{
    	Long ret = new Long(0);

        if (Build.VERSION.SDK_INT < 21)
        {
            Log.e(TAG, "Doze idle time is supported only from Marshmallow");
        }

        if (Build.VERSION.SDK_INT < 24)
        {

            try
            {
                //Parameters Types
                @SuppressWarnings("rawtypes")
                Class[] paramTypes = new Class[2];
                paramTypes[0] = long.class;
                paramTypes[1] = int.class;

                @SuppressWarnings("unchecked")
                Method method = m_ClassDefinition.getMethod("getDeviceIdleModeEnabledTime", paramTypes);

                //Parameters
                Object[] params = new Object[2];
                params[0] = new Long(batteryRealtime);
                params[1] = new Integer(iStatsType);

                ret = (Long) method.invoke(m_Instance, params);

            } catch (IllegalArgumentException e)
            {
                throw e;
            } catch (Exception e)
            {
                ret = new Long(0);
                throw new BatteryInfoUnavailableException();
            }

            return ret;
        }
        else
        {
            try
            {
                //Parameters Types
                @SuppressWarnings("rawtypes")
                Class[] paramTypes = new Class[3];
                paramTypes[0] = int.class;
                paramTypes[1] = long.class;
                paramTypes[2] = int.class;

                @SuppressWarnings("unchecked")
                Method method = m_ClassDefinition.getMethod("getDeviceIdleModeTime", paramTypes);

                //Parameters
                Object[] paramsLight = new Object[3];
                paramsLight[0] = new Integer(BatteryStatsTypes.DEVICE_IDLE_MODE_LIGHT);
                paramsLight[1] = new Long(batteryRealtime);
                paramsLight[2] = new Integer(iStatsType);

                //Parameters
                Object[] paramsDeep = new Object[3];
                paramsDeep[0] = new Integer(BatteryStatsTypes.DEVICE_IDLE_MODE_DEEP);
                paramsDeep[1] = new Long(batteryRealtime);
                paramsDeep[2] = new Integer(iStatsType);

                Long timeLight = (Long) method.invoke(m_Instance, paramsLight);
                Long timeDeep = (Long) method.invoke(m_Instance, paramsDeep);

                ret = timeLight + timeDeep;

            } catch (IllegalArgumentException e)
            {
                throw e;
            } catch (Exception e)
            {
                ret = new Long(0);
                throw new BatteryInfoUnavailableException();
            }

            return ret;

        }
	
	}

    /**
     * Returns the time in doze mode INTERACTIVE
     * @param batteryRealtime
     * @param iStatsType
     * @return
     */
    public long getInteractiveTime(long batteryRealtime, int iStatsType)  throws BatteryInfoUnavailableException
	{
    	Long ret = new Long(0);

        try
        {
          //Parameters Types
          @SuppressWarnings("rawtypes")
          Class[] paramTypes= new Class[2];
          paramTypes[0]= long.class;
          paramTypes[1]= int.class;          

          @SuppressWarnings("unchecked")
		  Method method = m_ClassDefinition.getMethod("getInteractiveTime", paramTypes);

          //Parameters
          Object[] params= new Object[2];
          params[0]= new Long(batteryRealtime);
          params[1]= new Integer(iStatsType);

          ret= (Long) method.invoke(m_Instance, params);

        }
        catch( IllegalArgumentException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            ret = new Long(0);
            throw new BatteryInfoUnavailableException();
        }

        return ret;

	
	}

    /**
     * Returns the time in doze mode IDLING
     * @param batteryRealtime
     * @param iStatsType
     * @return
     */
    public long getDeviceIdlingTime(long batteryRealtime, int iStatsType)  throws BatteryInfoUnavailableException
	{
    	Long ret = new Long(0);

        try
        {
          //Parameters Types
          @SuppressWarnings("rawtypes")
          Class[] paramTypes= new Class[2];
          paramTypes[0]= long.class;
          paramTypes[1]= int.class;          

          @SuppressWarnings("unchecked")
		  Method method = m_ClassDefinition.getMethod("getDeviceIdlingTime", paramTypes);

          //Parameters
          Object[] params= new Object[2];
          params[0]= new Long(batteryRealtime);
          params[1]= new Integer(iStatsType);

          ret= (Long) method.invoke(m_Instance, params);

        }
        catch( IllegalArgumentException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            ret = new Long(0);
            throw new BatteryInfoUnavailableException();
        }

        return ret;

	
	}


    /**
     * Returns the network activity
     * @param batteryRealtime
     * @param iStatsType
     * @return
     */
    public long getNetworkActivityBytes(long batteryRealtime, int iStatsType)  throws BatteryInfoUnavailableException
	{
    	Long ret = new Long(0);

        try
        {
          //Parameters Types
          @SuppressWarnings("rawtypes")
          Class[] paramTypes= new Class[2];
          paramTypes[0]= long.class;
          paramTypes[1]= int.class;          

          @SuppressWarnings("unchecked")
		  Method method = m_ClassDefinition.getMethod("getNetworkActivityBytes", paramTypes);

          //Parameters
          Object[] params= new Object[2];
          params[0]= new Long(batteryRealtime);
          params[1]= new Integer(iStatsType);

          ret= (Long) method.invoke(m_Instance, params);

        }
        catch( IllegalArgumentException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            ret = new Long(0);
            throw new BatteryInfoUnavailableException();
        }

        return ret;

	
	}

    
    /**
     * Returns the current battery percentage level if we are in a discharge cycle, otherwise
     * returns the level at the last plug event.
     */
    public int getDischargeCurrentLevel() throws BatteryInfoUnavailableException
    {
    	int ret = 0;

        try
        {
        	@SuppressWarnings("unchecked")
        	Method method = m_ClassDefinition.getMethod("getDischargeCurrentLevel");

        	Integer oRet = (Integer) method.invoke(m_Instance);
        	ret = oRet.intValue();

        }
        catch( IllegalArgumentException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            ret = 0;
            throw new BatteryInfoUnavailableException();
        }    
        
        return ret;
    }
	
	/**
     * Initalizes the collection of history items
     */
    public boolean startIteratingHistoryLocked() throws BatteryInfoUnavailableException
	{
    	Boolean ret = false;

        try
        {
          @SuppressWarnings("unchecked")
		  Method method = m_ClassDefinition.getMethod("startIteratingHistoryLocked");

          ret= (Boolean) method.invoke(m_Instance);

        }
        catch( IllegalArgumentException e )
        {
        	Log.e(TAG, "An exception occured in startIteratingHistoryLocked(). Message: " + e.getMessage() + ", cause: " + e.getCause().getMessage());
            throw e;
        }
        catch( Exception e )
        {
            ret = false;
            throw new BatteryInfoUnavailableException();
        }

        return ret;

	
	}
    
    /**
     * Initalizes the collection of history items
     */
    public boolean finishIteratingHistoryLocked() throws BatteryInfoUnavailableException
	{
    	Boolean ret = false;

        try
        {
          @SuppressWarnings("unchecked")
		  Method method = m_ClassDefinition.getMethod("finishIteratingHistoryLocked");

          ret= (Boolean) method.invoke(m_Instance);

        }
        catch( IllegalArgumentException e )
        {
        	Log.e(TAG, "An exception occured in finishIteratingHistoryLocked(). Message: " + e.getMessage() + ", cause: " + e.getCause().getMessage());
            throw e;
        }
        catch( Exception e )
        {
            ret = false;
            throw new BatteryInfoUnavailableException();
        }

        return ret;

	
	}
    
	/**
	 * Collect the UidStats using reflection and store them  
	 */
    @SuppressWarnings("unchecked")
	private void collectUidStats()
    {
        try
        {
        	Method method = m_ClassDefinition.getMethod("getUidStats");
        	
        	m_uidStats = (SparseArray<? extends Object>) method.invoke(m_Instance);	
        	
        }
        catch( IllegalArgumentException e )
        {
        	Log.e(TAG, "An exception occured in collectUidStats(). Message: " + e.getMessage() + ", cause: " + e.getCause().getMessage());
            throw e;
        }
        catch( Exception e )
        {
        	m_uidStats = null;
        }    

    }
	
	/**
	 * Obtain the wakelock stats as a list of Wakelocks (@see com.asksven.android.common.privateapiproxies.Wakelock}
	 * @param context a Context
	 * @param iWakeType a type of wakelock @see com.asksven.android.common.privateapiproxies.BatteryStatsTypes 
	 * @param iStatType a type of stat @see com.asksven.android.common.privateapiproxies.BatteryStatsTypes
	 * @return a List of Wakelock s
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<StatElement> getWakelockStats(Context context, int iWakeType, int iStatType, int iWlPctRef) throws Exception
	{
		// type checks
		boolean validTypes = (BatteryStatsTypes.assertValidWakeType(iWakeType)
				&& BatteryStatsTypes.assertValidStatType(iStatType)
				&& BatteryStatsTypes.assertValidWakelockPctRef(iWlPctRef));
		if (!validTypes)
		{
			Log.e(TAG, "Invalid WakeType or StatType");
			throw new Exception("Invalid WakeType of StatType");
		}

		String entropy =  Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID); // this is the best practice described here: https://android-developers.googleblog.com/2011/03/identifying-app-installations.html

        ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		
		this.collectUidStats();
		if (m_uidStats != null)
		{
			long uSecBatteryTime = this.computeBatteryRealtime(SystemClock.elapsedRealtime() * 1000, iStatType);
			long uSecAwakeTime = this.computeBatteryUptime(SystemClock.elapsedRealtime() * 1000, iStatType);
			long uSecScreenOnTime =this.getScreenOnTime(uSecBatteryTime, iStatType);
			 
            try
            {			
				ClassLoader cl = context.getClassLoader();
				@SuppressWarnings("rawtypes")
				Class iBatteryStatsUid = cl.loadClass("com.android.internal.os.BatteryStatsImpl$Uid");
				int NU = m_uidStats.size();
		        for (int iu = 0; iu < NU; iu++)
		        {
		        	// Object is an instance of BatteryStats.Uid
		            Object myUid = m_uidStats.valueAt(iu);
	            
					// Process wake lock usage
					Method methodGetWakelockStats = iBatteryStatsUid.getMethod("getWakelockStats");

					// Map of String, BatteryStats.Uid.Wakelock
					Map<String, ? extends Object> wakelockStats = (Map<String, ? extends Object>)  methodGetWakelockStats.invoke(myUid);
					
					Method methodGetUid	= iBatteryStatsUid.getMethod("getUid");
					Integer uid 		= (Integer) methodGetUid.invoke(myUid);
					
					long wakelockTime = 0;
					int wakelockCount = 0;
							            
			        // Map of String, BatteryStats.Uid.Wakelock
		            for (Map.Entry<String, ? extends Object> wakelockEntry : wakelockStats.entrySet())
		            {
		                // BatteryStats.Uid.Wakelock
		            	Object wakelock = wakelockEntry.getValue();

		            	@SuppressWarnings("rawtypes")
						Class batteryStatsUidWakelock = cl.loadClass("com.android.internal.os.BatteryStatsImpl$Uid$Wakelock");

						//Parameters Types
						@SuppressWarnings("rawtypes")
						Class[] paramTypesGetWakeTime= new Class[1];
						paramTypesGetWakeTime[0]= int.class;    

						Method methodGetWakeTime = batteryStatsUidWakelock.getMethod("getWakeTime", paramTypesGetWakeTime);
						
						
						//Parameters
						Object[] paramsGetWakeTime= new Object[1];

						// Partial wake locks BatteryStatsTypes.WAKE_TYPE_PARTIAL 
						// are the ones that should normally be of interest but
						// WAKE_TYPE_PARTIAL, WAKE_TYPE_FULL, WAKE_TYPE_WINDOW
		                // are possible
						paramsGetWakeTime[0]= Integer.valueOf(iWakeType);
						
						// BatteryStats.Timer
						Object wakeTimer = methodGetWakeTime.invoke(wakelock, paramsGetWakeTime);
						if (wakeTimer != null)
						{
			            	@SuppressWarnings("rawtypes")
							Class iBatteryStatsTimer = cl.loadClass("com.android.internal.os.BatteryStatsImpl$Timer");

							//Parameters Types
							@SuppressWarnings("rawtypes")
							Class[] paramTypesGetTotalTimeLocked= new Class[2];
							paramTypesGetTotalTimeLocked[0]= long.class;
							paramTypesGetTotalTimeLocked[1]= int.class;    

							Method methodGetTotalTimeLocked = iBatteryStatsTimer.getMethod("getTotalTimeLocked", paramTypesGetTotalTimeLocked);
														
							//Parameters
							Object[] paramsGetTotalTimeLocked= new Object[2];
							paramsGetTotalTimeLocked[0]= new Long(uSecBatteryTime);
							paramsGetTotalTimeLocked[1]= Integer.valueOf(iStatType);
							
							Long wake = (Long) methodGetTotalTimeLocked.invoke(wakeTimer, paramsGetTotalTimeLocked);
//							Log.d(TAG, "Wakelocks inner: Process = " + wakelockEntry.getKey() + " wakelock [s] " + wake);
							wakelockTime += wake;

							//Parameters Types
							@SuppressWarnings("rawtypes")
							Class[] paramTypesGetCountLocked= new Class[1];
							paramTypesGetCountLocked[0]= int.class;    

							Method methodGetCountLocked = iBatteryStatsTimer.getMethod("getCountLocked", paramTypesGetCountLocked);
														
							//Parameters
							Object[] paramsGetCountLocked= new Object[1];
							paramsGetCountLocked[0]= new Integer(iStatType);

							Integer count = (Integer) methodGetCountLocked.invoke(wakeTimer, paramsGetCountLocked);
//							Log.d(TAG, "Wakelocks inner: Process = " + wakelockEntry.getKey() + " count " + count);
							wakelockCount += count;

		                }
//						else
//						{
//							Log.d(TAG, "Wakelocks: Process = " + wakelockEntry.getKey() + " with no Timer spotted");
//						}
						// convert so milliseconds
						wakelockTime /= 1000;
						
						long uSec = 0;
						switch (iWlPctRef)
						{
							case 0:
								uSec = uSecBatteryTime;
								break;
							case 1:
								uSec = uSecAwakeTime;
								break;
							case 2:
								uSec = (uSecAwakeTime - uSecScreenOnTime);
								break;
						}
						
						// On L Preview partial wakelocks are expressed in milliseconds
						Wakelock myWl = null;
						if (Build.VERSION.SDK_INT >= 20)
						{
							myWl = new Wakelock(entropy, iWakeType, wakelockEntry.getKey(), wakelockTime, uSec, wakelockCount);
								
						}
						else
						{
							myWl = new Wakelock(entropy, iWakeType, wakelockEntry.getKey(), wakelockTime, uSec / 1000, wakelockCount);
						}

                        myStats.add(myWl);
                        // opt for lazy loading: do no populate UidInfo, just uid. UidInfo will be fetched on demand
                        myWl.setUid(uid);

//						Log.d(TAG, "Wakelocks: Process = " + wakelockEntry.getKey() + " wakelock [s] " + wakelockTime + ", count " + wakelockCount);
		            }
		        }
            }
            catch( Exception e )
            {
            	Log.e(TAG, "An exception occured in getWakelockStats(). Message: " + e.getMessage() + ", cause: " + e.getCause().getMessage());
                throw e;
            }
		}	
		return myStats;
	}

	/**
	 * Obtain the wakelock stats as a list of Wakelocks (@see com.asksven.android.common.privateapiproxies.Wakelock}
	 * @param context a Context
	 * @return a List of Wakelock s
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<StatElement> getKernelWakelockStats(Context context, int iStatType, boolean bAlternate) throws Exception
	{
		// type checks
		boolean validTypes = BatteryStatsTypes.assertValidStatType(iStatType);
		if (!validTypes)
		{
			Log.e(TAG, "Invalid WakeType or StatType");
			throw new Exception("Invalid WakeType or StatType");
		}
		
		Log.d(TAG, "getWakelockStats was called with params "
				+"[iStatType] = " + iStatType);
		
		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		
		long uSecBatteryTime = this.computeBatteryRealtime(SystemClock.elapsedRealtime() * 1000, iStatType);
		//long uSecBatteryTime = this.computeBatteryRealtime(SystemClock.elapsedRealtime() * 1000, iStatType);
		//long msSinceBoot = SystemClock.elapsedRealtime();
		 
        try
        {			
			ClassLoader cl = context.getClassLoader();
			@SuppressWarnings("rawtypes")
			Class iBatteryStats = cl.loadClass("com.android.internal.os.BatteryStatsImpl");

			Field fKernelWakelockStats = iBatteryStats.getDeclaredField("mTmpWakelockStats");
			fKernelWakelockStats.setAccessible(true);

			// Process wake lock usage
			Method methodGetKernelWakelockStats = iBatteryStats.getMethod("getKernelWakelockStats");
            // Map of String, BatteryStatsImpl.SamplingTimer
//            Map<String, ? extends Object> kernelWakelockStats2 = (Map<String, ? extends Object>)  fKernelWakelockStats.get(m_Instance);


            Class classSamplingTimer = cl.loadClass("com.android.internal.os.BatteryStatsImpl$SamplingTimer");

			Field currentReportedCount  		= classSamplingTimer.getDeclaredField("mCurrentReportedCount");
			Field currentReportedTotalTime  	= classSamplingTimer.getDeclaredField("mCurrentReportedTotalTime");
			Field unpluggedReportedCount  		= classSamplingTimer.getDeclaredField("mUnpluggedReportedCount");
			Field unpluggedReportedTotalTime  	= classSamplingTimer.getDeclaredField("mUnpluggedReportedTotalTime");
			//Field inDischarge  					= classSamplingTimer.getDeclaredField("mInDischarge");
			Field trackingReportedValues  		= classSamplingTimer.getDeclaredField("mTrackingReportedValues");
			
			currentReportedCount.setAccessible(true);
			currentReportedTotalTime.setAccessible(true);
			unpluggedReportedCount.setAccessible(true);
			unpluggedReportedTotalTime.setAccessible(true);
			//inDischarge.setAccessible(true);
			trackingReportedValues.setAccessible(true);
			
			//Parameters
			Object[] params= new Object[1];


			// Map of String, BatteryStatsImpl.SamplingTimer
			Map<String, ? extends Object> kernelWakelockStats = (Map<String, ? extends Object>)  methodGetKernelWakelockStats.invoke(m_Instance);

					            
	        // Map of String, BatteryStats.Uid.Wakelock
            for (Map.Entry<String, ? extends Object> wakelockEntry : kernelWakelockStats.entrySet())
            {
                // BatteryStats.SamplingTimer
            	String wakelockName = wakelockEntry.getKey();
            	Object samplingTimer = wakelockEntry.getValue();
            	
            	params[0]= samplingTimer;
            	
            	// read private fields
            	Integer currentReportedCountVal 	= (Integer) currentReportedCount.get(params[0]);
            	Long currentReportedTotalTimeVal 	= (Long) currentReportedTotalTime.get(params[0]);
            	
            	Integer unpluggedReportedCountVal 	= (Integer) unpluggedReportedCount.get(params[0]);
            	Long unpluggedReportedTotalTimeVal 	= (Long) unpluggedReportedTotalTime.get(params[0]);
            	
            	//Boolean inDischargeVal 				= (Boolean) inDischarge.get(params[0]);
            	Boolean trackingReportedValuesVal 	= (Boolean) trackingReportedValues.get(params[0]);
            	
            	if (CommonLogSettings.DEBUG)
            	{
	            	Log.d(TAG, "Kernel wakelock '" + wakelockEntry.getKey() + "'"
	            			+ " : reading fields from SampleTimer: " 
	            			+ " [currentReportedCountVal] = " + currentReportedCountVal
	            			+ " [currentReportedTotalTimeVal] = " + currentReportedTotalTimeVal
	            			+ " [unpluggedReportedCountVal] = " + unpluggedReportedCountVal
	            			+ " [mUnpluggedReportedTotalTimeVal] = " + unpluggedReportedTotalTimeVal
	            			//+ " [mInDischarge] = " + inDischargeVal
	            			+ " [mTrackingReportedValues] = " + trackingReportedValuesVal);
            	}
            	
//            	
//            	@SuppressWarnings("rawtypes")
//				Class batteryStatsSamplingTimerClass = cl.loadClass("com.android.internal.os.BatteryStatsImpl$SamplingTimer");

				//Parameters Types
//				@SuppressWarnings("rawtypes")
//				Class[] paramTypesGetTotalTimeLocked= new Class[2];
//				paramTypesGetTotalTimeLocked[0]= long.class;
//				paramTypesGetTotalTimeLocked[1]= int.class;
//
//				//Parameters
//				Object[] paramGetTotalTimeLocked= new Object[2];
//				paramGetTotalTimeLocked[0]= new Long(uSecBatteryTime);
//				paramGetTotalTimeLocked[1]= new Integer(iStatType);
//				
//
//				Method methodGetTotalTimeLocked = classSamplingTimer
//						.getMethod("getTotalTimeLocked", paramTypesGetTotalTimeLocked);
//
//				//Parameters Types
//				@SuppressWarnings("rawtypes")
//				Class[] paramTypesGetCountLocked= new Class[1];
//				paramTypesGetCountLocked[0]= int.class;
//
//				//Parameters
//				Object[] paramGetCountLocked= new Object[1];
//				paramGetCountLocked[0]= new Integer(iStatType);
//
//				Method methodGetCountLocked = classSamplingTimer
//						.getMethod("getCountLocked", paramTypesGetCountLocked);
//					
//				
//				Long wake = (Long) methodGetTotalTimeLocked.invoke(samplingTimer, paramGetTotalTimeLocked);
//				
//				Integer count = (Integer) methodGetCountLocked.invoke(samplingTimer, paramGetCountLocked);
//				
//				if (CommonLogSettings.DEBUG)
//				{
//					Log.d(TAG, "Kernel wakelock: " + wakelockEntry.getKey() + " wakelock [s] " + wake / 1000
//							+ " count " + count);
//				}
				
				// public NativeKernelWakelock(String name, String details, int count, int expire_count, int wake_count,
				// long active_since, long total_time, long sleep_time, long max_time, long last_change, long time)
				//public KernelWakelock(String name, long duration, long time, int count)
				// return the data depending on the method 
				//if (!bAlternate)
				//{
					//KernelWakelock myWl = new KernelWakelock(wakelockEntry.getKey(), wake / 1000, uSecBatteryTime / 1000, count);
				
//					NativeKernelWakelock myWl = new NativeKernelWakelock(wakelockEntry.getKey() + " *api*", "", count.intValue(), 0, 0, 
//							0L, wake/1000, wake/1000, 0L, 0L, uSecBatteryTime / 1000);
//					myStats.add(myWl);	
				//}
//				else
//				{
				if (CommonLogSettings.DEBUG)
				{
					Log.d(TAG, "Kernel wakelock: " + wakelockEntry.getKey() + " wakelock [s] " + currentReportedTotalTimeVal / 1000
							+ " count " + currentReportedCountVal);
				}

				NativeKernelWakelock myWl = new NativeKernelWakelock(
							wakelockEntry.getKey(), "*api*", currentReportedCountVal, 0, 0, 0L, 
							currentReportedTotalTimeVal / 1000, currentReportedTotalTimeVal / 1000, 0L, 0L,
							uSecBatteryTime / 1000);
					myStats.add(myWl);
//				}	
            }
        }
        catch( Exception e )
        {
        	Log.e(TAG, "An exception occured in getKernelWakelockStats(). Message: " + e.getMessage() + ", cause: " + e.getCause().getMessage());
            throw e;
        }

        return myStats;
	}

	/**
	 * Obtain the process stats as a list of Processes (@see com.asksven.android.common.privateapiproxies.Process}
	 * @param context a Context
	 * @param iStatType a type of stat @see com.asksven.android.common.privateapiproxies.BatteryStatsTypes
	 * @return a List of Process es
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<StatElement> getProcessStats(Context context, int iStatType) throws Exception
	{
		// type checks
		boolean validTypes = BatteryStatsTypes.assertValidStatType(iStatType);
		if (!validTypes)
		{
			Log.e(TAG, "Invalid WakeType or StatType");
			throw new Exception("Invalid StatType");
		}
		
		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		
		this.collectUidStats();
		if (m_uidStats != null)
		{
            try
            {			
				ClassLoader cl = context.getClassLoader();
				@SuppressWarnings("rawtypes")
				Class iBatteryStatsUid = cl.loadClass("com.android.internal.os.BatteryStatsImpl$Uid");
				int NU = m_uidStats.size();
		        for (int iu = 0; iu < NU; iu++)
		        {
		        	// Object is an instance of BatteryStats.Uid
		            Object myUid = m_uidStats.valueAt(iu);
	            
	            	Method methodGetProcessStats = iBatteryStatsUid.getMethod("getProcessStats");
	            	
	            	Method methodGetUid	= iBatteryStatsUid.getMethod("getUid");
					Integer uid 		= (Integer) methodGetUid.invoke(myUid);
					
					// Map of String, BatteryStats.Uid.Proc
					Map<String, ? extends Object> processStats = (Map<String, ? extends Object>)  methodGetProcessStats.invoke(myUid);
					
					if (processStats.size() > 0)
					{
					    for (Map.Entry<String, ? extends Object> ent : processStats.entrySet())
					    {
					    	if (CommonLogSettings.TRACE)
					    	{
					    		Log.d(TAG, "Process name = " + ent.getKey());
					    	}
						    // Object is a BatteryStatsTypes.Uid.Proc
						    Object ps = ent.getValue();
							@SuppressWarnings("rawtypes")
							Class batteryStatsUidProc = cl.loadClass("com.android.internal.os.BatteryStatsImpl$Uid$Proc");

							//Parameters Types
							@SuppressWarnings("rawtypes")
							Class[] paramTypesGetXxxTime= new Class[1];
							paramTypesGetXxxTime[0]= int.class; 
							
							Method methodGetUserTime 	= batteryStatsUidProc.getMethod("getUserTime", paramTypesGetXxxTime);
							Method methodGetSystemTime 	= batteryStatsUidProc.getMethod("getSystemTime", paramTypesGetXxxTime);
							Method methodGetStarts 		= batteryStatsUidProc.getMethod("getStarts", paramTypesGetXxxTime);
	
							//Parameters
							Object[] paramsGetXxxTime= new Object[1];
							paramsGetXxxTime[0]= new Integer(iStatType);
							
							Long userTime = (Long) methodGetUserTime.invoke(ps, paramsGetXxxTime);
							Long systemTime = (Long) methodGetSystemTime.invoke(ps, paramsGetXxxTime);
							Integer starts = (Integer) methodGetStarts.invoke(ps, paramsGetXxxTime);

							// starting in kitkat usertime and system time are expressed in 1/100s
							// @see https://android.googlesource.com/platform/frameworks/base/+/android-4.4_r1.1/core/java/android/os/BatteryStats.java
							// line 343ff
							if (Build.VERSION.SDK_INT >= 19)
							{
								userTime 	*= 10;
								systemTime 	*= 10;
							}

							
							if (CommonLogSettings.TRACE)
							{
								Log.d(TAG, "UserTime = " + userTime);
								Log.d(TAG, "SystemTime = " + systemTime);
								Log.d(TAG, "Starts = " + starts);
							}
							
							boolean ignore = false;

							// take only the processes with CPU time
							if ((userTime + systemTime) > 100)
							{
								Process myPs = new Process(ent.getKey(), userTime, systemTime, starts);
								// opt for lazy loading: do no populate UidInfo, just uid. UidInfo will be fetched on demand
								myPs.setUid(uid);
								// try resolving names
//								String myName = m_nameResolver.getLabel(context, ent.getKey());
								
								myStats.add(myPs);
							}
							else
							{
								if (CommonLogSettings.TRACE)
								{
									Log.d(TAG, "Process " + ent.getKey() + " was discarded (CPU time =0)");
								}
							}
					    }		
		            }
		        }
            }
            catch( Exception e )
            {
            	Log.e(TAG, "An exception occured in getProcessStats(). Message: " + e.getMessage() + ", cause: " + e.getCause().getMessage());
                throw e;
            }
		}	
		return myStats;
	}

	/**
	 * Obtain the wakeup stats as a list of Alarms (@see com.asksven.android.common.privateapiproxies.Alarm}
	 * @param context a Context
	 * @param iStatType a type of stat @see com.asksven.android.common.privateapiproxies.BatteryStatsTypes
	 * @return a List of Process es
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<StatElement> getWakeupStats(Context context, int iStatType) throws Exception
	{
		if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.LOLLIPOP_MR1)
    	{
    		return getWakeupStatsPre6(context, iStatType);
    	}
    	else
    	{
    		return getWakeupStatsPost6(context, iStatType);
    	}
	}
	
	
	@SuppressWarnings("unchecked")
	public ArrayList<StatElement> getWakeupStatsPre6(Context context, int iStatType) throws Exception
	{
		// type checks
		boolean validTypes = BatteryStatsTypes.assertValidStatType(iStatType);
		if (!validTypes)
		{
			Log.e(TAG, "Invalid WakeType or StatType");
			throw new Exception("Invalid StatType");
		}
		
		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		
		this.collectUidStats();
		if (m_uidStats != null)
		{
            try
            {			
				ClassLoader cl = context.getClassLoader();
				@SuppressWarnings("rawtypes")
				Class iBatteryStatsUid = cl.loadClass("com.android.internal.os.BatteryStatsImpl$Uid");
				int NU = m_uidStats.size();
		        for (int iu = 0; iu < NU; iu++)
		        {
		        	// Object is an instance of BatteryStats.Uid
		            Object myUid = m_uidStats.valueAt(iu);
	            
	            	Method methodGetPackageStats = iBatteryStatsUid.getMethod("getPackageStats");
	            	
	            	Method methodGetUid	= iBatteryStatsUid.getMethod("getUid");
					Integer uid 		= (Integer) methodGetUid.invoke(myUid);
					
					// Map of String, BatteryStats.Uid.Proc
					Map<String, ? extends Object> packageStats = (Map<String, ? extends Object>)  methodGetPackageStats.invoke(myUid);
					
					if (packageStats.size() > 0)
					{
					    for (Map.Entry<String, ? extends Object> ent : packageStats.entrySet())
					    {
					    	if (CommonLogSettings.TRACE)
					    	{
					    		Log.d(TAG, "Package name = " + ent.getKey());
					    	}
						    // Object is a BatteryStatsTypes.Uid.Proc
						    Object ps = ent.getValue();
							@SuppressWarnings("rawtypes")
							Class batteryStatsUidPkg = cl.loadClass("com.android.internal.os.BatteryStatsImpl$Uid$Pkg");

							//Parameters Types
							@SuppressWarnings("rawtypes")
							Class[] paramTypesGetWakeups= new Class[1];
							paramTypesGetWakeups[0]= int.class; 
							
							Method methodGetWakeups 	= batteryStatsUidPkg.getMethod("getWakeups", paramTypesGetWakeups);
	
							//Parameters
							Object[] paramsGetWakeups= new Object[1];
							paramsGetWakeups[0]= new Integer(iStatType);
							
							int wakeups = (Integer) methodGetWakeups.invoke(ps, paramsGetWakeups);
							
							if (CommonLogSettings.TRACE)
							{
								Log.d(TAG, "uid = " + uid);
								Log.d(TAG, "wkeups = " + wakeups);
							}
							
							boolean ignore = false;

							if ((wakeups) > 0)
							{
								Alarm myWakeup = new Alarm(ent.getKey(), "*api*");
								myWakeup.setWakeups(wakeups);
								// opt for lazy loading: do no populate UidInfo, just uid. UidInfo will be fetched on demand
								myWakeup.setUid(uid);
								// try resolving names
//								String myName = m_nameResolver.getLabel(context, ent.getKey());
								
								myStats.add(myWakeup);
							}
							else
							{
								if (CommonLogSettings.TRACE)
								{
									Log.d(TAG, "Process " + ent.getKey() + " was discarded (CPU time =0)");
								}
							}
					    }		
		            }
		        }
            }
            catch( Exception e )
            {
            	Log.e(TAG, "An exception occured in getWakeupStats(). Message: " + e.getMessage() + ", cause: " + e.getCause().getMessage());
                throw e;
            }
		}	
		return myStats;
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("unchecked")
	public ArrayList<StatElement> getWakeupStatsPost6(Context context, int iStatType) throws Exception
	{
		// type checks
		boolean validTypes = BatteryStatsTypes.assertValidStatType(iStatType);
		if (!validTypes)
		{
			Log.e(TAG, "Invalid WakeType or StatType");
			throw new Exception("Invalid StatType");
		}
		
		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		
		this.collectUidStats();
		if (m_uidStats != null)
		{
            try
            {			
				ClassLoader cl = context.getClassLoader();
				@SuppressWarnings("rawtypes")
				Class iBatteryStatsUid = cl.loadClass("com.android.internal.os.BatteryStatsImpl$Uid");
				int NU = m_uidStats.size();
		        for (int iu = 0; iu < NU; iu++)
		        {
		        	// Object is an instance of BatteryStats.Uid
		            Object myUid = m_uidStats.valueAt(iu);
	            
	            	Method methodGetPackageStats = iBatteryStatsUid.getMethod("getPackageStats");
	            	
	            	Method methodGetUid	= iBatteryStatsUid.getMethod("getUid");
					Integer uid 		= (Integer) methodGetUid.invoke(myUid);
					
					// Map of String, BatteryStats.Uid.Proc
					Map<String, ? extends Object> packageStats = (Map<String, ? extends Object>)  methodGetPackageStats.invoke(myUid);
					
					if (packageStats.size() > 0)
					{
					    for (Map.Entry<String, ? extends Object> ent : packageStats.entrySet())
					    {
					    	if (CommonLogSettings.TRACE)
					    	{
					    		Log.d(TAG, "Package name = " + ent.getKey());
					    	}
						    // Object is a BatteryStatsTypes.Uid.Proc
						    Object ps = ent.getValue();
							@SuppressWarnings("rawtypes")
							Class batteryStatsUidPkg = cl.loadClass("com.android.internal.os.BatteryStatsImpl$Uid$Pkg");

							Method methodGetWakeupAlarmStats = batteryStatsUidPkg.getMethod("getWakeupAlarmStats");
							Map<String, ? extends Object> wakeupStats =
									(Map<String, ? extends Object>)  methodGetWakeupAlarmStats.invoke(ps);
							
							for (Map.Entry<String, ? extends Object> wa : wakeupStats.entrySet())
							{
								Class batteryStatsCounter = cl.loadClass("com.android.internal.os.BatteryStatsImpl$Counter");
	
								//Parameters Types
								@SuppressWarnings("rawtypes")
								Class[] paramsTypesGetCountLocked= new Class[1];
								paramsTypesGetCountLocked[0]= int.class; 
	
								Method methodGetCountLocked = batteryStatsCounter.getMethod("getCountLocked", paramsTypesGetCountLocked);
	
								// Parameters
								Object[] paramsGetCountLocked= new Object[1];
								paramsGetCountLocked[0]= new Integer(iStatType);
														
								Object counter = wa.getValue();

								int wakeups = (Integer) methodGetCountLocked.invoke(counter, paramsGetCountLocked);
								
								if ((wakeups) > 0)
								{
									Alarm myWakeup = new Alarm(ent.getKey(), "*api*");
									myWakeup.setWakeups(wakeups);
									// opt for lazy loading: do no populate UidInfo, just uid. UidInfo will be fetched on demand
									myWakeup.setUid(uid);
									
									myStats.add(myWakeup);
								}
								else
								{
									if (CommonLogSettings.TRACE)
									{
										Log.d(TAG, "Process " + ent.getKey() + " was discarded (CPU time =0)");
									}
								}
								
							}
					    }		
		            }
		        }
            }
            catch( Exception e )
            {
            	Log.e(TAG, "An exception occured in getWakeupStats(). Message: " + e.getMessage() + ", cause: " + e.getCause().getMessage());
                throw e;
            }
		}	
		return myStats;
	}

	@SuppressWarnings("unchecked")
	public Long getSyncOnTime(Context context, long batteryRealtime, int iStatsType) throws Exception
	{
		// type checks
		boolean validTypes = BatteryStatsTypes.assertValidStatType(iStatsType);
		if (!validTypes)
		{
			Log.e(TAG, "Invalid WakeType or StatType");
			throw new Exception("Invalid StatType");
		}
		
		Long ret = 0L;
		
		this.collectUidStats();
		if (m_uidStats != null)
		{
            try
            {			
				ClassLoader cl = context.getClassLoader();
				@SuppressWarnings("rawtypes")
				Class iBatteryStatsUid = cl.loadClass("com.android.internal.os.BatteryStatsImpl$Uid");
				int NU = m_uidStats.size();
		        for (int iu = 0; iu < NU; iu++)
		        {
		        	// Object is an instance of BatteryStats.Uid
		            Object myUid = m_uidStats.valueAt(iu);
	            
	            	Method methodGetSyncStats = iBatteryStatsUid.getMethod("getSyncStats");
	            	
	            	Method methodGetUid	= iBatteryStatsUid.getMethod("getUid");
					Integer uid 		= (Integer) methodGetUid.invoke(myUid);
					
					// Map of String, BatteryStats.Uid.Proc
					Map<String, ? extends Object> syncStats = (Map<String, ? extends Object>)  methodGetSyncStats.invoke(myUid);
					
					if (syncStats.size() > 0)
					{
					    for (Map.Entry<String, ? extends Object> ent : syncStats.entrySet())
					    {
					    	if (CommonLogSettings.TRACE)
					    	{
					    		Log.d(TAG, "Package name = " + ent.getKey());
					    	}
						    // Object is a BatteryStatsTypes.Uid.Proc
						    Object timer = ent.getValue();
							@SuppressWarnings("rawtypes")
							Class batteryStatsTimer = cl.loadClass("com.android.internal.os.BatteryStatsImpl$Timer");

							//Parameters Types
							@SuppressWarnings("rawtypes")
							Class[] paramsTypesGetTotalTimeLocked= new Class[1];
							paramsTypesGetTotalTimeLocked[0]= long.class;

							// method is protected so we must make it accessible
							Method computeRunTimeLocked = batteryStatsTimer.getDeclaredMethod("computeRunTimeLocked", paramsTypesGetTotalTimeLocked);
							computeRunTimeLocked.setAccessible(true);

							
				        	//Parameters
				        	Object[] params= new Object[1];
				        	params[0]= new Long(batteryRealtime);

							// call public long getTotalTimeLocked(long elapsedRealtimeUs, int which)
				        	Long value = (Long) computeRunTimeLocked.invoke(timer, params);
				        	ret += value;
							
					    }		
		            }
		        }
            }
            catch( Exception e )
            {
            	Log.e(TAG, "An exception occured in getSyncOnTime(). Message: " + e.getMessage());
                throw e;
            }
		}	
		return ret;
	}

	/**
	 * Obtain the network usage stats as a list of NetworkUsages (@see com.asksven.android.common.privateapiproxies.NetworkUsage}
	 * @param context a Context
	 * @param iStatType a type of stat @see com.asksven.android.common.privateapiproxies.BatteryStatsTypes
	 * @return a List of NetworkUsage s
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<StatElement> getNetworkUsageStats(Context context, int iStatType) throws Exception
	{
		// type checks
		boolean validTypes = BatteryStatsTypes.assertValidStatType(iStatType);
		if (!validTypes)
		{
			Log.e(TAG, "Invalid WakeType or StatType");
			throw new Exception("Invalid StatType");
		}
		
		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		
		this.collectUidStats();
		if (m_uidStats != null)
		{
            try
            {			
				ClassLoader cl = context.getClassLoader();
				@SuppressWarnings("rawtypes")
				Class iBatteryStatsUid = cl.loadClass("com.android.internal.os.BatteryStatsImpl$Uid");
				int NU = m_uidStats.size();
		        for (int iu = 0; iu < NU; iu++)
		        {
		        	// Object is an instance of BatteryStats.Uid
		            Object myUid = m_uidStats.valueAt(iu);

                    Long bytesReceived 			= 0L;
                    Long bytesSent 				= 0L;
                    Long bytesReceivedWifi 		= 0L;
                    Long bytesReceivedMobile 	= 0L;
                    Long bytesSentWifi 			= 0L;
                    Long bytesSentMobile 		= 0L;
                    

                    // getTcpBytesReceived and getTcpBytesSent are available in API level < 19.
                    // They are replaced by getNetworkActivityCount in API level >= 19.
                    if (Build.VERSION.SDK_INT < 19)
                    {
                        //Parameters Types
                        @SuppressWarnings("rawtypes")
                        Class[] paramTypesGetTcpBytesXxx = new Class[1];
                        paramTypesGetTcpBytesXxx[0] = int.class;

                        Method methodGetTcpBytesReceived = iBatteryStatsUid.getMethod("getTcpBytesReceived", paramTypesGetTcpBytesXxx);
                        Method methodGetTcpBytesSent = iBatteryStatsUid.getMethod("getTcpBytesSent", paramTypesGetTcpBytesXxx);

                        //Parameters
                        Object[] paramGetTcpBytesXxx = new Object[1];
                        paramGetTcpBytesXxx[0] = new Integer(iStatType);

                        bytesReceived = (Long) methodGetTcpBytesReceived.invoke(myUid, paramGetTcpBytesXxx);
                        bytesSent = (Long) methodGetTcpBytesSent.invoke(myUid, paramGetTcpBytesXxx);
                    }
                    else if (Build.VERSION.SDK_INT <= 21)
                    {
                        @SuppressWarnings("rawtypes")
                        Class[] paramTypesGetNetworkActivity = new Class[] {int.class, int.class};
                        Method methodGetNetworkActivity = iBatteryStatsUid.getMethod("getNetworkActivityCount",
                                paramTypesGetNetworkActivity);
                        // Parameters for getting received bytes from mobile
                        Object paramGetNetworkActivityCount [] = {NETWORK_MOBILE_RX_BYTES,
                                iStatType};
                        bytesReceivedMobile = (Long) methodGetNetworkActivity.invoke(myUid, paramGetNetworkActivityCount);
                        // change parameter to get received bytes from wifi
                        paramGetNetworkActivityCount[0] = NETWORK_WIFI_RX_BYTES;
                        // add together for now
                        bytesReceivedWifi = (Long) methodGetNetworkActivity.invoke(myUid, paramGetNetworkActivityCount);

                        // same for transmitted bytes
                        paramGetNetworkActivityCount[0] = NETWORK_MOBILE_TX_BYTES;
                        bytesSentMobile = (Long) methodGetNetworkActivity.invoke(myUid, paramGetNetworkActivityCount);
                        paramGetNetworkActivityCount[0] = NETWORK_WIFI_TX_BYTES;
                        bytesSentWifi = (Long) methodGetNetworkActivity.invoke(myUid, paramGetNetworkActivityCount);
                    }
                    else
                    {
                        @SuppressWarnings("rawtypes")
                        Class[] paramTypesGetNetworkActivity = new Class[] {int.class, int.class};
                        Method methodGetNetworkActivity = iBatteryStatsUid.getMethod("getNetworkActivityBytes",
                                paramTypesGetNetworkActivity);
                        
                     // Parameters for getting received bytes from mobile
                        Object paramGetNetworkActivityCount [] = {NETWORK_MOBILE_RX_BYTES,
                                iStatType};
                        bytesReceivedMobile = (Long) methodGetNetworkActivity.invoke(myUid, paramGetNetworkActivityCount);
                        // change parameter to get received bytes from wifi
                        paramGetNetworkActivityCount[0] = NETWORK_WIFI_RX_BYTES;
                        // add together for now
                        bytesReceivedWifi = (Long) methodGetNetworkActivity.invoke(myUid, paramGetNetworkActivityCount);

                        // same for transmitted bytes
                        paramGetNetworkActivityCount[0] = NETWORK_MOBILE_TX_BYTES;
                        bytesSentMobile = (Long) methodGetNetworkActivity.invoke(myUid, paramGetNetworkActivityCount);
                        paramGetNetworkActivityCount[0] = NETWORK_WIFI_TX_BYTES;
                        bytesSentWifi = (Long) methodGetNetworkActivity.invoke(myUid, paramGetNetworkActivityCount);
                    }

					Method methodGetUid	= iBatteryStatsUid.getMethod("getUid");
					Integer uid 		= (Integer) methodGetUid.invoke(myUid);
				
					if (CommonLogSettings.DEBUG)
					{
						Log.d(TAG, "Uid = " + uid + ": received:" + bytesReceived + ", sent: " + bytesSent);
					}
					
					NetworkUsage myData = null;
                    if (Build.VERSION.SDK_INT >= 19)
                    {
                    	// we have data separated for Wifi and Mobile
						myData = new NetworkUsage(uid, "Wifi", bytesReceivedWifi, bytesSentWifi);
						// try resolving names
						UidInfo myInfo = UidNameResolver.getInstance().getNameForUid(uid);
						myData.setUidInfo(myInfo);
						myStats.add(myData);

						myData = new NetworkUsage(uid, "Mobile", bytesReceivedMobile, bytesSentMobile);
						// try resolving names
						myData.setUidInfo(myInfo);
						myStats.add(myData);

                    }
                    else 
                    {
						myData = new NetworkUsage(uid, bytesReceived, bytesSent);
						// try resolving names
						UidInfo myInfo = UidNameResolver.getInstance().getNameForUid(uid);
						myData.setUidInfo(myInfo);
						myStats.add(myData);
                    }
		        }
            }
            catch( Exception e )
            {
                throw e;
            }
		}	
		return myStats;
	}
	

	/**
	 * Obtain the network usage stats as a list of NetworkUsages (@see com.asksven.android.common.privateapiproxies.NetworkUsage}
	 * @return a List of NetworkUsage s
	 * @throws Exception
	 */
    public ArrayList<NetworkUsage> getKernelNetworkStats(int iStatsType)
	{

		ArrayList<NetworkUsage> myRet = new ArrayList<NetworkUsage>(); 
		try
		{
			@SuppressWarnings("unchecked")
			// we must use getDeclaredMethod as that method is private
			Method method = m_ClassDefinition.getDeclaredMethod("getNetworkStatsDetailGroupedByUid");
			method.setAccessible(true);
	
			Object networkStats = method.invoke(m_Instance);
			String myRes = "tada";
		}
		catch (Exception e)
		{
			myRet = null;
		}
        return myRet;

	
	}
    
    @SuppressWarnings("unchecked")
	public ArrayList<HistoryItem> getHistory(Context context) throws Exception
	{
		
		ArrayList<HistoryItem> myStats = new ArrayList<HistoryItem>();
	        	 
        try
        {			
			ClassLoader cl = context.getClassLoader();
			@SuppressWarnings("rawtypes")
			Class classHistoryItem = cl.loadClass("android.os.BatteryStats$HistoryItem");
												   
			
			// get constructor
			Constructor cctor = classHistoryItem.getConstructor();
			
			Object myHistoryItem = cctor.newInstance();

			// prepare the method call for getNextHistoryItem
			//Parameters Types
			@SuppressWarnings("rawtypes")
			Class[] paramTypes= new Class[1];
			paramTypes[0]= classHistoryItem;


			@SuppressWarnings("unchecked")
			Method methodNext = m_ClassDefinition.getMethod("getNextHistoryLocked", paramTypes);

			//Parameters
			Object[] params= new Object[1];

			// initalize hist and iterate like this
			// if (stats.startIteratingHistoryLocked()) {
            // final HistoryItem rec = new HistoryItem();
            // while (stats.getNextHistoryLocked(rec)) {
			int statsType = 0;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			{
				statsType = BatteryStatsTypesLolipop.STATS_CURRENT;
			}
			else
			{
				statsType = BatteryStatsTypes.STATS_CURRENT;
			}	
			// read the time of query for history
	        Long statTimeRef = Long.valueOf(this.computeBatteryRealtime(SystemClock.elapsedRealtime() * 1000,
	                statsType));
	        statTimeRef = System.currentTimeMillis(); 
	        
	        if (CommonLogSettings.DEBUG)
	        {	
	        	Log.d(TAG, "Reference time (" + statTimeRef + ": " + DateUtils.format(DateUtils.DATE_FORMAT_NOW, statTimeRef));
	        }
	        // statTimeLast stores the timestamp of the last sample
	        Long statTimeLast = Long.valueOf(0);
	        
			if (this.startIteratingHistoryLocked())
			{
				params[0]= myHistoryItem;
				Boolean bNext = (Boolean) methodNext.invoke(m_Instance, params);
				while (bNext)
				{
					// process stats: create HistoryItems from params
					Field timeField 				= classHistoryItem.getField("time"); 			// long
					
					
					Field cmdField 					= classHistoryItem.getField("cmd"); 			// byte
					Byte cmdValue = (Byte) cmdField.get(params[0]);
					
					// process only valid items
					byte updateCmd = 0;
					
					// different versions -> different HistoryItem constants
					if ( (Build.VERSION.SDK_INT == Build.VERSION_CODES.ICE_CREAM_SANDWICH) || (Build.VERSION.SDK_INT == Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) )
					{
						updateCmd = HistoryItemIcs.CMD_UPDATE;
					}
					else if ( (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) || (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT_WATCH) )
					{
						updateCmd = HistoryItemKitKat.CMD_UPDATE;
					}
					else if ( (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) || (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1) || (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2))
					{
						updateCmd = HistoryItemJellyBean.CMD_UPDATE;
					}
					else if ( (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) || (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) )
					{
						updateCmd = HistoryItemLolipop.CMD_UPDATE;
					}
                    else if ( (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) || (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) )
                    {
                        updateCmd = HistoryItemMarshmallow.CMD_UPDATE;
                    }
                    else if ( (Build.VERSION.SDK_INT == Build.VERSION_CODES.N) || (Build.VERSION.SDK_INT == Build.VERSION_CODES.N) )
                    {
                        updateCmd = HistoryItemNougat.CMD_UPDATE;
                    }
                    else
					{
						updateCmd = HistoryItemOreo.CMD_UPDATE;
					}
					
					if (cmdValue == updateCmd)
					{
				        Field batteryLevelField 		= classHistoryItem.getField("batteryLevel"); 	// byte
				        Field batteryStatusField 		= classHistoryItem.getField("batteryStatus"); 	// byte
				        Field batteryHealthField 		= classHistoryItem.getField("batteryHealth"); 	// byte
				        Field batteryPlugTypeField 		= classHistoryItem.getField("batteryPlugType"); // byte
				        
				        Field batteryTemperatureField 	= classHistoryItem.getField("batteryTemperature"); // char
				        Field batteryVoltageField 		= classHistoryItem.getField("batteryVoltage"); 	// char
				        
				        Field statesField 				= classHistoryItem.getField("states"); 			// int
                        Field states2Field              = null;
                        try
                        {
                            states2Field = classHistoryItem.getField("states2");            // int
                        }
                        catch (NoSuchFieldException e)
                        {
                            // it's ok, this field exists only since Lolipop
                        }

				        // retrieve all values
				        @SuppressWarnings("rawtypes")
				        Long timeValue = (Long) timeField.get(params[0]);
				        
				        // store values only once
				        if (!statTimeLast.equals(timeValue))
				        {
					        Byte batteryLevelValue = (Byte) batteryLevelField.get(params[0]);
					        Byte batteryStatusValue = (Byte) batteryStatusField.get(params[0]);
					        Byte batteryHealthValue = (Byte) batteryHealthField.get(params[0]);
					        Byte batteryPlugTypeValue = (Byte) batteryPlugTypeField.get(params[0]);
					        
					        String batteryTemperatureValue = String.valueOf(batteryTemperatureField.get(params[0]));
					        String batteryVoltageValue = String.valueOf(batteryVoltageField.get(params[0]));
					        
					        Integer statesValue = (Integer) statesField.get(params[0]);
                            Integer states2Value = 0;

                            if (states2Field != null)
                            {
                                states2Value = (Integer) states2Field.get(params[0]);
                            }

					        HistoryItem myItem = null;
					        
							// different versions -> different HistoryItem constants
							if ( (Build.VERSION.SDK_INT == Build.VERSION_CODES.ICE_CREAM_SANDWICH) || (Build.VERSION.SDK_INT == Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) )
							{
								myItem = new HistoryItemIcs(timeValue, cmdValue, batteryLevelValue,
						        		batteryStatusValue, batteryHealthValue, batteryPlugTypeValue,
						        		batteryTemperatureValue, batteryVoltageValue, statesValue, 0);
							}
							else if ( (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) || (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT_WATCH) )
							{
								myItem = new HistoryItemKitKat(timeValue, cmdValue, batteryLevelValue,
						        		batteryStatusValue, batteryHealthValue, batteryPlugTypeValue,
						        		batteryTemperatureValue, batteryVoltageValue, statesValue, 0);
							}
							else if ( (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) || (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1) || (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2))
							{
								myItem = new HistoryItemJellyBean(timeValue, cmdValue, batteryLevelValue,
						        		batteryStatusValue, batteryHealthValue, batteryPlugTypeValue,
						        		batteryTemperatureValue, batteryVoltageValue, statesValue, 0);
							}
							else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP)
							{
								myItem = new HistoryItemLolipop(timeValue, cmdValue, batteryLevelValue,
						        		batteryStatusValue, batteryHealthValue, batteryPlugTypeValue,
						        		batteryTemperatureValue, batteryVoltageValue, statesValue, states2Value);
							}
                            else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M)
                            {
                                myItem = new HistoryItemLolipop(timeValue, cmdValue, batteryLevelValue,
                                        batteryStatusValue, batteryHealthValue, batteryPlugTypeValue,
                                        batteryTemperatureValue, batteryVoltageValue, statesValue, states2Value);
                            }
                            else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N)
                            {
                                myItem = new HistoryItemLolipop(timeValue, cmdValue, batteryLevelValue,
                                        batteryStatusValue, batteryHealthValue, batteryPlugTypeValue,
                                        batteryTemperatureValue, batteryVoltageValue, statesValue, states2Value);
                            }
                            else
							{
								myItem = new HistoryItemOreo(timeValue, cmdValue, batteryLevelValue,
						        		batteryStatusValue, batteryHealthValue, batteryPlugTypeValue,
						        		batteryTemperatureValue, batteryVoltageValue, statesValue, states2Value);
							}
							
					        myStats.add(myItem);

				        }
					    // overwrite the time of the last sample
				        statTimeLast = timeValue;

					}
					else
					{
						Log.d(TAG, "Skipped item");
					}
					
					bNext = (Boolean) methodNext.invoke(m_Instance, params);
				}
				
				// norm the time of each sample
				// stat time last is the number of millis since
				// the stats is being collected
				// the ref time is a full plain time (with date)
				Long offset = statTimeRef - statTimeLast;
				
				if (CommonLogSettings.DEBUG)
				{
					Log.d(TAG, "Reference time (" + statTimeRef + ")" + DateUtils.format(DateUtils.DATE_FORMAT_NOW, statTimeRef));
					
					Log.d(TAG, "Last sample (" + statTimeLast + ")" + DateUtils.format(DateUtils.DATE_FORMAT_NOW, statTimeLast));
					
					Log.d(TAG, "Correcting all HistoryItem times by an offset of (" + offset + ")" + DateUtils.formatDuration(offset * 1000));
				}
				
				for (int i=0; i < myStats.size(); i++)
				{
					myStats.get(i).setOffset(offset);
				}
			}
        }
        catch( Exception e )
        {
        	Log.e(TAG, "An exception occured in getHistory(). Message: " + e.getMessage() + ", cause: " + e.getCause().getMessage());
            throw e;
        }
			
		return myStats;
	}
	

//	@SuppressWarnings("unchecked")
//	public ArrayList<HistoryItem> dumpHistory(Context context) throws Exception
//	{
//
//		ArrayList<HistoryItem> myStats = new ArrayList<HistoryItem>();
//
//        try
//        {
//			ClassLoader cl = context.getClassLoader();
//			@SuppressWarnings("rawtypes")
//			Class classHistoryItem = cl.loadClass("android.os.BatteryStats$HistoryItem");
//
//
//			// get constructor
//			Constructor cctor = classHistoryItem.getConstructor();
//
//			Object myHistoryItem = cctor.newInstance();
//
//			// prepare the method call for getNextHistoryItem
//			//Parameters Types
//			@SuppressWarnings("rawtypes")
//			Class[] paramTypes= new Class[1];
//			paramTypes[0]= classHistoryItem;
//
//
//			@SuppressWarnings("unchecked")
//			Method methodNext = m_ClassDefinition.getMethod("getNextHistoryLocked", paramTypes);
//
//			//Parameters
//			Object[] params= new Object[1];
//
//			// initalize hist and iterate like this
//			// if (stats.startIteratingHistoryLocked()) {
//            // final HistoryItem rec = new HistoryItem();
//            // while (stats.getNextHistoryLocked(rec)) {
//			int statsType = 0;
//			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
//			{
//				statsType = BatteryStatsTypesLolipop.STATS_CURRENT;
//			}
//			else
//			{
//				statsType = BatteryStatsTypes.STATS_CURRENT;
//			}
//
//			// read the time of query for history
//	        Long statTimeRef = Long.valueOf(this.computeBatteryRealtime(SystemClock.elapsedRealtime() * 1000,
//	                statsType));
//	        statTimeRef = System.currentTimeMillis();
//
//	        if (CommonLogSettings.DEBUG)
//	        {
//	        	Log.d(TAG, "Reference time (" + statTimeRef + ": " + DateUtils.format(DateUtils.DATE_FORMAT_NOW, statTimeRef));
//	        }
//	        // statTimeLast stores the timestamp of the last sample
//	        Long statTimeLast = Long.valueOf(0);
//
//			if (this.startIteratingHistoryLocked())
//			{
//				params[0]= myHistoryItem;
//				Boolean bNext = (Boolean) methodNext.invoke(m_Instance, params);
//				while (bNext)
//				{
//					// process stats: create HistoryItems from params
//					Field timeField 				= classHistoryItem.getField("time"); 			// long
//
//
//					Field cmdField 					= classHistoryItem.getField("cmd"); 			// byte
//					Byte cmdValue = (Byte) cmdField.get(params[0]);
//
//					// process only valid items
//					byte updateCmd = 0;
//
//					// ICS has a different implementation of HistoryItems constants
//					if (AndroidVersion.isIcs())
//					{
//						updateCmd = HistoryItemIcs.CMD_UPDATE;
//					}
//					else
//					{
//						updateCmd = HistoryItem.CMD_UPDATE;
//					}
//
//					if (true) //(cmdValue == updateCmd)
//					{
//				        Field batteryLevelField 		= classHistoryItem.getField("batteryLevel"); 	// byte
//				        Field batteryStatusField 		= classHistoryItem.getField("batteryStatus"); 	// byte
//				        Field batteryHealthField 		= classHistoryItem.getField("batteryHealth"); 	// byte
//				        Field batteryPlugTypeField 		= classHistoryItem.getField("batteryPlugType"); // byte
//
//				        Field batteryTemperatureField 	= classHistoryItem.getField("batteryTemperature"); // char
//				        Field batteryVoltageField 		= classHistoryItem.getField("batteryVoltage"); 	// char
//
//				        Field statesField 				= classHistoryItem.getField("states"); 			// int
//
//				        // retrieve all values
//				        @SuppressWarnings("rawtypes")
//				        Long timeValue = (Long) timeField.get(params[0]);
//
//				        Byte batteryLevelValue = (Byte) batteryLevelField.get(params[0]);
//				        Byte batteryStatusValue = (Byte) batteryStatusField.get(params[0]);
//				        Byte batteryHealthValue = (Byte) batteryHealthField.get(params[0]);
//				        Byte batteryPlugTypeValue = (Byte) batteryPlugTypeField.get(params[0]);
//
//				        String batteryTemperatureValue = String.valueOf(batteryTemperatureField.get(params[0]));
//				        String batteryVoltageValue = String.valueOf(batteryVoltageField.get(params[0]));
//
//				        Integer statesValue = (Integer) statesField.get(params[0]);
//
//				        HistoryItem myItem = null;
//
//				        // There different implementation of HistoryItems constants
//				        if (AndroidVersion.isLolipop())
//						{
//							myItem = new HistoryItemLolipop(timeValue, cmdValue, batteryLevelValue,
//					        		batteryStatusValue, batteryHealthValue, batteryPlugTypeValue,
//					        		batteryTemperatureValue, batteryVoltageValue, statesValue);
//						}
//				        if (AndroidVersion.isKitKat())
//						{
//							myItem = new HistoryItemKitKat(timeValue, cmdValue, batteryLevelValue,
//					        		batteryStatusValue, batteryHealthValue, batteryPlugTypeValue,
//					        		batteryTemperatureValue, batteryVoltageValue, statesValue);
//						}
//				        else if (AndroidVersion.isIcs())
//						{
//							myItem = new HistoryItemIcs(timeValue, cmdValue, batteryLevelValue,
//					        		batteryStatusValue, batteryHealthValue, batteryPlugTypeValue,
//					        		batteryTemperatureValue, batteryVoltageValue, statesValue);
//						}
//						else
//						{
//							myItem = new HistoryItem(timeValue, cmdValue, batteryLevelValue,
//					        		batteryStatusValue, batteryHealthValue, batteryPlugTypeValue,
//					        		batteryTemperatureValue, batteryVoltageValue, statesValue);
//						}
//
//				        myStats.add(myItem);
//					}
//					else
//					{
//						Log.d(TAG, "Skipped item");
//					}
//
//					bNext = (Boolean) methodNext.invoke(m_Instance, params);
//				}
//
//				// norm the time of each sample
//				// stat time last is the number of millis since
//				// the stats is being collected
//				// the ref time is a full plain time (with date)
//				Long offset = statTimeRef - statTimeLast;
//
//				// be sure to release
////				this.finishIteratingHistoryLocked();
//
//				for (int i=0; i < myStats.size(); i++)
//				{
//					myStats.get(i).setOffset(offset);
//				}
//
//			}
//        }
//        catch( Exception e )
//        {
//        	Log.e(TAG, "An exception occured in dumpHistory(). Message: " + e.getMessage() + ", cause: " + e.getCause().getMessage());
//            throw e;
//        }
//
//        int oldVal = 0;
//        // iterate over myStats
//        for (int i=0; i < myStats.size(); i++)
//        {
//        	HistoryItem myItem = myStats.get(i);
//        	Log.i(TAG, myItem.toString() + " " + myItem.printBitDescriptions(oldVal, myItem.m_statesValue));
//        	oldVal = myItem.m_statesValue;
//        }
//        	return myStats;
//	}


}

