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
package com.asksven.betterbatterystats.data;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ReceiverCallNotAllowedException;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.asksven.android.common.kernelutils.AlarmsDumpsys;
import com.asksven.android.contrib.Util;
import com.asksven.android.common.CommonLogSettings;
import com.asksven.android.common.kernelutils.CpuStates;
import com.asksven.android.common.kernelutils.ProcessStatsDumpsys;
import com.asksven.android.common.kernelutils.State;
import com.asksven.android.common.kernelutils.Wakelocks;
import com.asksven.android.common.kernelutils.WakeupSources;
import com.asksven.android.common.privateapiproxies.Alarm;
import com.asksven.android.common.privateapiproxies.BatteryInfoUnavailableException;
import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.android.common.privateapiproxies.BatteryStatsTypes;
import com.asksven.android.common.privateapiproxies.BatteryStatsTypesLolipop;
import com.asksven.android.common.privateapiproxies.Misc;
import com.asksven.android.common.privateapiproxies.NativeKernelWakelock;
import com.asksven.android.common.privateapiproxies.NetworkUsage;
import com.asksven.android.common.privateapiproxies.Notification;
import com.asksven.android.common.privateapiproxies.Process;
import com.asksven.android.common.privateapiproxies.SensorUsage;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.android.common.privateapiproxies.Wakelock;
import com.asksven.android.common.utils.DataStorage;
import com.asksven.android.common.utils.DateUtils;
import com.asksven.android.common.utils.GenericLogger;
import com.asksven.android.common.utils.StringUtils;
import com.asksven.android.common.utils.SysUtils;
import com.asksven.betterbatterystats.ActiveMonAlarmReceiver;
import com.asksven.betterbatterystats.BbsApplication;
import com.asksven.betterbatterystats.LogSettings;
import com.asksven.betterbatterystats.R;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Singleton provider for all the statistics
 *
 * 
 * @author sven
 * 
 */
public class StatsProvider
{
	/** the singleton instance */
	static StatsProvider m_statsProvider = null;

	/** constant for custom stats */
	// dependent on arrays.xml
	public final static int STATS_CHARGED 		= 0;
	public final static int STATS_UNPLUGGED 	= 3;
	public final static int STATS_CUSTOM 		= 4;
	public final static int STATS_SCREEN_OFF 	= 5;
	public final static int STATS_BOOT 			= 6;
	
	public final static String LABEL_MISC_AWAKE = "Awake (Screen Off)";

	/** the logger tag */
	static String TAG = "StatsProvider";
	static String TAG_TEST = "StatsProviderTestSuite";

	/**
	 * The constructor (hidden)
	 */
	private StatsProvider()
	{
	}

	/**
	 * returns a singleton instance
	 * 
	 * @return the singleton StatsProvider
	 */
	public static StatsProvider getInstance()
	{
		if (m_statsProvider == null)
		{
			m_statsProvider = new StatsProvider();
		}

		return m_statsProvider;
	}

	/**
	 * Get the Stat to be displayed
	 * 
	 * @return a List of StatElements sorted (descending)
	 */
	public ArrayList<StatElement> getStatList(int iStat, String refFromName,
			int iSort, String refToName) throws Exception
	{
		Context ctx = BbsApplication.getAppContext();

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(BbsApplication.getAppContext());
		boolean bFilterStats = sharedPrefs.getBoolean("filter_data", true);
		boolean developerMode = sharedPrefs.getBoolean("developer", false);
		
		Reference refFrom = ReferenceStore.getReferenceByName(refFromName, ctx);
		Reference refTo = ReferenceStore.getReferenceByName(refToName, ctx);
		
		if ((refFrom == null) || (refTo == null) || (refFromName == null) || (refToName == null) || (refFromName.equals("")) || (refToName.equals("")))
		{
			Log.e(TAG, "Reference from or to are empty: (" + refFromName + ", " + refToName +")");
			return null;
		}
		if (refFrom.equals(refToName))
		{
			Toast.makeText(ctx, ctx.getString(R.string.message_identical_references, refFromName, refToName), Toast.LENGTH_LONG).show();

		} 
		
		int iPctType = 0;

		if ((!developerMode) && (this.getIsCharging(ctx)))
		{
			ArrayList<StatElement> myRet = new ArrayList<StatElement>();
			myRet.add(new Notification(ctx.getString(R.string.NO_STATS_WHEN_CHARGING)));
			return myRet;
		}

		switch (iStat)
		{
		case 0:
			return getOtherUsageStatList(bFilterStats, refFrom, true, false, refTo);
		case 1:
			return getKernelWakelockStatList(bFilterStats, refFrom,
					iPctType, iSort, refTo);
		case 2:
			return getWakelockStatList(bFilterStats, refFrom, iPctType, iSort, refTo);
		case 3:
			return getAlarmsStatList(bFilterStats, refFrom, refTo);
		case 4:
			return getNetworkUsageStatList(bFilterStats, refFrom, refTo);
		case 5:
			return getCpuStateList(refFrom, refTo, bFilterStats);
		case 6:
			return getProcessStatList(bFilterStats, refFrom, iSort, refTo);
		case 7:
			return getSensorStatList(bFilterStats, refFrom, refTo);

		}

		return new ArrayList<StatElement>();
	}

	/**
	 * Return the timespan between two references
	 * @param refFrom
	 * @param refTo
	 * @return
	 */
	public long getSince(Reference refFrom, Reference refTo)
	{
		long ret = 0;
		long since = 0;

		if ((refFrom != null) && (refTo != null))
		{
			ret =  refTo.m_refBatteryRealtime - refFrom.m_refBatteryRealtime;
			since = refTo.m_creationTime - refFrom.m_creationTime;
			if (LogSettings.DEBUG)
			{
				Log.d(TAG, "Since (not used anymore): " + DateUtils.formatDuration(ret));
				Log.d(TAG, "Since: " + DateUtils.formatDuration(since));
			}

		}
		else
		{
			ret = -1;
		}

		return since;
	}

	/**
	 * Get the Alarm Stat to be displayed
	 * 
	 * @param bFilter
	 *            defines if zero-values should be filtered out
	 * @return a List of Other usages sorted by duration (descending)
	 * @throws Exception
	 *             if the API call failed
	 */

	public ArrayList<StatElement> getAlarmsStatList(boolean bFilter,
			Reference refFrom, Reference refTo) throws Exception
	{
		Context ctx = BbsApplication.getAppContext();

		ArrayList<StatElement> myStats = new ArrayList<StatElement>();

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(ctx);

		// stop straight away of root features are disabled
		// to process alarms we need either root or the perms to access the private API
		if (!SysUtils.hasBatteryStatsPermission(ctx))
		{
			myStats.add(new Notification(ctx.getString(R.string.NO_PERM_ERR)));
			return myStats;
		}

		if ((refFrom == null) || (refTo == null))
		{
				myStats.add(new Notification(ctx.getString(R.string.NO_REF_ERR)));
			return myStats;
		}

		ArrayList<StatElement> myAlarms = null;
		// get the current value
		if ((refTo.m_refAlarms != null) && (!refTo.m_refAlarms.isEmpty()))
		{
			myAlarms = refTo.m_refAlarms;	
		}
		else
		{
			myStats.add(new Notification(ctx.getString(R.string.NO_STATS)));
			return myStats;
		}
		//Collections.sort(myAlarms);

		ArrayList<Alarm> myRetAlarms = new ArrayList<Alarm>();
		// if we are using custom ref. always retrieve "stats current"

		// sort @see
		// com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		String strCurrent = myAlarms.toString();
		String strRef = "";
		String strRefDescr = "";

		if (LogSettings.DEBUG)
		{
			if (refFrom != null)
			{
				strRefDescr = refFrom.whoAmI();
				if (refFrom.m_refAlarms != null)
				{
					strRef = refFrom.m_refAlarms.toString();
				}
				else
				{
					strRef = "Alarms is null";
				}
			}
			else
			{
				strRefDescr = "Reference is null";
			}
			Log.d(TAG, "Processing alarms from " + refFrom.m_fileName + " to " + refTo.m_fileName);

			Log.d(TAG, "Reference used: " + strRefDescr);
			Log.d(TAG, "It is now " + DateUtils.now());

			Log.d(TAG, "Substracting " + strRef);
			Log.d(TAG, "from " + strCurrent);
		}

		for (int i = 0; i < myAlarms.size(); i++)
		{
			Alarm alarm = ((Alarm) myAlarms.get(i)).clone();
			if ((!bFilter) || ((alarm.getWakeups()) > 0))
			{

				alarm.substractFromRef(refFrom.m_refAlarms);

				// we must recheck if the delta process is still above
				// threshold
				if ((!bFilter) || ((alarm.getWakeups()) > 0))
				{
					myRetAlarms.add(alarm);
				}
			}
		}

		Collections.sort(myRetAlarms);

		for (int i = 0; i < myRetAlarms.size(); i++)
		{
			myStats.add((StatElement) myRetAlarms.get(i));
		}

		if (LogSettings.DEBUG)
		{
			Log.d(TAG, "Result " + myStats.toString());
		}

		return myStats;

	}

	public ArrayList<StatElement> getCurrentAlarmsStatList(boolean bFilter) throws Exception
    {

        Context ctx = BbsApplication.getAppContext();
        ArrayList<StatElement> myStats = new ArrayList<StatElement>();

        // stop straight away of root features are disabled
        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(ctx);
        boolean permsNotNeeded = sharedPrefs.getBoolean("ignore_system_app", false);

        ArrayList<StatElement> myAlarms = null;

        // use root if available as root delivers more data
        if (SysUtils.hasBatteryStatsPermission(ctx) && SysUtils.hasDumpsysPermission(ctx))
        {
            myAlarms = AlarmsDumpsys.getAlarms();
        }
        else if (permsNotNeeded || SysUtils.hasBatteryStatsPermission(ctx))
        {
            Log.i(TAG, "Accessing Alarms in API mode as dumpsys has failed");
            BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(ctx);
            int statsType = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            {
                statsType = BatteryStatsTypesLolipop.STATS_CURRENT;
            }
            else
            {
                statsType = BatteryStatsTypes.STATS_CURRENT;
            }

            myAlarms = mStats.getWakeupStats(ctx, statsType);
        }
        else
        {
            return myStats;
        }

        ArrayList<Alarm> myRetAlarms = new ArrayList<Alarm>();
        // if we are using custom ref. always retrieve "stats current"

        // sort @see
        // com.asksven.android.common.privateapiproxies.Walkelock.compareTo

        long elapsedRealtime = SystemClock.elapsedRealtime();
        for (int i = 0; i < myAlarms.size(); i++)
        {
            Alarm alarm = (Alarm) myAlarms.get(i);
            if (alarm != null)
            {
                if ((!bFilter) || ((alarm.getWakeups()) > 0))
                {
                    alarm.setTimeRunning(elapsedRealtime);
                    myRetAlarms.add(alarm);
                }
            }
        }

        Collections.sort(myRetAlarms);

        for (int i = 0; i < myRetAlarms.size(); i++)
        {
            myStats.add((StatElement) myRetAlarms.get(i));
        }

        if (LogSettings.DEBUG)
        {
            Log.d(TAG, "Result " + myStats.toString());
        }

        return myStats;

    }

//	public ArrayList<StatElement> getCurrentAlarmsStatList(boolean bFilter) throws Exception
//	{
//
//		Context ctx = BbsApplication.getAppContext();
//		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
//
//		// stop straight away of root features are disabled
//		SharedPreferences sharedPrefs = PreferenceManager
//				.getDefaultSharedPreferences(ctx);
//
//		ArrayList<StatElement> myAlarms = null;
//
//        BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(ctx);
//        int statsType = 0;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
//        {
//            statsType = BatteryStatsTypesLolipop.STATS_CURRENT;
//        }
//        else
//        {
//            statsType = BatteryStatsTypes.STATS_CURRENT;
//        }
//
//        myAlarms = mStats.getWakeupStats(ctx, statsType);
//
//		ArrayList<Alarm> myRetAlarms = new ArrayList<Alarm>();
//		// if we are using custom ref. always retrieve "stats current"
//
//		// sort @see
//		// com.asksven.android.common.privateapiproxies.Walkelock.compareTo
//
//		long elapsedRealtime = SystemClock.elapsedRealtime();
//		for (int i = 0; i < myAlarms.size(); i++)
//		{
//			Alarm alarm = (Alarm) myAlarms.get(i);
//			if (alarm != null)
//			{
//				if ((!bFilter) || ((alarm.getWakeups()) > 0))
//				{
//					alarm.setTimeRunning(elapsedRealtime);
//					myRetAlarms.add(alarm);
//				}
//			}
//		}
//
//		Collections.sort(myRetAlarms);
//
//		for (int i = 0; i < myRetAlarms.size(); i++)
//		{
//			myStats.add((StatElement) myRetAlarms.get(i));
//		}
//
//		if (LogSettings.DEBUG)
//		{
//			Log.d(TAG, "Result " + myStats.toString());
//		}
//
//		return myStats;
//
//	}

