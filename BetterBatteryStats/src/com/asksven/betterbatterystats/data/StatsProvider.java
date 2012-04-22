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

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.asksven.android.common.kernelutils.Alarm;
import com.asksven.android.common.kernelutils.AlarmsDumpsys;
import com.asksven.android.common.kernelutils.NativeKernelWakelock;
import com.asksven.android.common.kernelutils.RootDetection;
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
	public final static int STATS_CHARGED = 0;
	public final static int STATS_UNPLUGGED = 3;
	public final static int STATS_CUSTOM 	= 4;
	
	/** the logger tag */
	static String TAG = "StatsProvider";
	
	/** the text when no custom reference is set */
	static String NO_CUST_REF = "No custom reference was set";
	
	/** the storage for references */
	static References m_myRefs 					= null;
	static References m_myRefSinceUnplugged 	= null;
	static References m_myRefSinceCharged 		= null;



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
			m_myRefs = new References();
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

			switch (iStat)
			{
				// constants are related to arrays.xml string-array name="stats"
				case 4:
					return getProcessStatList(bFilterStats, iStatType, iSort);
				case 1:
					return getWakelockStatList(bFilterStats, iStatType, iPctType, iSort);
				case 0:
					return getOtherUsageStatList(bFilterStats, iStatType);	
				case 2:
					return getNativeKernelWakelockStatList(bFilterStats, iStatType, iPctType, iSort);
				case 3:
					return getAlarmsStatList(bFilterStats, iStatType);
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
		switch (iStatType)
		{
			case STATS_UNPLUGGED:									
				if ( (m_myRefSinceUnplugged != null) && (m_myRefSinceUnplugged.m_refAlarms != null) )
				{
					strRef = m_myRefSinceUnplugged.m_refAlarms.toString();
				}
				break;
			case STATS_CHARGED:
				if ( (m_myRefSinceCharged != null) && (m_myRefSinceCharged.m_refAlarms != null) )
				{
					strRef = m_myRefSinceCharged.m_refAlarms.toString();
				}
				break;
			case STATS_CUSTOM:
				if ( (m_myRefs != null) && (m_myRefs.m_refAlarms != null))
				{
					strRef = m_myRefs.m_refAlarms.toString();
				}
				break;
			case BatteryStatsTypes.STATS_CURRENT:
				strRef = "no reference to substract";
				break;
			default:
				Log.e(TAG, "Unknown StatType " + iStatType + ". No reference found");
				break;
		}
		
//		Log.i(TAG, "Substracting " + strRef + " from " + strCurrent);
		
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
		if (iStatType == STATS_CUSTOM)
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
		if (iStatType == STATS_CUSTOM)
		{
			myWakelocks = mStats.getWakelockStats(m_context, BatteryStatsTypes.WAKE_TYPE_PARTIAL, BatteryStatsTypes.STATS_CURRENT, iPctType);
		}
		else
		{
			myWakelocks = mStats.getWakelockStats(m_context, BatteryStatsTypes.WAKE_TYPE_PARTIAL, iStatType, iPctType);
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

		// @todo add sorting by settings here: Collections.sort......
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
		ArrayList<NativeKernelWakelock> myKernelWakelocks = Wakelocks.parseProcWakelocks();

		ArrayList<NativeKernelWakelock> myRetKernelWakelocks = new ArrayList<NativeKernelWakelock>();
		// if we are using custom ref. always retrieve "stats current"


		// sort @see com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		Collections.sort(myKernelWakelocks);

		String strCurrent = myKernelWakelocks.toString();
		String strRef = "";
		switch (iStatType)
		{
			case STATS_UNPLUGGED:									
				if ( (m_myRefSinceUnplugged != null) && (m_myRefSinceUnplugged.m_refKernelWakelocks != null) )
				{
					strRef = m_myRefSinceUnplugged.m_refKernelWakelocks.toString();
				}
				break;
			case STATS_CHARGED:
				if ( (m_myRefSinceCharged != null) && (m_myRefSinceCharged.m_refKernelWakelocks != null) )
				{
					strRef = m_myRefSinceCharged.m_refKernelWakelocks.toString();
				}
				break;
			case STATS_CUSTOM:
				if ( (m_myRefs != null) && (m_myRefs.m_refKernelWakelocks != null))
				{
					strRef = m_myRefs.m_refKernelWakelocks.toString();
				}
				break;
			case BatteryStatsTypes.STATS_CURRENT:
				strRef = "no reference to substract";
				break;
			default:
				Log.e(TAG, "Unknown StatType " + iStatType + ". No reference found");
				break;
		}
		
//		Log.i(TAG, "Substracting " + strRef + " from " + strCurrent);
		
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
						if (m_myRefs != null)
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
							myRetKernelWakelocks.add(new NativeKernelWakelock(NO_CUST_REF, 1, 1, 1, 1, 1, 1, 1, 1, 1));
						}
						break;
					case STATS_UNPLUGGED:
						if (m_myRefSinceUnplugged != null)
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
							myRetKernelWakelocks.add(new NativeKernelWakelock("No reference since unplugged set yet", 1, 1, 1, 1, 1, 1, 1, 1, 1));

						}
						break;

					case STATS_CHARGED:
						if (m_myRefSinceCharged != null)
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
							myRetKernelWakelocks.add(new NativeKernelWakelock("No reference since charged yet", 1, 1, 1, 1, 1, 1, 1, 1, 1));
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
		
