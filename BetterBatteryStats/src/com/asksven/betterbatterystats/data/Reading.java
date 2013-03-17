/*
 * Copyright (C) 2011-2013 asksven
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
import java.io.Serializable;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.asksven.andoid.common.contrib.Shell;
import com.asksven.android.common.kernelutils.NativeKernelWakelock;
import com.asksven.android.common.kernelutils.State;
import com.asksven.android.common.privateapiproxies.Alarm;
import com.asksven.android.common.privateapiproxies.Misc;
import com.asksven.android.common.privateapiproxies.NetworkUsage;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.android.common.privateapiproxies.Wakelock;
import com.asksven.android.common.privateapiproxies.Process;
import com.asksven.android.common.utils.DataStorage;
import com.asksven.android.common.utils.DateUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * A reading represents the data that was collected on a device between two references.
 * This class is designed to be dumped as text or as JSON representation
 * @author sven
 *
 */
public class Reading implements Serializable
{
	
	static final String TAG = "Reading";
	String bbsVersion;
	String creationDate;
	String statType;
	String duration;
	String buildVersionRelease;
	String buildBrand;
	String buildDevice;
	String buildManufacturer;
	String buildModel;
	String osVersion;
	String buildBootloader;
	String buildHardware;
	String buildFingerprint;
	String buildId;
	String buildTags;
	String buildUser;
	String buildProduct;
	String buildRadio;
	boolean rooted;
	int batteryLevelLost;
	int batteryVoltageLost;
	
	
	ArrayList<Misc> otherStats;
	ArrayList<NativeKernelWakelock> kernelWakelockStats;
	ArrayList<Wakelock> partialWakelockStats;
	ArrayList<Alarm> alarmStats;
	ArrayList<NetworkUsage> networkStats;
	ArrayList<StatElement> cpuStateStats;
	ArrayList<Process> processStats;
	