	/**
	 * Get the Alarm Stat to be displayed
	 * 
	 * @param bFilter
	 *            defines if zero-values should be filtered out
	 * @return a List of Other usages sorted by duration (descending)
	 * @throws Exception
	 *             if the API call failed
	 */

	public ArrayList<StatElement> getSensorStatList(boolean bFilter,
			Reference refFrom, Reference refTo) throws Exception
	{
        Context ctx = BbsApplication.getAppContext();
		ArrayList<StatElement> myStats = new ArrayList<StatElement>();

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(ctx);

		// stop straight away of root features are disabled
		// to process alarms we need either root or the perms to access the private API
		if (!SysUtils.hasBatteryStatsPermission(ctx))
		{
			myStats.add(new Notification(ctx.getString(R.string.NO_PERM_ERR)));
			return myStats;
		}

		if ((refFrom == null) || (refTo == null))
		{
				myStats.add(new Notification(ctx.getString(R.string.NO_REF_ERR)));
			return myStats;
		}

		ArrayList<StatElement> mySensorStats = null;

		if ((refTo.m_refSensorUsage != null) && (!refTo.m_refSensorUsage.isEmpty()))
		{
			mySensorStats = refTo.m_refSensorUsage;	
		}
		else
		{
			myStats.add(new Notification(ctx.getString(R.string.NO_STATS)));
			return myStats;
		}

		ArrayList<SensorUsage> myRetSensorStats = new ArrayList<SensorUsage>();
		// if we are using custom ref. always retrieve "stats current"

		// sort @see
		// com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		String strCurrent = mySensorStats.toString();
		String strRef = "";
		String strRefDescr = "";

		if (LogSettings.DEBUG)
		{
			if (refFrom != null)
			{
				strRefDescr = refFrom.whoAmI();
				if (refFrom.m_refSensorUsage != null)
				{
					strRef = refFrom.m_refSensorUsage.toString();
				}
				else
				{
					strRef = "SensorUsage is null";
				}
			}
			else
			{
				strRefDescr = "Reference is null";
			}
			Log.d(TAG, "Processing sensor stats from " + refFrom.m_fileName + " to " + refTo.m_fileName);

			Log.d(TAG, "Reference used: " + strRefDescr);
			Log.d(TAG, "It is now " + DateUtils.now());

			Log.d(TAG, "Substracting " + strRef);
			Log.d(TAG, "from " + strCurrent);
		}

		for (int i = 0; i < mySensorStats.size(); i++)
		{
			SensorUsage sensor = ((SensorUsage) mySensorStats.get(i)).clone();
			if ((!bFilter) || ((sensor.getTotal()) > 0))
			{

				sensor.substractFromRef(refFrom.m_refSensorUsage);

				// we must recheck if the delta process is still above
				// threshold
				if ((!bFilter) || ((sensor.getTotal()) > 0))
				{
					myRetSensorStats.add(sensor);
				}
			}
		}

		Collections.sort(myRetSensorStats);

		for (int i = 0; i < myRetSensorStats.size(); i++)
		{
			myStats.add((StatElement) myRetSensorStats.get(i));
		}

		if (LogSettings.DEBUG)
		{
			Log.d(TAG, "Result " + myStats.toString());
		}

		return myStats;

	}
	
	public ArrayList<StatElement> getCurrentSensorStatList(boolean bFilter) throws Exception
	{
		Context ctx = BbsApplication.getAppContext();

		ArrayList<StatElement> myRetStats = new ArrayList<StatElement>();

		// stop straight away of root features are disabled
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(ctx);

		BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(ctx);
		int statsType = 0;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			statsType = BatteryStatsTypesLolipop.STATS_CURRENT;
		}
		else
		{
			statsType = BatteryStatsTypes.STATS_CURRENT;
		}		

		long elapsedRealtime = SystemClock.elapsedRealtime();

		ArrayList<SensorUsage> mySensorStats = mStats.getSensorStats(ctx, elapsedRealtime, statsType);
		ArrayList<SensorUsage> myStats = new ArrayList<SensorUsage>();
		
		for (int i = 0; i < mySensorStats.size(); i++)
		{
			SensorUsage sensor = (SensorUsage) mySensorStats.get(i);
			if (sensor != null)
			{
				if ((!bFilter) || ((sensor.getTotal()) > 0))
				{
					myStats.add(sensor);
				}
			}
		}

		Collections.sort(myStats);

		for (int i = 0; i < myStats.size(); i++)
		{
			myRetStats.add((StatElement) myStats.get(i));
		}

		if (LogSettings.DEBUG)
		{
			Log.d(TAG, "Result " + myStats.toString());
		}