//		Log.i(TAG, "Result " + myStats.toString());
		
		return myStats;
	}


	/**
	 * Get the Network Usage Stat to be displayed
	 * @param bFilter defines if zero-values should be filtered out
	 * @return a List of Network usages sorted by duration (descending)
	 * @throws Exception if the API call failed
	 */
	public ArrayList<StatElement> getNetworkUsageStatList(boolean bFilter, int iStatType) throws Exception
	{
		ArrayList<StatElement> myStats = new ArrayList<StatElement>();
		
		BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(m_context);

		ArrayList<NetworkUsage> myUsages = null;
		
		
		// if we are using custom ref. always retrieve "stats current"
		if (iStatType == STATS_CUSTOM)
		{
			myUsages = mStats.getNetworkUsageStats(m_context, BatteryStatsTypes.STATS_CURRENT);
		}
		else
		{
			myUsages = mStats.getNetworkUsageStats(m_context, iStatType);
		}

		// sort @see com.asksven.android.common.privateapiproxies.Walkelock.compareTo
		Collections.sort(myUsages);
		
		for (int i = 0; i < myUsages.size(); i++)
		{
			NetworkUsage usage = myUsages.get(i); 
			if ( (!bFilter) || ((usage.getBytesReceived() + usage.getBytesSent()) > 0) )
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
					usage.substractFromRef(m_myRefs.m_refNetwork);
					
					// we must recheck if the delta process is still above threshold
					if ( (!bFilter) || ((usage.getBytesReceived() + usage.getBytesSent()) > 0) )
					{
						myStats.add((StatElement) usage);
					}
				}
				else
				{
					// case b) nothing special
					myStats.add((StatElement) usage);
				}

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
	public ArrayList<StatElement> getOtherUsageStatList(boolean bFilter, int iStatType) throws Exception
	{
		BatteryStatsProxy mStats = BatteryStatsProxy.getInstance(m_context);

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
		if (iStatType == STATS_CUSTOM)
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
        long timeElapsed    = SystemClock.elapsedRealtime();
        
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
        
        if (timeWifiOn > 0)
        {
        	myUsages.add(new Misc("Wifi On", timeWifiOn, whichRealtime));
        }
        
        if (timeWifiRunning > 0)
        {
        	myUsages.add(new Misc("Wifi Running", timeWifiRunning, whichRealtime));
        }
        
        if (timeBluetoothOn > 0)
        {
        	myUsages.add(new Misc("Bluetooth On", timeBluetoothOn, whichRealtime)); 
        }
        
        if (timeNoDataConnection > 0)
        {
        	myUsages.add(new Misc("No Data Connection", timeNoDataConnection, whichRealtime));
        }

        if (timeSignalNone > 0)
        {
        	myUsages.add(new Misc("No or Unknown Signal", timeSignalNone, whichRealtime));
        }

        if (timeSignalPoor > 0)
        {
        	myUsages.add(new Misc("Poor Signal", timeSignalPoor, whichRealtime));
        }

        if (timeSignalModerate > 0)
        {
        	myUsages.add(new Misc("Moderate Signal", timeSignalModerate, whichRealtime));
        }

        if (timeSignalGood > 0)
        {
        	myUsages.add(new Misc("Good Signal", timeSignalGood, whichRealtime));
        }

        if (timeSignalGreat > 0)
        {
        	myUsages.add(new Misc("Great Signal", timeSignalGreat, whichRealtime));
        }

//        if (timeWifiMulticast > 0)
//        {
//        	myUsages.add(new Misc("Wifi Multicast On", timeWifiMulticast, whichRealtime)); 
//        }
//
//        if (timeWifiLocked > 0)
//        {
//        	myUsages.add(new Misc("Wifi Locked", timeWifiLocked, whichRealtime)); 
//        }
//
//        if (timeWifiScan > 0)
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

		for (int i = 0; i < myUsages.size(); i++)
		{
			Misc usage = myUsages.get(i); 
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
					if (m_myRefs != null)
					{
						usage.substractFromRef(m_myRefs.m_refOther);
						if ( (!bFilter) || (usage.getTimeOn() > 0) )
						{
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
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(m_context);
		
		boolean bFilterStats = sharedPrefs.getBoolean("filter_data", true);
		int iPctType = Integer.valueOf(sharedPrefs.getString("default_wl_ref", "0"));
		
		try
    	{			
			if (m_myRefs != null)
			{
				m_myRefs.m_refOther 			= null;
				m_myRefs.m_refWakelocks 		= null;
				m_myRefs.m_refKernelWakelocks 	= null;
				m_myRefs.m_refAlarms		 	= null;
				m_myRefs.m_refProcesses 		= null;
				m_myRefs.m_refNetwork 			= null;			
			}
			else
			{
				m_myRefs = new References();
			}
		
			// create a copy of each list for further reference
			m_myRefs.m_refOther 			= getOtherUsageStatList(
					bFilterStats, BatteryStatsTypes.STATS_CURRENT);
			m_myRefs.m_refWakelocks 		= getWakelockStatList(
					bFilterStats, BatteryStatsTypes.STATS_CURRENT, iPctType, iSort);
			m_myRefs.m_refKernelWakelocks 	= getNativeKernelWakelockStatList(
					bFilterStats, BatteryStatsTypes.STATS_CURRENT, iPctType, iSort);
			m_myRefs.m_refAlarms			= getAlarmsStatList(
					bFilterStats, BatteryStatsTypes.STATS_CURRENT);
			m_myRefs.m_refProcesses 		= getProcessStatList(
					bFilterStats, BatteryStatsTypes.STATS_CURRENT, iSort);
			m_myRefs.m_refNetwork 			= getNetworkUsageStatList(
					bFilterStats, BatteryStatsTypes.STATS_CURRENT);
			m_myRefs.m_refBatteryRealtime 	= getBatteryRealtime(BatteryStatsTypes.STATS_CURRENT);
			
			serializeCustomRefToFile();
    	}
    	catch (Exception e)
    	{
    		Log.e(TAG, "Exception: " + e.getMessage());
    		//Toast.makeText(m_context, "an error occured while creating the custom reference", Toast.LENGTH_SHORT).show();
    		m_myRefs.m_refOther 			= null;
    		m_myRefs.m_refWakelocks 		= null;
    		m_myRefs.m_refKernelWakelocks 	= null;
    		m_myRefs.m_refAlarms			= null;
    		m_myRefs.m_refProcesses 		= null;
    		m_myRefs.m_refNetwork 			= null;
			
    		m_myRefs.m_refBatteryRealtime 	= 0;
    	}			
	}

	/**
	 * Saves data when battery is fully charged
	 * This data will be used in the "since charged" stat type
	 */
	public void setReferenceSinceCharged(int iSort)
	{
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(m_context);
		
		boolean bFilterStats = sharedPrefs.getBoolean("filter_data", true);
		int iPctType = Integer.valueOf(sharedPrefs.getString("default_wl_ref", "0"));
		
		try
    	{			
			m_myRefSinceCharged = new References();
			m_myRefSinceCharged.m_refOther 				= null;
			m_myRefSinceCharged.m_refWakelocks 			= null;
			m_myRefSinceCharged.m_refKernelWakelocks 	= null;
			m_myRefSinceCharged.m_refAlarms				= null;
			m_myRefSinceCharged.m_refProcesses 			= null;
			m_myRefSinceCharged.m_refNetwork 			= null;			
    	
			m_myRefSinceCharged.m_refKernelWakelocks 	= getNativeKernelWakelockStatList(
					bFilterStats, BatteryStatsTypes.STATS_CURRENT, iPctType, iSort);
			m_myRefSinceCharged.m_refAlarms				= getAlarmsStatList(
					bFilterStats, BatteryStatsTypes.STATS_CURRENT);
			m_myRefSinceCharged.m_refOther			 	= getOtherUsageStatList(
					bFilterStats, BatteryStatsTypes.STATS_CURRENT);

			m_myRefSinceCharged.m_refBatteryRealtime 	= getBatteryRealtime(BatteryStatsTypes.STATS_CURRENT);

			serializeSinceChargedRefToFile();
    	}
    	catch (Exception e)
    	{
    		Log.e(TAG, "Exception: " + e.getMessage());
    		Toast.makeText(m_context, "an error occured while creating the custom reference", Toast.LENGTH_SHORT).show();
    		m_myRefSinceCharged.m_refOther 				= null;
    		m_myRefSinceCharged.m_refWakelocks 			= null;
    		m_myRefSinceCharged.m_refKernelWakelocks 	= null;
    		m_myRefSinceCharged.m_refAlarms				= null;
    		m_myRefSinceCharged.m_refProcesses 			= null;
    		m_myRefSinceCharged.m_refNetwork 			= null;
			
    		m_myRefSinceCharged.m_refBatteryRealtime 	= 0;

    	}			
	}

	/**
	 * Saves data when the phone is unplugged
	 * This data will be used in the "since unplugged" stat type
	 */
	public void setReferenceSinceUnplugged(int iSort)
	{
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(m_context);
		
		boolean bFilterStats = sharedPrefs.getBoolean("filter_data", true);
		int iPctType = Integer.valueOf(sharedPrefs.getString("default_wl_ref", "0"));
		
		try
    	{	
			m_myRefSinceUnplugged = new References();
			m_myRefSinceUnplugged.m_refOther 			= null;
			m_myRefSinceUnplugged.m_refWakelocks 		= null;
			m_myRefSinceUnplugged.m_refKernelWakelocks 	= null;
			m_myRefSinceUnplugged.m_refAlarms			= null;
			m_myRefSinceUnplugged.m_refProcesses 		= null;
			m_myRefSinceUnplugged.m_refNetwork 			= null;			
    	
			m_myRefSinceUnplugged.m_refKernelWakelocks 	= getNativeKernelWakelockStatList(
					bFilterStats, BatteryStatsTypes.STATS_CURRENT, iPctType, iSort);
			m_myRefSinceUnplugged.m_refAlarms = getAlarmsStatList(
					bFilterStats, BatteryStatsTypes.STATS_CURRENT);
			m_myRefSinceUnplugged.m_refOther		 	= getOtherUsageStatList(
					bFilterStats, BatteryStatsTypes.STATS_CURRENT);
					
			m_myRefSinceUnplugged.m_refBatteryRealtime 	= getBatteryRealtime(BatteryStatsTypes.STATS_CURRENT);
			

			
			serializeSinceUnpluggedRefToFile();
    	}
    	catch (Exception e)
    	{
    		Log.e(TAG, "Exception: " + e.getMessage());
    		Toast.makeText(m_context, "an error occured while creating the custom reference", Toast.LENGTH_SHORT).show();
    		m_myRefSinceUnplugged.m_refOther 			= null;
    		m_myRefSinceUnplugged.m_refWakelocks 		= null;
    		m_myRefSinceUnplugged.m_refKernelWakelocks 	= null;
    		m_myRefSinceUnplugged.m_refAlarms			= null;
    		m_myRefSinceUnplugged.m_refProcesses 		= null;
    		m_myRefSinceUnplugged.m_refNetwork 			= null;
			
    		m_myRefSinceUnplugged.m_refBatteryRealtime 	= 0;

    	}			
	}

	/**
	 * Restores state from a bundle
	 * @param savedInstanceState a bundle
	 */
	public void restoreFromBundle(Bundle savedInstanceState)
	{
		m_myRefs.m_refWakelocks 		= (ArrayList<StatElement>) savedInstanceState.getSerializable("wakelockstate");
		m_myRefs.m_refKernelWakelocks 	= (ArrayList<StatElement>) savedInstanceState.getSerializable("nativekernelwakelockstate");
		m_myRefs.m_refAlarms		 	= (ArrayList<StatElement>) savedInstanceState.getSerializable("alarmstate");
		m_myRefs.m_refProcesses 		= (ArrayList<StatElement>) savedInstanceState.getSerializable("processstate");
		m_myRefs.m_refOther 			= (ArrayList<StatElement>) savedInstanceState.getSerializable("otherstate");
		m_myRefs.m_refBatteryRealtime 	= (Long) savedInstanceState.getSerializable("batteryrealtime");


	}
	
	/**
	 * Writes states to a bundle to be temporarily persisted
	 * @param savedInstanceState a bundle
	 */
	public void writeToBundle(Bundle savedInstanceState)
	{
    	if (hasCustomRef())
    	{		
    		savedInstanceState.putSerializable("wakelockstate", m_myRefs.m_refWakelocks);
    		savedInstanceState.putSerializable("nativekernelwakelockstate", m_myRefs.m_refKernelWakelocks);
    		savedInstanceState.putSerializable("alarmstate", m_myRefs.m_refAlarms);
    		savedInstanceState.putSerializable("processstate", m_myRefs.m_refProcesses);
    		savedInstanceState.putSerializable("otherstate", m_myRefs.m_refOther);
    		savedInstanceState.putSerializable("networkstate", m_myRefs.m_refNetwork);
    		savedInstanceState.putSerializable("batteryrealtime", m_myRefs.m_refBatteryRealtime);
    		
        }

	}

	public void serializeCustomRefToFile()
	{
		if (hasCustomRef())
		{
			DataStorage.objectToFile(m_context, "custom_ref", m_myRefs);
		}
	}
	
	public void serializeSinceChargedRefToFile()
	{
		DataStorage.objectToFile(m_context, "since_charged_ref", m_myRefSinceCharged);
	}

	public void serializeSinceUnpluggedRefToFile()
	{
	
		DataStorage.objectToFile(m_context, "since_unplugged_ref", m_myRefSinceUnplugged);
	}

	public void deserializeFromFile()
	{
		m_myRefs = (References) DataStorage.fileToObject(m_context, "custom_ref");
		m_myRefSinceCharged = (References) DataStorage.fileToObject(m_context, "since_charged_ref");
		m_myRefSinceUnplugged = (References) DataStorage.fileToObject(m_context, "since_unplugged_ref");

	}

	public void deletedSerializedRefs()
	{
		References myEmptyRef = new References();
		DataStorage.objectToFile(m_context, "custom_ref", myEmptyRef);
		DataStorage.objectToFile(m_context, "since_charged_ref", myEmptyRef);
		DataStorage.objectToFile(m_context, "since_unplugged_ref", myEmptyRef);
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
				out.write("RADIO: "+Build.RADIO+"\n");
				out.write("BOOTLOADER: "+Build.BOOTLOADER+"\n");
				out.write("FINGERPRINT: "+Build.FINGERPRINT+"\n");
				out.write("HARDWARE: "+Build.HARDWARE+"\n");
				out.write("ID: "+Build.ID+"\n");
				out.write("TAGS: "+Build.TAGS+"\n");
				out.write("USER: "+Build.USER+"\n");
				out.write("PRODUCT: "+Build.PRODUCT+"\n");
				out.write("RADIO: "+Build.getRadioVersion()+"\n");
				out.write("Rooted: "+ RootDetection.hasSuRights("dumpsys alarm") + "\n");
				
				
				// write timing info
				boolean bDumpChapter = sharedPrefs.getBoolean("show_other", true);
				if (bDumpChapter)
				{
					out.write("===========\n");
					out.write("Other Usage\n");
					out.write("===========\n");
					dumpList(getOtherUsageStatList(bFilterStats, iStatType), out);
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
					// write kernel wakelock info
					out.write("================\n");
					out.write("Kernel Wakelocks\n");
					out.write("================\n");
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

				// write network info
				//out.write("=======\n");
				//out.write("Network\n");
				//out.write("=======\n");
				//dumpList(getNetworkUsageStatList(bFilterStats, m_iStatType), out);
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
				// see http://androidsnippets.com/show-all-running-services
				// close file
				out.close();
				Toast.makeText(m_context, "Dump witten: " + strFilename, Toast.LENGTH_SHORT).show();
				
		    }
		    else
		    {
	    		Log.i(TAG, "Write error. " + Environment.getExternalStorageDirectory() + " couldn't be written");
	    		Toast.makeText(m_context, "No dump created. " + Environment.getExternalStorageDirectory() + " is probably unmounted.", Toast.LENGTH_SHORT).show();		    	
		    }
    	}
    	catch (Exception e)
    	{
    		Log.e(TAG, "Exception: " + e.getMessage());
//    		Toast.makeText(m_context, "an error occured while dumping the statistics", Toast.LENGTH_SHORT).show();
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
		
//		switch (iStat)
//		{
//			// constants are related to arrays.xml string-array name="stats"
//			case 0:
//				strRet = "Process";
//				break;
//				
//			case 1:
//				strRet = "Partial Wakelocks";
//				break;
//				
//			case 2:
//				strRet = "Other";
//				break;
//					
//			case 3:
//				strRet = "Kernel Wakelocks";
//				break;
//
//			case 4:
//				strRet = "Alarms";
//				break;
//
//		}
		
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
			case 0:
				iRet = 0;
				break;
			case 1:
				iRet = 1;
				break;
			case 2:
				iRet = 2;
				break;
				
		}
		return iRet;
	}


}
