/*
 * Copyright (C) 2011-2012<Re asksven
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
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
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;






//import com.asksven.andoid.common.contrib.Shell;
//import com.asksven.andoid.common.contrib.Shell.SU;
import com.asksven.andoid.common.contrib.Util;
import com.asksven.android.common.CommonLogSettings;
import com.asksven.android.common.RootShell;
import com.asksven.android.common.kernelutils.AlarmsDumpsys;
import com.asksven.android.common.kernelutils.CpuStates;
import com.asksven.android.common.kernelutils.NativeKernelWakelock;
import com.asksven.android.common.kernelutils.Netstats;
import com.asksven.android.common.kernelutils.OtherStatsDumpsys;
import com.asksven.android.common.kernelutils.PartialWakelocksDumpsys;
import com.asksven.android.common.kernelutils.ProcessStatsDumpsys;
import com.asksven.android.common.kernelutils.State;
import com.asksven.android.common.kernelutils.Wakelocks;
import com.asksven.android.common.kernelutils.WakeupSources;
import com.asksven.android.common.privateapiproxies.Alarm;
import com.asksven.android.common.privateapiproxies.BatteryInfoUnavailableException;
import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.android.common.privateapiproxies.BatteryStatsTypes;
import com.asksven.android.common.privateapiproxies.Misc;
import com.asksven.android.common.privateapiproxies.NetworkUsage;
import com.asksven.android.common.privateapiproxies.Process;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.android.common.privateapiproxies.Wakelock;
import com.asksven.android.common.utils.DataStorage;
import com.asksven.android.common.utils.DateUtils;
import com.asksven.android.common.utils.GenericLogger;
import com.asksven.android.common.utils.StringUtils;
import com.asksven.android.common.utils.SysUtils;
import com.asksven.betterbatterystats.ActiveMonAlarmReceiver;
import com.asksven.betterbatterystats.LogSettings;
import com.asksven.betterbatterystats.R;

/**
 * Singleton provider for all the statistics
 * 
 * @author sven
 * 
 */
public class StatsProvider
{
	/** the singleton instance */
	static StatsProvider m_statsProvider = null;

	/** the application context */
	static Context m_context = null;

	/** constant for custom stats */
	// dependent on arrays.xml
	public final static int STATS_CHARGED 		= 0;
	public final static int STATS_UNPLUGGED 	= 3;
	public final static int STATS_CUSTOM 		= 4;
	public final static int STATS_SCREEN_OFF 	= 5;
	public final static int STATS_BOOT 			= 6;

	/** the logger tag */
	static String TAG = "StatsProvider";

	/**
	 * The constructor (hidden)
	 */
	private StatsProvider()
	{
	}