		return myRetStats;

	}
	
	/**
	 * Get the Process Stat to be displayed
	 * 
	 * @param bFilter
	 *            defines if zero-values should be filtered out
	 * @return a List of Wakelocks sorted by duration (descending)
	 * @throws Exception
	 *             if the API call failed
	 */
	public ArrayList<StatElement> getProcessStatList(boolean bFilter,
			Reference refFrom, int iSort, Reference refTo) throws Exception
	{

		Context ctx = BbsApplication.getAppContext();

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(ctx);

		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		
		if (!SysUtils.hasBatteryStatsPermission(ctx))
		{
			// stop straight away of root features are disabled
				myStats.add(new Notification(ctx.getString(R.string.NO_PERM_ERR)));
				return myStats;
		}
		
		if ((refFrom == null) || (refTo == null))
		{
				myStats.add(new Notification(ctx.getString(R.string.NO_REF_ERR)));
			return myStats;
		}

		ArrayList<StatElement> myProcesses = null;
		ArrayList<Process> myRetProcesses = new ArrayList<Process>();

		if ((refTo.m_refProcesses != null) && (!refTo.m_refProcesses.isEmpty()))
		{
			myProcesses = refTo.m_refProcesses;
		}
		else
		{
			myStats.add(new Notification(ctx.getString(R.string.NO_STATS)));
			return myStats;
		}
		
		String strCurrent = myProcesses.toString();
		String strRef = "";
		String strRefDescr = "";

		if (LogSettings.DEBUG)
		{
			if (refFrom != null)
			{
				strRefDescr = refFrom.whoAmI();
				if (refFrom.m_refProcesses != null)
				{
					strRef = refFrom.m_refProcesses.toString();
				}
				else
				{
					strRef = "Process is null";
				}
			}
			else
			{
				strRefDescr = "Reference is null";
			}
			Log.d(TAG, "Processing processes from " + refFrom.m_fileName + " to " + refTo.m_fileName);

			Log.d(TAG, "Reference used: " + strRefDescr);
			Log.d(TAG, "It is now " + DateUtils.now());

			Log.d(TAG, "Substracting " + strRef);
			Log.d(TAG, "from " + strCurrent);
		}

		// add relevant elements and recalculate the total
		long total = 0;
		for (int i = 0; i < myProcesses.size(); i++)
		{
			Process ps = ((Process) myProcesses.get(i)).clone();
			if ((!bFilter) || ((ps.getSystemTime() + ps.getUserTime()) > 0))
			{
				ps.substractFromRef(refFrom.m_refProcesses);

				// we must recheck if the delta process is still above
				// threshold
				if ((!bFilter)	|| ((ps.getSystemTime() + ps.getUserTime()) > 0))
				{
					total += ps.getSystemTime() + ps.getUserTime();
					myRetProcesses.add(ps);
				}
			}
		}

		// sort by Duration
		Comparator<Process> myCompTime = new Process.ProcessTimeComparator();
		Collections.sort(myRetProcesses, myCompTime);

		for (int i = 0; i < myRetProcesses.size(); i++)
		{
			myRetProcesses.get(i).setTotal(total);
			myStats.add((StatElement) myRetProcesses.get(i));
		}

		if (LogSettings.DEBUG)
		{
			Log.d(TAG, "Result " + myStats.toString());
		}

		return myStats;

	}

	public ArrayList<StatElement> getCurrentProcessStatList(boolean bFilter,
			int iSort) throws Exception
	{

		Context ctx = BbsApplication.getAppContext();

		ArrayList<StatElement> myProcesses = null;
		ArrayList<Process> myRetProcesses = new ArrayList<Process>();

		if ( !SysUtils.hasBatteryStatsPermission(ctx) )
		{
			myProcesses = ProcessStatsDumpsys.getProcesses(ctx);
		}
		else
		{
			BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(ctx);
			int statsType = 0;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			{
				statsType = BatteryStatsTypesLolipop.STATS_CURRENT;
			}
			else
			{
				statsType = BatteryStatsTypes.STATS_CURRENT;
			}		

			myProcesses = mStats.getProcessStats(ctx, statsType);
		}

		// add elements and recalculate the total
		long total = 0;
		for (int i = 0; i < myProcesses.size(); i++)
		{
			Process ps = (Process) myProcesses.get(i);
			if ((!bFilter) || ((ps.getSystemTime() + ps.getUserTime()) > 0))
			{
				total += ps.getSystemTime() + ps.getSystemTime();
				myRetProcesses.add(ps);
			}
		}
		
		// sort by Duration
		Comparator<Process> myCompTime = new Process.ProcessTimeComparator();
		Collections.sort(myRetProcesses, myCompTime);

		if (LogSettings.DEBUG)
		{
			Log.d(TAG, "Result " + myProcesses.toString());
		}

		myProcesses.clear();
		for (int i=0; i < myRetProcesses.size(); i++)
		{
			myRetProcesses.get(i).setTotal(total);
			myProcesses.add(myRetProcesses.get(i));
		}
		
		return myProcesses;

	}

	/**
	 * Get the Wakelock Stat to be displayed
	 * 
	 * @param bFilter
	 *            defines if zero-values should be filtered out
	 * @return a List of Wakelocks sorted by duration (descending)
	 * @throws Exception
	 *             if the API call failed
	 */
	public ArrayList<StatElement> getWakelockStatList(boolean bFilter,
			Reference refFrom, int iPctType, int iSort, Reference refTo) throws Exception
	{
		Context ctx = BbsApplication.getAppContext();
		String entropy = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID); // this is the best practice described here: https://android-developers.googleblog.com/2011/03/identifying-app-installations.html


		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		
		if ( !SysUtils.hasBatteryStatsPermission(ctx))
		{
			myStats.add(new Notification(ctx.getString(R.string.NO_PERM_ERR)));
			return myStats;
		}

		if ((refFrom == null) || (refTo == null))
		{
				myStats.add(new Notification(ctx.getString(R.string.NO_REF_ERR)));
			return myStats;
		}


		ArrayList<StatElement> myWakelocks = null;
		if ((refTo.m_refWakelocks != null) && (!refTo.m_refWakelocks.isEmpty()))
		{
			myWakelocks = refTo.m_refWakelocks;
		}
		else
		{
			myStats.add(new Notification(ctx.getString(R.string.NO_STATS)));
			return myStats;
		}

		ArrayList<Wakelock> myRetWakelocks = new ArrayList<Wakelock>();

		String strCurrent = myWakelocks.toString();

		String strRef = "";
		String strRefDescr = "";

		if (LogSettings.DEBUG)
		{
			if (refFrom != null)
			{
				strRefDescr = refFrom.whoAmI();
				if (refFrom.m_refWakelocks != null)
				{
					strRef = refFrom.m_refWakelocks.toString();
				}
				else
				{
					strRef = "Wakelocks is null";
				}
			}
			else
			{
				strRefDescr = "Reference is null";
			}
			Log.d(TAG, "Processing kernel wakelocks from "
					+ refFrom.m_fileName + " to " + refTo.m_fileName);

			Log.d(TAG, "Reference used: " + strRefDescr);
			Log.d(TAG, "It is now " + DateUtils.now());

			Log.d(TAG, "Substracting " + strRef);
			Log.d(TAG, "from " + strCurrent);
		}

		// sort @see
		// com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		// Collections.sort(myWakelocks);

		for (int i = 0; i < myWakelocks.size(); i++)
		{
			Wakelock wl = ((Wakelock) myWakelocks.get(i)).clone(entropy);
			if ((!bFilter) || ((wl.getDuration() / 1000) > 0))
			{
				// we must distinguish two situations
				// a) we use custom stat type
				// b) we use regular stat type

				wl.substractFromRef(refFrom.m_refWakelocks);

				// we must recheck if the delta process is still above
				// threshold
				if ((!bFilter) || ((wl.getDuration() / 1000) > 0))
				{
					myRetWakelocks.add(wl);
				} else
				{
					if (LogSettings.DEBUG)
					{
						Log.i(TAG, "Skipped " + wl.toString()
								+ " because duration < 1s");
					}
				}
			}
		}

		if (LogSettings.DEBUG)
		{
			Log.i(TAG, "Result has " + myRetWakelocks.size() + " entries");
		}
		// sort @see
		// com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		switch (iSort)
		{
		case 0:
			// by Duration
			Comparator<Wakelock> myCompTime = new Wakelock.WakelockTimeComparator();
			Collections.sort(myRetWakelocks, myCompTime);
			break;
		case 1:
			// by Count
			Comparator<Wakelock> myCompCount = new Wakelock.WakelockCountComparator();
			Collections.sort(myRetWakelocks, myCompCount);
			break;
		}

		for (int i = 0; i < myRetWakelocks.size(); i++)
		{
			myStats.add((StatElement) myRetWakelocks.get(i));
		}

		if (LogSettings.DEBUG)
		{
			Log.d(TAG, "Result " + myStats.toString());
		}

		return myStats;
	}

	public ArrayList<StatElement> getCurrentWakelockStatList(boolean bFilter,
			int iPctType, int iSort) throws Exception
	{
		Context ctx = BbsApplication.getAppContext();

		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		ArrayList<StatElement> myWakelocks = null;
		
		BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(ctx);

		int statsType = 0;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			statsType = BatteryStatsTypesLolipop.STATS_CURRENT;
		}
		else
		{
			statsType = BatteryStatsTypes.STATS_CURRENT;
		}		

		myWakelocks = mStats.getWakelockStats(ctx,
				BatteryStatsTypes.WAKE_TYPE_PARTIAL,
				statsType, iPctType);


		ArrayList<Wakelock> myRetWakelocks = new ArrayList<Wakelock>();


		// sort @see
		// com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		// Collections.sort(myWakelocks);

		for (int i = 0; i < myWakelocks.size(); i++)
		{
			Wakelock wl = (Wakelock) myWakelocks.get(i);
			if ((!bFilter) || ((wl.getDuration() / 1000) > 0))
			{
				myRetWakelocks.add(wl);
			}
		}

		if (LogSettings.DEBUG)
		{
			Log.i(TAG, "Result has " + myRetWakelocks.size() + " entries");
		}
		// sort @see
		// com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		switch (iSort)
		{
		case 0:
			// by Duration
			Comparator<Wakelock> myCompTime = new Wakelock.WakelockTimeComparator();
			Collections.sort(myRetWakelocks, myCompTime);
			break;
		case 1:
			// by Count
			Comparator<Wakelock> myCompCount = new Wakelock.WakelockCountComparator();
			Collections.sort(myRetWakelocks, myCompCount);
			break;
		}

		for (int i = 0; i < myRetWakelocks.size(); i++)
		{
			myStats.add((StatElement) myRetWakelocks.get(i));
		}

		if (LogSettings.DEBUG)
		{
			Log.d(TAG, "Result " + myStats.toString());
		}

		return myStats;
	}

	/**
	 * Get the Kernel Wakelock Stat to be displayed
	 * 
	 * @param bFilter
	 *            defines if zero-values should be filtered out
	 * @return a List of Wakelocks sorted by duration (descending)
	 * @throws Exception
	 *             if the API call failed
	 */
	public ArrayList<StatElement> getKernelWakelockStatList(
			boolean bFilter, Reference refFrom, int iPctType, int iSort, Reference refTo)
			throws Exception
	{
		Context ctx = BbsApplication.getAppContext();

		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(ctx);

		if (!(Wakelocks.fileIsWorldReadableExists() || WakeupSources.fileIsWorldReadableExists()  || SysUtils.hasBatteryStatsPermission(ctx)))
		{
			myStats.add(new Notification(ctx.getString(R.string.KWL_ACCESS_ERROR)));
			return myStats;
		}
		
		if ((refFrom == null) || (refTo == null))
		{
			myStats.add(new Notification(ctx.getString(R.string.NO_REF_ERR)));
			return myStats;
		}

		ArrayList<StatElement> myKernelWakelocks = null;
		
		if ((refTo.m_refKernelWakelocks != null) && (!refTo.m_refKernelWakelocks.isEmpty()))
		{ 
			myKernelWakelocks = refTo.m_refKernelWakelocks;
		}
		else
		{
			myStats.add(new Notification(ctx.getString(R.string.NO_STATS)));
			return myStats;
		}
		
		ArrayList<NativeKernelWakelock> myRetKernelWakelocks = new ArrayList<NativeKernelWakelock>();
		// if we are using custom ref. always retrieve "stats current"

		// sort @see
		// com.asksven.android.common.privateapiproxies.Walkelock.compareTo
//		Collections.sort(myKernelWakelocks);

		String strCurrent = myKernelWakelocks.toString();
		String strRef = "";
		String strRefDescr = "";

		if (LogSettings.DEBUG)
		{
			if (refFrom != null)
			{
				strRefDescr = refFrom.whoAmI();
				if (refFrom.m_refKernelWakelocks != null)
				{
					strRef = refFrom.m_refKernelWakelocks.toString();
				}
				else
				{
					strRef = "kernel wakelocks is null";
				}

			}
			else
			{
				strRefDescr = "Reference is null";
			}
			Log.d(TAG, "Processing kernel wakelocks from "
					+ refFrom.m_fileName + " to " + refTo.m_fileName);

			Log.d(TAG, "Reference used: " + strRefDescr);
			Log.d(TAG, "It is now " + DateUtils.now());

			Log.d(TAG, "Substracting " + strRef);
			Log.d(TAG, "from " + strCurrent);
		}

		for (int i = 0; i < myKernelWakelocks.size(); i++)
		{
			NativeKernelWakelock wl = ((NativeKernelWakelock) myKernelWakelocks.get(i)).clone();
			if ((!bFilter) || ((wl.getDuration()) > 0))
			{

				wl.substractFromRef(refFrom.m_refKernelWakelocks);

				// we must recheck if the delta process is still above
				// threshold
				if ((!bFilter) || ((wl.getDuration()) > 0))
				{
					myRetKernelWakelocks.add(wl);
				}
			}
		}

		// sort @see
		// com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		switch (iSort)
		{
		case 0:
			// by Duration
			Comparator<NativeKernelWakelock> myCompTime = new NativeKernelWakelock.TimeComparator();
			Collections.sort(myRetKernelWakelocks, myCompTime);
			break;
		case 1:
			// by Count
			Comparator<NativeKernelWakelock> myCompCount = new NativeKernelWakelock.CountComparator();
			Collections.sort(myRetKernelWakelocks, myCompCount);
			break;
		}

		for (int i = 0; i < myRetKernelWakelocks.size(); i++)
		{
			myStats.add((StatElement) myRetKernelWakelocks.get(i));
		}

		if (LogSettings.DEBUG)
		{
			Log.d(TAG, "Result " + myStats.toString());
		}

		return myStats;
	}

	public ArrayList<StatElement> getCurrentKernelWakelockStatList(boolean bFilter, int iPctType, int iSort)
			throws Exception
	{
		Context ctx = BbsApplication.getAppContext();

		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		ArrayList<StatElement> myKernelWakelocks = null;
		
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		
		BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(ctx);

		int statsType = 0;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			statsType = BatteryStatsTypesLolipop.STATS_CURRENT;
		}
		else
		{
			statsType = BatteryStatsTypes.STATS_CURRENT;
		}

		myKernelWakelocks = mStats.getKernelWakelockStats(ctx, statsType, false);

		ArrayList<NativeKernelWakelock> myRetKernelWakelocks = new ArrayList<NativeKernelWakelock>();
		// if we are using custom ref. always retrieve "stats current"

		// sort @see
		// com.asksven.android.common.privateapiproxies.Walkelock.compareTo
