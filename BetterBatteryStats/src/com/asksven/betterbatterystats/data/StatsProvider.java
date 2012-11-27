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
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ReceiverCallNotAllowedException;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.ServiceInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.asksven.andoid.common.contrib.Util;
import com.asksven.android.common.kernelutils.AlarmsDumpsys;
import com.asksven.android.common.kernelutils.CpuStates;
import com.asksven.android.common.kernelutils.NativeKernelWakelock;
import com.asksven.android.common.kernelutils.Netstats;
import com.asksven.android.common.kernelutils.RootDetection;
import com.asksven.android.common.kernelutils.State;
import com.asksven.android.common.kernelutils.Wakelocks;
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

	/** the text when no reference is set */
	static String NO_CUST_REF = "No custom reference was set. Please use the menu to do so";
	static String NO_BOOT_REF = "Boot event was not registered yet, it will at next reboot";
	static String NO_SCREEN_OFF_REF = "Screen off event was not registered yet";
	static String NO_SINCE_UNPLUGGED_REF = "No reference since unplugged was saved yet, plug/unplug you phone";
	static String NO_SINCE_CHARGED_REF = "No reference since charged was saved yet, it will the next time you charge to 100%";

	/** the storage for references */
//	static Map<String, Reference> m_refStore = new HashMap<String, Reference>();

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
	public ArrayList<StatElement> getStatList(int iStat, int iStatTypeFrom,
			int iSort, String refToName) throws Exception
	{
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(m_context);
		boolean bFilterStats = sharedPrefs.getBoolean("filter_data", true);
		boolean developerMode = sharedPrefs.getBoolean("developer", false);
		
		Reference refTo = ReferenceStore.getReferenceByName(refToName, m_context);
		
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
			return getOtherUsageStatList(bFilterStats, iStatTypeFrom, true, false, refTo);
		case 1:
			return getNativeKernelWakelockStatList(bFilterStats, iStatTypeFrom,
					iPctType, iSort, refTo);
		case 2:
			return getWakelockStatList(bFilterStats, iStatTypeFrom, iPctType, iSort, refTo);
		case 3:
			return getAlarmsStatList(bFilterStats, iStatTypeFrom, refTo);
		case 4:
			return getNativeNetworkUsageStatList(bFilterStats, iStatTypeFrom, refTo);
		case 5:
			return getCpuStateList(iStatTypeFrom, refTo);
		case 6:
			return getProcessStatList(bFilterStats, iStatTypeFrom, iSort, refTo);

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
	 * Get the Stat to be displayed
	 * 
	 * @return a List of StatElements sorted (descending)
	 * @throws BatteryInfoUnavailableException 
	 */
	public long getSince(int iStatTypeFrom, String refToName)
	{
		long ret = 0;

		Reference myReferenceFrom 	= ReferenceStore.getReference(iStatTypeFrom, m_context);
		Reference myReferenceTo	 	= ReferenceStore.getReferenceByName(refToName, m_context);

		if ((myReferenceTo != null) && (myReferenceFrom != null))
		{
			ret =  myReferenceTo.m_refBatteryRealtime - myReferenceFrom.m_refBatteryRealtime;
			Log.d(TAG, "Since: " + DateUtils.formatDuration(ret));

		}
		else
		{
			ret = -1;
		}

		return ret;
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
			int iStatTypeFrom, Reference refTo) throws Exception
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

		Reference myReference = ReferenceStore.getReference(iStatTypeFrom, m_context);

		if (LogSettings.DEBUG)
		{
			if (myReference != null)
			{
				strRefDescr = myReference.whoAmI();
				if (myReference.m_refAlarms != null)
				{
					strRef = myReference.m_refAlarms.toString();
				} else
				{
					strRef = "Alarms is null";
				}
			} else
			{
				strRefDescr = "Reference is null";
			}
			Log.d(TAG, "Processing alarms since " + statTypeToLabel(iStatTypeFrom)
					+ "(" + iStatTypeFrom + ")");

			Log.d(TAG, "Reference used: " + strRefDescr);
			Log.d(TAG, "It is now " + DateUtils.now());

			Log.d(TAG, "Substracting " + strRef);
			Log.d(TAG, "from " + strCurrent);
		}

		for (int i = 0; i < myAlarms.size(); i++)
		{
			Alarm alarm = (Alarm) myAlarms.get(i);
			if ((!bFilter) || ((alarm.getWakeups()) > 0))
			{
				if (iStatTypeFrom == BatteryStatsTypes.STATS_CURRENT)
				{
					myRetAlarms.add(alarm);
				} else
				{
					if ((myReference != null)
							&& (myReference.m_refAlarms != null))
					{
						alarm.substractFromRef(myReference.m_refAlarms);

						// we must recheck if the delta process is still above
						// threshold
						if ((!bFilter) || ((alarm.getWakeups()) > 0))
						{
							myRetAlarms.add(alarm);
						}
					} else
					{
						myRetAlarms.clear();
						if (myReference != null)
						{
							myRetAlarms.add(new Alarm(myReference
									.getMissingRefError()));
						} else
						{
							myRetAlarms.add(new Alarm(
									Reference.GENERIC_REF_ERR));
						}
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
			int iStatTypeFrom, int iSort, Reference refTo) throws Exception
	{
		BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(m_context);

		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
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

		Reference myReference = ReferenceStore.getReference(iStatTypeFrom, m_context);

		if (LogSettings.DEBUG)
		{
			if (myReference != null)
			{
				strRefDescr = myReference.whoAmI();
				if (myReference.m_refProcesses != null)
				{
					strRef = myReference.m_refProcesses.toString();
				} else
				{
					strRef = "Process is null";
				}
			} else
			{
				strRefDescr = "Reference is null";
			}
			Log.d(TAG, "Processing processes since "
					+ statTypeToLabel(iStatTypeFrom) + "(" + iStatTypeFrom + ")");

			Log.d(TAG, "Reference used: " + strRefDescr);
			Log.d(TAG, "It is now " + DateUtils.now());

			Log.d(TAG, "Substracting " + strRef);
			Log.d(TAG, "from " + strCurrent);
		}

		for (int i = 0; i < myProcesses.size(); i++)
		{
			Process ps = (Process) myProcesses.get(i);
			if ((!bFilter) || ((ps.getSystemTime() + ps.getUserTime()) > 0))
			{
				// we must distinguish two situations
				// a) we use custom stat type
				// b) we use regular stat type
				if (iStatTypeFrom == BatteryStatsTypes.STATS_CURRENT)
				{
					myRetProcesses.add(ps);
				}
				else
				{

					if ((myReference != null)
							&& (myReference.m_refProcesses != null))
					{
						ps.substractFromRef(myReference.m_refProcesses);

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
						if (myReference != null)
						{
							myRetProcesses.add(new Process(myReference
									.getMissingRefError(), 1, 1, 1));
						} else
						{
							myRetProcesses.add(new Process(
									Reference.GENERIC_REF_ERR, 1, 1, 1));
						}
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
		BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(m_context);

		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		ArrayList<StatElement> myProcesses = null;
		ArrayList<Process> myRetProcesses = new ArrayList<Process>();

		myProcesses = mStats.getProcessStats(m_context,
				BatteryStatsTypes.STATS_CURRENT);


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
			int iStatTypeFrom, int iPctType, int iSort, Reference refTo) throws Exception
	{
		ArrayList<StatElement> myStats = new ArrayList<StatElement>();

		BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(m_context);

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

		Reference myReference = ReferenceStore.getReference(iStatTypeFrom, m_context);
		String strCurrent = myWakelocks.toString();

		String strRef = "";
		String strRefDescr = "";

		if (LogSettings.DEBUG)
		{
			if (myReference != null)
			{
				strRefDescr = myReference.whoAmI();
				if (myReference.m_refWakelocks != null)
				{
					strRef = myReference.m_refWakelocks.toString();
				} else
				{
					strRef = "Wakelocks is null";
				}
			} else
			{
				strRefDescr = "Reference is null";
			}
			Log.d(TAG, "Processing wakelocks since "
					+ statTypeToLabel(iStatTypeFrom) + "(" + iStatTypeFrom + ")");

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
			Wakelock wl = (Wakelock) myWakelocks.get(i);
			if ((!bFilter) || ((wl.getDuration() / 1000) > 0))
			{
				// we must distinguish two situations
				// a) we use custom stat type
				// b) we use regular stat type

				if (iStatTypeFrom == BatteryStatsTypes.STATS_CURRENT)
				{
					myRetWakelocks.add(wl);
				} else
				{
					if ((myReference != null)
							&& (myReference.m_refWakelocks != null))
					{
						wl.substractFromRef(myReference.m_refWakelocks);

						// we must recheck if the delta process is still above
						// threshold
						if ((!bFilter) || ((wl.getDuration() / 1000) > 0))
						{
							if (LogSettings.DEBUG)
							{
								Log.i(TAG, "Adding " + wl.toString());
							}
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
						if (myReference != null)
						{
							myRetWakelocks.add(new Wakelock(1, myReference
									.getMissingRefError(), 1, 1, 1));
						} else
						{
							myRetWakelocks.add(new Wakelock(1,
									Reference.GENERIC_REF_ERR, 1, 1, 1));
						}
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

		BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(m_context);

		ArrayList<StatElement> myWakelocks = null;
		myWakelocks = mStats.getWakelockStats(m_context,
				BatteryStatsTypes.WAKE_TYPE_PARTIAL,
				BatteryStatsTypes.STATS_CURRENT, iPctType);
		
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
			boolean bFilter, int iStatTypeFrom, int iPctType, int iSort, Reference refTo)
			throws Exception
	{
		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
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
		Reference myReference = ReferenceStore.getReference(iStatTypeFrom, m_context);

		if (LogSettings.DEBUG)
		{
			if (myReference != null)
			{
				strRefDescr = myReference.whoAmI();
				if (myReference.m_refKernelWakelocks != null)
				{
					strRef = myReference.m_refKernelWakelocks.toString();
				} else
				{
					strRef = "kernel wakelocks is null";
				}

			} else
			{
				strRefDescr = "Reference is null";
			}
			Log.d(TAG, "Processing kernel wakelocks since "
					+ statTypeToLabel(iStatTypeFrom) + "(" + iStatTypeFrom + ")");

			Log.d(TAG, "Reference used: " + strRefDescr);
			Log.d(TAG, "It is now " + DateUtils.now());

			Log.d(TAG, "Substracting " + strRef);
			Log.d(TAG, "from " + strCurrent);
		}

		for (int i = 0; i < myKernelWakelocks.size(); i++)
		{
			NativeKernelWakelock wl = (NativeKernelWakelock) myKernelWakelocks.get(i);
			if ((!bFilter) || ((wl.getDuration()) > 0))
			{
				if ((myReference != null) && (myReference.m_refKernelWakelocks != null))
				{
					wl.substractFromRef(myReference.m_refKernelWakelocks);

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
					if (myReference != null)
					{
						myRetKernelWakelocks.add(new NativeKernelWakelock(
								myReference.getMissingRefError(), "", 1, 1,
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
		
		myKernelWakelocks = Wakelocks.parseProcWakelocks(m_context);
		
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
			boolean bFilter, int iStatTypeFrom, Reference refTo) throws Exception
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

		Reference myReference = ReferenceStore.getReference(iStatTypeFrom, m_context);

		if (LogSettings.DEBUG)
		{
			if (myReference != null)
			{
				strRefDescr = myReference.whoAmI();
				if (myReference.m_refNetworkStats != null)
				{
					strRef = myReference.m_refNetworkStats.toString();
				} else
				{
					strRef = "Network stats is null";
				}

			} else
			{
				strRefDescr = "Reference is null";
			}
			Log.d(TAG, "Processing network stats since "
					+ statTypeToLabel(iStatTypeFrom) + "(" + iStatTypeFrom + ")");

			Log.d(TAG, "Reference used: " + strRefDescr);
			Log.d(TAG, "It is now " + DateUtils.now());

			Log.d(TAG, "Substracting " + strRef);
			Log.d(TAG, "from " + strCurrent);
		}

		for (int i = 0; i < myNetworkStats.size(); i++)
		{
			NetworkUsage netStat = (NetworkUsage) myNetworkStats.get(i);
			if ((!bFilter) || ((netStat.getTotalBytes()) > 0))
			{
				if ((myReference != null)
						&& (myReference.m_refNetworkStats != null))
				{
					netStat.substractFromRef(myReference.m_refNetworkStats);

					// we must recheck if the delta process is still above
					// threshold
					if ((!bFilter) || ((netStat.getTotalBytes()) > 0))
					{
						myRetNetworkStats.add(netStat);
					}
				} else
				{
					myRetNetworkStats.clear();
					if (myReference != null)
					{
						myRetNetworkStats.add(new NetworkUsage(myReference
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
	public ArrayList<StatElement> getCpuStateList(int iStatTypeFrom, Reference refTo)
			throws Exception

	{
		// List to store the other usages to
		ArrayList<StatElement> myStates = refTo.m_refCpuStates;

		ArrayList<StatElement> myStats = new ArrayList<StatElement>();

		if (myStates == null)
		{
			return myStats;
		}

		String strCurrent = myStates.toString();
		String strRef = "";
		String strRefDescr = "";

		Reference myReference = ReferenceStore.getReference(iStatTypeFrom, m_context);

		if (LogSettings.DEBUG)
		{
			if (myReference != null)
			{
				strRefDescr = myReference.whoAmI();
				if (myReference.m_refCpuStates != null)
				{
					strRef = myReference.m_refCpuStates.toString();
				} else
				{
					strRef = "CPU States is null";
				}

			} else
			{
				strRefDescr = "Reference is null";
			}
			Log.d(TAG, "Processing CPU States since "
					+ statTypeToLabel(iStatTypeFrom));

			Log.d(TAG, "Reference used: " + strRefDescr);
			Log.d(TAG, "It is now " + DateUtils.now());

			Log.d(TAG, "Substracting " + strRef);
			Log.d(TAG, "from " + strCurrent);
		}

		for (int i = 0; i < myStates.size(); i++)
		{
			State state = (State) myStates.get(i);


			if ((myReference != null)
					&& (myReference.m_refCpuStates != null))
			{
				state.substractFromRef(myReference.m_refCpuStates);
				myStats.add(state);
			} else
			{
				myStats.clear();
				myStats.add(new State(1, 1));
			}
		}
		return myStats;

	}

	public ArrayList<StatElement> getCurrentCpuStateList()
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

			myStats.add(state);
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
			int iStatType, boolean bFilterView, boolean bWidget, Reference refTo)
			throws Exception
	{
	
		BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(m_context);

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(m_context);

		ArrayList<StatElement> myStats = new ArrayList<StatElement>();

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
//		Collections.sort(myUsages);

		String strCurrent = myUsages.toString();
		String strRef = "";
		String strRefDescr = "";

		Reference myReference = ReferenceStore.getReference(iStatType, m_context);

		if (LogSettings.DEBUG)
		{
			if (myReference != null)
			{
				strRefDescr = myReference.whoAmI();
				if (myReference.m_refOther != null)
				{
					strRef = myReference.m_refOther.toString();
				} else
				{
					strRef = "Other is null";
				}

			} else
			{
				strRefDescr = "Reference is null";
			}
			Log.d(TAG, "Processing Other since " + statTypeToLabel(iStatType));

			Log.d(TAG, "Reference used: " + strRefDescr);
			Log.d(TAG, "It is now " + DateUtils.now());

			Log.d(TAG, "Substracting " + strRef);
			Log.d(TAG, "from " + strCurrent);
		}

		for (int i = 0; i < myUsages.size(); i++)
		{
			Misc usage = (Misc)myUsages.get(i);
			Log.d(TAG,
					"Current value: " + usage.getName() + " " + usage.getData());
			if ((!bFilter) || (usage.getTimeOn() > 0))
			{
				if ((myReference != null)
						&& (myReference.m_refOther != null))
				{
					usage.substractFromRef(myReference.m_refOther);
					if ((!bFilter) || (usage.getTimeOn() > 0))
					{
						Log.d(TAG, "Result value: " + usage.getName() + " "
								+ usage.getData());
						myStats.add((StatElement) usage);
					}
				}
				else
				{
					myStats.clear();
					if (myReference != null)
					{
						myStats.add(new Misc(myReference
								.getMissingRefError(), 1, 1));
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
	
		BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(m_context);

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(m_context);

		ArrayList<StatElement> myStats = new ArrayList<StatElement>();

		// List to store the other usages to
		ArrayList<StatElement> myUsages = new ArrayList<StatElement>();

		long rawRealtime = SystemClock.elapsedRealtime() * 1000;
		
		long batteryRealtime = mStats.getBatteryRealtime(rawRealtime);

		long whichRealtime = mStats.computeBatteryRealtime(rawRealtime,
				BatteryStatsTypes.STATS_CURRENT) / 1000;
		
		long timeBatteryUp = mStats.computeBatteryUptime(
				SystemClock.uptimeMillis() * 1000,
				BatteryStatsTypes.STATS_CURRENT) / 1000;
		Log.i(TAG, "whichRealtime = " + whichRealtime + " batteryRealtime = " + batteryRealtime + " timeBatteryUp=" + timeBatteryUp);
		
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

		Misc deepSleepUsage = new Misc("Deep Sleep", timeDeepSleep,
				whichRealtime);
		Log.d(TAG, "Added Deep sleep:" + deepSleepUsage.getData());


		if ((!bFilter) || (deepSleepUsage.getTimeOn() > 0))
		{
			myUsages.add(deepSleepUsage);
		}

		if (timeBatteryUp > 0)
		{
			myUsages.add(new Misc("Awake", timeBatteryUp, whichRealtime));
		}

		if (timeScreenOn > 0)
		{
			myUsages.add(new Misc("Screen On", timeScreenOn, whichRealtime));
		}

		if (timePhoneOn > 0)
		{
			myUsages.add(new Misc("Phone On", timePhoneOn, whichRealtime));
		}

		if ((timeWifiOn > 0)
				&& (!bFilterView || sharedPrefs.getBoolean("show_other_wifi",
						true)))
		{
			myUsages.add(new Misc("Wifi On", timeWifiOn, whichRealtime));
		}

		if ((timeWifiRunning > 0)
				&& (!bFilterView || sharedPrefs.getBoolean("show_other_wifi",
						true)))

		{
			myUsages.add(new Misc("Wifi Running", timeWifiRunning,
					whichRealtime));
		}

		if ((timeBluetoothOn > 0)
				&& (!bFilterView || sharedPrefs.getBoolean("show_other_bt",
						true)))

		{
			myUsages.add(new Misc("Bluetooth On", timeBluetoothOn,
					whichRealtime));
		}

		if ((timeNoDataConnection > 0)
				&& (!bFilterView || sharedPrefs.getBoolean(
						"show_other_connection", true)))

		{
			myUsages.add(new Misc("No Data Connection", timeNoDataConnection,
					whichRealtime));
		}

		if ((timeSignalNone > 0)
				&& (!bFilterView || sharedPrefs.getBoolean("show_other_signal",
						true)))

		{
			myUsages.add(new Misc("No or Unknown Signal", timeSignalNone,
					whichRealtime));
		}

		if ((timeSignalPoor > 0)
				&& (!bFilterView || sharedPrefs.getBoolean("show_other_signal",
						true)))
		{
			myUsages.add(new Misc("Poor Signal", timeSignalPoor, whichRealtime));
		}

		if ((timeSignalModerate > 0)
				&& (!bFilterView || sharedPrefs.getBoolean("show_other_signal",
						true)))
		{
			myUsages.add(new Misc("Moderate Signal", timeSignalModerate,
					whichRealtime));
		}

		if ((timeSignalGood > 0)
				&& (!bFilterView || sharedPrefs.getBoolean("show_other_signal",
						true)))
		{
			myUsages.add(new Misc("Good Signal", timeSignalGood, whichRealtime));
		}

		if ((timeSignalGreat > 0)
				&& (!bFilterView || sharedPrefs.getBoolean("show_other_signal",
						true)))
		{
			myUsages.add(new Misc("Great Signal", timeSignalGreat,
					whichRealtime));
		}

		if ((timeScreenDark > 0)
				&& (!bFilterView || sharedPrefs.getBoolean("show_other_screen_brightness",
						true)))

		{
			myUsages.add(new Misc("Screen dark", timeScreenDark,
					whichRealtime));
		}

		if ((timeScreenDim > 0)
				&& (!bFilterView || sharedPrefs.getBoolean("show_other_screen_brightness",
						true)))

		{
			myUsages.add(new Misc("Screen dimmed", timeScreenDim,
					whichRealtime));
		}

		if ((timeScreenMedium > 0)
				&& (!bFilterView || sharedPrefs.getBoolean("show_other_screen_brightness",
						true)))

		{
			myUsages.add(new Misc("Screen medium", timeScreenMedium,
					whichRealtime));
		}
		if ((timeScreenLight > 0)
				&& (!bFilterView || sharedPrefs.getBoolean("show_other_screen_brightness",
						true)))

		{
			myUsages.add(new Misc("Screen light", timeScreenLight,
					whichRealtime));
		}

		if ((timeScreenBright > 0)
				&& (!bFilterView || sharedPrefs.getBoolean("show_other_screen_brightness",
						true)))

		{
			myUsages.add(new Misc("Screen bright", timeScreenBright,
					whichRealtime));
		}

		// if ( (timeWifiMulticast > 0) && (!bFilterView ||
		// sharedPrefs.getBoolean("show_other_wifi", true)) )
		// {
		// myUsages.add(new Misc("Wifi Multicast On", timeWifiMulticast,
		// whichRealtime));
		// }
		//
		// if ( (timeWifiLocked > 0) && (!bFilterView ||(!bFilterView ||
		// sharedPrefs.getBoolean("show_other_wifi", true)) )
		// {
		// myUsages.add(new Misc("Wifi Locked", timeWifiLocked, whichRealtime));
		// }
		//
		// if ( (timeWifiScan > 0) && (!bFilterView ||
		// sharedPrefs.getBoolean("show_other_wifi", true)) )
		// {
		// myUsages.add(new Misc("Wifi Scan", timeWifiScan, whichRealtime));
		// }
		//
		// if (timeAudioOn > 0)
		// {
		// myUsages.add(new Misc("Video On", timeAudioOn, whichRealtime));
		// }
		//
		// if (timeVideoOn > 0)
		// {
		// myUsages.add(new Misc("Video On", timeVideoOn, whichRealtime));
		// }

		// sort @see
		// com.asksven.android.common.privateapiproxies.Walkelock.compareTo
//		Collections.sort(myUsages);

		for (int i = 0; i < myUsages.size(); i++)
		{
			Misc usage = (Misc)myUsages.get(i);
			Log.d(TAG,
					"Current value: " + usage.getName() + " " + usage.getData());
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
	public int getBatteryLevelStat(int iStatType)
	{
		// deep sleep times are independent of stat type
		int level = getBatteryLevel();

		Log.d(TAG, "Current Battery Level:" + level);
		Reference myReference = ReferenceStore.getReference(iStatType, m_context);
		if (myReference != null)
		{
			level = myReference.m_refBatteryLevel - level;
		}

		Log.d(TAG, "Battery Level since " + iStatType + ":" + level);

		return level;
	}

	/**
	 * Get the battery level lost since a given ref
	 * 
	 * @param iStatType
	 *            the reference
	 * @return the lost battery level
	 */
	public String getBatteryLevelFromTo(int iStatType, String refToName)
	{
		// deep sleep times are independent of stat type
		long lLevelTo = getBatteryLevel();
		long lLevelFrom = 0;
		long sinceH = -1;
		String levelTo = String.valueOf(lLevelTo);
		
		String levelFrom = "-";
		Log.d(TAG, "Current Battery Level:" + levelTo);
		Reference myReference = ReferenceStore.getReference(iStatType, m_context);
		
		if (myReference != null)
		{
			lLevelFrom = myReference.m_refBatteryLevel;
			levelFrom = String.valueOf(lLevelFrom);
		}

		Log.d(TAG, "Battery Level since " + iStatType + ":" + levelFrom);

		String drop_per_hour = "";
		try
		{
			sinceH = getSince(iStatType, refToName);
			// since is in ms but x 100 in order to respect proportions of formatRatio (we call it with % and it normally calculates % so there is a factor 100
			sinceH = sinceH / 10 / 60 / 60;
			drop_per_hour = StringUtils.formatRatio(lLevelFrom - lLevelTo, sinceH) + "/h";
		}
		catch (Exception e)
		{
			drop_per_hour = "";
			Log.e(TAG, "Error retrieving since");
		}
		
		return "Bat.: " + getBatteryLevelStat(iStatType) + "% (" + levelFrom
				+ "% to " + levelTo + "%)" + " [" + drop_per_hour + "]";
	}

	/**
	 * Get the battery voltage lost since a given ref
	 * 
	 * @param iStatType
	 *            the reference
	 * @return the lost battery level
	 */
	public int getBatteryVoltageStat(int iStatType)
	{
		// deep sleep times are independent of stat type
		int voltage = getBatteryVoltage();

		Log.d(TAG, "Current Battery Voltage:" + voltage);
		Reference myReference = ReferenceStore.getReference(iStatType, m_context);
		if (myReference != null)
		{
			voltage = myReference.m_refBatteryVoltage - voltage;
		}

		Log.d(TAG, "Battery Voltage since " + iStatType + ":" + voltage);

		return voltage;
	}

	/**
	 * Get the battery voltage lost since a given ref
	 * 
	 * @param iStatType
	 *            the reference
	 * @return the lost battery level
	 */
	public String getBatteryVoltageFromTo(int iStatType, String refToName)
	{
		// deep sleep times are independent of stat type
		int voltageTo = getBatteryVoltage();
		int voltageFrom = -1;
		long sinceH = -1;


		Log.d(TAG, "Current Battery Voltage:" + voltageTo);
		Reference myReference = ReferenceStore.getReference(iStatType, m_context);
		if (myReference != null)
		{
			voltageFrom = myReference.m_refBatteryVoltage;
		}

		Log.d(TAG, "Battery Voltage since " + iStatType + ":" + voltageFrom);
		
		String drop_per_hour = "";
		try
		{
			sinceH = getSince(iStatType, refToName);
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
		Reference thisRef = new Reference(Reference.CUSTOM_REF_FILENAME);
		ReferenceStore.put(Reference.CUSTOM_REF_FILENAME, populateReference(iSort, thisRef), m_context);
	}

	/**
	 * Saves all data at the current point in time
	 */
	public void setCurrentReference(int iSort)
	{
		Reference thisRef = new Reference(Reference.CURRENT_REF_FILENAME);
		ReferenceStore.put(Reference.CURRENT_REF_FILENAME, populateReference(iSort, thisRef), m_context);
	}

	/**
	 * Saves all data to a point in time when the screen goes off This data will
	 * be used in a custom "since..." stat type
	 */
	public void setReferenceSinceScreenOff(int iSort)
	{
		Reference thisRef = new Reference(Reference.SCREEN_OFF_REF_FILENAME);
		ReferenceStore.put(Reference.SCREEN_OFF_REF_FILENAME, populateReference(iSort, thisRef), m_context);
	}

	/**
	 * Saves data when battery is fully charged This data will be used in the
	 * "since charged" stat type
	 */
	public void setReferenceSinceCharged(int iSort)
	{
		Reference thisRef = new Reference(Reference.CHARGED_REF_FILENAME);
		ReferenceStore.put(Reference.CHARGED_REF_FILENAME, populateReference(iSort, thisRef), m_context);
	}

	/**
	 * Saves data when the phone is unplugged This data will be used in the
	 * "since unplugged" stat type
	 */
	public void setReferenceSinceUnplugged(int iSort)
	{
		Reference thisRef = new Reference(Reference.UNPLUGGED_REF_FILENAME);
		ReferenceStore.put(Reference.UNPLUGGED_REF_FILENAME, populateReference(iSort, thisRef), m_context);
	}

	/**
	 * Saves data when the phone is booted This data will be used in the
	 * "since unplugged" stat type
	 */
	public void setReferenceSinceBoot(int iSort)
	{
		Reference thisRef = new Reference(Reference.BOOT_REF_FILENAME);
		ReferenceStore.put(Reference.BOOT_REF_FILENAME, populateReference(iSort, thisRef), m_context);
	}

	/**
	 * Saves a reference to cache and persists it
	 */
	private Reference populateReference(int iSort, Reference refs)
	{
		
		// we are going to retrieve a reference: make sure data does not come from the cache
		BatteryStatsProxy.getInstance(m_context).invalidate();
		
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(m_context);

		boolean bFilterStats = sharedPrefs.getBoolean("filter_data", true);
		int iPctType = Integer.valueOf(sharedPrefs.getString("default_wl_ref",
				"0"));

		try
		{
			refs.m_refOther = null;
			refs.m_refWakelocks = null;
			refs.m_refKernelWakelocks = null;
			refs.m_refNetworkStats = null;

			refs.m_refAlarms = null;
			refs.m_refProcesses = null;
			refs.m_refCpuStates = null;

			refs.m_refKernelWakelocks = getCurrentNativeKernelWakelockStatList(bFilterStats, iPctType, iSort);
			refs.m_refWakelocks = getCurrentWakelockStatList(bFilterStats, iPctType, iSort);

			refs.m_refOther = getCurrentOtherUsageStatList(bFilterStats, false, false);
			refs.m_refCpuStates = getCurrentCpuStateList();

			refs.m_refProcesses = getCurrentProcessStatList(bFilterStats, iSort);

			refs.m_refBatteryRealtime = getBatteryRealtime(BatteryStatsTypes.STATS_CURRENT);

			try
			{
				refs.m_refBatteryLevel = getBatteryLevel();
				refs.m_refBatteryVoltage = getBatteryVoltage();
			} catch (ReceiverCallNotAllowedException e)
			{
				Log.e("TAG", "An exception occured. Message: " + e.getMessage());
				Log.e(TAG, "Exception: " + Log.getStackTraceString(e));
				refs.m_refBatteryLevel = 0;
				refs.m_refBatteryVoltage = 0;
			}

			// only root features active
			boolean rootEnabled = sharedPrefs
					.getBoolean("root_features", false);
			if (rootEnabled)
			{
				// After that we go on and try to write the rest. If this part
				// fails at least there will be a partial ref saved
				refs.m_refNetworkStats = getCurrentNativeNetworkUsageStatList(bFilterStats);

				refs.m_refAlarms = getCurrentAlarmsStatList(bFilterStats);

			}
		} catch (Exception e)
		{
			Log.e("TAG", "An exception occured. Message: " + e.getMessage());
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
	 * Returns the battery realtime since a given reference
	 * 
	 * @param iStatType
	 *            the reference
	 * @return the battery realtime
	 */
	public long getBatteryRealtime(int iStatType)
			throws BatteryInfoUnavailableException
	{
		BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(m_context);

		if (mStats == null)
		{
			// an error has occured
			return -1;
		}

		long whichRealtime = 0;
		long rawRealtime = SystemClock.elapsedRealtime() * 1000;
		if ((iStatType == StatsProvider.STATS_CUSTOM) && (ReferenceStore.getReferenceByName(Reference.CUSTOM_REF_FILENAME, m_context) != null))
		{
			whichRealtime = mStats.computeBatteryRealtime(rawRealtime,
					BatteryStatsTypes.STATS_CURRENT) / 1000;
			whichRealtime -= ReferenceStore.getReferenceByName(Reference.CUSTOM_REF_FILENAME, m_context).m_refBatteryRealtime;
		}
		else if ((iStatType == StatsProvider.STATS_SCREEN_OFF)
				&& (ReferenceStore.getReferenceByName(Reference.SCREEN_OFF_REF_FILENAME, m_context) != null))
		{
			whichRealtime = mStats.computeBatteryRealtime(rawRealtime,
					BatteryStatsTypes.STATS_CURRENT) / 1000;
			whichRealtime -= ReferenceStore.getReferenceByName(Reference.SCREEN_OFF_REF_FILENAME, m_context).m_refBatteryRealtime;
		}
		else if ((iStatType == StatsProvider.STATS_BOOT)
				&& (ReferenceStore.getReferenceByName(Reference.BOOT_REF_FILENAME, m_context) != null))
		{
			whichRealtime = mStats.computeBatteryRealtime(rawRealtime,
					BatteryStatsTypes.STATS_CURRENT) / 1000;
			whichRealtime -= ReferenceStore.getReferenceByName(Reference.BOOT_REF_FILENAME, m_context).m_refBatteryRealtime;
		}
		else
		{
			whichRealtime = mStats.computeBatteryRealtime(rawRealtime,
					iStatType) / 1000;
		}
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
	public void writeDumpToFile(int iStatType, int iSort, String refToName)
	{
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(m_context);
		boolean bFilterStats = sharedPrefs.getBoolean("filter_data", true);
		int iPctType = Integer.valueOf(sharedPrefs.getString("default_wl_ref",
				"0"));

		Reference refTo = ReferenceStore.getReferenceByName(refToName, m_context);

		if (!DataStorage.isExternalStorageWritable())
		{
			Log.e(TAG, "External storage can not be written");
			Toast.makeText(m_context, "External Storage can not be written",
					Toast.LENGTH_SHORT).show();
		}
		try
		{
			// open file for writing
			File root = Environment.getExternalStorageDirectory();

			// check if file can be written
			if (root.canWrite())
			{
				String strFilename = "BetterBatteryStats-"
						+ DateUtils.now("yyyy-MM-dd_HHmmssSSS") + ".txt";
				File dumpFile = new File(root, strFilename);
				FileWriter fw = new FileWriter(dumpFile);
				BufferedWriter out = new BufferedWriter(fw);

				// write header
				out.write("===================\n");
				out.write("General Information\n");
				out.write("===================\n");
				PackageInfo pinfo = m_context.getPackageManager()
						.getPackageInfo(m_context.getPackageName(), 0);
				out.write("BetterBatteryStats version: " + pinfo.versionName
						+ "\n");
				out.write("Creation Date: " + DateUtils.now() + "\n");
				out.write("Statistic Type: (" + iStatType + ") "
						+ statTypeToLabel(iStatType) + "\n");
				out.write("Since "
						+ DateUtils.formatDuration(getSince(iStatType, null)) + "\n");
				out.write("VERSION.RELEASE: " + Build.VERSION.RELEASE + "\n");
				out.write("BRAND: " + Build.BRAND + "\n");
				out.write("DEVICE: " + Build.DEVICE + "\n");
				out.write("MANUFACTURER: " + Build.MANUFACTURER + "\n");
				out.write("MODEL: " + Build.MODEL + "\n");
				out.write("OS.VERSION: " + System.getProperty("os.version")
						+ "\n");

				if (Build.VERSION.SDK_INT >= 8)
				{

					out.write("BOOTLOADER: " + Build.BOOTLOADER + "\n");
					out.write("HARDWARE: " + Build.HARDWARE + "\n");
				}
				out.write("FINGERPRINT: " + Build.FINGERPRINT + "\n");
				out.write("ID: " + Build.ID + "\n");
				out.write("TAGS: " + Build.TAGS + "\n");
				out.write("USER: " + Build.USER + "\n");
				out.write("PRODUCT: " + Build.PRODUCT + "\n");
				String radio = "";

				// from API14
				if (Build.VERSION.SDK_INT >= 14)
				{
					radio = Build.getRadioVersion();
				}

				else if (Build.VERSION.SDK_INT >= 8)
				{
					radio = Build.RADIO;
				}

				out.write("RADIO: " + radio + "\n");
				out.write("Rooted: "
						+ RootDetection.hasSuRights("dumpsys alarm") + "\n");

				out.write("============\n");
				out.write("Battery Info\n");
				out.write("============\n");
				out.write("Level lost [%]: " + getBatteryLevelStat(iStatType)
						+ " " + getBatteryLevelFromTo(iStatType, refToName) + "\n");
				out.write("Voltage lost [mV]: "
						+ getBatteryVoltageStat(iStatType) + " "
						+ getBatteryVoltageFromTo(iStatType, refToName) + "\n");

				// write timing info
				boolean bDumpChapter = sharedPrefs.getBoolean("show_other",
						true);
				if (bDumpChapter)
				{
					out.write("===========\n");
					out.write("Other Usage\n");
					out.write("===========\n");
					dumpList(
							getOtherUsageStatList(bFilterStats, iStatType,
									false, false, refTo), out);
				}

				bDumpChapter = sharedPrefs.getBoolean("show_pwl", true);
				if (bDumpChapter)
				{
					// write wakelock info
					out.write("=========\n");
					out.write("Wakelocks\n");
					out.write("=========\n");
					dumpList(
							getWakelockStatList(bFilterStats, iStatType,
									iPctType, iSort, refTo), out);
					
				}

				bDumpChapter = sharedPrefs.getBoolean("show_kwl", true);
				if (bDumpChapter)
				{
					if (Wakelocks.isDiscreteKwlPatch())
					{
						// write kernel wakelock info
						out.write("===================================\n");
						out.write("Kernel Wakelocks (!!! discrete !!!)\n");
						out.write("===================================\n");
					} else
					{
						// write kernel wakelock info
						out.write("================\n");
						out.write("Kernel Wakelocks\n");
						out.write("================\n");
					}
					dumpList(
							getNativeKernelWakelockStatList(bFilterStats,
									iStatType, iPctType, iSort, refTo), out);
				}

				bDumpChapter = sharedPrefs.getBoolean("show_proc", false);
				if (bDumpChapter)
				{
					// write process info
					out.write("=========\n");
					out.write("Processes\n");
					out.write("=========\n");
					dumpList(
							getProcessStatList(bFilterStats, iStatType, iSort, refTo),
							out);
				}

				bDumpChapter = sharedPrefs.getBoolean("show_alarm", true);
				if (bDumpChapter)
				{
					// write alarms info
					out.write("======================\n");
					out.write("Alarms (requires root)\n");
					out.write("======================\n");
					dumpList(getAlarmsStatList(bFilterStats, iStatType, refTo), out);
				}

				bDumpChapter = sharedPrefs.getBoolean("show_network", true);
				if (bDumpChapter)
				{
					// write alarms info
					out.write("======================\n");
					out.write("Network (requires root)\n");
					out.write("======================\n");
					dumpList(
							getNativeNetworkUsageStatList(bFilterStats,
									iStatType, refTo), out);
				}

				bDumpChapter = sharedPrefs.getBoolean("show_cpustates", true);
				if (bDumpChapter)
				{
					// write alarms info
					out.write("==========\n");
					out.write("CPU States\n");
					out.write("==========\n");
					dumpList(getCpuStateList(iStatType, refTo), out);
				}

				bDumpChapter = sharedPrefs.getBoolean("show_serv", false);
				if (bDumpChapter)
				{
					out.write("========\n");
					out.write("Services\n");
					out.write("========\n");
					out.write("Active since: The time when the service was first made active, either by someone starting or binding to it.\n");
					out.write("Last activity: The time when there was last activity in the service (either explicit requests to start it or clients binding to it)\n");
					out.write("See http://developer.android.com/reference/android/app/ActivityManager.RunningServiceInfo.html\n");
					ActivityManager am = (ActivityManager) m_context
							.getSystemService(m_context.ACTIVITY_SERVICE);
					List<ActivityManager.RunningServiceInfo> rs = am
							.getRunningServices(50);

					for (int i = 0; i < rs.size(); i++)
					{
						ActivityManager.RunningServiceInfo rsi = rs.get(i);
						out.write(rsi.process + " ("
								+ rsi.service.getClassName() + ")\n");
						out.write("  Active since: "
								+ DateUtils.formatDuration(rsi.activeSince)
								+ "\n");
						out.write("  Last activity: "
								+ DateUtils
										.formatDuration(rsi.lastActivityTime)
								+ "\n");
						out.write("  Crash count:" + rsi.crashCount + "\n");
					}
				}

				// add chapter for reference info
				out.write("==================\n");
				out.write("Reference overview\n");
				out.write("==================\n");
				if (ReferenceStore.getReferenceByName(Reference.CUSTOM_REF_FILENAME, m_context) != null)
				{
					out.write("Custom: " + ReferenceStore.getReferenceByName(Reference.CUSTOM_REF_FILENAME, m_context).whoAmI() + "\n");
				} else
				{
					out.write("Custom: " + "null" + "\n");
				}

				if (ReferenceStore.getReferenceByName(Reference.CHARGED_REF_FILENAME, m_context) != null)
				{
					out.write("Since charged: " + ReferenceStore.getReferenceByName(Reference.CHARGED_REF_FILENAME, m_context).whoAmI()
							+ "\n");
				} else
				{
					out.write("Since charged: " + "null" + "\n");
				}

				if (ReferenceStore.getReferenceByName(Reference.SCREEN_OFF_REF_FILENAME, m_context) != null)
				{
					out.write("Since screen off: "
							+ ReferenceStore.getReferenceByName(Reference.SCREEN_OFF_REF_FILENAME, m_context).whoAmI() + "\n");
				} else
				{
					out.write("Since screen off: " + "null" + "\n");
				}

				if (ReferenceStore.getReferenceByName(Reference.UNPLUGGED_REF_FILENAME, m_context) != null)
				{
					out.write("Since unplugged: "
							+ ReferenceStore.getReferenceByName(Reference.UNPLUGGED_REF_FILENAME, m_context).whoAmI() + "\n");
				} else
				{
					out.write("Since unplugged: " + "null" + "\n");
				}

				if (ReferenceStore.getReferenceByName(Reference.BOOT_REF_FILENAME, m_context) != null)
				{
					out.write("Since boot: " + ReferenceStore.getReferenceByName(Reference.BOOT_REF_FILENAME, m_context).whoAmI() + "\n");
				} else
				{
					out.write("Since boot: " + "null" + "\n");
				}

				// close file
				out.close();
				// Toast.makeText(m_context, "Dump witten: " + strFilename,
				// Toast.LENGTH_SHORT).show();

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
	}

	public void writeLogcatToFile()
	{
		if (!DataStorage.isExternalStorageWritable())
		{
			Log.e(TAG, "External storage can not be written");
			Toast.makeText(m_context, "External Storage can not be written",
					Toast.LENGTH_SHORT).show();
		}
		try
		{
			// open file for writing
			File root = Environment.getExternalStorageDirectory();
			String path = root.getAbsolutePath();
			// check if file can be written
			if (root.canWrite())
			{
				String filename = "logcat-"
						+ DateUtils.now("yyyy-MM-dd_HHmmssSSS") + ".txt";
				Util.run("logcat -d > " + path + "/" + filename);
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
	}

	public void writeDmesgToFile()
	{
		if (!DataStorage.isExternalStorageWritable())
		{
			Log.e(TAG, "External storage can not be written");
			Toast.makeText(m_context, "External Storage can not be written",
					Toast.LENGTH_SHORT).show();
		}
		try
		{
			// open file for writing
			File root = Environment.getExternalStorageDirectory();
			String path = root.getAbsolutePath();
			// check if file can be written
			if (root.canWrite())
			{
				String filename = "dmesg-"
						+ DateUtils.now("yyyy-MM-dd_HHmmssSSS") + ".txt";
				Util.run("dmesg > " + path + "/" + filename);
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
	}

	/**
	 * Dump the elements on one list
	 * 
	 * @param myList
	 *            a list of StatElement
	 */
	private void dumpList(List<StatElement> myList, BufferedWriter out)
			throws IOException
	{
		if (myList != null)
		{
			for (int i = 0; i < myList.size(); i++)
			{
				out.write(myList.get(i).getDumpData(m_context) + "\n");

			}
		}
	}

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

}