	/**
	 * returns a singleton instance
	 * 
	 * @param ctx
	 *            the application context
	 * @return the singleton StatsProvider
	 */
	public static StatsProvider getInstance(Context ctx)
	{
		if (m_statsProvider == null)
		{
			m_statsProvider = new StatsProvider();
			m_context = ctx;

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
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(m_context);
		boolean bFilterStats = sharedPrefs.getBoolean("filter_data", true);
		boolean developerMode = sharedPrefs.getBoolean("developer", false);
		
		Reference refFrom = ReferenceStore.getReferenceByName(refFromName, m_context);
		Reference refTo = ReferenceStore.getReferenceByName(refToName, m_context);
		
		if ((refFrom == null) || (refTo == null) || (refFromName == null) || (refToName == null) || (refFromName.equals("")) || (refToName.equals("")))
		{
			Log.e(TAG, "Reference from or to are empty: (" + refFromName + ", " + refToName +")");
			return null;
		}
		if (refFrom.equals(refToName))
		{
			Toast.makeText(m_context, "An error occured. Both stats are identical (" + refFromName + ")", Toast.LENGTH_LONG).show();			

		} 
		
		int iPctType = Integer.valueOf(sharedPrefs.getString("default_wl_ref",
				"0"));

		if ((!developerMode) && (this.getIsCharging()))
		{
			ArrayList<StatElement> myRet = new ArrayList<StatElement>();
			myRet.add(new Misc(Reference.NO_STATS_WHEN_CHARGING, 0, 0));
			return myRet;
		}
		// try
		// {
		// constants are related to arrays.xml string-array name="stats"
		switch (iStat)
		{
		case 0:
			return getOtherUsageStatList(bFilterStats, refFrom, true, false, refTo);
		case 1:
			return getNativeKernelWakelockStatList(bFilterStats, refFrom,
					iPctType, iSort, refTo);
		case 2:
			return getWakelockStatList(bFilterStats, refFrom, iPctType, iSort, refTo);
		case 3:
			return getAlarmsStatList(bFilterStats, refFrom, refTo);
		case 4:
			return getNativeNetworkUsageStatList(bFilterStats, refFrom, refTo);
		case 5:
			return getCpuStateList(refFrom, refTo, bFilterStats);
		case 6:
			return getProcessStatList(bFilterStats, refFrom, iSort, refTo);

		}

		// }
		// catch (BatteryInfoUnavailableException e)
		// {
		//
		// }
		// catch (Exception e)
		// {
		// Log.e(TAG, "Exception: " + e.getMessage());
		// Log.e(TAG, "Callstack: " + e.fillInStackTrace());
		// throw new Exception();
		//
		// }

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



//	public static Reference getReferenceByName(String refName)
//	{
//		if (m_refStore.containsKey(refName))
//		{
//			return m_refStore.get(refName);
//		}
//		else
//		{
//			Log.e(TAG, "getReference was called with an unknown name "
//					+ refName + ". No reference found");
//			return null;
//		}
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

	public ArrayList<StatElement> getAlarmsStatList(boolean bFilter,
			Reference refFrom, Reference refTo) throws Exception
	{
		ArrayList<StatElement> myStats = new ArrayList<StatElement>();

		// stop straight away of root features are disabled
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(m_context);
		boolean rootEnabled = sharedPrefs.getBoolean("root_features", false);
		if (!rootEnabled)
		{
			myStats.add(new Misc(Reference.NO_ROOT_ERR, 1, 1));
			return myStats;
		}

		if ((refFrom == null) || (refTo == null))
		{
				myStats.add(new Misc(Reference.GENERIC_REF_ERR, 1, 1));
			return myStats;
		}

		ArrayList<StatElement> myAlarms = null;
//		// get the current value
		if ((refTo != null) && (refTo.m_refAlarms != null))
		{
			myAlarms = refTo.m_refAlarms;	
		}
		else
		{
			return null;
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
				} else
				{
					strRef = "Alarms is null";
				}
			} else
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
				if ((refFrom != null)
						&& (refFrom.m_refAlarms != null))
				{
					alarm.substractFromRef(refFrom.m_refAlarms);

					// we must recheck if the delta process is still above
					// threshold
					if ((!bFilter) || ((alarm.getWakeups()) > 0))
					{
						myRetAlarms.add(alarm);
					}
				} else
				{
					myRetAlarms.clear();
					if (refFrom != null)
					{
						myRetAlarms.add(new Alarm(refFrom
								.getMissingRefError()));
					} else
					{
						myRetAlarms.add(new Alarm(
								Reference.GENERIC_REF_ERR));
					}
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
		ArrayList<StatElement> myStats = new ArrayList<StatElement>();

		// stop straight away of root features are disabled
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(m_context);
		boolean rootEnabled = sharedPrefs.getBoolean("root_features", false);
		if (!rootEnabled)
		{
			return myStats;
		}

		ArrayList<StatElement> myAlarms = null;
		// get the current value
		myAlarms = AlarmsDumpsys.getAlarms();
		//Collections.sort(myAlarms);

		ArrayList<Alarm> myRetAlarms = new ArrayList<Alarm>();
		// if we are using custom ref. always retrieve "stats current"

		// sort @see
		// com.asksven.android.common.privateapiproxies.Walkelock.compareTo

		for (int i = 0; i < myAlarms.size(); i++)
		{
			Alarm alarm = (Alarm) myAlarms.get(i);
			if ((!bFilter) || ((alarm.getWakeups()) > 0))
			{
				myRetAlarms.add(alarm);
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

		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		
		if ( (Build.VERSION.SDK_INT >= 19) && !SysUtils.hasBatteryStatsPermission(m_context) )
		{
			// stop straight away of root features are disabled
			SharedPreferences sharedPrefs = PreferenceManager
					.getDefaultSharedPreferences(m_context);

			boolean rootEnabled = sharedPrefs.getBoolean("root_features", false);
			if (!rootEnabled)
			{
				myStats.add(new Misc(Reference.NO_ROOT_ERR, 1, 1));
				return myStats;
			}
		}
		
		if ((refFrom == null) || (refTo == null))
		{
				myStats.add(new Misc(Reference.GENERIC_REF_ERR, 1, 1));
			return myStats;
		}

		ArrayList<StatElement> myProcesses = null;
		ArrayList<Process> myRetProcesses = new ArrayList<Process>();

		if ((refTo != null) && (refTo.m_refProcesses != null))
		{
			myProcesses = refTo.m_refProcesses;
		}
		else
		{
			return null;
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
				} else
				{
					strRef = "Process is null";
				}
			} else
			{
				strRefDescr = "Reference is null";
			}
			Log.d(TAG, "Processing processes from " + refFrom.m_fileName + " to " + refTo.m_fileName);

			Log.d(TAG, "Reference used: " + strRefDescr);
			Log.d(TAG, "It is now " + DateUtils.now());

			Log.d(TAG, "Substracting " + strRef);
			Log.d(TAG, "from " + strCurrent);
		}

		for (int i = 0; i < myProcesses.size(); i++)
		{
			Process ps = ((Process) myProcesses.get(i)).clone();
			if ((!bFilter) || ((ps.getSystemTime() + ps.getUserTime()) > 0))
			{
				// we must distinguish two situations
				// a) we use custom stat type
				// b) we use regular stat type
				if ((refFrom != null)
						&& (refFrom.m_refProcesses != null))
				{
					ps.substractFromRef(refFrom.m_refProcesses);

					// we must recheck if the delta process is still above
					// threshold
					if ((!bFilter)
							|| ((ps.getSystemTime() + ps.getUserTime()) > 0))
					{
						myRetProcesses.add(ps);
					}
				} else
				{
					myRetProcesses.clear();
					if (refFrom != null)
					{
						myRetProcesses.add(new Process(refFrom
								.getMissingRefError(), 1, 1, 1));
					} else
					{
						myRetProcesses.add(new Process(
								Reference.GENERIC_REF_ERR, 1, 1, 1));
					}
				}
			}
		}

		// sort @see
		// com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		switch (iSort)
		{
		case 0:
			// by Duration
			Comparator<Process> myCompTime = new Process.ProcessTimeComparator();
			Collections.sort(myRetProcesses, myCompTime);
			break;
		case 1:
			// by Count
			Comparator<Process> myCompCount = new Process.ProcessCountComparator();
			Collections.sort(myRetProcesses, myCompCount);
			break;
		}

		for (int i = 0; i < myRetProcesses.size(); i++)
		{
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

		ArrayList<StatElement> myProcesses = null;
		ArrayList<Process> myRetProcesses = new ArrayList<Process>();

		if ( (Build.VERSION.SDK_INT >= 19) && !SysUtils.hasBatteryStatsPermission(m_context) )
		{
			myProcesses = ProcessStatsDumpsys.getProcesses(m_context);
		}
		else
		{
			BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(m_context);
			myProcesses = mStats.getProcessStats(m_context,
					BatteryStatsTypes.STATS_CURRENT);
		}

		for (int i = 0; i < myProcesses.size(); i++)
		{
			Process ps = (Process) myProcesses.get(i);
			if ((!bFilter) || ((ps.getSystemTime() + ps.getUserTime()) > 0))
			{
				myRetProcesses.add(ps);
			}
		}

		// sort @see
		// com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		switch (iSort)
		{
		case 0:
			// by Duration
			Comparator<Process> myCompTime = new Process.ProcessTimeComparator();
			Collections.sort(myRetProcesses, myCompTime);
			break;
		case 1:
			// by Count
			Comparator<Process> myCompCount = new Process.ProcessCountComparator();
			Collections.sort(myRetProcesses, myCompCount);
			break;
		}

		if (LogSettings.DEBUG)
		{
			Log.d(TAG, "Result " + myProcesses.toString());
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
		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		
		if ( (Build.VERSION.SDK_INT >= 19) && !SysUtils.hasBatteryStatsPermission(m_context) )
		{
			// stop straight away of root features are disabled
			SharedPreferences sharedPrefs = PreferenceManager
					.getDefaultSharedPreferences(m_context);

			boolean rootEnabled = sharedPrefs.getBoolean("root_features", false);
			if (!rootEnabled)
			{
				myStats.add(new Misc(Reference.NO_ROOT_ERR, 1, 1));
				return myStats;
			}
		}

		if ((refFrom == null) || (refTo == null))
		{
				myStats.add(new Misc(Reference.GENERIC_REF_ERR, 1, 1));
			return myStats;
		}


		ArrayList<StatElement> myWakelocks = null;
		if ((refTo != null) && (refTo.m_refWakelocks != null))
		{
			myWakelocks = refTo.m_refWakelocks;
		}
		else
		{
			return null;
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
				} else
				{
					strRef = "Wakelocks is null";
				}
			} else
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
			Wakelock wl = ((Wakelock) myWakelocks.get(i)).clone();
			if ((!bFilter) || ((wl.getDuration() / 1000) > 0))
			{
				// we must distinguish two situations
				// a) we use custom stat type
				// b) we use regular stat type

				if ((refFrom != null)
						&& (refFrom.m_refWakelocks != null))
				{
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
				} else
				{
					myRetWakelocks.clear();
					if (refFrom != null)
					{
						myRetWakelocks.add(new Wakelock(1, refFrom
								.getMissingRefError(), 1, 1, 1));
					} else
					{
						myRetWakelocks.add(new Wakelock(1,
								Reference.GENERIC_REF_ERR, 1, 1, 1));
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
		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		ArrayList<StatElement> myWakelocks = null;
		
		if ( (Build.VERSION.SDK_INT >= 19) && !SysUtils.hasBatteryStatsPermission(m_context) )
		{
			myWakelocks = PartialWakelocksDumpsys.getPartialWakelocks(m_context);
		}
		else
		{
			BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(m_context);
	
			myWakelocks = mStats.getWakelockStats(m_context,
					BatteryStatsTypes.WAKE_TYPE_PARTIAL,
					BatteryStatsTypes.STATS_CURRENT, iPctType);
		}
		
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
	public ArrayList<StatElement> getNativeKernelWakelockStatList(
			boolean bFilter, Reference refFrom, int iPctType, int iSort, Reference refTo)
			throws Exception
	{
		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		if ((refFrom == null) || (refTo == null))
		{
				myStats.add(new Misc(Reference.GENERIC_REF_ERR, 1, 1));
			return myStats;
		}

		ArrayList<StatElement> myKernelWakelocks = null;
		
		if ((refTo != null) && (refTo.m_refKernelWakelocks != null))
		{ 
			myKernelWakelocks = refTo.m_refKernelWakelocks;
		}
		else
		{
			return null;
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
				} else
				{
					strRef = "kernel wakelocks is null";
				}

			} else
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
				if ((refFrom != null) && (refFrom.m_refKernelWakelocks != null))
				{
					wl.substractFromRef(refFrom.m_refKernelWakelocks);

					// we must recheck if the delta process is still above
					// threshold
					if ((!bFilter) || ((wl.getDuration()) > 0))
					{
						myRetKernelWakelocks.add(wl);
					}
				}
				else
				{
					myRetKernelWakelocks.clear();
					if (refFrom != null)
					{
						myRetKernelWakelocks.add(new NativeKernelWakelock(
								refFrom.getMissingRefError(), "", 1, 1,
								1, 1, 1, 1, 1, 1, 1));
					} else
					{
						myRetKernelWakelocks.add(new NativeKernelWakelock(
								Reference.GENERIC_REF_ERR, "", 1, 1, 1, 1,
								1, 1, 1, 1, 1));

					}
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

	public ArrayList<StatElement> getCurrentNativeKernelWakelockStatList(boolean bFilter, int iPctType, int iSort)
			throws Exception
	{
		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		ArrayList<StatElement> myKernelWakelocks = null;
		
		// we must support both "old" (/proc/wakelocks) and "new formats
		if (Wakelocks.fileExists())
		{
			myKernelWakelocks = Wakelocks.parseProcWakelocks(m_context);	
		}
		else
		{
			myKernelWakelocks = WakeupSources.parseWakeupSources(m_context);
		}
		
		
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
	public ArrayList<StatElement> getNativeNetworkUsageStatList(
			boolean bFilter, Reference refFrom, Reference refTo) throws Exception
	{
		ArrayList<StatElement> myStats = new ArrayList<StatElement>();			

		// stop straight away of root features are disabled
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(m_context);

		boolean rootEnabled = sharedPrefs.getBoolean("root_features", false);
		if (!rootEnabled)
		{
			myStats.add(new Misc(Reference.NO_ROOT_ERR, 1, 1));
			return myStats;
		}

		
		
		if ((refFrom == null) || (refTo == null))
		{
				myStats.add(new Misc(Reference.GENERIC_REF_ERR, 1, 1));
			return myStats;
		}

		ArrayList<StatElement> myNetworkStats = null;
		
		if ((refTo != null) && (refTo.m_refNetworkStats != null))
		{
			myNetworkStats = refTo.m_refNetworkStats;
		}
		else
		{
			myNetworkStats = Netstats.parseNetstats();
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
				} else
				{
					strRef = "Network stats is null";
				}

			} else
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
				if ((refFrom != null)
						&& (refFrom.m_refNetworkStats != null))
				{
					netStat.substractFromRef(refFrom.m_refNetworkStats);

					// we must recheck if the delta process is still above
					// threshold
					if ((!bFilter) || ((netStat.getTotalBytes()) > 0))
					{
						myRetNetworkStats.add(netStat);
					}
				} else
				{
					myRetNetworkStats.clear();
					if (refFrom != null)
					{
						myRetNetworkStats.add(new NetworkUsage(refFrom
								.getMissingRefError(), -1, 1, 0));
					} else
					{
						myRetNetworkStats.add(new NetworkUsage(
								Reference.GENERIC_REF_ERR, -1, 1, 0));

					}
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

	public ArrayList<StatElement> getCurrentNativeNetworkUsageStatList(boolean bFilter) throws Exception
	{
		ArrayList<StatElement> myStats = new ArrayList<StatElement>();

		// stop straight away of root features are disabled
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(m_context);
		boolean rootEnabled = sharedPrefs.getBoolean("root_features", false);
		if (!rootEnabled)
		{
			return myStats;
		}

		ArrayList<StatElement> myNetworkStats = null;
		
		myNetworkStats = Netstats.parseNetstats();

		ArrayList<NetworkUsage> myRetNetworkStats = new ArrayList<NetworkUsage>();
		// if we are using custom ref. always retrieve "stats current"

		// sort @see
		// com.asksven.android.common.privateapiproxies.Walkelock.compareTo
//		Collections.sort(myNetworkStats);


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
		// List to store the other usages to
		ArrayList<StatElement> myStates = refTo.m_refCpuStates;

		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		if ((refFrom == null) || (refTo == null))
		{
				myStats.add(new Misc(Reference.GENERIC_REF_ERR, 1, 1));
			return myStats;
		}

		if (myStates == null)
		{
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

		for (int i = 0; i < myStates.size(); i++)
		{
			State state = ((State) myStates.get(i)).clone();


			if ((refFrom != null)
					&& (refFrom.m_refCpuStates != null))
			{
				state.substractFromRef(refFrom.m_refCpuStates);
				if ((!bFilter) || ((state.m_duration) > 0))
				{
					myStats.add(state);
				}
			} else
			{
				myStats.clear();
				myStats.add(new State(1, 1));
			}
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
						PackageManager.GET_PERMISSIONS);
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
	
		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		// if on of the refs is null return
		if ((refFrom == null) || (refTo == null))
		{
				myStats.add(new Misc(Reference.GENERIC_REF_ERR, 1, 1));
			return myStats;
		}
		
		// List to store the other usages to
		ArrayList<StatElement> myUsages = new ArrayList<StatElement>();

		if ((refTo != null) && (refTo.m_refOther != null))
		{
			myUsages = refTo.m_refOther;
		}
		else
		{
			return null;
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
				Log.d(TAG, "Current value: " + usage.getName() + " " + usage.getData());
			}
			if ((!bFilter) || (usage.getTimeOn() > 0))
			{
				if ((refFrom != null)
						&& (refFrom.m_refOther != null))
				{
					usage.substractFromRef(refFrom.m_refOther);
					if ((!bFilter) || (usage.getTimeOn() > 0))
					{
						if (LogSettings.DEBUG)
						{
							Log.d(TAG, "Result value: " + usage.getName() + " "	+ usage.getData());
						}
						myStats.add((StatElement) usage);
					}
				}
				else
				{
					myStats.clear();
					if (refFrom != null)
					{
						myStats.add(new Misc(refFrom.getMissingRefError(), 1, 1));
					} else
					{
						myStats.add(new Misc(Reference.GENERIC_REF_ERR, 1,
								1));
					}
				}
			}
		}
		return myStats;
	}

	public ArrayList<StatElement> getCurrentOtherUsageStatList(boolean bFilter,
			boolean bFilterView, boolean bWidget)
			throws Exception
	{
		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		// List to store the other usages to
		ArrayList<StatElement> myUsages = new ArrayList<StatElement>();

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(m_context);

		if ( (Build.VERSION.SDK_INT >= 19) && !SysUtils.hasBatteryStatsPermission(m_context) )
		{
			myUsages = OtherStatsDumpsys.getOtherStats(
					sharedPrefs.getBoolean("show_other_wifi", true) && !bWidget,
					sharedPrefs.getBoolean("show_other_bt", true) && !bWidget);

			// basic fonctionality
			// elapsedRealtime(): Returns milliseconds since boot, including time spent in sleep.
			// uptimeMillis(): Returns milliseconds since boot, not counting time spent in deep sleep.
			long elapsedRealtime = SystemClock.elapsedRealtime();
			long uptimeMillis = SystemClock.uptimeMillis();
			Misc deepSleepUsage = new Misc("Deep Sleep", elapsedRealtime - uptimeMillis, elapsedRealtime);
			myUsages.add(deepSleepUsage);

			Misc awakeUsage = new Misc("Awake", uptimeMillis, elapsedRealtime);
			myUsages.add(awakeUsage);
			
		}
		else
		{	
			BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(m_context);	
	
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
	
			long whichRealtime = mStats.computeBatteryRealtime(rawRealtime,
					BatteryStatsTypes.STATS_CURRENT) / 1000;
			
			long timeBatteryUp = mStats.computeBatteryUptime(
					SystemClock.uptimeMillis() * 1000,
					BatteryStatsTypes.STATS_CURRENT) / 1000;
			
			if (CommonLogSettings.DEBUG)
			{
				Log.i(TAG, "whichRealtime = " + whichRealtime + " batteryRealtime = " + batteryRealtime + " timeBatteryUp=" + timeBatteryUp);
			}
			
			long timeScreenOn = mStats.getScreenOnTime(batteryRealtime,
					BatteryStatsTypes.STATS_CURRENT) / 1000;
			long timePhoneOn = mStats.getPhoneOnTime(batteryRealtime,
					BatteryStatsTypes.STATS_CURRENT) / 1000;
	
			long timeWifiOn = 0;
			long timeWifiRunning = 0;
			if (sharedPrefs.getBoolean("show_other_wifi", true) && !bWidget)
			{
				try
				{
					timeWifiOn = mStats.getWifiOnTime(batteryRealtime,
							BatteryStatsTypes.STATS_CURRENT) / 1000;
					timeWifiRunning = mStats.getGlobalWifiRunningTime(
							batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
					// long timeWifiMulticast =
					// mStats.getWifiMulticastTime(m_context, batteryRealtime,
					// BatteryStatsTypes.STATS_CURRENT) / 1000;
					// long timeWifiLocked = mStats.getFullWifiLockTime(m_context,
					// batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
					// long timeWifiScan = mStats.getScanWifiLockTime(m_context,
					// batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
				} catch (BatteryInfoUnavailableException e)
				{
					timeWifiOn = 0;
					timeWifiRunning = 0;
					Log.e(TAG,
							"A batteryinfo error occured while retrieving Wifi data");
				}
			}
			// long timeAudioOn = mStats.getAudioTurnedOnTime(m_context,
			// batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
			// long timeVideoOn = mStats.getVideoTurnedOnTime(m_context,
			// batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
			long timeBluetoothOn = 0;
			if (sharedPrefs.getBoolean("show_other_bt", true) && !bWidget)
			{
				try
				{
					timeBluetoothOn = mStats.getBluetoothOnTime(batteryRealtime,
							BatteryStatsTypes.STATS_CURRENT) / 1000;
				} catch (BatteryInfoUnavailableException e)
				{
					timeBluetoothOn = 0;
					Log.e(TAG,
							"A batteryinfo error occured while retrieving BT data");
				}
	
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
							batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
					timeSignalNone = mStats.getPhoneSignalStrengthTime(
							BatteryStatsTypes.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
							batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
					timeSignalPoor = mStats.getPhoneSignalStrengthTime(
							BatteryStatsTypes.SIGNAL_STRENGTH_POOR,
							batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
					timeSignalModerate = mStats.getPhoneSignalStrengthTime(
							BatteryStatsTypes.SIGNAL_STRENGTH_MODERATE,
							batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
					timeSignalGood = mStats.getPhoneSignalStrengthTime(
							BatteryStatsTypes.SIGNAL_STRENGTH_GOOD,
							batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
					timeSignalGreat = mStats.getPhoneSignalStrengthTime(
							BatteryStatsTypes.SIGNAL_STRENGTH_GREAT,
							batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
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
							batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
					timeScreenDim = mStats.getScreenBrightnessTime(
							BatteryStatsTypes.SCREEN_BRIGHTNESS_DIM,
							batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
					timeScreenMedium = mStats.getScreenBrightnessTime(
							BatteryStatsTypes.SCREEN_BRIGHTNESS_MEDIUM,
							batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
					timeScreenLight = mStats.getScreenBrightnessTime(
							BatteryStatsTypes.SCREEN_BRIGHTNESS_LIGHT,
							batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
					timeScreenBright = mStats.getScreenBrightnessTime(
							BatteryStatsTypes.SCREEN_BRIGHTNESS_BRIGHT,
							batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
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
			// long whichRealtime = SystemClock.elapsedRealtime();
			// long timeElapsed = mStats.computeBatteryRealtime(rawRealtime,
			// BatteryStatsTypes.STATS_CURRENT) / 1000;
			// SystemClock.elapsedRealtime();
	
			Misc deepSleepUsage = new Misc("Deep Sleep", timeDeepSleep, elaspedRealtime);
			Log.d(TAG, "Added Deep sleep:" + deepSleepUsage.getData());
	
	
			if ((!bFilter) || (deepSleepUsage.getTimeOn() > 0))
			{
				myUsages.add(deepSleepUsage);
			}
	
			if (timeBatteryUp > 0)
			{
				myUsages.add(new Misc("Awake", timeBatteryUp, elaspedRealtime));
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
	
			// if ( (timeWifiMulticast > 0) && (!bFilterView ||
			// sharedPrefs.getBoolean("show_other_wifi", true)) )
			// {
			// myUsages.add(new Misc("Wifi Multicast On", timeWifiMulticast,
			// elaspedRealtme));
			// }
			//
			// if ( (timeWifiLocked > 0) && (!bFilterView ||(!bFilterView ||
			// sharedPrefs.getBoolean("show_other_wifi", true)) )
			// {
			// myUsages.add(new Misc("Wifi Locked", timeWifiLocked, elaspedRealtme));
			// }
			//
			// if ( (timeWifiScan > 0) && (!bFilterView ||
			// sharedPrefs.getBoolean("show_other_wifi", true)) )
			// {
			// myUsages.add(new Misc("Wifi Scan", timeWifiScan, elaspedRealtme));
			// }
			//
			// if (timeAudioOn > 0)
			// {
			// myUsages.add(new Misc("Video On", timeAudioOn, elaspedRealtme));
			// }
			//
			// if (timeVideoOn > 0)
			// {
			// myUsages.add(new Misc("Video On", timeVideoOn, elaspedRealtme));
			// }
	
			// sort @see
			// com.asksven.android.common.privateapiproxies.Walkelock.compareTo
	//		Collections.sort(myUsages);
		}
		for (int i = 0; i < myUsages.size(); i++)
		{
			Misc usage = (Misc)myUsages.get(i);
			if (LogSettings.DEBUG)
			{
				Log.d(TAG,	"Current value: " + usage.getName() + " " + usage.getData());
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
	 * @param iStatType
	 *            the reference
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
	 * @param iStatType
	 *            the reference
	 * @return the lost battery level
	 */
	public String getBatteryLevelFromTo(Reference refFrom, Reference refTo)
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
		
		return "Bat.: " + getBatteryLevelStat(refFrom, refTo) + "% (" + levelFrom
				+ "% to " + levelTo + "%)" + " [" + drop_per_hour + "]";
	}

	/**
	 * Get the battery voltage lost since a given ref
	 * 
	 * @param iStatType
	 *            the reference
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
	 * @param iStatType
	 *            the reference
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
		Reference thisRef = ReferenceStore.getReferenceByName(Reference.SCREEN_OFF_REF_FILENAME, m_context);

		return ((thisRef != null) && (thisRef.m_refOther != null));
	}

	/**
	 * Returns true if a custom ref was stored
	 * 
	 * @return true is a custom ref exists
	 */
	public boolean hasCustomRef()
	{
		Reference thisRef = ReferenceStore.getReferenceByName(Reference.CUSTOM_REF_FILENAME, m_context);
		return ((thisRef != null) && (thisRef.m_refOther != null));
	}

	/**
	 * Returns true if a since charged ref was stored
	 * 
	 * @return true is a since charged ref exists
	 */
	public boolean hasSinceChargedRef()
	{
		Reference thisRef = ReferenceStore.getReferenceByName(Reference.CHARGED_REF_FILENAME, m_context);

		return ((thisRef != null) && (thisRef.m_refKernelWakelocks != null));
	}

	/**
	 * Returns true if a since unplugged ref was stored
	 * 
	 * @return true is a since unplugged ref exists
	 */
	public boolean hasSinceUnpluggedRef()
	{
		Reference thisRef = ReferenceStore.getReferenceByName(Reference.UNPLUGGED_REF_FILENAME, m_context);

		return ((thisRef != null) && (thisRef.m_refKernelWakelocks != null));
	}

	/**
	 * Returns true if a since boot ref was stored
	 * 
	 * @return true is a since unplugged ref exists
	 */
	public boolean hasSinceBootRef()
	{
		Reference thisRef = ReferenceStore.getReferenceByName(Reference.BOOT_REF_FILENAME, m_context);

		return ((thisRef != null) && (thisRef.m_refKernelWakelocks != null));
	}

	/**
	 * Saves all data to a point in time defined by user This data will be used
	 * in a custom "since..." stat type
	 */
	public void setCustomReference(int iSort)
	{
		Reference thisRef = new Reference(Reference.CUSTOM_REF_FILENAME, Reference.TYPE_CUSTOM);
		ReferenceStore.put(Reference.CUSTOM_REF_FILENAME, populateReference(iSort, thisRef), m_context);
	}

	/**
	 * Saves all data at the current point in time
	 */
	public void setCurrentReference(int iSort)
	{
		Reference thisRef = new Reference(Reference.CURRENT_REF_FILENAME, Reference.TYPE_CURRENT);
		ReferenceStore.put(Reference.CURRENT_REF_FILENAME, populateReference(iSort, thisRef), m_context);
	}

	/**
	 * Saves all data at the current point in time
	 */
	public synchronized String setTimedReference(int iSort)
	{
		String fileName = Reference.TIMER_REF_FILENAME + DateUtils.format(System.currentTimeMillis(), DateUtils.DATE_FORMAT_NOW);
		Reference thisRef = new Reference(fileName, Reference.TYPE_TIMER);
		ReferenceStore.put(fileName, populateReference(iSort, thisRef), m_context);
		
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
		ReferenceStore.put(Reference.SCREEN_OFF_REF_FILENAME, populateReference(iSort, thisRef), m_context);
		
		// clean "current from cache"
//		ReferenceStore.invalidate(Reference.CURRENT_REF_FILENAME, m_context);
	}

	/**
	 * Saves all data to a point in time when the screen goes in
	 * be used in a custom "since..." stat type
	 */
	public void setReferenceScreenOn(int iSort)
	{
		Reference thisRef = new Reference(Reference.SCREEN_ON_REF_FILENAME, Reference.TYPE_EVENT);
		ReferenceStore.put(Reference.SCREEN_ON_REF_FILENAME, populateReference(iSort, thisRef), m_context);
		
		// clean "current from cache"
		ReferenceStore.invalidate(Reference.CURRENT_REF_FILENAME, m_context);
	}

	/**
	 * Saves data when battery is fully charged This data will be used in the
	 * "since charged" stat type
	 */
	public void setReferenceSinceCharged(int iSort)
	{
		Reference thisRef = new Reference(Reference.CHARGED_REF_FILENAME, Reference.TYPE_EVENT);
		ReferenceStore.put(Reference.CHARGED_REF_FILENAME, populateReference(iSort, thisRef), m_context);
		
		// clean "current from cache"
		ReferenceStore.invalidate(Reference.CURRENT_REF_FILENAME, m_context);

	}

	/**
	 * Saves data when the phone is unplugged This data will be used in the
	 * "since unplugged" stat type
	 */
	public void setReferenceSinceUnplugged(int iSort)
	{
		Reference thisRef = new Reference(Reference.UNPLUGGED_REF_FILENAME, Reference.TYPE_EVENT);
		ReferenceStore.put(Reference.UNPLUGGED_REF_FILENAME, populateReference(iSort, thisRef), m_context);
		
		// clean "current from cache"
		ReferenceStore.invalidate(Reference.CURRENT_REF_FILENAME, m_context);
	}

	/**
	 * Saves data when the phone is booted This data will be used in the
	 * "since unplugged" stat type
	 */
	public void setReferenceSinceBoot(int iSort)
	{
		Reference thisRef = new Reference(Reference.BOOT_REF_FILENAME, Reference.TYPE_EVENT);
		ReferenceStore.put(Reference.BOOT_REF_FILENAME, populateReference(iSort, thisRef), m_context);
	}

	/**
	 * Saves a reference to cache and persists it
	 */
	private synchronized Reference populateReference(int iSort, Reference refs)
	{
		
		// we are going to retrieve a reference: make sure data does not come from the cache
		if (Build.VERSION.SDK_INT < 19) BatteryStatsProxy.getInstance(m_context).invalidate();
		
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(m_context);

		boolean bFilterStats = sharedPrefs.getBoolean("filter_data", true);
		int iPctType = Integer.valueOf(sharedPrefs.getString("default_wl_ref",
				"0"));

		boolean rootEnabled = sharedPrefs
				.getBoolean("root_features", false);

		try
		{
			refs.m_refOther = null;
			refs.m_refWakelocks = null;
			refs.m_refKernelWakelocks = null;
			refs.m_refNetworkStats = null;

			refs.m_refAlarms = null;
			refs.m_refProcesses = null;
			refs.m_refCpuStates = null;

			try
			{
				refs.m_refKernelWakelocks = getCurrentNativeKernelWakelockStatList(bFilterStats, iPctType, iSort);
			}
			catch (Exception e)
			{
				Log.e(TAG, "An exception occured processing kernel wakelocks. Message: " + e.getMessage());
				Log.e(TAG, "Exception: " + Log.getStackTraceString(e));				
			}
			
			if ( rootEnabled || SysUtils.hasBatteryStatsPermission(m_context) )
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

			if ( rootEnabled || SysUtils.hasBatteryStatsPermission(m_context) )
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
			try
			{
				refs.m_refBatteryRealtime = getBatteryRealtime(BatteryStatsTypes.STATS_CURRENT);
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

			if (rootEnabled)
			{
				// After that we go on and try to write the rest. If this part
				// fails at least there will be a partial ref saved
				Log.i(TAG, "Trace: Calling root operations" + DateUtils.now());
				try
				{
					refs.m_refNetworkStats = getCurrentNativeNetworkUsageStatList(bFilterStats);
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

				Log.i(TAG, "Trace: Finished root operations" + DateUtils.now());
				
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
		BatteryStatsProxy.getInstance(m_context).invalidate();
		
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(m_context);

		boolean bFilterStats = sharedPrefs.getBoolean("filter_data", true);
		int iPctType = Integer.valueOf(sharedPrefs.getString("default_wl_ref",
				"0"));

		boolean rootEnabled = sharedPrefs
				.getBoolean("root_features", false);

		try
		{
			refs.m_refOther 			= null;
			refs.m_refWakelocks 		= null;
			refs.m_refKernelWakelocks 	= null;
			refs.m_refAlarms 			= null;
			refs.m_refProcesses 		= null;
			refs.m_refCpuStates 		= null;

			refs.m_refKernelWakelocks 	= getCurrentNativeKernelWakelockStatList(bFilterStats, iPctType, iSort);
			if ( rootEnabled || SysUtils.hasBatteryStatsPermission(m_context) )
			{
				refs.m_refWakelocks 		= getCurrentWakelockStatList(bFilterStats, iPctType, iSort);
			}
			refs.m_refOther 			= getCurrentOtherUsageStatList(bFilterStats, false, false);
			refs.m_refBatteryRealtime 	= getBatteryRealtime(BatteryStatsTypes.STATS_CURRENT);


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
		long rawRealtime = SystemClock.elapsedRealtime() * 1000;
		long whichRealtime = 0;
		
		if ( (Build.VERSION.SDK_INT >= 19) && !SysUtils.hasBatteryStatsPermission(m_context) )
		{
			whichRealtime = rawRealtime;
			return whichRealtime;
		}

		BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(m_context);

		whichRealtime = mStats.computeBatteryRealtime(rawRealtime,
				BatteryStatsTypes.STATS_CURRENT) / 1000;

		if ((iStatType == StatsProvider.STATS_CUSTOM) && (ReferenceStore.getReferenceByName(Reference.CUSTOM_REF_FILENAME, m_context) != null))
		{			
			whichRealtime -= ReferenceStore.getReferenceByName(Reference.CUSTOM_REF_FILENAME, m_context).m_refBatteryRealtime;	
		}
		else if ((iStatType == StatsProvider.STATS_SCREEN_OFF)
				&& (ReferenceStore.getReferenceByName(Reference.SCREEN_OFF_REF_FILENAME, m_context) != null))
		{
			whichRealtime -= ReferenceStore.getReferenceByName(Reference.SCREEN_OFF_REF_FILENAME, m_context).m_refBatteryRealtime;
		}
		else if ((iStatType == StatsProvider.STATS_BOOT)
				&& (ReferenceStore.getReferenceByName(Reference.BOOT_REF_FILENAME, m_context) != null))
		{
			whichRealtime -= ReferenceStore.getReferenceByName(Reference.BOOT_REF_FILENAME, m_context).m_refBatteryRealtime;
		}
		
		Log.i(TAG, "rawRealtime = " + rawRealtime);
		Log.i(TAG, "whichRealtime = " + whichRealtime);
		
		return whichRealtime;
	}

	/**
	 * Returns the battery realtime since a given reference
	 * 
	 * @param iStatType
	 *            the reference
	 * @return the battery realtime
	 */
	public boolean getIsCharging() throws BatteryInfoUnavailableException
	{
		if ( (Build.VERSION.SDK_INT >= 19) && !SysUtils.hasBatteryStatsPermission(m_context) ) return false;
		
		BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(m_context);

		if (mStats == null)
		{
			// an error has occured
			return false;
		}
		
		return !mStats.getIsOnBattery(m_context);
	}

	/**
	 * Dumps relevant data to an output file
	 * 
	 */


	@SuppressLint("NewApi")
	public Uri writeLogcatToFile()
	{
		Uri fileUri = null;
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(m_context);

		if (!DataStorage.isExternalStorageWritable())
		{
			Log.e(TAG, "External storage can not be written");
			Toast.makeText(m_context, "External Storage can not be written",
					Toast.LENGTH_SHORT).show();
		}
		try
		{
			// open file for writing
			// open file for writing
			File root;
			boolean bSaveToPrivateStorage = sharedPrefs.getBoolean("files_to_private_storage", false);

			if (bSaveToPrivateStorage)
			{
				try
				{
					root = m_context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
				}
				catch (Exception e)
				{
					root = Environment.getExternalStorageDirectory();
				}
			}
			else
			{
				root = Environment.getExternalStorageDirectory();
			}

			String path = root.getAbsolutePath();
			// check if file can be written
			if (root.canWrite())
			{
				String filename = "logcat-"
						+ DateUtils.now("yyyy-MM-dd_HHmmssSSS") + ".txt";
				Util.run("logcat -v time -d > " + path + "/" + filename);
				fileUri = Uri.fromFile(new File(path + "/" + filename));

//				Toast.makeText(m_context, "Dump witten: " + path + "/" + filename, Toast.LENGTH_SHORT).show();

			} else
			{
				Log.i(TAG,
						"Write error. "
								+ Environment.getExternalStorageDirectory()
								+ " couldn't be written");
			}
		} catch (Exception e)
		{
			Log.e(TAG, "Exception: " + e.getMessage());
		}
		return fileUri;
	}

	@SuppressLint("NewApi")
	public Uri writeDmesgToFile()
	{
		Uri fileUri = null;
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(m_context);

		if (!DataStorage.isExternalStorageWritable())
		{
			Log.e(TAG, "External storage can not be written");
			Toast.makeText(m_context, "External Storage can not be written",
					Toast.LENGTH_SHORT).show();
		}
		try
		{
			// open file for writing
			File root;
			boolean bSaveToPrivateStorage = sharedPrefs.getBoolean("files_to_private_storage", false);

			if (bSaveToPrivateStorage)
			{
				try
				{
					root = m_context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
				}
				catch (Exception e)
				{
					root = Environment.getExternalStorageDirectory();
				}
			}
			else
			{
				root = Environment.getExternalStorageDirectory();
			}

			String path = root.getAbsolutePath();
			// check if file can be written
			if (root.canWrite())
			{
				String filename = "dmesg-"
						+ DateUtils.now("yyyy-MM-dd_HHmmssSSS") + ".txt";
				if (RootShell.getInstance().rooted()) //Shell.SU.available())
				{
					RootShell.getInstance().run("dmesg > " + path + "/" + filename); //Shell.SU.run("dmesg > " + path + "/" + filename);
				}
				else
				{
					Util.run("dmesg > " + path + "/" + filename);
				}
				fileUri = Uri.fromFile(new File(path + "/" + filename));
//				Toast.makeText(m_context, "Dump witten: " + path + "/" + filename, Toast.LENGTH_SHORT).show();

			}
			else
			{
				Log.i(TAG,
						"Write error. "
								+ Environment.getExternalStorageDirectory()
								+ " couldn't be written");
			}
		} catch (Exception e)
		{
			Log.e(TAG, "Exception: " + e.getMessage());
		}
		return fileUri;
	}

	/**
	 * Writes a reading in json format
	 * @param refFrom 
	 * @param iSort
	 * @param refTo
	 */
	@SuppressLint("NewApi")
	public void writeJsonToFile(Reference refFrom, int iSort, Reference refTo)
	{
		Reading reading = new Reading(m_context, refFrom, refTo);
		reading.writeToFileJson(m_context);
		
	}
	
	
//	/**
//	 * Dump the elements on one list
//	 * 
//	 * @param myList
//	 *            a list of StatElement
//	 */
//	private void dumpList(List<StatElement> myList, BufferedWriter out)
//			throws IOException
//	{
//		if (myList != null)
//		{
//			for (int i = 0; i < myList.size(); i++)
//			{
//				out.write(myList.get(i).getDumpData(m_context) + "\n");
//
//			}
//		}
//	}

	/**
	 * translate the stat type (see arrays.xml) to the corresponding label
	 * 
	 * @param position
	 *            the spinner position
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
	 * @param position
	 *            the spinner position
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
	 * @param position
	 *            the spinner position
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
	 * @param position
	 *            the spinner position
	 * @return the stat
	 */
	private String statToLabel(int iStat)
	{
		String strRet = "";
		String[] statsArray = m_context.getResources().getStringArray(
				R.array.stats);
		strRet = statsArray[iStat];

		return strRet;
	}

	/**
	 * translate the stat (see arrays.xml) to the corresponding label
	 * 
	 * @param position
	 *            the spinner position
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
		Intent batteryIntent = m_context.getApplicationContext()
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
		Intent batteryIntent = m_context.getApplicationContext()
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
}