	@SuppressLint({ "InlinedApi", "NewApi" })
	public Reading(Context context, Reference refFrom, Reference refTo)
	{
		otherStats 				= new ArrayList<Misc>();
		kernelWakelockStats 	= new ArrayList<NativeKernelWakelock>();
		partialWakelockStats 	= new ArrayList<Wakelock>();
		alarmStats 				= new ArrayList<Alarm>();
		networkStats 			= new ArrayList<NetworkUsage>();
		cpuStateStats 			= new ArrayList<StatElement>();
		processStats 			= new ArrayList<Process>();

		try
		{
			PackageInfo pinfo = context.getPackageManager()
					.getPackageInfo(context.getPackageName(), 0);
			bbsVersion 			= pinfo.versionName;
		}
		catch (Exception e)
		{
			// do nothing
			bbsVersion = "unknown";
		}
				
		creationDate 		= DateUtils.now();
		statType 			= refFrom.getLabel() + " to "+ refTo.getLabel();
		duration 			= DateUtils.formatDuration(StatsProvider.getInstance(context).getSince(refFrom, refTo));
		buildVersionRelease = Build.VERSION.RELEASE;
		buildBrand 			= Build.BRAND;
		buildDevice 		= Build.DEVICE;
		buildManufacturer 	= Build.MANUFACTURER;
		buildModel 			= Build.MODEL;
		osVersion 			= System.getProperty("os.version");

		if (Build.VERSION.SDK_INT >= 8)
		{
			buildBootloader = Build.BOOTLOADER;
			buildHardware 	= Build.HARDWARE;
		}
		buildFingerprint 	= Build.FINGERPRINT;
		buildId 			= Build.ID;
		buildTags 			= Build.TAGS;
		buildUser 			= Build.USER;
		buildProduct 		= Build.PRODUCT;
		buildRadio 			= "";

		// from API14
		if (Build.VERSION.SDK_INT >= 14)
		{
			buildRadio 		= Build.getRadioVersion();
		}
		else if (Build.VERSION.SDK_INT >= 8)
		{
			buildRadio 		= Build.RADIO;
		}

		rooted 				= Shell.SU.available();
		
		batteryLevelLost 	= StatsProvider.getInstance(context).getBatteryLevelStat(refFrom, refTo);
		batteryVoltageLost 	= StatsProvider.getInstance(context).getBatteryVoltageStat(refFrom, refTo);

		// populate the stats
		SharedPreferences sharedPrefs 	= PreferenceManager.getDefaultSharedPreferences(context);
		boolean bFilterStats 			= sharedPrefs.getBoolean("filter_data", true);
		int iPctType 					= Integer.valueOf(sharedPrefs.getString("default_wl_ref", "0"));
		int iSort 						= 0;
		
		try
		{
			ArrayList<StatElement> tempStats = StatsProvider.getInstance(context).getOtherUsageStatList(bFilterStats, refFrom, false, false, refTo);
			for (int i = 0; i < tempStats.size(); i++)
			{
				if (tempStats.get(i) instanceof Misc)
				{
					otherStats.add((Misc) tempStats.get(i));
				}
				else
				{
					Log.e(TAG, tempStats.get(i).toString() + " is not of type Misc");
				}
			}
			
			tempStats = StatsProvider.getInstance(context).getWakelockStatList(bFilterStats, refFrom, iPctType, iSort, refTo);
			for (int i = 0; i < tempStats.size(); i++)
			{
				if (tempStats.get(i) instanceof Misc)
				{
					partialWakelockStats.add((Wakelock) tempStats.get(i));
				}
				else
				{
					Log.e(TAG, tempStats.get(i).toString() + " is not of type Misc");
				}
			}

			tempStats = StatsProvider.getInstance(context).getNativeKernelWakelockStatList(bFilterStats, refFrom, iPctType, iSort, refTo);
			for (int i = 0; i < tempStats.size(); i++)
			{
				if (tempStats.get(i) instanceof NativeKernelWakelock)
				{
					kernelWakelockStats.add((NativeKernelWakelock) tempStats.get(i));
				}
				else
				{
					Log.e(TAG, tempStats.get(i).toString() + " is not of type Misc");
				}
			}
			
			tempStats = StatsProvider.getInstance(context).getProcessStatList(bFilterStats, refFrom, iSort, refTo);
			for (int i = 0; i < tempStats.size(); i++)
			{
				if (tempStats.get(i) instanceof Process)
				{
					processStats.add((Process) tempStats.get(i));
				}
				else
				{
					Log.e(TAG, tempStats.get(i).toString() + " is not of type Misc");
				}
			}
			
			tempStats = StatsProvider.getInstance(context).getAlarmsStatList(bFilterStats, refFrom, refTo);
			for (int i = 0; i < tempStats.size(); i++)
			{
				if (tempStats.get(i) instanceof Alarm)
				{
					alarmStats.add((Alarm) tempStats.get(i));
				}
				else
				{
					Log.e(TAG, tempStats.get(i).toString() + " is not of type Misc");
				}
			}
			
			tempStats = StatsProvider.getInstance(context).getNativeNetworkUsageStatList(bFilterStats, refFrom, refTo);
			for (int i = 0; i < tempStats.size(); i++)
			{
				if (tempStats.get(i) instanceof NetworkUsage)
				{
					networkStats.add((NetworkUsage) tempStats.get(i));
				}
				else
				{
					Log.e(TAG, tempStats.get(i).toString() + " is not of type Misc");
				}
			}
			
			tempStats = StatsProvider.getInstance(context).getCpuStateList(refFrom, refTo, bFilterStats);
			for (int i = 0; i < tempStats.size(); i++)
			{
				if (tempStats.get(i) instanceof State)
				{
					cpuStateStats.add((State) tempStats.get(i));
				}
				else
				{
					Log.e(TAG, tempStats.get(i).toString() + " is not of type Misc");
				}
			}		
		}
		catch (Exception e)
		{
			Log.e(TAG, "An exception occured: " + e.getMessage());
		}
	}
	
	private String toJson()
	{
		Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
		String json = gson.toJson(this);
		
		return json;
	}
	
	@SuppressLint("NewApi") 
	public void writeToFile(Context context)
	{
		
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(context);

		if (!DataStorage.isExternalStorageWritable())
		{
			Log.e(TAG, "External storage can not be written");
			Toast.makeText(context, "External Storage can not be written",
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
					root = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
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

			// check if file can be written
			if (root.canWrite())
			{
				String strFilename = "BetterBatteryStats-"
						+ DateUtils.now("yyyy-MM-dd_HHmmssSSS") + ".json";
				File dumpFile = new File(root, strFilename);
				FileWriter fw = new FileWriter(dumpFile);
				BufferedWriter out = new BufferedWriter(fw);
				out.write(this.toJson());
				out.close();
			}
		}
		catch (Exception e)
		{
			Log.e(TAG, "Exception: " + e.getMessage());
		}
	}
}
