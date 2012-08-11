/*
 * Copyright (C) 2011-2012 asksven
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.asksven.android.common.kernelutils.Alarm;
import com.asksven.android.common.kernelutils.AlarmsDumpsys;
import com.asksven.android.common.kernelutils.CpuStates;
import com.asksven.android.common.kernelutils.NativeKernelWakelock;
import com.asksven.android.common.kernelutils.Netstats;
import com.asksven.android.common.kernelutils.RootDetection;
import com.asksven.android.common.kernelutils.State;
import com.asksven.android.common.kernelutils.Wakelocks;
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
import com.asksven.betterbatterystats.R;

/**
 * Singleton provider for all the statistics
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
	public final static int STATS_CHARGED 	= 0;
	public final static int STATS_UNPLUGGED 	= 3;
	public final static int STATS_CUSTOM 		= 4;
	public final static int STATS_SCREEN_OFF	= 5;
	
	/** the logger tag */
	static String TAG = "StatsProvider";
	
	/** the text when no custom reference is set */
	static String NO_CUST_REF = "No custom reference was set";
	
	/** the storage for references */
	static References m_myRefs 					= new References(References.CUSTOM_REF_FILENAME);
	static References m_myRefSinceUnplugged 	= new References(References.SINCE_UNPLUGGED_REF_FILENAME);
	static References m_myRefSinceCharged 		= new References(References.SINCE_CHARGED_REF_FILENAME);
	static References m_myRefSinceScreenOff		= new References(References.SINCE_SCREEN_OFF_REF_FILENAME);

	/**
	 * The constructor (hidden)
	 */
	private StatsProvider()
	{
	}
	
	/**
	 * returns a singleton instance
	 * @param ctx the application context
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
	 * @return a List of StatElements sorted (descending)
	 */
	public ArrayList<StatElement> getStatList(int iStat, int iStatType, int iSort)
	{
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(m_context);
		boolean bFilterStats = sharedPrefs.getBoolean("filter_data", true);
		int iPctType = Integer.valueOf(sharedPrefs.getString("default_wl_ref", "0"));
		
		try
    	{			
			// constants are related to arrays.xml string-array name="stats"
			switch (iStat)
			{
				case 0:
					return getOtherUsageStatList(bFilterStats, iStatType, true);
				case 1:
					return getNativeKernelWakelockStatList(bFilterStats, iStatType, iPctType, iSort);
				case 2:
					return getWakelockStatList(bFilterStats, iStatType, iPctType, iSort);
				case 3:
					return getAlarmsStatList(bFilterStats, iStatType);	
				case 4:
					return getNativeNetworkUsageStatList(bFilterStats, iStatType);
				case 5:
					return getCpuStateList(iStatType);
				case 6:
					return getProcessStatList(bFilterStats, iStatType, iSort);
	
			}
			
    	}
    	catch (Exception e)
    	{
    		Log.e(TAG, "Exception: " + e.getMessage());
    	}
		
		return new ArrayList<StatElement>();
	}

	/**
	 * Get the Alarm Stat to be displayed
	 * @param bFilter defines if zero-values should be filtered out
	 * @return a List of Other usages sorted by duration (descending)
	 * @throws Exception if the API call failed
	 */
	
	public ArrayList<StatElement> getAlarmsStatList(boolean bFilter, int iStatType) throws Exception
	{
		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		ArrayList<Alarm> myAlarms = AlarmsDumpsys.getAlarms();
		Collections.sort(myAlarms);
		
		ArrayList<Alarm> myRetAlarms = new ArrayList<Alarm>();
		// if we are using custom ref. always retrieve "stats current"


		// sort @see com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		String strCurrent = myAlarms.toString();
		String strRef = "";
		String strRefDescr = "";
		if (Log.isLoggable(TAG, Log.DEBUG))
		{
			switch (iStatType)
			{
				case STATS_UNPLUGGED:									
					if ( (m_myRefSinceUnplugged != null) && (m_myRefSinceUnplugged.m_refAlarms != null) )
					{
						strRef = m_myRefSinceUnplugged.m_refAlarms.toString();
						strRefDescr = m_myRefSinceUnplugged.whoAmI();
					}
					break;
				case STATS_CHARGED:
					if ( (m_myRefSinceCharged != null) && (m_myRefSinceCharged.m_refAlarms != null) )
					{
						strRef = m_myRefSinceCharged.m_refAlarms.toString();
						strRefDescr = m_myRefSinceUnplugged.whoAmI();
					}
					break;
				case STATS_CUSTOM:
					if ( (m_myRefs != null) && (m_myRefs.m_refAlarms != null))
					{
						strRef = m_myRefs.m_refAlarms.toString();
						strRefDescr = m_myRefSinceUnplugged.whoAmI();
					}
					break;
				case STATS_SCREEN_OFF:
					if ( (m_myRefSinceScreenOff != null) && (m_myRefSinceScreenOff.m_refAlarms != null))
					{
						strRef = m_myRefSinceScreenOff.m_refAlarms.toString();
						strRefDescr = m_myRefSinceUnplugged.whoAmI();
					}
					break;
				case BatteryStatsTypes.STATS_CURRENT:
					strRef = "no reference to substract";
					break;
				default:
					Log.e(TAG, "Unknown StatType " + iStatType + ". No reference found");
					break;
			}
			Log.d(TAG, "Processing alarms since " + statTypeToLabel(iStatType));
	
			Log.d(TAG, "Reference used: " + strRefDescr);
			Log.d(TAG, "It is now " + DateUtils.now());
	
			Log.d(TAG, "Substracting " + strCurrent);
			Log.d(TAG, "from " + strRef);
		}
		
		for (int i = 0; i < myAlarms.size(); i++)
		{
			Alarm alarm = myAlarms.get(i);
			if ( true ) //(!bFilter) || ((alarm.getWakeups()) > 0) )
			{					
				switch (iStatType)
				{
					case STATS_CUSTOM:					
						// case a)
						// we need t return a delta containing
						//   if a process is in the new list but not in the custom ref
						//	   the full time is returned
						//   if a process is in the reference return the delta
						//	 a process can not have disapeared in btwn so we don't need
						//	 to test the reverse case
						if (m_myRefs != null)
						{
							alarm.substractFromRef(m_myRefs.m_refAlarms);
		
		
							// we must recheck if the delta process is still above threshold
							if ( (!bFilter) || ((alarm.getWakeups()) > 0) )
							{
								myRetAlarms.add( alarm);
							}
						}
						else
						{
							myRetAlarms.clear();
							myRetAlarms.add(new Alarm(NO_CUST_REF));
						}
						break;
					case STATS_UNPLUGGED:
						if (m_myRefSinceUnplugged != null)
						{
							alarm.substractFromRef(m_myRefSinceUnplugged.m_refAlarms);
							
							
							// we must recheck if the delta process is still above threshold
							if ( (!bFilter) || ((alarm.getWakeups()) > 0) )
							{
								myRetAlarms.add( alarm);
							}
						}
						else
						{
							myRetAlarms.clear();
							myRetAlarms.add(new Alarm("No reference since unplugged set yet"));
	
						}
						break;
	
					case STATS_CHARGED:
						if (m_myRefSinceCharged != null)
						{
							alarm.substractFromRef(m_myRefSinceCharged.m_refAlarms);
													
							// we must recheck if the delta process is still above threshold
							if ( (!bFilter) || ((alarm.getWakeups()) > 0) )
							{
								myRetAlarms.add(alarm);
							}
						}
						else
						{
							myRetAlarms.clear();
							myRetAlarms.add(new Alarm("No reference since charged yet"));
						}
						break;
					case STATS_SCREEN_OFF:
						if (m_myRefSinceScreenOff != null)
						{
							alarm.substractFromRef(m_myRefSinceScreenOff.m_refAlarms);
													
							// we must recheck if the delta process is still above threshold
							if ( (!bFilter) || ((alarm.getWakeups()) > 0) )
							{
								myRetAlarms.add(alarm);
							}
						}
						else
						{
							myRetAlarms.clear();
							myRetAlarms.add(new Alarm("No reference since screen off yet"));
						}
						break;

					case BatteryStatsTypes.STATS_CURRENT:
						// we must recheck if the delta process is still above threshold
						myRetAlarms.add(alarm);
						break;
						
				}
	
			}
		}
		
		Collections.sort(myRetAlarms);
		
		for (int i=0; i < myRetAlarms.size(); i++)
		{
			myStats.add((StatElement) myRetAlarms.get(i));
		}
		
		if (Log.isLoggable(TAG, Log.DEBUG))
		{
			Log.d(TAG, "Result " + myStats.toString());
		}

		return myStats;

	}

	
	/**
	 * Get the Process Stat to be displayed
	 * @param bFilter defines if zero-values should be filtered out
	 * @return a List of Wakelocks sorted by duration (descending)
	 * @throws Exception if the API call failed
	 */
	public ArrayList<StatElement> getProcessStatList(boolean bFilter, int iStatType, int iSort) throws Exception
	{
		BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(m_context);
		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		ArrayList<Process> myProcesses = null;
		ArrayList<Process> myRetProcesses = new ArrayList<Process>();
		
		// if we are using custom ref. always retrieve "stats current"
		if ((iStatType == STATS_CUSTOM) || (iStatType == STATS_SCREEN_OFF))
		{
			myProcesses = mStats.getProcessStats(m_context, BatteryStatsTypes.STATS_CURRENT);
		}
		else
		{
			myProcesses = mStats.getProcessStats(m_context, iStatType);
		}
		
		// sort @see com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		//Collections.sort(myProcesses);
		
		for (int i = 0; i < myProcesses.size(); i++)
		{
			Process ps = myProcesses.get(i);
			if ( (!bFilter) || ((ps.getSystemTime() + ps.getUserTime()) > 0) )
			{
				// we must distinguish two situations
				// a) we use custom stat type
				// b) we use regular stat type
				
				if (iStatType == STATS_CUSTOM)
				{
					// case a)
					// we need t return a delta containing
					//   if a process is in the new list but not in the custom ref
					//	   the full time is returned
					//   if a process is in the reference return the delta
					//	 a process can not have disapeared in btwn so we don't need
					//	 to test the reverse case
					if (m_myRefs != null)
					{
						ps.substractFromRef(m_myRefs.m_refProcesses);
						
						// we must recheck if the delta process is still above threshold
						if ( (!bFilter) || ((ps.getSystemTime() + ps.getUserTime()) > 0) )
						{
							myRetProcesses.add(ps);
						}

					}
					else
					{
						myRetProcesses.clear();
						myRetProcesses.add(new Process(NO_CUST_REF, 1, 1, 1));
					}
					
				}
				else
				{
					// case b) nothing special
					myRetProcesses.add(ps);
				}
			}
		}
		
		// sort @see com.asksven.android.common.privateapiproxies.Walkelock.compareTo
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
		
		for (int i=0; i < myRetProcesses.size(); i++)
		{
			myStats.add((StatElement) myRetProcesses.get(i));
		}
		
		if (Log.isLoggable(TAG, Log.DEBUG))
		{
			Log.d(TAG, "Result " + myStats.toString());
		}

		return myStats;
		
	}

	/**
	 * Get the Wakelock Stat to be displayed
	 * @param bFilter defines if zero-values should be filtered out
	 * @return a List of Wakelocks sorted by duration (descending)
	 * @throws Exception if the API call failed
	 */
	public ArrayList<StatElement> getWakelockStatList(boolean bFilter, int iStatType, int iPctType, int iSort) throws Exception
	{
		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		
		BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(m_context);
		
		ArrayList<Wakelock> myWakelocks = null;
		ArrayList<Wakelock> myRetWakelocks = new ArrayList<Wakelock>();
		// if we are using custom ref. always retrieve "stats current"
		String strRef = "";
		String strRefDescr = "";

		if (iStatType == STATS_CUSTOM)
		{
			myWakelocks = mStats.getWakelockStats(m_context, BatteryStatsTypes.WAKE_TYPE_PARTIAL, BatteryStatsTypes.STATS_CURRENT, iPctType);
			if (Log.isLoggable(TAG, Log.DEBUG))
			{
				strRef = m_myRefs.m_refWakelocks.toString();
				strRefDescr = m_myRefs.whoAmI();
			}
		}
		else if (iStatType == STATS_SCREEN_OFF)
		{
			myWakelocks = mStats.getWakelockStats(m_context, BatteryStatsTypes.WAKE_TYPE_PARTIAL, BatteryStatsTypes.STATS_CURRENT, iPctType);
			if (Log.isLoggable(TAG, Log.DEBUG))
			{
				strRef = m_myRefSinceScreenOff.m_refWakelocks.toString();
				strRefDescr = m_myRefSinceScreenOff.whoAmI();
			}
		}

		else
		{
			myWakelocks = mStats.getWakelockStats(m_context, BatteryStatsTypes.WAKE_TYPE_PARTIAL, iStatType, iPctType);
			if (Log.isLoggable(TAG, Log.DEBUG))
			{
				strRefDescr = "native stat " + iStatType;
			}
		}

		if (Log.isLoggable(TAG, Log.DEBUG))
		{
			Log.d(TAG, "Processing partial wakelocks since " + statTypeToLabel(iStatType));
			Log.d(TAG, "Reference used: " + strRefDescr);
			Log.d(TAG, "It is now " + DateUtils.now());
			Log.d(TAG, "Substracting " + myWakelocks.toString());
			Log.d(TAG, "from " + strRef);
		}
		// sort @see com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		Collections.sort(myWakelocks);
		
		for (int i = 0; i < myWakelocks.size(); i++)
		{
			Wakelock wl = myWakelocks.get(i);
			if ( (!bFilter) || ((wl.getDuration()/1000) > 0) )
			{
				// we must distinguish two situations
				// a) we use custom stat type
				// b) we use regular stat type
				
				if (iStatType == STATS_CUSTOM)
				{
					// case a)
					// we need t return a delta containing
					//   if a process is in the new list but not in the custom ref
					//	   the full time is returned
					//   if a process is in the reference return the delta
					//	 a process can not have disapeared in btwn so we don't need
					//	 to test the reverse case
					if (m_myRefs != null)
					{
						wl.substractFromRef(m_myRefs.m_refWakelocks);
						
						// we must recheck if the delta process is still above threshold
						if ( (!bFilter) || ((wl.getDuration()/1000) > 0) )
						{
							myRetWakelocks.add( wl);
						}
					}
					else
					{
						myRetWakelocks.clear();
						myRetWakelocks.add(new Wakelock(1, NO_CUST_REF, 1, 1, 1));
					}
				}
				else if (iStatType == STATS_SCREEN_OFF)
				{
					// case a)
					// we need t return a delta containing
					//   if a process is in the new list but not in the custom ref
					//	   the full time is returned
					//   if a process is in the reference return the delta
					//	 a process can not have disapeared in btwn so we don't need
					//	 to test the reverse case
					if (m_myRefs != null)
					{
						wl.substractFromRef(m_myRefSinceScreenOff.m_refWakelocks);
						
						// we must recheck if the delta process is still above threshold
						if ( (!bFilter) || ((wl.getDuration()/1000) > 0) )
						{
							myRetWakelocks.add( wl);
						}
					}
					else
					{
						myRetWakelocks.clear();
						myRetWakelocks.add(new Wakelock(1, "No screen off ref available", 1, 1, 1));
					}
				}

				else
				{
					// case b) nothing special
					myRetWakelocks.add(wl);
				}

			}
		}

		// sort @see com.asksven.android.common.privateapiproxies.Walkelock.compareTo
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

		
		
		for (int i=0; i < myRetWakelocks.size(); i++)
		{
			myStats.add((StatElement) myRetWakelocks.get(i));
		}

		if (Log.isLoggable(TAG, Log.DEBUG))
		{
			Log.d(TAG, "Result " + myStats.toString());
		}

		return myStats;
	}
	
	/**
	 * Get the Kernel Wakelock Stat to be displayed
	 * @param bFilter defines if zero-values should be filtered out
	 * @return a List of Wakelocks sorted by duration (descending)
	 * @throws Exception if the API call failed
	 */
	public ArrayList<StatElement> getNativeKernelWakelockStatList(boolean bFilter, int iStatType, int iPctType, int iSort) throws Exception
	{
		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		ArrayList<NativeKernelWakelock> myKernelWakelocks = Wakelocks.parseProcWakelocks(m_context);

		ArrayList<NativeKernelWakelock> myRetKernelWakelocks = new ArrayList<NativeKernelWakelock>();
		// if we are using custom ref. always retrieve "stats current"


		// sort @see com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		Collections.sort(myKernelWakelocks);

		String strCurrent = myKernelWakelocks.toString();
		String strRef = "";
		String strRefDescr = "";
		if (Log.isLoggable(TAG, Log.DEBUG))
		{
			switch (iStatType)
			{
				case STATS_UNPLUGGED:									
					if ( (m_myRefSinceUnplugged != null) && (m_myRefSinceUnplugged.m_refKernelWakelocks != null) )
					{
						strRef = m_myRefSinceUnplugged.m_refKernelWakelocks.toString();
						strRefDescr = m_myRefSinceUnplugged.whoAmI();
					}
					break;
				case STATS_CHARGED:
					if ( (m_myRefSinceCharged != null) && (m_myRefSinceCharged.m_refKernelWakelocks != null) )
					{
						strRef = m_myRefSinceCharged.m_refKernelWakelocks.toString();
						strRefDescr = m_myRefSinceCharged.whoAmI();
					}
					break;
				case STATS_SCREEN_OFF:
					if ( (m_myRefSinceScreenOff != null) && (m_myRefSinceScreenOff.m_refKernelWakelocks != null) )
					{
						strRef = m_myRefSinceScreenOff.m_refKernelWakelocks.toString();
						strRefDescr = m_myRefSinceScreenOff.whoAmI();
					}
					break;
				case STATS_CUSTOM:
					if ( (m_myRefs != null) && (m_myRefs.m_refKernelWakelocks != null))
					{
						strRef = m_myRefs.m_refKernelWakelocks.toString();
						strRefDescr = m_myRefs.whoAmI();
					}
					break;
				case BatteryStatsTypes.STATS_CURRENT:
					strRef = "no reference to substract";
					break;
				default:
					Log.e(TAG, "Unknown StatType " + iStatType + ". No reference found");
					break;
			}
			
			Log.d(TAG, "Processing native kernel wakelocks  since " + statTypeToLabel(iStatType));
			Log.d(TAG, "Reference used: " + strRefDescr);
			Log.d(TAG, "It is now " + DateUtils.now());
			Log.d(TAG, "Substracting " + strCurrent);
			Log.d(TAG," from " +  strRef);
		}		
		for (int i = 0; i < myKernelWakelocks.size(); i++)
		{
			NativeKernelWakelock wl = myKernelWakelocks.get(i);
			if ( (!bFilter) || ((wl.getDuration()) > 0) )
			{	
				// native kernel wakelocks are parsed from /proc/wakelocks
				// and do not know any references "since charged" and "since unplugged"
				// those are implemented using special references
				
				switch (iStatType)
				{
					case STATS_CUSTOM:					
						// case a)
						// we need t return a delta containing
						//   if a process is in the new list but not in the custom ref
						//	   the full time is returned
						//   if a process is in the reference return the delta
						//	 a process can not have disapeared in btwn so we don't need
						//	 to test the reverse case
						if ( (m_myRefs != null) && (m_myRefs.m_refKernelWakelocks != null) )
						{
							wl.substractFromRef(m_myRefs.m_refKernelWakelocks);
		
		
							// we must recheck if the delta process is still above threshold
							if ( (!bFilter) || ((wl.getDuration()) > 0) )
							{
								myRetKernelWakelocks.add( wl);
							}
						}
						else
						{
							myRetKernelWakelocks.clear();
							myRetKernelWakelocks.add(new NativeKernelWakelock(NO_CUST_REF, "", 1, 1, 1, 1, 1, 1, 1, 1, 1));
						}
						break;
					case STATS_UNPLUGGED:
						if ( (m_myRefSinceUnplugged != null) && (m_myRefSinceUnplugged.m_refKernelWakelocks != null) )
						{
							wl.substractFromRef(m_myRefSinceUnplugged.m_refKernelWakelocks);
							
							
							// we must recheck if the delta process is still above threshold
							if ( (!bFilter) || ((wl.getDuration()) > 0) )
							{
								myRetKernelWakelocks.add( wl);
							}
						}
						else
						{
							myRetKernelWakelocks.clear();
							myRetKernelWakelocks.add(new NativeKernelWakelock("No reference since unplugged set yet", "", 1, 1, 1, 1, 1, 1, 1, 1, 1));

						}
						break;

					case STATS_CHARGED:
						if ( (m_myRefSinceCharged != null) && (m_myRefSinceCharged.m_refKernelWakelocks != null) )
						{
							wl.substractFromRef(m_myRefSinceCharged.m_refKernelWakelocks);
													
							// we must recheck if the delta process is still above threshold
							if ( (!bFilter) || ((wl.getDuration()) > 0) )
							{
								myRetKernelWakelocks.add( wl);
							}
						}
						else
						{
							myRetKernelWakelocks.clear();
							myRetKernelWakelocks.add(new NativeKernelWakelock("No reference since charged yet", "", 1, 1, 1, 1, 1, 1, 1, 1, 1));
						}
						break;
					case STATS_SCREEN_OFF:
						if ( (m_myRefSinceScreenOff != null) && (m_myRefSinceScreenOff.m_refKernelWakelocks != null))
						{
							wl.substractFromRef(m_myRefSinceScreenOff.m_refKernelWakelocks);
													
							// we must recheck if the delta process is still above threshold
							if ( (!bFilter) || ((wl.getDuration()) > 0) )
							{
								myRetKernelWakelocks.add( wl);
							}
						}
						else
						{
							myRetKernelWakelocks.clear();
							myRetKernelWakelocks.add(new NativeKernelWakelock("No reference since screen off yet", "", 1, 1, 1, 1, 1, 1, 1, 1, 1));
						}
						break;
					case BatteryStatsTypes.STATS_CURRENT:
						// we must recheck if the delta process is still above threshold
						myRetKernelWakelocks.add( wl);
						break;
						
				}

			}
		}

		// sort @see com.asksven.android.common.privateapiproxies.Walkelock.compareTo
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

		
		
		for (int i=0; i < myRetKernelWakelocks.size(); i++)
		{
			myStats.add((StatElement) myRetKernelWakelocks.get(i));
		}
		
		if (Log.isLoggable(TAG, Log.DEBUG))
		{
			Log.d(TAG, "Result " + myStats.toString());
		}
		
		return myStats;
	}



	/**
	 * Get the Kernel Wakelock Stat to be displayed
	 * @param bFilter defines if zero-values should be filtered out
	 * @return a List of Wakelocks sorted by duration (descending)
	 * @throws Exception if the API call failed
	 */
	public ArrayList<StatElement> getNativeNetworkUsageStatList(boolean bFilter, int iStatType) throws Exception
	{
		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		ArrayList<NetworkUsage> myNetworkStats = Netstats.parseNetstats();

		ArrayList<NetworkUsage> myRetNetworkStats = new ArrayList<NetworkUsage>();
		// if we are using custom ref. always retrieve "stats current"


		// sort @see com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		Collections.sort(myNetworkStats);

		String strCurrent	= "";
		String strRef 		= "";
		String strRefDescr 	= "";

		if (Log.isLoggable(TAG, Log.DEBUG))
		{
			strCurrent = myNetworkStats.toString();

			switch (iStatType)
			{
				case STATS_UNPLUGGED:									
					if ( (m_myRefSinceUnplugged != null) && (m_myRefSinceUnplugged.m_refNetworkStats != null) )
					{
						strRef = m_myRefSinceUnplugged.m_refNetworkStats.toString();
						strRefDescr = m_myRefSinceUnplugged.whoAmI();
					}
					break;
				case STATS_CHARGED:
					if ( (m_myRefSinceCharged != null) && (m_myRefSinceCharged.m_refNetworkStats != null) )
					{
						strRef = m_myRefSinceCharged.m_refNetworkStats.toString();
						strRefDescr = m_myRefSinceCharged.whoAmI();
					}
					break;
				case STATS_SCREEN_OFF:
					if ( (m_myRefSinceScreenOff != null) && (m_myRefSinceScreenOff.m_refNetworkStats != null) )
					{
						strRef = m_myRefSinceScreenOff.m_refNetworkStats.toString();
						strRefDescr = m_myRefSinceScreenOff.whoAmI();
					}
					break;
				case STATS_CUSTOM:
					if ( (m_myRefs != null) && (m_myRefs.m_refNetworkStats != null))
					{
						strRef = m_myRefs.m_refNetworkStats.toString();
						strRefDescr = m_myRefs.whoAmI();
					}
					break;
				case BatteryStatsTypes.STATS_CURRENT:
					strRef = "no reference to substract";
					break;
				default:
					Log.e(TAG, "Unknown StatType " + iStatType + ". No reference found");
					break;
			}
			
			Log.d(TAG, "Processing network stats  since " + statTypeToLabel(iStatType));
			Log.d(TAG, "Reference used: " + strRefDescr);
			Log.d(TAG, "It is now " + DateUtils.now());
			Log.d(TAG, "Substracting " + strCurrent);
			Log.d(TAG, " from " + strRef);
		}
		
		for (int i = 0; i < myNetworkStats.size(); i++)
		{
			NetworkUsage netStat = myNetworkStats.get(i);
			if ( (!bFilter) || ((netStat.getTotalBytes()) > 0) )
			{	
				// network stats are parsed from /proc/net
				// and do not know any references "since charged" and "since unplugged"
				// those are implemented using special references
				
				switch (iStatType)
				{
					case STATS_CUSTOM:					
						// case a)
						// we need t return a delta containing
						//   if a process is in the new list but not in the custom ref
						//	   the full time is returned
						//   if a process is in the reference return the delta
						//	 a process can not have disapeared in btwn so we don't need
						//	 to test the reverse case
						if ( (m_myRefs != null) && (m_myRefs.m_refNetworkStats != null) )
						{
							netStat.substractFromRef(m_myRefs.m_refNetworkStats);
		
		
							// we must recheck if the delta process is still above threshold
							if ( (!bFilter) || ((netStat.getTotalBytes()) > 0) )
							{
								myRetNetworkStats.add( netStat);
							}
						}
						else
						{
							myRetNetworkStats.clear();
							myRetNetworkStats.add(new NetworkUsage(NO_CUST_REF, -1, 1, 0));
						}
						break;
					case STATS_UNPLUGGED:
						if ( (m_myRefSinceUnplugged != null) && (m_myRefSinceUnplugged.m_refNetworkStats != null) )
						{
							netStat.substractFromRef(m_myRefSinceUnplugged.m_refNetworkStats);
							
							
							// we must recheck if the delta process is still above threshold
							if ( (!bFilter) || ((netStat.getTotalBytes()) > 0) )
							{
								myRetNetworkStats.add( netStat);
							}
						}
						else
						{
							myRetNetworkStats.clear();
							myRetNetworkStats.add(new NetworkUsage("No reference since unplugged set yet", -1, 1, 0));

						}
						break;

					case STATS_CHARGED:
						if ( (m_myRefSinceCharged != null) && (m_myRefSinceCharged.m_refNetworkStats != null) )
						{
							netStat.substractFromRef(m_myRefSinceCharged.m_refNetworkStats);
													
							// we must recheck if the delta process is still above threshold
							if ( (!bFilter) || ((netStat.getTotalBytes()) > 0) )
							{
								myRetNetworkStats.add( netStat);
							}
						}
						else
						{
							myRetNetworkStats.clear();
							myRetNetworkStats.add(new NetworkUsage("No reference since charged yet", -1, 1, 0));
						}
						break;
					case STATS_SCREEN_OFF:
						if ( (m_myRefSinceScreenOff != null) && (m_myRefSinceScreenOff.m_refNetworkStats != null) )
						{
							netStat.substractFromRef(m_myRefSinceScreenOff.m_refNetworkStats);
													
							// we must recheck if the delta process is still above threshold
							if ( (!bFilter) || ((netStat.getTotalBytes()) > 0) )
							{
								myRetNetworkStats.add( netStat);
							}
						}
						else
						{
							myRetNetworkStats.clear();
							myRetNetworkStats.add(new NetworkUsage("No reference since screen off yet", -1, 1, 0));
						}
						break;

					case BatteryStatsTypes.STATS_CURRENT:
						// we must recheck if the delta process is still above threshold
						myRetNetworkStats.add( netStat);
						break;
						
				}

			}
		}

		// recalculate the total
		long total = 0;
		for (int i=0; i < myRetNetworkStats.size(); i++)
		{
			total += myRetNetworkStats.get(i).getTotalBytes();
		}
		
		Collections.sort(myRetNetworkStats);
		
		
		for (int i=0; i < myRetNetworkStats.size(); i++)
		{
			myRetNetworkStats.get(i).setTotal(total);
			myStats.add((StatElement) myRetNetworkStats.get(i));
		}

		if (Log.isLoggable(TAG, Log.DEBUG))
		{
			Log.d(TAG, "Result " + myStats.toString());
		}
		
		return myStats;
	}

	/**
	 * Get the CPU states to be displayed
	 * @param bFilter defines if zero-values should be filtered out
	 * @return a List of Other usages sorted by duration (descending)
	 * @throws Exception if the API call failed
	 */
	public ArrayList<StatElement> getCpuStateList(int iStatType) throws Exception

	{
		// List to store the other usages to
		ArrayList<State> myStates = CpuStates.getTimesInStates();

		ArrayList<StatElement> myStats = new ArrayList<StatElement> ();
		
		for (int i = 0; i < myStates.size(); i++)
		{
			State state = myStates.get(i); 
			if (iStatType == STATS_CUSTOM)
			{
				if ( (m_myRefs != null) && (m_myRefs.m_refCpuStates != null) )
				{
					state.substractFromRef(m_myRefs.m_refCpuStates);
					myStats.add(state);
				}	
				else
				{
					myStats.clear();
					myStats.add(new State(1, 1)); 
				}
	        }
	        else if (iStatType == STATS_CHARGED)
	        {
				if ( (m_myRefSinceCharged != null) && (m_myRefSinceCharged.m_refCpuStates != null) )
				{
					state.substractFromRef(m_myRefSinceCharged.m_refCpuStates);
					myStats.add(state);
				}	
				else
				{
					myStats.clear();
					myStats.add(new State(1, 1)); 
				}
	        }
	        else if (iStatType == STATS_SCREEN_OFF)
	        {
				if ( (m_myRefSinceScreenOff != null) && (m_myRefSinceScreenOff.m_refCpuStates != null) )
				{
					state.substractFromRef(m_myRefSinceScreenOff.m_refCpuStates);
					myStats.add(state);
				}
				else
				{
					myStats.clear();
					myStats.add(new State(1, 1)); 
				}
	
	        }
	        else if (iStatType == STATS_UNPLUGGED)
	        {
				if ( (m_myRefSinceUnplugged != null) && (m_myRefSinceUnplugged.m_refCpuStates != null) )
				{
					state.substractFromRef(m_myRefSinceUnplugged.m_refCpuStates);
					myStats.add(state);
				}
				else
				{
					myStats.clear();
					myStats.add(new State(1, 1)); 
				}
	
	        }
	        else
	        {
	        	myStats.add(state);
	        }
		}
    	return myStats;

	}

	/**
	 * Get the Other Usage Stat to be displayed
	 * @param bFilter defines if zero-values should be filtered out
	 * @return a List of Other usages sorted by duration (descending)
	 * @throws Exception if the API call failed
	 */
	public ArrayList<StatElement> getOtherUsageStatList(boolean bFilter, int iStatType, boolean bFilterView) throws Exception
	{
		BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(m_context);

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(m_context);
		
		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		
		// List to store the other usages to
		ArrayList<Misc> myUsages = new ArrayList<Misc>();

		long rawRealtime = SystemClock.elapsedRealtime() * 1000;
        long batteryRealtime = mStats.getBatteryRealtime(rawRealtime);

        long whichRealtime 			= 0;
        long timeBatteryUp 			= 0;
        long timeScreenOn			= 0;
        long timePhoneOn			= 0;
        long timeWifiOn				= 0;
        long timeWifiRunning		= 0;
        long timeWifiMulticast		= 0;
        long timeWifiLocked			= 0;
        long timeWifiScan			= 0;
        long timeAudioOn			= 0;
        long timeVideoOn			= 0;
        long timeBluetoothOn		= 0;
        long timeDeepSleep			= 0;
        long timeNoDataConnection	= 0;
        long timeSignalNone			= 0;
        long timeSignalPoor			= 0;
        long timeSignalModerate		= 0;
        long timeSignalGood			= 0;
        long timeSignalGreat		= 0;
        
		// if we are using custom ref. always retrieve "stats current"
		if ( (iStatType == STATS_CUSTOM) || (iStatType == STATS_SCREEN_OFF)) 
		{
	        whichRealtime 		= mStats.computeBatteryRealtime(rawRealtime, BatteryStatsTypes.STATS_CURRENT)  / 1000;      
	        timeBatteryUp 		= mStats.computeBatteryUptime(SystemClock.uptimeMillis() * 1000, BatteryStatsTypes.STATS_CURRENT) / 1000;
	        timeScreenOn 		= mStats.getScreenOnTime(batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
	        timePhoneOn 		= mStats.getPhoneOnTime(batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
	        timeWifiOn 			= mStats.getWifiOnTime(batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
	        timeWifiRunning		= mStats.getGlobalWifiRunningTime(batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
	        timeWifiMulticast	= mStats.getWifiMulticastTime(m_context, batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
	        timeWifiLocked		= mStats.getFullWifiLockTime(m_context, batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
	        timeWifiScan		= mStats.getScanWifiLockTime(m_context, batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
	        timeAudioOn			= mStats.getAudioTurnedOnTime(m_context, batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
	        timeVideoOn			= mStats.getVideoTurnedOnTime(m_context, batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
	        timeBluetoothOn 	= mStats.getBluetoothOnTime(batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
	        timeNoDataConnection= mStats.getPhoneDataConnectionTime(BatteryStatsTypes.DATA_CONNECTION_NONE, batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
	        timeSignalNone		= mStats.getPhoneSignalStrengthTime(BatteryStatsTypes.SIGNAL_STRENGTH_NONE_OR_UNKNOWN, batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
	        timeSignalPoor		= mStats.getPhoneSignalStrengthTime(BatteryStatsTypes.SIGNAL_STRENGTH_POOR, batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
	        timeSignalModerate	= mStats.getPhoneSignalStrengthTime(BatteryStatsTypes.SIGNAL_STRENGTH_MODERATE, batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
	        timeSignalGood		= mStats.getPhoneSignalStrengthTime(BatteryStatsTypes.SIGNAL_STRENGTH_GOOD, batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
	        timeSignalGreat		= mStats.getPhoneSignalStrengthTime(BatteryStatsTypes.SIGNAL_STRENGTH_GREAT, batteryRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;	        	        
		}
		else
		{
	        whichRealtime 		= mStats.computeBatteryRealtime(rawRealtime, iStatType)  / 1000;      
	        timeBatteryUp 		= mStats.computeBatteryUptime(SystemClock.uptimeMillis() * 1000, iStatType) / 1000;		
	        timeScreenOn 		= mStats.getScreenOnTime(batteryRealtime, iStatType) / 1000;
	        timePhoneOn 		= mStats.getPhoneOnTime(batteryRealtime, iStatType) / 1000;
	        timeWifiOn 			= mStats.getWifiOnTime(batteryRealtime, iStatType) / 1000;
	        timeWifiRunning 	= mStats.getGlobalWifiRunningTime(batteryRealtime, iStatType) / 1000;
	        timeWifiMulticast	= mStats.getWifiMulticastTime(m_context, batteryRealtime, iStatType) / 1000;
	        timeWifiLocked		= mStats.getFullWifiLockTime(m_context, batteryRealtime, iStatType) / 1000;
	        timeWifiScan		= mStats.getScanWifiLockTime(m_context, batteryRealtime, iStatType) / 1000;
	        timeAudioOn			= mStats.getAudioTurnedOnTime(m_context, batteryRealtime, iStatType) / 1000;
	        timeVideoOn			= mStats.getVideoTurnedOnTime(m_context, batteryRealtime, iStatType) / 1000;
	        timeBluetoothOn 	= mStats.getBluetoothOnTime(batteryRealtime, iStatType) / 1000;
	        timeNoDataConnection= mStats.getPhoneDataConnectionTime(BatteryStatsTypes.DATA_CONNECTION_NONE, batteryRealtime, iStatType) / 1000;
	        timeSignalNone		= mStats.getPhoneSignalStrengthTime(BatteryStatsTypes.SIGNAL_STRENGTH_NONE_OR_UNKNOWN, batteryRealtime, iStatType) / 1000;
	        timeSignalPoor		= mStats.getPhoneSignalStrengthTime(BatteryStatsTypes.SIGNAL_STRENGTH_POOR, batteryRealtime, iStatType) / 1000;
	        timeSignalModerate	= mStats.getPhoneSignalStrengthTime(BatteryStatsTypes.SIGNAL_STRENGTH_MODERATE, batteryRealtime, iStatType) / 1000;
	        timeSignalGood		= mStats.getPhoneSignalStrengthTime(BatteryStatsTypes.SIGNAL_STRENGTH_GOOD, batteryRealtime, iStatType) / 1000;
	        timeSignalGreat		= mStats.getPhoneSignalStrengthTime(BatteryStatsTypes.SIGNAL_STRENGTH_GREAT, batteryRealtime, iStatType) / 1000;	        
		}
		
		// deep sleep times are independent of stat type
        timeDeepSleep		= (SystemClock.elapsedRealtime() - SystemClock.uptimeMillis());
        long timeElapsed    = mStats.computeBatteryRealtime(rawRealtime, BatteryStatsTypes.STATS_CURRENT)  / 1000;      
        // SystemClock.elapsedRealtime();
        
        Misc deepSleepUsage = new Misc("Deep Sleep", timeDeepSleep, timeElapsed);
        Log.d(TAG, "Added Deep sleep:" + deepSleepUsage.getData());

        // special processing for deep sleep: we must calculate times for stat types != CUSTOM
        if (iStatType == STATS_CHARGED)
        {
			if (m_myRefSinceCharged != null)
			{
				deepSleepUsage.substractFromRef(m_myRefSinceCharged.m_refOther);
				if ( (!bFilter) || (deepSleepUsage.getTimeOn() > 0) )
				{
					myUsages.add(deepSleepUsage);
				}
			}	
        }
//        if (iStatType == STATS_SCREEN_OFF)
//        {
//			if (m_myRefSinceScreenOff != null)
//			{
//				deepSleepUsage.substractFromRef(m_myRefSinceScreenOff.m_refOther);
//				if ( (!bFilter) || (deepSleepUsage.getTimeOn() > 0) )
//				{
//					myUsages.add(deepSleepUsage);
//				}
//			}	
//        }
        else if (iStatType == STATS_UNPLUGGED)
        {
			if (m_myRefSinceUnplugged != null)
			{
				deepSleepUsage.substractFromRef(m_myRefSinceUnplugged.m_refOther);
				if ( (!bFilter) || (deepSleepUsage.getTimeOn() > 0) )
				{
					myUsages.add(deepSleepUsage);
				}
			}
        }
        else
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
        
        if ( (timeWifiOn > 0) && (!bFilterView || sharedPrefs.getBoolean("show_other_wifi", true)) )
        {
        	myUsages.add(new Misc("Wifi On", timeWifiOn, whichRealtime));
        }
        
        if ( (timeWifiRunning > 0) && (!bFilterView || sharedPrefs.getBoolean("show_other_wifi", true)) )

        {
        	myUsages.add(new Misc("Wifi Running", timeWifiRunning, whichRealtime));
        }
        
        if ( (timeBluetoothOn > 0) && (!bFilterView || sharedPrefs.getBoolean("show_other_bt", true)) )

        {
        	myUsages.add(new Misc("Bluetooth On", timeBluetoothOn, whichRealtime)); 
        }
        
        if ( (timeNoDataConnection > 0) && (!bFilterView || sharedPrefs.getBoolean("show_other_connection", true)) )

        {
        	myUsages.add(new Misc("No Data Connection", timeNoDataConnection, whichRealtime));
        }

        if ( (timeSignalNone > 0) && (!bFilterView || sharedPrefs.getBoolean("show_other_signal", true)) )

        {
        	myUsages.add(new Misc("No or Unknown Signal", timeSignalNone, whichRealtime));
        }

        if ( (timeSignalPoor > 0) && (!bFilterView || sharedPrefs.getBoolean("show_other_signal", true)) )
        {
        	myUsages.add(new Misc("Poor Signal", timeSignalPoor, whichRealtime));
        }

        if ( (timeSignalModerate > 0) && (!bFilterView || sharedPrefs.getBoolean("show_other_signal", true)) )
        {
        	myUsages.add(new Misc("Moderate Signal", timeSignalModerate, whichRealtime));
        }

        if ( (timeSignalGood > 0) &&  (!bFilterView || sharedPrefs.getBoolean("show_other_signal", true)) )
        {
        	myUsages.add(new Misc("Good Signal", timeSignalGood, whichRealtime));
        }

        if ( (timeSignalGreat > 0) && (!bFilterView || sharedPrefs.getBoolean("show_other_signal", true)) )
        {
        	myUsages.add(new Misc("Great Signal", timeSignalGreat, whichRealtime));
        }
        

//        if ( (timeWifiMulticast > 0) && (!bFilterView || sharedPrefs.getBoolean("show_other_wifi", true)) )
//        {
//        	myUsages.add(new Misc("Wifi Multicast On", timeWifiMulticast, whichRealtime)); 
//        }
//
//        if ( (timeWifiLocked > 0) && (!bFilterView ||(!bFilterView || sharedPrefs.getBoolean("show_other_wifi", true)) )
//        {
//        	myUsages.add(new Misc("Wifi Locked", timeWifiLocked, whichRealtime)); 
//        }
//
//        if ( (timeWifiScan > 0) && (!bFilterView || sharedPrefs.getBoolean("show_other_wifi", true)) )
//        {
//        	myUsages.add(new Misc("Wifi Scan", timeWifiScan, whichRealtime)); 
//        }
//
//        if (timeAudioOn > 0)
//        {
//        	myUsages.add(new Misc("Video On", timeAudioOn, whichRealtime)); 
//        }
//
//        if (timeVideoOn > 0)
//        {
//        	myUsages.add(new Misc("Video On", timeVideoOn, whichRealtime)); 
//        }

        // sort @see com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		Collections.sort(myUsages);
		if (iStatType == STATS_SCREEN_OFF)
		{
			Log.d(TAG, "Processing other stats  since " + statTypeToLabel(iStatType));
			String strRefDescr = m_myRefSinceScreenOff.whoAmI();
			Log.d(TAG, "Reference used: " + strRefDescr);
			Log.d(TAG, "It is now " + DateUtils.now());
		}

		Log.d(TAG, "Processing " + myUsages.size() + " elements");
		for (int i = 0; i < myUsages.size(); i++)
		{
			Misc usage = myUsages.get(i); 
			Log.d(TAG, "Current value: " + usage.getName() + " " + usage.getData());
			if ( (!bFilter) || (usage.getTimeOn() > 0) )
			{
				if (iStatType == STATS_CUSTOM)
				{
					// case a)
					// we need t return a delta containing
					//   if a process is in the new list but not in the custom ref
					//	   the full time is returned
					//   if a process is in the reference return the delta
					//	 a process can not have disapeared in btwn so we don't need
					//	 to test the reverse case
					if ( (m_myRefs != null) && (m_myRefs.m_refOther != null) )
					{
						usage.substractFromRef(m_myRefs.m_refOther);
						if ( (!bFilter) || (usage.getTimeOn() > 0) )
						{
							Log.d(TAG, "Result value: " + usage.getName() + " " + usage.getData());
							myStats.add((StatElement) usage);
						}
					}
					else
					{
						myStats.clear();
						myStats.add(new Misc(NO_CUST_REF, 1, 1)); 
					}
				}
				else if (iStatType == STATS_SCREEN_OFF)
				{
					// case a)
					// we need t return a delta containing
					//   if a process is in the new list but not in the custom ref
					//	   the full time is returned
					//   if a process is in the reference return the delta
					//	 a process can not have disapeared in btwn so we don't need
					//	 to test the reverse case
					if ( (m_myRefSinceScreenOff != null) && (m_myRefSinceScreenOff.m_refOther != null) )
					{
						usage.substractFromRef(m_myRefSinceScreenOff.m_refOther);
						if ( (!bFilter) || (usage.getTimeOn() > 0) )
						{
							Log.d(TAG, "Result value: " + usage.getName() + " " +usage.getData());
							myStats.add((StatElement) usage);
						}
					}
					else
					{
						myStats.clear();
						myStats.add(new Misc(NO_CUST_REF, 1, 1)); 
					}
				}

				else
				{
					// case b)
					// nothing special
					myStats.add((StatElement) usage);
				}
				
			}
		}
		return myStats;
	}

	/**
	 * Get the battery level lost since a given ref
	 * @param iStatType the reference
	 * @return the lost battery level
	 */
	public int getBatteryLevelStat(int iStatType)
	{
		// deep sleep times are independent of stat type
        int level		= getBatteryLevel();
        
        Log.d(TAG, "Current Battery Level:" + level);

        if (iStatType == STATS_CHARGED)
        {
			if (m_myRefSinceCharged != null)
			{
				level = m_myRefSinceCharged.m_refBatteryLevel - level;
			}	
        }
        else if (iStatType == STATS_UNPLUGGED)
        {
			if (m_myRefSinceUnplugged != null)
			{
				level = m_myRefSinceUnplugged.m_refBatteryLevel - level;
			}
        }
        else if (iStatType == STATS_SCREEN_OFF)
        {
			if (m_myRefSinceScreenOff != null)
			{
				level = m_myRefSinceScreenOff.m_refBatteryLevel - level;
			}
        }
        else if (iStatType == STATS_CUSTOM)
		{
			if (m_myRefs != null)
			{
				level = m_myRefs.m_refBatteryLevel - level;
			}
		}
        Log.d(TAG, "Battery Level since " + iStatType + ":" + level);

		return level;
	}
	
	/**
	 * Get the battery voltage lost since a given ref
	 * @param iStatType the reference
	 * @return the lost battery level
	 */
	public int getBatteryVoltageStat(int iStatType)
	{
		// deep sleep times are independent of stat type
        int voltage		= getBatteryVoltage();
        
        Log.d(TAG, "Current Battery Voltage:" + voltage);

        if (iStatType == STATS_CHARGED)
        {
			if (m_myRefSinceCharged != null)
			{
				voltage = m_myRefSinceCharged.m_refBatteryVoltage - voltage;
			}	
        }
        else if (iStatType == STATS_UNPLUGGED)
        {
			if (m_myRefSinceUnplugged != null)
			{
				voltage = m_myRefSinceUnplugged.m_refBatteryVoltage - voltage;
			}
        }
        else if (iStatType == STATS_SCREEN_OFF)
        {
			if (m_myRefSinceScreenOff != null)
			{
				voltage = m_myRefSinceScreenOff.m_refBatteryVoltage - voltage;
			}
        }
        else if (iStatType == STATS_CUSTOM)
		{
			if (m_myRefs != null)
			{
				voltage = m_myRefs.m_refBatteryVoltage - voltage;
			}
		}
        Log.d(TAG, "Battery Voltage since " + iStatType + ":" + voltage);

		return voltage;
	}

	public StatElement getElementByKey(ArrayList<StatElement> myList, String key)
	{
		StatElement ret = null;
		
		if (myList == null)
		{
			Log.e(TAG, "getElementByKey failed: null list");
			return null;
		}
		
		for (int i=0; i < myList.size(); i++)
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
	
	public long sum (ArrayList<StatElement> myList)
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
		
		for (int i=0; i < myList.size(); i++)
		{
			// make sure nothing goes wrong
			try
			{
				StatElement item = myList.get(i); 
				ret += item.getValues()[0];
			}
			catch (Exception e)
			{
				Log.e(TAG, "An error occcured " + e.getMessage());
				GenericLogger.stackTrace(TAG, e.getStackTrace());
			}
		}
		return ret;
		
	}

	/**
	 * Returns true if a ref "since screen off" was stored
	 * @return true is a custom ref exists
	 */
	public boolean hasScreenOffRef()
	{
		return ( (m_myRefSinceScreenOff != null) && (m_myRefSinceScreenOff.m_refOther != null) );
	}

	/**
	 * Returns true if a custom ref was stored
	 * @return true is a custom ref exists
	 */
	public boolean hasCustomRef()
	{
		return ( (m_myRefs != null) && (m_myRefs.m_refOther != null) );
	}
	
	/**
	 * Returns true if a since charged ref was stored
	 * @return true is a since charged ref exists
	 */
	public boolean hasSinceChargedRef()
	{
		return ( (m_myRefSinceCharged != null) && (m_myRefSinceCharged.m_refKernelWakelocks != null) );
	}

	/**
	 * Returns true if a since unplugged ref was stored
	 * @return true is a since unplugged ref exists
	 */
	public boolean hasSinceUnpluggedRef()
	{
		return ( (m_myRefSinceUnplugged != null) && (m_myRefSinceUnplugged.m_refKernelWakelocks != null) );
	}

	/**
	 * Saves all data to a point in time defined by user
	 * This data will be used in a custom "since..." stat type
	 */
	public void setCustomReference(int iSort)
	{
		m_myRefs = new References(References.CUSTOM_REF_FILENAME);
		m_myRefs = setReference(iSort, m_myRefs);
	}

	/**
	 * Saves all data to a point in time when the screen goes off
	 * This data will be used in a custom "since..." stat type
	 */
	public void setReferenceSinceScreenOff(int iSort)
	{
		m_myRefSinceScreenOff = new References(References.SINCE_SCREEN_OFF_REF_FILENAME);
		m_myRefSinceScreenOff = setReference(iSort, m_myRefSinceScreenOff);
	}

	/**
	 * Saves data when battery is fully charged
	 * This data will be used in the "since charged" stat type
	 */
	public void setReferenceSinceCharged(int iSort)
	{
		m_myRefSinceCharged = new References(References.SINCE_CHARGED_REF_FILENAME);
		m_myRefSinceCharged = setReference(iSort, m_myRefSinceCharged);
	}

	/**
	 * Saves data when the phone is unplugged
	 * This data will be used in the "since unplugged" stat type
	 */
	public void setReferenceSinceUnplugged(int iSort)
	{
		m_myRefSinceUnplugged = new References(References.SINCE_UNPLUGGED_REF_FILENAME);
		m_myRefSinceUnplugged = setReference(iSort, m_myRefSinceUnplugged);
	}

	/**
	 * Saves data when the phone is unpluggediSort
	 * This data will be used in the "since unplugged" stat type
	 */
	public References setReference(int iSort, References refs)
	{
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(m_context);
		
		boolean bFilterStats = sharedPrefs.getBoolean("filter_data", true);
		int iPctType = Integer.valueOf(sharedPrefs.getString("default_wl_ref", "0"));
		
		try
    	{	
			refs.m_refOther 			= null;
			refs.m_refWakelocks 		= null;
			refs.m_refKernelWakelocks 	= null;
			refs.m_refNetworkStats		= null;

			refs.m_refAlarms			= null;
			refs.m_refProcesses 		= null;
			refs.m_refCpuStates			= null;
    	
			refs.m_refKernelWakelocks 	= getNativeKernelWakelockStatList(
					bFilterStats, BatteryStatsTypes.STATS_CURRENT, iPctType, iSort);
			refs.m_refWakelocks 	= getWakelockStatList(
					bFilterStats, BatteryStatsTypes.STATS_CURRENT, iPctType, iSort);

			refs.m_refNetworkStats 	= getNativeNetworkUsageStatList(bFilterStats, BatteryStatsTypes.STATS_CURRENT);

			refs.m_refAlarms = getAlarmsStatList(
					bFilterStats, BatteryStatsTypes.STATS_CURRENT);
			refs.m_refOther		 	= getOtherUsageStatList(
					bFilterStats, BatteryStatsTypes.STATS_CURRENT, false);
			refs.m_refCpuStates		 	= getCpuStateList(BatteryStatsTypes.STATS_CURRENT);
					
			refs.m_refBatteryRealtime 	= getBatteryRealtime(BatteryStatsTypes.STATS_CURRENT);
			
			refs.m_refBatteryLevel		= getBatteryLevel();
			refs.m_refBatteryVoltage	= getBatteryVoltage();


			
			serializeRefToFile(refs);
    	}
    	catch (Exception e)
    	{
    		Log.e(TAG, "Exception: " + e.getMessage());
    		refs.m_refOther 			= null;
    		refs.m_refWakelocks 		= null;
    		refs.m_refKernelWakelocks 	= null;
    		refs.m_refNetworkStats		= null;
    		refs.m_refAlarms			= null;
    		refs.m_refProcesses 		= null;
    		refs.m_refCpuStates			= null;
			
    		refs.m_refBatteryRealtime 	= 0;
    		refs.m_refBatteryLevel		= 0;
    		refs.m_refBatteryVoltage	= 0;

    	}
		
		return refs;
	}


	public void serializeRefToFile(References refs)
	{
		DataStorage.objectToFile(m_context, refs.m_fileName, refs);
		Log.i(TAG, "Saved ref " + refs.m_fileName);
	}

	public void deserializeFromFile()
	{
		m_myRefs = (References) DataStorage.fileToObject(m_context, References.CUSTOM_REF_FILENAME);
		if (m_myRefs != null)
		{
			Log.i(TAG, "Retrieved ref " + m_myRefs.m_fileName + " created at " + m_myRefs.m_creationDate);
		}
		
		m_myRefSinceCharged = (References) DataStorage.fileToObject(m_context, References.SINCE_CHARGED_REF_FILENAME);
		if (m_myRefSinceCharged != null)
		{
			Log.i(TAG, "Retrieved ref " + m_myRefSinceCharged.m_fileName + " created at " + m_myRefSinceCharged.m_creationDate);
		}
		
		m_myRefSinceScreenOff = (References) DataStorage.fileToObject(m_context, References.SINCE_SCREEN_OFF_REF_FILENAME);
		if (m_myRefSinceScreenOff != null)
		{
			Log.i(TAG, "Retrieved ref " + m_myRefSinceScreenOff.m_fileName + " created at " + m_myRefSinceScreenOff.m_creationDate);
		}
		
		m_myRefSinceUnplugged = (References) DataStorage.fileToObject(m_context, References.SINCE_UNPLUGGED_REF_FILENAME);
		if (m_myRefSinceUnplugged != null)
		{
			Log.i(TAG, "Retrieved ref " + m_myRefSinceUnplugged.m_fileName + " created at " + m_myRefSinceUnplugged.m_creationDate);
		}
	}

	public void deletedSerializedRefs()
	{
		References myEmptyRef = new References(References.CUSTOM_REF_FILENAME);
		DataStorage.objectToFile(m_context, References.CUSTOM_REF_FILENAME, myEmptyRef);
		
		myEmptyRef = new References(References.SINCE_CHARGED_REF_FILENAME);
		DataStorage.objectToFile(m_context, References.SINCE_CHARGED_REF_FILENAME, myEmptyRef);
		
		myEmptyRef = new References(References.SINCE_SCREEN_OFF_REF_FILENAME);
		DataStorage.objectToFile(m_context, References.SINCE_SCREEN_OFF_REF_FILENAME, myEmptyRef);
		
		myEmptyRef = new References(References.SINCE_UNPLUGGED_REF_FILENAME);
		DataStorage.objectToFile(m_context, References.SINCE_UNPLUGGED_REF_FILENAME, myEmptyRef);
	}

	/**
	 * Returns the battery realtime since a given reference
	 * @param iStatType the reference
	 * @return the battery realtime
	 */
	public  long getBatteryRealtime(int iStatType)
	{
        BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(m_context);
        
        if (mStats == null)
        {
        	// an error has occured
        	return -1;
        }
        
        long whichRealtime = 0;
		long rawRealtime = SystemClock.elapsedRealtime() * 1000;
		if ( (iStatType == StatsProvider.STATS_CUSTOM) && (m_myRefs != null) )
		{
			whichRealtime 	= mStats.computeBatteryRealtime(rawRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
			whichRealtime -= m_myRefs.m_refBatteryRealtime;	
		}
		else if ( (iStatType == StatsProvider.STATS_SCREEN_OFF) && (m_myRefSinceScreenOff != null) )
		{
			whichRealtime 	= mStats.computeBatteryRealtime(rawRealtime, BatteryStatsTypes.STATS_CURRENT) / 1000;
			whichRealtime -= m_myRefSinceScreenOff.m_refBatteryRealtime;	
		}

		else
		{
			whichRealtime 	= mStats.computeBatteryRealtime(rawRealtime, iStatType) / 1000;
		}
		return whichRealtime;
	}


	/** 
	 * Dumps relevant data to an output file
	 * 
	 */

	@SuppressLint("NewApi")
	public void writeDumpToFile(int iStatType, int iSort)
	{
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(m_context);
		boolean bFilterStats = sharedPrefs.getBoolean("filter_data", true);
		int iPctType = Integer.valueOf(sharedPrefs.getString("default_wl_ref", "0"));
		
		if (!DataStorage.isExternalStorageWritable())
		{
			Log.e(TAG, "External storage can not be written");
    		Toast.makeText(m_context, "External Storage can not be written", Toast.LENGTH_SHORT).show();
		}
		try
    	{		
			// open file for writing
			File root = Environment.getExternalStorageDirectory();
			
			// check if file can be written
		    if (root.canWrite())
		    {
		    	String strFilename = "BetterBatteryStats-" + DateUtils.now("yyyy-MM-dd_HHmmssSSS") + ".txt";
		    	File dumpFile = new File(root, strFilename);
		        FileWriter fw = new FileWriter(dumpFile);
		        BufferedWriter out = new BufferedWriter(fw);
			  
				// write header
		        out.write("===================\n");
				out.write("General Information\n");
				out.write("===================\n");
				PackageInfo pinfo = m_context.getPackageManager().getPackageInfo(m_context.getPackageName(), 0);
				out.write("BetterBatteryStats version: " + pinfo.versionName + "\n");
				out.write("Creation Date: " + DateUtils.now() + "\n");
				out.write("Statistic Type: (" + iStatType + ") " + statTypeToLabel(iStatType) + "\n");
				out.write("Since " + DateUtils.formatDuration(getBatteryRealtime(iStatType)) + "\n");
				out.write("VERSION.RELEASE: " + Build.VERSION.RELEASE+"\n");
				out.write("BRAND: "+Build.BRAND+"\n");
				out.write("DEVICE: "+Build.DEVICE+"\n");
				out.write("MANUFACTURER: "+Build.MANUFACTURER+"\n");
				out.write("MODEL: "+Build.MODEL+"\n");
				out.write("OS.VERSION: "+System.getProperty("os.version") + "\n");
				

				if (Build.VERSION.SDK_INT >= 8)
				{
					
					out.write("BOOTLOADER: "+Build.BOOTLOADER+"\n");
					out.write("HARDWARE: "+Build.HARDWARE+"\n");
				}
				out.write("FINGERPRINT: "+Build.FINGERPRINT+"\n");
				out.write("ID: "+Build.ID+"\n");
				out.write("TAGS: "+Build.TAGS+"\n");
				out.write("USER: "+Build.USER+"\n");
				out.write("PRODUCT: "+Build.PRODUCT+"\n");
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
				
				out.write("RADIO: "+ radio + "\n");
				out.write("Rooted: "+ RootDetection.hasSuRights("dumpsys alarm") + "\n");
				
				out.write("============\n");
				out.write("Battery Info\n");
				out.write("============\n");
				out.write("Level lost [%]: " + getBatteryLevelStat(iStatType) + "\n");
				out.write("Voltage lost [mV]: " + getBatteryVoltageStat(iStatType) + "\n");
				
				
				// write timing info
				boolean bDumpChapter = sharedPrefs.getBoolean("show_other", true);
				if (bDumpChapter)
				{
					out.write("===========\n");
					out.write("Other Usage\n");
					out.write("===========\n");
					dumpList(getOtherUsageStatList(bFilterStats, iStatType, false), out);
				}

				bDumpChapter = sharedPrefs.getBoolean("show_pwl", true);
				if (bDumpChapter)
				{
					// write wakelock info
					out.write("=========\n");
					out.write("Wakelocks\n");
					out.write("=========\n");
					dumpList(getWakelockStatList(bFilterStats, iStatType, iPctType, iSort), out);
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
					}
					else
					{
						// write kernel wakelock info
						out.write("================\n");
						out.write("Kernel Wakelocks\n");
						out.write("================\n");
					}
					dumpList(getNativeKernelWakelockStatList(bFilterStats, iStatType, iPctType, iSort), out);
				}
				
				bDumpChapter = sharedPrefs.getBoolean("show_proc", false);
				if (bDumpChapter)
				{
					// write process info
					out.write("=========\n");
					out.write("Processes\n");
					out.write("=========\n");
					dumpList(getProcessStatList(bFilterStats, iStatType, iSort), out);
				}
				
				bDumpChapter = sharedPrefs.getBoolean("show_alarm", true);
				if (bDumpChapter)
				{
					// write alarms info
					out.write("======================\n");
					out.write("Alarms (requires root)\n");
					out.write("======================\n");
					dumpList(getAlarmsStatList(bFilterStats, iStatType), out);
				}

				bDumpChapter = sharedPrefs.getBoolean("show_network", true);
				if (bDumpChapter)
				{
					// write alarms info
					out.write("======================\n");
					out.write("Network (requires root)\n");
					out.write("======================\n");
					dumpList(getNativeNetworkUsageStatList(bFilterStats, iStatType), out);
				}

				bDumpChapter = sharedPrefs.getBoolean("show_cpustates", true);
				if (bDumpChapter)
				{
					// write alarms info
					out.write("==========\n");
					out.write("CPU States\n");
					out.write("==========\n");
					dumpList(getCpuStateList(iStatType), out);
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
					ActivityManager am = (ActivityManager)m_context.getSystemService(m_context.ACTIVITY_SERVICE);
					List<ActivityManager.RunningServiceInfo> rs = am.getRunningServices(50);
					         
					for (int i=0; i < rs.size(); i++) {
					  ActivityManager.RunningServiceInfo  rsi = rs.get(i);
					  out.write(rsi.process + " (" + rsi.service.getClassName() + ")\n");
					  out.write("  Active since: " + DateUtils.formatDuration(rsi.activeSince) + "\n");
					  out.write("  Last activity: " + DateUtils.formatDuration(rsi.lastActivityTime) + "\n");
					  out.write("  Crash count:" + rsi.crashCount + "\n");
					}
				}			
				
				// add chapter for reference info
				out.write("==================\n");
				out.write("Reference overview\n");
				out.write("==================\n");
				if (m_myRefs != null)
				{
					out.write("Custom: " + m_myRefs.whoAmI() + "\n");
				}
				else
				{
					out.write("Custom: " + "null" + "\n");
				}
				
				if (m_myRefSinceCharged != null)
				{
					out.write("Since charged: " + m_myRefSinceCharged.whoAmI() + "\n");
				}
				else
				{
					out.write("Since charged: " + "null" + "\n");
				}
				
				if (m_myRefSinceScreenOff != null)
				{
					out.write("Since screen off: " + m_myRefSinceScreenOff.whoAmI() + "\n");
				}
				else
				{
					out.write("Since screen off: " + "null" + "\n");
				}
				
				if (m_myRefSinceUnplugged != null)
				{
					out.write("Since unplugged: " + m_myRefSinceUnplugged.whoAmI() + "\n");
				}
				else
				{
					out.write("Since unplugged: " + "null" + "\n");
				}

				// close file
				out.close();
//				Toast.makeText(m_context, "Dump witten: " + strFilename, Toast.LENGTH_SHORT).show();
				
		    }
		    else
		    {
	    		Log.i(TAG, "Write error. " + Environment.getExternalStorageDirectory() + " couldn't be written");
		    }
    	}
    	catch (Exception e)
    	{
    		Log.e(TAG, "Exception: " + e.getMessage());
    	}		
	}
	
	/**
	 * Dump the elements on one list
	 * @param myList a list of StatElement
	 */
	private void dumpList(List<StatElement> myList, BufferedWriter out) throws IOException
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
	 * @param position the spinner position
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
	
		}
		return strRet;
	}
	
	/**
	 * translate the stat type (see arrays.xml) to the corresponding short label
	 * @param position the spinner position
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
	
		}
		return strRet;
	}
	

	/**
	 * translate the stat type (see arrays.xml) to the corresponding label
	 * @param position the spinner position
	 * @return the stat type
	 */
	public String statTypeToUrl(int statType)
	{
		String strRet = statTypeToLabel(statType);
		
		// remove spaces
		  StringTokenizer st = new StringTokenizer(strRet," ",false);
		  
		  String strCleaned = "";
		  while (st.hasMoreElements())
		  {
			  strCleaned += st.nextElement();
		  }
		  return strCleaned;
	}

	/**
	 * translate the stat (see arrays.xml) to the corresponding label
	 * @param position the spinner position
	 * @return the stat
	 */
	private String statToLabel(int iStat)
	{
		String strRet = "";
		String[] statsArray = m_context.getResources().getStringArray(R.array.stats); 
		strRet = statsArray[iStat];
		
		return strRet;
	}
	
	/**
	 * translate the stat (see arrays.xml) to the corresponding label
	 * @param position the spinner position
	 * @return the stat
	 */
	public String statToUrl(int stat)
	{
		String strRet = statToLabel(stat);
		
		// remove spaces
		  StringTokenizer st = new StringTokenizer(strRet," ",false);
		  
		  String strCleaned = "";
		  while (st.hasMoreElements())
		  {
			  strCleaned += st.nextElement();
		  }
		  return strCleaned;
	}
	
	/**
	 * translate the spinner position (see arrays.xml) to the stat type
	 * @param position the spinner position
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
				
		}
		return iRet;
	}
	
	/**
	 * translate the stat type to the spinner position (see arrays.xml)
	 * @param iStatType the stat type
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
				
		}
		return iRet;
	}
	
	/** 
	 * Returns the current battery level as an int [0..100] or -1 if invalid
	 */
	int getBatteryLevel()
	{
		// check the battery level and if 100% the store "since charged" ref
		Intent batteryIntent = m_context.getApplicationContext().registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

		int rawlevel = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		double scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
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
		Intent batteryIntent = m_context.getApplicationContext().registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

		int voltage = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
		return voltage;
	}

}