//		Collections.sort(myKernelWakelocks);


		for (int i = 0; i < myKernelWakelocks.size(); i++)
		{
			NativeKernelWakelock wl = (NativeKernelWakelock) myKernelWakelocks.get(i);
			if ((!bFilter) || ((wl.getDuration()) > 0))
			{
				myRetKernelWakelocks.add(wl);
			}
		}

		// sort @see
		// com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		switch (iSort)
		{
		case 0:
			// by Duration
			Comparator<NativeKernelWakelock> myCompTime = new NativeKernelWakelock.TimeComparator();
			Collections.sort(myRetKernelWakelocks, myCompTime);
			break;
		case 1:
			// by Count
			Comparator<NativeKernelWakelock> myCompCount = new NativeKernelWakelock.CountComparator();
			Collections.sort(myRetKernelWakelocks, myCompCount);
			break;
		}

		for (int i = 0; i < myRetKernelWakelocks.size(); i++)
		{
			myStats.add((StatElement) myRetKernelWakelocks.get(i));
		}

		if (LogSettings.DEBUG)
		{
			Log.d(TAG, "Result " + myStats.toString());
		}

		return myStats;
	}

	/**
	 * Get the Kernel Wakelock Stat to be displayed
	 * 
	 * @param bFilter
	 *            defines if zero-values should be filtered out
	 * @return a List of Wakelocks sorted by duration (descending)
	 * @throws Exception
	 *             if the API call failed
	 */
	public ArrayList<StatElement> getNetworkUsageStatList(
			boolean bFilter, Reference refFrom, Reference refTo) throws Exception
	{
		Context ctx = BbsApplication.getAppContext();

		ArrayList<StatElement> myStats = new ArrayList<StatElement>();			

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(ctx);

		// stop straight away if no root permissions or no perms to access data directly
		if (!SysUtils.hasBatteryStatsPermission(ctx))
		{
			myStats.add(new Notification(ctx.getString(R.string.NO_PERM_ERR)));
			return myStats;
		}

		if ((refFrom == null) || (refTo == null))
		{
				myStats.add(new Notification(ctx.getString(R.string.NO_REF_ERR)));
			return myStats;
		}

		ArrayList<StatElement> myNetworkStats = null;
		
		if ((refTo.m_refNetworkStats != null) && (!refTo.m_refNetworkStats.isEmpty()))
		{
			myNetworkStats = refTo.m_refNetworkStats;
		}
		else
		{
			myStats.add(new Notification(ctx.getString(R.string.NO_STATS)));
			return myStats;
		}

		ArrayList<NetworkUsage> myRetNetworkStats = new ArrayList<NetworkUsage>();
		// if we are using custom ref. always retrieve "stats current"

		// sort @see
		// com.asksven.android.common.privateapiproxies.Walkelock.compareTo
//		Collections.sort(myNetworkStats);

		String strCurrent = "";
		String strRef = "";
		String strRefDescr = "";

		if (LogSettings.DEBUG)
		{
			if (refFrom != null)
			{
				strRefDescr = refFrom.whoAmI();
				if (refFrom.m_refNetworkStats != null)
				{
					strRef = refFrom.m_refNetworkStats.toString();
				}
				else
				{
					strRef = "Network stats is null";
				}

			}
			else
			{
				strRefDescr = "Reference is null";
			}
			Log.d(TAG, "Processing network stats from " + refFrom.m_fileName + " to " + refTo.m_fileName);

			Log.d(TAG, "Reference used: " + strRefDescr);
			Log.d(TAG, "It is now " + DateUtils.now());

			Log.d(TAG, "Substracting " + strRef);
			Log.d(TAG, "from " + strCurrent);
		}

		for (int i = 0; i < myNetworkStats.size(); i++)
		{
			NetworkUsage netStat = ((NetworkUsage) myNetworkStats.get(i)).clone();
			if ((!bFilter) || ((netStat.getTotalBytes()) > 0))
			{
				netStat.substractFromRef(refFrom.m_refNetworkStats);

				// we must recheck if the delta process is still above
				// threshold
				if ((!bFilter) || ((netStat.getTotalBytes()) > 0))
				{
					myRetNetworkStats.add(netStat);
				}
			}
		}

		// recalculate the total
		long total = 0;
		for (int i = 0; i < myRetNetworkStats.size(); i++)
		{
			total += myRetNetworkStats.get(i).getTotalBytes();
		}

		Collections.sort(myRetNetworkStats);

		for (int i = 0; i < myRetNetworkStats.size(); i++)
		{
			myRetNetworkStats.get(i).setTotal(total);
			myStats.add((StatElement) myRetNetworkStats.get(i));
		}

		if (LogSettings.DEBUG)
		{
			Log.d(TAG, "Result " + myStats.toString());
		}

		return myStats;
	}

	public ArrayList<StatElement> getCurrentNetworkUsageStatList(boolean bFilter) throws Exception
	{
		Context ctx = BbsApplication.getAppContext();

		ArrayList<StatElement> myStats = new ArrayList<StatElement>();

		// stop straight away of root features are disabled
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		

		ArrayList<StatElement> myNetworkStats = null;

		BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(ctx);

		int statsType = 0;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			statsType = BatteryStatsTypesLolipop.STATS_CURRENT;
		}
		else
		{
			statsType = BatteryStatsTypes.STATS_CURRENT;
		}

		myNetworkStats = mStats.getNetworkUsageStats(ctx, statsType);

		ArrayList<NetworkUsage> myRetNetworkStats = new ArrayList<NetworkUsage>();


		for (int i = 0; i < myNetworkStats.size(); i++)
		{
			NetworkUsage netStat = (NetworkUsage) myNetworkStats.get(i);
			if ((!bFilter) || ((netStat.getTotalBytes()) > 0))
			{
				myRetNetworkStats.add(netStat);
			}
		}

		// recalculate the total
		long total = 0;
		for (int i = 0; i < myRetNetworkStats.size(); i++)
		{
			total += myRetNetworkStats.get(i).getTotalBytes();
		}

		Collections.sort(myRetNetworkStats);

		for (int i = 0; i < myRetNetworkStats.size(); i++)
		{
			myRetNetworkStats.get(i).setTotal(total);
			myStats.add((StatElement) myRetNetworkStats.get(i));
		}

		if (LogSettings.DEBUG)
		{
			Log.d(TAG, "Result " + myStats.toString());
		}

		return myStats;
	}

	/**
	 * Get the CPU states to be displayed
	 * 
	 * @param bFilter
	 *            defines if zero-values should be filtered out
	 * @return a List of Other usages sorted by duration (descending)
	 * @throws Exception
	 *             if the API call failed
	 */
	public ArrayList<StatElement> getCpuStateList(Reference refFrom, Reference refTo, boolean bFilter)
			throws Exception

	{
        Context ctx = BbsApplication.getAppContext();

		// List to store the other usages to
		ArrayList<StatElement> myStates = refTo.m_refCpuStates;

		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		if ((refFrom == null) || (refTo == null))
		{
				myStats.add(new Notification(ctx.getString(R.string.NO_REF_ERR)));
			return myStats;
		}

		if (refTo.m_refCpuStates == null)
		{
			myStats.add(new Notification(ctx.getString(R.string.NO_STATS)));
			return myStats;
		}

		String strCurrent = myStates.toString();
		String strRef = "";
		String strRefDescr = "";

		if (LogSettings.DEBUG)
		{
			if (refFrom != null)
			{
				strRefDescr = refFrom.whoAmI();
				if (refFrom.m_refCpuStates != null)
				{
					strRef = refFrom.m_refCpuStates.toString();
				} else
				{
					strRef = "CPU States is null";
				}

			}
			Log.d(TAG, "Processing CPU States from " + refFrom.m_fileName + " to " + refTo.m_fileName);

			Log.d(TAG, "Reference used: " + strRefDescr);
			Log.d(TAG, "It is now " + DateUtils.now());

			Log.d(TAG, "Substracting " + strRef);
			Log.d(TAG, "from " + strCurrent);
		}

		ArrayList<State> myResultStates = new ArrayList<State>();
		
		for (int i = 0; i < myStates.size(); i++)
		{
			State state = ((State) myStates.get(i)).clone();

			state.substractFromRef(refFrom.m_refCpuStates);
			if ((!bFilter) || ((state.m_duration) > 0))
			{
				myResultStates.add(state);
			}
		}
		
		Collections.sort(myResultStates);
		
		for (int i=0; i < myResultStates.size(); i++)
		{
			myStats.add(myResultStates.get(i));
		}
		
		return myStats;

	}

	public ArrayList<StatElement> getCurrentCpuStateList(boolean bFilter)
			throws Exception

	{
		// List to store the other usages to
		ArrayList<State> myStates = CpuStates.getTimesInStates();

		ArrayList<StatElement> myStats = new ArrayList<StatElement>();

		if (myStates == null)
		{
			return myStats;
		}

		String strCurrent = myStates.toString();
		String strRef = "";
		String strRefDescr = "";


		for (int i = 0; i < myStates.size(); i++)
		{
			State state = myStates.get(i);
			if ((!bFilter) || ((state.m_duration) > 0))
			{
				myStats.add(state);
			}
		}
		return myStats;

	}

	/**
	 * Get the permissions
	 * 
	 * @return a List of permissions
	 */
	public Map<String, Permission> getPermissionMap(Context context)
	{
		Map<String, Permission> myStats = new Hashtable<String, Permission>();
		Class<Manifest.permission> perms = Manifest.permission.class;
		Field[] fields = perms.getFields();
		int size = fields.length;
		PackageManager pm = context.getPackageManager();
		for (int i = 0; i < size; i++)
		{
			try
			{
				Field field = fields[i];
				PermissionInfo info = pm.getPermissionInfo(
						field.get(field.getName()).toString(),
						PackageManager.GET_META_DATA);
				Permission perm = new Permission();
				perm.name = info.name;
				final CharSequence chars = info.loadDescription(context
						.getPackageManager());
				perm.description = chars == null ? "no description" : chars
						.toString();
				perm.level = info.protectionLevel;
				myStats.put(perm.name, perm);
			} catch (Exception e)
			{
				Log.e(TAG, e.getMessage());
			}
		}
		return myStats;

	}

	/**
	 * Returns the permissions <uses-permissions> requested by a package in its manifest
	 * @param context
	 * @param packageName 
	 * @return the list of permissions
	 */
	public ArrayList<String> getRequestedPermissionListForPackage(Context context, String packageName)
	{
		ArrayList<String> myStats = new ArrayList<String>();
		
		try
		{
			PackageInfo pkgInfo = context.getPackageManager().getPackageInfo(
				    packageName, 
				    PackageManager.GET_PERMISSIONS
				  );
			String[] requestedPermissions = pkgInfo.requestedPermissions;
		    if (requestedPermissions == null)
		    {
		    	myStats.add("No requested permissions");
		    }
		    else
		    {
		    	for (int i = 0; i < requestedPermissions.length; i++)
		    	{
		    		myStats.add(requestedPermissions[i]);
				}			    	
		    }
		} catch (Exception e)
		{
			Log.e(TAG, e.getMessage());
		}
		return myStats;

	}

	/**
	 * Returns the permissions <uses-permissions> requested by a package in its manifest
	 * @param context
	 * @param packageName 
	 * @return the list of permissions
	 */
	public ArrayList<String> getReceiverListForPackage(Context context, String packageName)
	{
		ArrayList<String> myStats = new ArrayList<String>();
		
		try
		{
			PackageInfo pkgInfo = context.getPackageManager().getPackageInfo(
				    packageName, 
				    PackageManager.GET_RECEIVERS
				  );
			ActivityInfo[] receivers = pkgInfo.receivers;
		    if (receivers == null)
		    {
		    	myStats.add("No receivers");
		    }
		    else
		    {
		    	for (int i = 0; i < receivers.length; i++)
		    	{
		    		myStats.add(receivers[i].name);
				}			    	
		    }
		} catch (Exception e)
		{
			Log.e(TAG, e.getMessage());
		}
		return myStats;

	}

	/**
	 * Returns the services <service> defined by a package in its manifest
	 * @param context
	 * @param packageName 
	 * @return the list of services
	 */
	public ArrayList<String> getServiceListForPackage(Context context, String packageName)
	{
		ArrayList<String> myStats = new ArrayList<String>();
		
		try
		{
			PackageInfo pkgInfo = context.getPackageManager().getPackageInfo(
				    packageName, 
				    PackageManager.GET_SERVICES
				  );
			ServiceInfo[] services = pkgInfo.services;
		    if (services == null)
		    {
		    	myStats.add("None");
		    }
		    else
		    {
		    	for (int i = 0; i < services.length; i++)
		    	{
		    		myStats.add(services[i].name);
				}			    	
		    }
		} catch (Exception e)
		{
			Log.e(TAG, e.getMessage());
		}
		return myStats;

	}

	/**
	 * Get the Other Usage Stat to be displayed
	 * 
	 * @param bFilter
	 *            defines if zero-values should be filtered out
	 * @return a List of Other usages sorted by duration (descending)
	 * @throws Exception
	 *             if the API call failed
	 */
	public ArrayList<StatElement> getOtherUsageStatList(boolean bFilter,
			Reference refFrom, boolean bFilterView, boolean bWidget, Reference refTo)
			throws Exception
	{

        Context ctx = BbsApplication.getAppContext();

		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		// if on of the refs is null return
		if ((refFrom == null) || (refTo == null))
		{
				myStats.add(new Notification(ctx.getString(R.string.NO_REF_ERR)));
			return myStats;
		}
		
		// List to store the other usages to
		ArrayList<StatElement> myUsages = new ArrayList<StatElement>();

		if ((refTo.m_refOther != null) && (!refTo.m_refOther.isEmpty()))
		{
			myUsages = refTo.m_refOther;
		}
		else
		{
			myStats.add(new Notification(ctx.getString(R.string.NO_STATS)));
			return myStats;
		}

		String strRefFrom = "";
		String strRefTo = "";
		String strRefFromDescr = "";
		String strRefToDescr = "";

		if (LogSettings.DEBUG)
		{
			if (refFrom != null)
			{
				strRefFromDescr = refFrom.whoAmI();
				if (refFrom.m_refOther != null)
				{
					strRefFrom = refFrom.m_refOther.toString();
				} else
				{
					strRefFrom = "Other is null";
				}

			}
			
			if (refTo != null)
			{
				strRefToDescr = refTo.whoAmI();
				if (refTo.m_refOther != null)
				{
					strRefTo = refTo.m_refOther.toString();
				} else
				{
					strRefTo = "Other is null";
				}

			}

			Log.d(TAG, "Processing Other from " + refFrom.m_fileName + " to " + refTo.m_fileName);

			Log.d(TAG, "Reference from: " + strRefFromDescr);
			Log.d(TAG, "Reference to: " + strRefToDescr);
			Log.d(TAG, "It is now " + DateUtils.now());

			Log.d(TAG, "Substracting " + strRefFrom);
			Log.d(TAG, "from " + strRefTo);
		}

		for (int i = 0; i < myUsages.size(); i++)
		{
			Misc usage = ((Misc)myUsages.get(i)).clone();
			if (LogSettings.DEBUG)
			{
				Log.d(TAG, "Current value: " + usage.getName() + " " + usage.getData(StatsProvider.getInstance().getSince(refFrom, refTo)));
			}
			if ((!bFilter) || (usage.getTimeOn() > 0))
			{
				usage.substractFromRef(refFrom.m_refOther);
				if ((!bFilter) || (usage.getTimeOn() > 0))
				{
					if (LogSettings.DEBUG)
					{
						Log.d(TAG, "Result value: " + usage.getName() + " "	+ usage.getData(StatsProvider.getInstance().getSince(refFrom, refTo)));
					}
					myStats.add((StatElement) usage);
				}
			}
		}
		return myStats;
	}

	public ArrayList<StatElement> getCurrentOtherUsageStatList(boolean bFilter,
			boolean bFilterView, boolean bWidget)
			throws Exception
	{
		Context ctx = BbsApplication.getAppContext();

		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		// List to store the other usages to
		ArrayList<StatElement> myUsages = new ArrayList<StatElement>();

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(ctx);

		BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(ctx);

		long rawRealtime = SystemClock.elapsedRealtime() * 1000;

		long uptime = SystemClock.uptimeMillis();

		long elaspedRealtime = rawRealtime / 1000;

		long batteryRealtime = 0;
		try
		{
			batteryRealtime = mStats.getBatteryRealtime(rawRealtime);
		}
		catch (Exception e)
		{
			Log.e(TAG, "An exception occured processing battery realtime. Message: " + e.getMessage());
			Log.e(TAG, "Exception: " + Log.getStackTraceString(e));
		}

		int statsType = 0;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			statsType = BatteryStatsTypesLolipop.STATS_CURRENT;
		}
		else
		{
			statsType = BatteryStatsTypes.STATS_CURRENT;
		}

		long whichRealtime = mStats.computeBatteryRealtime(rawRealtime, statsType) / 1000;

		long timeBatteryUp = mStats.computeBatteryUptime(
				SystemClock.uptimeMillis() * 1000, statsType) / 1000;

		if (CommonLogSettings.DEBUG)
		{
			Log.i(TAG, "whichRealtime = " + whichRealtime + " batteryRealtime = " + batteryRealtime + " timeBatteryUp=" + timeBatteryUp);
		}

		long timeScreenOn = mStats.getScreenOnTime(batteryRealtime, statsType) / 1000;
		long timePhoneOn = mStats.getPhoneOnTime(batteryRealtime, statsType) / 1000;

		long timeWifiOn = 0;
		long timeWifiRunning = 0;
		if (sharedPrefs.getBoolean("show_other_wifi", true) && !bWidget)
		{
			try
			{
				timeWifiOn = mStats.getWifiOnTime(batteryRealtime, statsType) / 1000;
				timeWifiRunning = mStats.getGlobalWifiRunningTime(
						batteryRealtime, statsType) / 1000;
			} catch (BatteryInfoUnavailableException e)
			{
				timeWifiOn = 0;
				timeWifiRunning = 0;
				Log.e(TAG,
						"A batteryinfo error occured while retrieving Wifi data");
			}
		}

		long timeBluetoothOn 		= 0;

		if (sharedPrefs.getBoolean("show_other_bt", true) && !bWidget)
		{
			try
			{
				if (Build.VERSION.SDK_INT >= 21)
				{
					timeBluetoothOn 	= mStats.getBluetoothInStateTime(ctx, statsType) / 1000;
				}
				else
				{
					timeBluetoothOn = mStats.getBluetoothOnTime(batteryRealtime, statsType) / 1000;
				}
			}
			catch (BatteryInfoUnavailableException e)
			{
				timeBluetoothOn = 0;
				Log.e(TAG,
						"A batteryinfo error occured while retrieving BT data");
			}

		}

		long interactiveTime			= 0;
		long powerSaveModeEnabledTime 	= 0;
		long deviceIdleModeEnabledTime 	= 0;
		long getDeviceIdlingTime 		= 0;


		if (sharedPrefs.getBoolean("show_other_doze", true) && !bWidget)
		{
			try
			{
				if (Build.VERSION.SDK_INT >= 21)
				{
					interactiveTime 			= mStats.getInteractiveTime(batteryRealtime, statsType) / 1000;
					powerSaveModeEnabledTime 	= mStats.getPowerSaveModeEnabledTime(batteryRealtime, statsType) / 1000;
					deviceIdleModeEnabledTime = mStats.getDeviceIdleModeEnabledTime(batteryRealtime, statsType) / 1000;

					// these are not available anymore from SDK24 on
					if (Build.VERSION.SDK_INT <= 23)
					{
						getDeviceIdlingTime = mStats.getDeviceIdlingTime(batteryRealtime, statsType) / 1000;
					}
					else
					{
						// we need to switch to getDeviceIdleModeTime
					}
				}
			}
			catch (BatteryInfoUnavailableException e)
			{
				timeBluetoothOn = 0;
				Log.e(TAG,
						"A batteryinfo error occured while retrieving doze mode data");
			}

		}

		long syncTime = 0;
		try
		{
			if ((Build.VERSION.SDK_INT >= 21) && (Build.VERSION.SDK_INT < 27))
			{
				syncTime 	= mStats.getSyncOnTime(ctx, batteryRealtime, statsType) / 1000;
			}
		}
		catch (BatteryInfoUnavailableException e)
		{
			Log.e(TAG, "A batteryinfo error occured while retrieving sensor and sync stats");
		}

		long timeNoDataConnection = 0;
		long timeSignalNone = 0;
		long timeSignalPoor = 0;
		long timeSignalModerate = 0;
		long timeSignalGood = 0;
		long timeSignalGreat = 0;
		if (sharedPrefs.getBoolean("show_other_signal", true))
		{
			try
			{
				timeNoDataConnection = mStats.getPhoneDataConnectionTime(
						BatteryStatsTypes.DATA_CONNECTION_NONE,
						batteryRealtime, statsType) / 1000;
				timeSignalNone = mStats.getPhoneSignalStrengthTime(
						BatteryStatsTypes.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
						batteryRealtime, statsType) / 1000;
				timeSignalPoor = mStats.getPhoneSignalStrengthTime(
						BatteryStatsTypes.SIGNAL_STRENGTH_POOR,
						batteryRealtime, statsType) / 1000;
				timeSignalModerate = mStats.getPhoneSignalStrengthTime(
						BatteryStatsTypes.SIGNAL_STRENGTH_MODERATE,
						batteryRealtime, statsType) / 1000;
				timeSignalGood = mStats.getPhoneSignalStrengthTime(
						BatteryStatsTypes.SIGNAL_STRENGTH_GOOD,
						batteryRealtime, statsType) / 1000;
				timeSignalGreat = mStats.getPhoneSignalStrengthTime(
						BatteryStatsTypes.SIGNAL_STRENGTH_GREAT,
						batteryRealtime, statsType) / 1000;
			} catch (BatteryInfoUnavailableException e)
			{
				timeNoDataConnection = 0;
				timeSignalNone = 0;
				timeSignalPoor = 0;
				timeSignalModerate = 0;
				timeSignalGood = 0;
				timeSignalGreat = 0;
				Log.e(TAG,
						"A batteryinfo error occured while retrieving Signal data");
			}
		}

		long timeScreenDark = 0;
		long timeScreenDim = 0;
		long timeScreenMedium = 0;
		long timeScreenLight = 0;
		long timeScreenBright = 0;
		if (sharedPrefs.getBoolean("show_other_screen_brightness", true))
		{
			try
			{
				timeScreenDark = mStats.getScreenBrightnessTime(
						BatteryStatsTypes.SCREEN_BRIGHTNESS_DARK,
						batteryRealtime, statsType) / 1000;
				timeScreenDim = mStats.getScreenBrightnessTime(
						BatteryStatsTypes.SCREEN_BRIGHTNESS_DIM,
						batteryRealtime, statsType) / 1000;
				timeScreenMedium = mStats.getScreenBrightnessTime(
						BatteryStatsTypes.SCREEN_BRIGHTNESS_MEDIUM,
						batteryRealtime, statsType) / 1000;
				timeScreenLight = mStats.getScreenBrightnessTime(
						BatteryStatsTypes.SCREEN_BRIGHTNESS_LIGHT,
						batteryRealtime, statsType) / 1000;
				timeScreenBright = mStats.getScreenBrightnessTime(
						BatteryStatsTypes.SCREEN_BRIGHTNESS_BRIGHT,
						batteryRealtime, statsType) / 1000;
			}
			catch (BatteryInfoUnavailableException e)
			{
				timeScreenDark = 0;
				timeScreenDim = 0;
				timeScreenMedium = 0;
				timeScreenLight = 0;
				timeScreenBright = 0;
				Log.e(TAG,
						"A batteryinfo error occured while retrieving Screen brightness data");
			}
		}

		// deep sleep times are independent of stat type
		long timeDeepSleep = (SystemClock.elapsedRealtime() - SystemClock
				.uptimeMillis());


		Misc deepSleepUsage = new Misc("Deep Sleep", timeDeepSleep, elaspedRealtime);
		if (LogSettings.DEBUG)
		{
			Log.d(TAG, "Added Deep sleep:" + deepSleepUsage.toString());
		}

		if ((!bFilter) || (deepSleepUsage.getTimeOn() > 0))
		{
			myUsages.add(deepSleepUsage);
		}

		if (timeBatteryUp > 0)
		{
			myUsages.add(new Misc(LABEL_MISC_AWAKE, timeBatteryUp - timeScreenOn, elaspedRealtime));
		}

		if (timeScreenOn > 0)
		{
			myUsages.add(new Misc("Screen On", timeScreenOn, elaspedRealtime));
		}

		if (timePhoneOn > 0)
		{
			myUsages.add(new Misc("Phone On", timePhoneOn, elaspedRealtime));
		}

		if ((timeWifiOn > 0)
				&& (!bFilterView || sharedPrefs.getBoolean("show_other_wifi",
						true)))
		{
			myUsages.add(new Misc("Wifi On", timeWifiOn, elaspedRealtime));
		}

		if ((timeWifiRunning > 0)
				&& (!bFilterView || sharedPrefs.getBoolean("show_other_wifi",
						true)))

		{
			myUsages.add(new Misc("Wifi Running", timeWifiRunning, elaspedRealtime));
		}

		if ((timeBluetoothOn > 0)
				&& (!bFilterView || sharedPrefs.getBoolean("show_other_bt",
						true)))

		{
			myUsages.add(new Misc("Bluetooth On", timeBluetoothOn, elaspedRealtime));
		}

		if (Build.VERSION.SDK_INT >= 6)
		{
			if ((interactiveTime > 0)
					&& (!bFilterView || sharedPrefs.getBoolean("show_other_doze",
							true)))
			{
				myUsages.add(new Misc("Doze Interactive Time", interactiveTime, elaspedRealtime));
			}

			if ((powerSaveModeEnabledTime > 0)
					&& (!bFilterView || sharedPrefs.getBoolean("show_other_doze",
							true)))
			{
				myUsages.add(new Misc("Doze Powersave Time", powerSaveModeEnabledTime, elaspedRealtime));
			}

			if ((deviceIdleModeEnabledTime > 0)
					&& (!bFilterView || sharedPrefs.getBoolean("show_other_doze",
							true)))
			{
				myUsages.add(new Misc("Doze Idle Mode Time", deviceIdleModeEnabledTime, elaspedRealtime));
			}

			if ((getDeviceIdlingTime > 0)
					&& (!bFilterView || sharedPrefs.getBoolean("show_other_doze",
							true)))
			{
				myUsages.add(new Misc("Doze Idling Time", getDeviceIdlingTime, elaspedRealtime));
			}

			if (syncTime > 0)
			{
				myUsages.add(new Misc("Sync", syncTime, elaspedRealtime));
			}
		}

		if ((timeNoDataConnection > 0)
				&& (!bFilterView || sharedPrefs.getBoolean(
						"show_other_connection", true)))

		{
			myUsages.add(new Misc("No Data Connection", timeNoDataConnection, elaspedRealtime));
		}

		if ((timeSignalNone > 0)
				&& (!bFilterView || sharedPrefs.getBoolean("show_other_signal",
						true)))

		{
			myUsages.add(new Misc("No or Unknown Signal", timeSignalNone, elaspedRealtime));
		}

		if ((timeSignalPoor > 0)
				&& (!bFilterView || sharedPrefs.getBoolean("show_other_signal",
						true)))
		{
			myUsages.add(new Misc("Poor Signal", timeSignalPoor, elaspedRealtime));
		}

		if ((timeSignalModerate > 0)
				&& (!bFilterView || sharedPrefs.getBoolean("show_other_signal",
						true)))
		{
			myUsages.add(new Misc("Moderate Signal", timeSignalModerate, elaspedRealtime));
		}

		if ((timeSignalGood > 0)
				&& (!bFilterView || sharedPrefs.getBoolean("show_other_signal",
						true)))
		{
			myUsages.add(new Misc("Good Signal", timeSignalGood, elaspedRealtime));
		}

		if ((timeSignalGreat > 0)
				&& (!bFilterView || sharedPrefs.getBoolean("show_other_signal",
						true)))
		{
			myUsages.add(new Misc("Great Signal", timeSignalGreat, elaspedRealtime));
		}

		if ((timeScreenDark > 0)
				&& (!bFilterView || sharedPrefs.getBoolean("show_other_screen_brightness",
						true)))

		{
			myUsages.add(new Misc("Screen dark", timeScreenDark, elaspedRealtime));
		}

		if ((timeScreenDim > 0)
				&& (!bFilterView || sharedPrefs.getBoolean("show_other_screen_brightness",
						true)))

		{
			myUsages.add(new Misc("Screen dimmed", timeScreenDim, elaspedRealtime));
		}

		if ((timeScreenMedium > 0)
				&& (!bFilterView || sharedPrefs.getBoolean("show_other_screen_brightness",
						true)))

		{
			myUsages.add(new Misc("Screen medium", timeScreenMedium, elaspedRealtime));
		}
		if ((timeScreenLight > 0)
				&& (!bFilterView || sharedPrefs.getBoolean("show_other_screen_brightness",
						true)))

		{
			myUsages.add(new Misc("Screen light", timeScreenLight, elaspedRealtime));
		}

		if ((timeScreenBright > 0)
				&& (!bFilterView || sharedPrefs.getBoolean("show_other_screen_brightness",
						true)))

		{
			myUsages.add(new Misc("Screen bright", timeScreenBright, elaspedRealtime));
		}


		for (int i = 0; i < myUsages.size(); i++)
		{
			Misc usage = (Misc)myUsages.get(i);
			if (LogSettings.DEBUG)
			{
				Log.d(TAG,	"Current value: " + usage.getName() + " " + usage.toString());
			}
			if ((!bFilter) || (usage.getTimeOn() > 0))
			{
				myStats.add((StatElement) usage);
			}
		}
		return myStats;
	}

	/**
	 * Get the battery level lost since a given ref
	 * 
	 * @return the lost battery level
	 */
	public int getBatteryLevelStat(Reference refFrom, Reference refTo)
	{
		long lLevelTo = 0;
		long lLevelFrom = 0;
		
		if (refFrom != null)
		{
			lLevelFrom = refFrom.m_refBatteryLevel;
		}

		if (refTo != null)
		{
			lLevelTo = refTo.m_refBatteryLevel;
		}
		if ((LogSettings.DEBUG) && (refFrom != null) && (refTo != null))
		{
			Log.d(TAG, "Current Battery Level:" + lLevelTo);
			Log.d(TAG, "Battery Level between " + refFrom.m_fileName + " and " + refTo.m_fileName + ":" + lLevelFrom + " to " + lLevelTo);
		}
		int level = (int) (lLevelTo - lLevelFrom);


		return level;
	}

	/**
	 * Get the battery level lost since a given ref
	 * 
	 * @return the lost battery level
	 */
	public String getBatteryLevelFromTo(Reference refFrom, Reference refTo, boolean concise)
	{
		// deep sleep times are independent of stat type
		long lLevelTo = 0;
		long lLevelFrom = 0;
		long sinceH = -1;

		String levelTo = "-";
		String levelFrom = "-";
		
		if (refFrom != null)
		{
			lLevelFrom = refFrom.m_refBatteryLevel;
			levelFrom = String.valueOf(lLevelFrom);
		}

		if (refTo != null)
		{
			lLevelTo = refTo.m_refBatteryLevel;
			levelTo = String.valueOf(lLevelTo);
		}
		
		if ((LogSettings.DEBUG) && (refFrom != null) && (refTo != null))
		{
			Log.d(TAG, "Current Battery Level:" + levelTo);
			Log.d(TAG, "Battery Level between " + refFrom.m_fileName + " and " + refTo.m_fileName + ":" + levelFrom);
		}
		
		String drop_per_hour = "";
		try
		{
			sinceH = getSince(refFrom, refTo);
			// since is in ms but x 100 in order to respect proportions of formatRatio (we call it with % and it normally calculates % so there is a factor 100
			sinceH = sinceH / 10 / 60 / 60;
			drop_per_hour = StringUtils.formatRatio(lLevelFrom - lLevelTo, sinceH) + "/h";
		}
		catch (Exception e)
		{
			drop_per_hour = "";
			Log.e(TAG, "Error retrieving since");
		}
		
		String ret = "Bat.: " + getBatteryLevelStat(refFrom, refTo) + "%";
		if (!concise) { ret += "(" + levelFrom	+ "% to " + levelTo + "%)"; }
		ret += " [" + drop_per_hour + "]";
		
		return ret;
	}

	/**
	 * Get the battery voltage lost since a given ref
	 * 
	 * @return the lost battery level
	 */
	public int getBatteryVoltageStat(Reference refFrom, Reference refTo)
	{
		int voltageTo = -1;
		int voltageFrom = -1;
		long sinceH = -1;


		if (refFrom != null)
		{
			voltageFrom = refFrom.m_refBatteryVoltage;
		}

		if (refTo != null)
		{
			voltageTo = refTo.m_refBatteryVoltage;
		}

		if ((LogSettings.DEBUG) && (refFrom != null) && (refTo != null))
		{
			Log.d(TAG, "Current Battery Voltage:" + voltageTo);
			Log.d(TAG, "Battery Voltage between " + refFrom.m_fileName + " and " + refTo.m_fileName + ":" + voltageFrom + " to " + voltageTo);
		}

		int voltage = (int) (voltageTo - voltageFrom);

		return voltage;
	}

	/**
	 * Get the battery voltage lost since a given ref
	 * 
	 * @return the lost battery level
	 */
	public String getBatteryVoltageFromTo(Reference refFrom, Reference refTo)
	{
		// deep sleep times are independent of stat type
		int voltageTo = getBatteryVoltage();
		int voltageFrom = -1;
		long sinceH = -1;


		if (refFrom != null)
		{
			voltageFrom = refFrom.m_refBatteryVoltage;
		}

		if ((LogSettings.DEBUG) && (refFrom != null) && (refTo != null))
		{
			Log.d(TAG, "Current Battery Voltage:" + voltageTo);
			Log.d(TAG, "Battery Voltage between " + refFrom.m_fileName + " and " + refTo.m_fileName + ":" + voltageFrom);
		}
		
		String drop_per_hour = "";
		try
		{
			sinceH = getSince(refFrom, refTo);
			// since is in ms but x 100 in order to respect proportions of formatRatio (we call it with % and it normally calculates % so there is a factor 100
			sinceH = sinceH / 10 / 60 / 60;
			drop_per_hour = StringUtils.formatRatio(voltageFrom - voltageTo, sinceH) + "/h";
		}
		catch (Exception e)
		{
			drop_per_hour = "";
			Log.e(TAG, "Error retrieving since");
		}
		

		return "(" + voltageFrom + "-" + voltageTo + ")" + " [" + drop_per_hour + "]";
	}

	public StatElement getElementByKey(ArrayList<StatElement> myList, String key)
	{
		Log.i(TAG, "Looking for key: " + key);
		StatElement ret = null;

		if (myList == null)
		{
			Log.e(TAG, "getElementByKey failed: null list");
			return null;
		}

		for (int i = 0; i < myList.size(); i++)
		{
			StatElement item = myList.get(i);
			if (item.getName().equals(key))

			{
				ret = item;
				break;
			}
		}

		if (ret == null)
		{
			Log.e(TAG, "getElementByKey failed: " + key + " was not found");
		}
		return ret;
	}

	public long sum(ArrayList<StatElement> myList)
	{
		long ret = 0;

		if (myList == null)
		{
			Log.d(TAG, "sum was called with a null list");
			return 0;
		}
		if (myList.size() == 0)
		{
			Log.d(TAG, "sum was called with an empty list");
			return 0;
		}

		for (int i = 0; i < myList.size(); i++)
		{
			// make sure nothing goes wrong
			try
			{
				StatElement item = myList.get(i);
				ret += item.getValues()[0];
			} catch (Exception e)
			{
				Log.e(TAG, "An error occcured " + e.getMessage());
				GenericLogger.stackTrace(TAG, e.getStackTrace());
			}
		}
		return ret;

	}

	/**
	 * Returns true if a ref "since screen off" was stored
	 * 
	 * @return true is a custom ref exists
	 */
	public boolean hasScreenOffRef()
	{
		Reference thisRef = ReferenceStore.getReferenceByName(Reference.SCREEN_OFF_REF_FILENAME, BbsApplication.getAppContext());

		return ((thisRef != null) && (thisRef.m_refOther != null));
	}

	/**
	 * Returns true if a custom ref was stored
	 * 
	 * @return true is a custom ref exists
	 */
	public boolean hasCustomRef()
	{
		Reference thisRef = ReferenceStore.getReferenceByName(Reference.CUSTOM_REF_FILENAME, BbsApplication.getAppContext());
		return ((thisRef != null) && (thisRef.m_refOther != null));
	}

	/**
	 * Returns true if a since charged ref was stored
	 * 
	 * @return true is a since charged ref exists
	 */
	public boolean hasSinceChargedRef()
	{
		Reference thisRef = ReferenceStore.getReferenceByName(Reference.CHARGED_REF_FILENAME, BbsApplication.getAppContext());

		return ((thisRef != null) && (thisRef.m_refKernelWakelocks != null));
	}

	/**
	 * Returns true if a since unplugged ref was stored
	 * 
	 * @return true is a since unplugged ref exists
	 */
	public boolean hasSinceUnpluggedRef()
	{
		Reference thisRef = ReferenceStore.getReferenceByName(Reference.UNPLUGGED_REF_FILENAME, BbsApplication.getAppContext());

		return ((thisRef != null) && (thisRef.m_refKernelWakelocks != null));
	}

	/**
	 * Returns true if a since boot ref was stored
	 * 
	 * @return true is a since unplugged ref exists
	 */
	public boolean hasSinceBootRef()
	{
		Reference thisRef = ReferenceStore.getReferenceByName(Reference.BOOT_REF_FILENAME, BbsApplication.getAppContext());

		return ((thisRef != null) && (thisRef.m_refKernelWakelocks != null));
	}

	/**
	 * Saves all data to a point in time defined by user This data will be used
	 * in a custom "since..." stat type
	 */
	public void setCustomReference(int iSort)
	{
		Reference thisRef = new Reference(Reference.CUSTOM_REF_FILENAME, Reference.TYPE_CUSTOM);
		ReferenceStore.put(Reference.CUSTOM_REF_FILENAME, populateReference(iSort, thisRef), BbsApplication.getAppContext());
	}

	/**
	 * Saves all data at the current point in time
	 */
	public void setCurrentReference(int iSort)
	{
		Reference thisRef = new Reference(Reference.CURRENT_REF_FILENAME, Reference.TYPE_CURRENT);
		ReferenceStore.put(Reference.CURRENT_REF_FILENAME, populateReference(iSort, thisRef), BbsApplication.getAppContext());
	}

	/**
	 * Saves all data at the current point in time
	 */
	public synchronized String setTimedReference(int iSort)
	{
		String fileName = Reference.TIMER_REF_FILENAME + DateUtils.format(System.currentTimeMillis(), DateUtils.DATE_FORMAT_NOW);
		Reference thisRef = new Reference(fileName, Reference.TYPE_TIMER);
		ReferenceStore.put(fileName, populateReference(iSort, thisRef), BbsApplication.getAppContext());
		
		return fileName;
	}

	/**
	 * Returns a n uncached current references with only "other", "wakelocks" and "kernel wakelocks" stats (for use in widgets)
	 */
	public Reference getUncachedPartialReference(int iSort)
	{
		Reference thisRef = new Reference(Reference.CURRENT_REF_FILENAME, Reference.TYPE_CURRENT);
		partiallyPopulateReference(iSort, thisRef);
		return thisRef;
	}

	/**
	 * Saves all data to a point in time when the screen goes off This data will
	 * be used in a custom "since..." stat type
	 */
	public void setReferenceSinceScreenOff(int iSort)
	{
		Reference thisRef = new Reference(Reference.SCREEN_OFF_REF_FILENAME, Reference.TYPE_EVENT);
		ReferenceStore.put(Reference.SCREEN_OFF_REF_FILENAME, populateReference(iSort, thisRef), BbsApplication.getAppContext());
		
	}

	/**
	 * Saves all data to a point in time when the screen goes in
	 * be used in a custom "since..." stat type
	 */
	public void setReferenceScreenOn(int iSort)
	{
		Reference thisRef = new Reference(Reference.SCREEN_ON_REF_FILENAME, Reference.TYPE_EVENT);
		ReferenceStore.put(Reference.SCREEN_ON_REF_FILENAME, populateReference(iSort, thisRef), BbsApplication.getAppContext());
		
		// clean "current from cache"
		ReferenceStore.invalidate(Reference.CURRENT_REF_FILENAME, BbsApplication.getAppContext());
	}

	/**
	 * Saves data when battery is fully charged This data will be used in the
	 * "since charged" stat type
	 */
	public void setReferenceSinceCharged(int iSort)
	{
		Reference thisRef = new Reference(Reference.CHARGED_REF_FILENAME, Reference.TYPE_EVENT);
		ReferenceStore.put(Reference.CHARGED_REF_FILENAME, populateReference(iSort, thisRef), BbsApplication.getAppContext());
		
		// clean "current from cache"
		ReferenceStore.invalidate(Reference.CURRENT_REF_FILENAME, BbsApplication.getAppContext());

	}

	/**
	 * Saves data when the phone is unplugged This data will be used in the
	 * "since unplugged" stat type
	 */
	public void setReferenceSinceUnplugged(int iSort)
	{
		Reference thisRef = new Reference(Reference.UNPLUGGED_REF_FILENAME, Reference.TYPE_EVENT);
		ReferenceStore.put(Reference.UNPLUGGED_REF_FILENAME, populateReference(iSort, thisRef), BbsApplication.getAppContext());
		
		// clean "current from cache"
		ReferenceStore.invalidate(Reference.CURRENT_REF_FILENAME, BbsApplication.getAppContext());
	}

	/**
	 * Saves data when the phone is booted This data will be used in the
	 * "since unplugged" stat type
	 */
	public void setReferenceSinceBoot(int iSort)
	{
		Reference thisRef = new Reference(Reference.BOOT_REF_FILENAME, Reference.TYPE_EVENT);
		ReferenceStore.put(Reference.BOOT_REF_FILENAME, populateReference(iSort, thisRef), BbsApplication.getAppContext());
	}

	/**
	 * Saves a reference to cache and persists it
	 */
	private synchronized Reference populateReference(int iSort, Reference refs)
	{
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(BbsApplication.getAppContext());
		
		int statsType = 0;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			statsType = BatteryStatsTypesLolipop.STATS_CURRENT;
		}
		else
		{
			statsType = BatteryStatsTypes.STATS_CURRENT;
		}		

		
		// we are going to retrieve a reference: make sure data does not come from the cache
		if (SysUtils.hasBatteryStatsPermission(BbsApplication.getAppContext()))
		{
			BatteryStatsProxy.getInstance(BbsApplication.getAppContext()).invalidate();
		}
		

		boolean bFilterStats = sharedPrefs.getBoolean("filter_data", true);
		int iPctType = 0;

		try
		{
			refs.m_refOther = null;
			refs.m_refWakelocks = null;
			refs.m_refKernelWakelocks = null;
			refs.m_refNetworkStats = null;

			refs.m_refAlarms = null;
			refs.m_refProcesses = null;
			refs.m_refCpuStates = null;
			refs.m_refSensorUsage = null;
			
			try
			{
				refs.m_refKernelWakelocks = getCurrentKernelWakelockStatList(bFilterStats, iPctType, iSort);
			}
			catch (Exception e)
			{
				Log.e(TAG, "An exception occured processing kernel wakelocks. Message: " + e.getMessage());
				Log.e(TAG, "Exception: " + Log.getStackTraceString(e));				
			}
			
			if ( SysUtils.hasBatteryStatsPermission(BbsApplication.getAppContext()))
			{
				try
				{
					refs.m_refWakelocks = getCurrentWakelockStatList(bFilterStats, iPctType, iSort);
				}
				catch (Exception e)
				{
					Log.e(TAG, "An exception occured processing partial wakelocks. Message: " + e.getMessage());
					Log.e(TAG, "Exception: " + Log.getStackTraceString(e));				
				}
			}
			else
			{
				Log.i(TAG, "Skipped getCurrentWakelockStatList: pre-conditions were not met");
			}
			
			try
			{
				refs.m_refOther = getCurrentOtherUsageStatList(bFilterStats, false, false);
			}
			catch (Exception e)
			{
				Log.e(TAG, "An exception occured processing other. Message: " + e.getMessage());
				Log.e(TAG, "Exception: " + Log.getStackTraceString(e));				
			}

			try
			{
				refs.m_refCpuStates = getCurrentCpuStateList(bFilterStats);
			}
			catch (Exception e)
			{
				Log.e(TAG, "An exception occured processing CPU states. Message: " + e.getMessage());
				Log.e(TAG, "Exception: " + Log.getStackTraceString(e));				
			}

			if ( SysUtils.hasBatteryStatsPermission(BbsApplication.getAppContext()))
			{
				try
				{
					refs.m_refProcesses = getCurrentProcessStatList(bFilterStats, iSort);
				}
				catch (Exception e)
				{
					Log.e(TAG, "An exception occured processing processes. Message: " + e.getMessage());
					Log.e(TAG, "Exception: " + Log.getStackTraceString(e));				
				}
			}
			else
			{
				Log.i(TAG, "Skipped getCurrentProcessStatList: pre-conditions were not met");
			}
			try
			{
				refs.m_refBatteryRealtime = getBatteryRealtime(statsType);
			}
			catch (Exception e)
			{
				Log.e(TAG, "An exception occured processing battery realtime. Message: " + e.getMessage());
				Log.e(TAG, "Exception: " + Log.getStackTraceString(e));				
			}

			try
			{
				refs.m_refBatteryLevel = getBatteryLevel();
				refs.m_refBatteryVoltage = getBatteryVoltage();
			} catch (ReceiverCallNotAllowedException e)
			{
				Log.e(TAG, "An exception occured. Message: " + e.getMessage());
				Log.e(TAG, "Exception: " + Log.getStackTraceString(e));
				refs.m_refBatteryLevel = 0;
				refs.m_refBatteryVoltage = 0;
			}

			// After that we go on and try to write the rest. If this part
			// fails at least there will be a partial ref saved
			try
			{
				refs.m_refNetworkStats = getCurrentNetworkUsageStatList(bFilterStats);
			}
			catch (Exception e)
			{
				Log.e(TAG, "An exception occured processing network. Message: " + e.getMessage());
				Log.e(TAG, "Exception: " + Log.getStackTraceString(e));				
			}

			try
			{
				refs.m_refAlarms = getCurrentAlarmsStatList(bFilterStats);
			}
			catch (Exception e)
			{
				Log.e(TAG, "An exception occured processing alarms. Message: " + e.getMessage());
				Log.e(TAG, "Exception: " + Log.getStackTraceString(e));				
			}

			try
			{
                refs.m_refSensorUsage = getCurrentSensorStatList(bFilterStats);
			}
			catch (Exception e)
			{
				Log.e(TAG, "An exception occured processing sensors. Message: " + e.getMessage());
				Log.e(TAG, "Exception: " + Log.getStackTraceString(e));				
			}

		}
		catch (Exception e)
		{
			Log.e(TAG, "An exception occured. Message: " + e.getMessage());
			// Log.e(TAG, "Callstack", e.fillInStackTrace());
			Log.e(TAG, "Exception: " + Log.getStackTraceString(e));

			refs.m_refOther = null;
			refs.m_refWakelocks = null;
			refs.m_refKernelWakelocks = null;
			refs.m_refNetworkStats = null;
			refs.m_refAlarms = null;
			refs.m_refProcesses = null;
			refs.m_refCpuStates = null;
			refs.m_refSensorUsage = null;
			
			refs.m_refBatteryRealtime = 0;
			refs.m_refBatteryLevel = 0;
			refs.m_refBatteryVoltage = 0;

		}
		// update timestamp
		refs.setTimestamp();

		return refs;
	}

	/**
	 * Saves a reference to cache and persists it
	 */
	private Reference partiallyPopulateReference(int iSort, Reference refs)
	{
		
		// we are going to retrieve a reference: make sure data does not come from the cache
		BatteryStatsProxy.getInstance(BbsApplication.getAppContext()).invalidate();
		
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(BbsApplication.getAppContext());
		
		int statsType = 0;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			statsType = BatteryStatsTypesLolipop.STATS_CURRENT;
		}
		else
		{
			statsType = BatteryStatsTypes.STATS_CURRENT;
		}		


		boolean bFilterStats = sharedPrefs.getBoolean("filter_data", true);
		int iPctType = 0;

		try
		{
			refs.m_refOther 			= null;
			refs.m_refWakelocks 		= null;
			refs.m_refKernelWakelocks 	= null;
			refs.m_refAlarms 			= null;
			refs.m_refProcesses 		= null;
			refs.m_refCpuStates 		= null;
			refs.m_refSensorUsage 		= null;

			refs.m_refKernelWakelocks 	= getCurrentKernelWakelockStatList(bFilterStats, iPctType, iSort);
			if ( SysUtils.hasBatteryStatsPermission(BbsApplication.getAppContext()))
			{
				refs.m_refWakelocks 		= getCurrentWakelockStatList(bFilterStats, iPctType, iSort);
			}
			else
			{
				Log.i(TAG, "Skipped getCurrentWakelockStatList: pre-conditions were not met");
			}
			
			refs.m_refOther 			= getCurrentOtherUsageStatList(bFilterStats, false, false);
			refs.m_refBatteryRealtime 	= getBatteryRealtime(statsType);


			try
			{
				refs.m_refBatteryLevel 	= getBatteryLevel();
				refs.m_refBatteryVoltage = getBatteryVoltage();
			} catch (ReceiverCallNotAllowedException e)
			{
				Log.e(TAG, "An exception occured. Message: " + e.getMessage());
				Log.e(TAG, "Exception: " + Log.getStackTraceString(e));
				refs.m_refBatteryLevel = 0;
				refs.m_refBatteryVoltage = 0;
			}
		}
		catch (Exception e)
		{
			Log.e(TAG, "An exception occured. Message: " + e.getMessage());
		}
		// update timestamp
		refs.setTimestamp();

		return refs;
	}

	/**
	 * Returns the battery realtime since a given reference
	 * 
	 * @param iStatType
	 *            the reference
	 * @return the battery realtime
	 */
	public long getBatteryRealtime(int iStatType)
			throws BatteryInfoUnavailableException
	{
		Context ctx = BbsApplication.getAppContext();

		long rawRealtime = SystemClock.elapsedRealtime() * 1000;
		long whichRealtime = 0;
		
		int statsType = 0;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			statsType = BatteryStatsTypesLolipop.STATS_CURRENT;
		}
		else
		{
			statsType = BatteryStatsTypes.STATS_CURRENT;
		}		

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);

		if (!SysUtils.hasBatteryStatsPermission(ctx))
		{
			whichRealtime = rawRealtime;
			return whichRealtime;
		}

		BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(ctx);

		whichRealtime = mStats.computeBatteryRealtime(rawRealtime,
				statsType) / 1000;

		if ((iStatType == StatsProvider.STATS_CUSTOM) && (ReferenceStore.getReferenceByName(Reference.CUSTOM_REF_FILENAME, ctx) != null))
		{			
			whichRealtime -= ReferenceStore.getReferenceByName(Reference.CUSTOM_REF_FILENAME, ctx).m_refBatteryRealtime;
		}
		else if ((iStatType == StatsProvider.STATS_SCREEN_OFF)
				&& (ReferenceStore.getReferenceByName(Reference.SCREEN_OFF_REF_FILENAME, ctx) != null))
		{
			whichRealtime -= ReferenceStore.getReferenceByName(Reference.SCREEN_OFF_REF_FILENAME, ctx).m_refBatteryRealtime;
		}
		else if ((iStatType == StatsProvider.STATS_BOOT)
				&& (ReferenceStore.getReferenceByName(Reference.BOOT_REF_FILENAME, ctx) != null))
		{
			whichRealtime -= ReferenceStore.getReferenceByName(Reference.BOOT_REF_FILENAME, ctx).m_refBatteryRealtime;
		}
		
		Log.i(TAG, "rawRealtime = " + rawRealtime);
		Log.i(TAG, "whichRealtime = " + whichRealtime);
		
		return whichRealtime;
	}

	public static boolean getIsCharging(Context context)
	{
	    boolean isPlugged= false;
	    Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
	    int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
	    isPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
	    if (VERSION.SDK_INT > VERSION_CODES.JELLY_BEAN)
	    {
	        isPlugged = isPlugged || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;
	    }
	    return isPlugged;
	}


	public static String getWritableFilePath()
    {
        Context ctx = BbsApplication.getAppContext();

        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(ctx);

        String path = "";
        try
        {
            // open file for writing
            File root;
			try
			{
				root = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
			}
			catch (Exception e)
			{
				root = Environment.getExternalStorageDirectory();
			}

            // check if file can be written
            if (root.canWrite())
            {
                path = root.getAbsolutePath();
            }
            else
            {
                // we need to fall back
                try
                {
                    root = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                }
                catch (Exception e)
                {
                    root = Environment.getExternalStorageDirectory();
                }

                if (root.canWrite())
                {
                    path = root.getAbsolutePath();
                }
                else
                {
                    path = "";
                    Log.i(TAG, "Error. " + path + " couldn't be written");

                }

            }
        } catch (Exception e)
        {
            Log.e(TAG, "Exception: " + e.getMessage());
        }

        return path;
    }

	/**
	 * Dumps relevant data to an output file
	 * 
	 */
	@SuppressLint("NewApi")
	public Uri writeLogcatToFile()
	{
		Context ctx = BbsApplication.getAppContext();

		Uri fileUri = null;

		if (!DataStorage.isExternalStorageWritable())
		{
			Log.e(TAG, "External storage can not be written");
			Toast.makeText(ctx, ctx.getString(R.string.message_external_storage_write_error),
					Toast.LENGTH_SHORT).show();
		}

		String path = getWritableFilePath();

		if (!path.equals(""))
        {
            String filename = "logcat-"
                    + DateUtils.now("yyyy-MM-dd_HHmmssSSS") + ".txt";
            Util.run("logcat -v time -d > " + path + "/" + filename);
            fileUri = Uri.fromFile(new File(path + "/" + filename));

            // workaround: force mediascanner to run
            DataStorage.forceMediaScanner(ctx, fileUri);
        }
        else
        {
				Log.i(TAG,"Write error. *" + path + "* couldn't be written");
		}

		return fileUri;
	}

	@SuppressLint("NewApi")
	public Uri writeDmesgToFile()
	{
		Context ctx = BbsApplication.getAppContext();

		Uri fileUri = null;
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(ctx);

		if (!DataStorage.isExternalStorageWritable())
		{
			Log.e(TAG, "External storage can not be written");
			Toast.makeText(ctx, ctx.getString(R.string.message_external_storage_write_error),
					Toast.LENGTH_SHORT).show();
		}

        String path = getWritableFilePath();

        if (!path.equals(""))
        {
            String filename = "dmesg-"
                    + DateUtils.now("yyyy-MM-dd_HHmmssSSS") + ".txt";

			Util.run("dmesg > " + path + "/" + filename);

            fileUri = Uri.fromFile(new File(path + "/" + filename));
            // workaround: force mediascanner to run
            DataStorage.forceMediaScanner(ctx, fileUri);
        }
        else
        {
            Log.i(TAG,"Write error. *" + path + "* couldn't be written");
        }


		return fileUri;
	}

	/**
	 * translate the stat type (see arrays.xml) to the corresponding label
	 * 
	 * @return the stat type
	 */
	public static String statTypeToLabel(int statType)
	{

		String strRet = "";
		switch (statType)
		{
		case 0:
			strRet = "Since Charged";
			break;
		case 3:
			strRet = "Since Unplugged";
			break;
		case 4:
			strRet = "Custom Reference";
			break;
		case 5:
			strRet = "Since Screen off";
			break;
		case 6:
			strRet = "Since Boot";
			break;
		default:
			Log.e(TAG, "No label was found for stat type " + statType);
			break;

		}
		return strRet;
	}

	/**
	 * translate the stat type (see arrays.xml) to the corresponding short label
	 * 
	 * @return the stat type
	 */
	public static String statTypeToLabelShort(int statType)
	{

		String strRet = "";
		switch (statType)
		{
		case 0:
			strRet = "Charged";
			break;
		case 3:
			strRet = "Unpl.";
			break;
		case 4:
			strRet = "Custom";
			break;
		case 5:
			strRet = "Scr. off";
			break;
		case 6:
			strRet = "Boot";
			break;
		default:
			Log.e(TAG, "No label was found for stat type " + statType);
			break;

		}
		return strRet;
	}

	/**
	 * translate the stat type (see arrays.xml) to the corresponding label
	 * 
	 * @return the stat type
	 */
	public String statTypeToUrl(int statType)
	{
		String strRet = statTypeToLabel(statType);

		// remove spaces
		StringTokenizer st = new StringTokenizer(strRet, " ", false);

		String strCleaned = "";
		while (st.hasMoreElements())
		{
			strCleaned += st.nextElement();
		}
		return strCleaned;
	}

	/**
	 * translate the stat (see arrays.xml) to the corresponding label
	 * 
	 * @return the stat
	 */
	private String statToLabel(int iStat)
	{
		String strRet = "";
		String[] statsArray = BbsApplication.getAppContext().getResources().getStringArray(
				R.array.stats);
		strRet = statsArray[iStat];

		return strRet;
	}

	/**
	 * translate the stat (see arrays.xml) to the corresponding label
	 * 
	 * @return the stat
	 */
	public String statToUrl(int stat)
	{
		String strRet = statToLabel(stat);

		// remove spaces
		StringTokenizer st = new StringTokenizer(strRet, " ", false);

		String strCleaned = "";
		while (st.hasMoreElements())
		{
			strCleaned += st.nextElement();
		}
		return strCleaned;
	}

	/**
	 * translate the spinner position (see arrays.xml) to the stat type
	 * 
	 * @param position
	 *            the spinner position
	 * @return the stat type
	 */
	public static int statTypeFromPosition(int position)
	{
		int iRet = 0;
		switch (position)
		{
		case 0:
			iRet = STATS_CHARGED;
			break;
		case 1:
			iRet = STATS_UNPLUGGED;
			break;
		case 2:
			iRet = STATS_CUSTOM;
			break;
		case 3:
			iRet = STATS_SCREEN_OFF;
			break;
		case 4:
			iRet = STATS_BOOT;
			break;
		default:
			Log.e(TAG, "No stat type was found for position " + position);
			break;

		}
		return iRet;
	}

	/**
	 * translate the stat type to the spinner position (see arrays.xml)
	 * 
	 * @param iStatType
	 *            the stat type
	 * @return the spinner position
	 */
	public int positionFromStatType(int iStatType)
	{
		int iRet = 0;
		switch (iStatType)
		{
		case STATS_CHARGED:
			iRet = 0;
			break;
		case STATS_UNPLUGGED:
			iRet = 1;
			break;
		case STATS_CUSTOM:
			iRet = 2;
			break;
		case STATS_SCREEN_OFF:
			iRet = 3;
			break;
		case STATS_BOOT:
			iRet = 4;
			break;
		default:
			Log.e(TAG, "No position was found for stat type " + iStatType);
			break;

		}
		return iRet;
	}

	/**
	 * Returns the current battery level as an int [0..100] or -1 if invalid
	 */
	int getBatteryLevel()
	{
		// check the battery level and if 100% the store "since charged" ref
		Intent batteryIntent = BbsApplication.getAppContext()
				.registerReceiver(null,
						new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

		int rawlevel = batteryIntent
				.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		double scale = batteryIntent
				.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		double level = -1;
		if (rawlevel >= 0 && scale > 0)
		{
			// normalize level to [0..1]
			level = rawlevel / scale;
		}
		return (int) (level * 100);
	}

	/**
	 * Returns the current battery voltage as a double [0..1] or -1 if invalid
	 */
	int getBatteryVoltage()
	{
		// check the battery level and if 100% the store "since charged" ref
		Intent batteryIntent = BbsApplication.getAppContext()
				.registerReceiver(null,
						new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

		int voltage = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE,
				-1);
		return voltage;
	}


	/**
	 * Adds an alarm to schedule a wakeup to save a reference
	 */
	public static boolean scheduleActiveMonAlarm(Context ctx)
	{
		Log.i(TAG, "active_mon_enabled called");
		
		// create a new one starting to count NOW
		
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    	    	
		int iInterval = prefs.getInt("active_mon_freq", 60);

		long fireAt = System.currentTimeMillis() + (iInterval * 60 * 1000);

		Intent intent = new Intent(ctx, ActiveMonAlarmReceiver.class);

		PendingIntent sender = PendingIntent.getBroadcast(ctx, ActiveMonAlarmReceiver.ACTIVE_MON_ALARM,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);

		// Get the AlarmManager service
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, fireAt, sender);

		return true;
	}
	
	/**
	 * Cancels the current alarm (if existing)
	 */
	public static void cancelActiveMonAlarm(Context ctx)
	{
		// check if there is an intent pending
		Intent intent = new Intent(ctx, ActiveMonAlarmReceiver.class);

		PendingIntent sender = PendingIntent.getBroadcast(ctx, ActiveMonAlarmReceiver.ACTIVE_MON_ALARM,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);

		if (sender != null)
		{

			// Get the AlarmManager service
			AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
			am.cancel(sender);
		}
	}
	
	public static boolean isActiveMonAlarmScheduled(Context ctx)
	{
		Intent intent = new Intent(ctx, ActiveMonAlarmReceiver.class);
		boolean alarmUp = (PendingIntent.getBroadcast(ctx, ActiveMonAlarmReceiver.ACTIVE_MON_ALARM, 
		        intent, 
		        PendingIntent.FLAG_NO_CREATE) != null);

		if (alarmUp)
		{
		    Log.i("myTag", "Alarm is already active");
		}
		
		return alarmUp;
	}
	
	public static void testAPI()
	{
		 
		// test against BatteryStatsProxy
		BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(BbsApplication.getAppContext());
		
		long rawRealtime = SystemClock.elapsedRealtime() * 1000; 	
		long batteryRealtime = 0;
		Long res = 0L;
		try
		{
			batteryRealtime = mStats.getBatteryRealtime(rawRealtime);
			if (batteryRealtime > 0)
			{
				Log.i(TAG_TEST, "Passed: getBatteryRealtime");
			}
			else
			{
				Log.e(TAG_TEST, "FAILED: getBatteryRealtime");
			}
			
			if (Build.VERSION.SDK_INT < 6)
			{
				res = mStats.getBluetoothOnTime(batteryRealtime, getStatsType());
				if (res > 0)
				{
					Log.i(TAG_TEST, "Passed: getBluetoothOnTime");
				}
				else
				{
					Log.e(TAG_TEST, "FAILED: getBluetoothOnTime");
				}
			}
			
			res = mStats.getSensorOnTime(BbsApplication.getAppContext(), batteryRealtime, getStatsType());
			if (res > 0)
			{
				Log.i(TAG_TEST, "Passed: getSensorOnTime");
			}
			else
			{
				Log.e(TAG_TEST, "FAILED: getSensorOnTime");
			}
			
			if (Build.VERSION.SDK_INT >= 6)
			{	
				res = mStats.getSyncOnTime(BbsApplication.getAppContext(), batteryRealtime, getStatsType());
				if (res > 0)
				{
					Log.i(TAG_TEST, "Passed: getSyncOnTime");
				}
				else
				{
					Log.e(TAG_TEST, "FAILED: getSyncOnTime");
				}
			}
		}
		catch (Exception e)
		{
			Log.e(TAG_TEST, "Test threw exception: " + e.getMessage());			
		}		
	}

	private static int getStatsType()
	{
		int statsType = 0;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			statsType = BatteryStatsTypesLolipop.STATS_CURRENT;
		}
		else
		{
			statsType = BatteryStatsTypes.STATS_CURRENT;
		}		
		
		return statsType;
	}
}
