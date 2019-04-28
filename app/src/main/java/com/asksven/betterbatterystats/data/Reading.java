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

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.asksven.android.common.kernelutils.State;
import com.asksven.android.common.kernelutils.Wakelocks;
import com.asksven.android.common.nameutils.UidNameResolver;
import com.asksven.android.common.privateapiproxies.Alarm;
import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.android.common.privateapiproxies.Misc;
import com.asksven.android.common.privateapiproxies.NativeKernelWakelock;
import com.asksven.android.common.privateapiproxies.NetworkUsage;
import com.asksven.android.common.privateapiproxies.Process;
import com.asksven.android.common.privateapiproxies.SensorUsage;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.android.common.privateapiproxies.Wakelock;
import com.asksven.android.common.utils.DataStorage;
import com.asksven.android.common.utils.DateUtils;
import com.asksven.android.common.utils.SysUtils;
import com.asksven.betterbatterystats.R;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * A reading represents the data that was collected on a device between two references.
 * This class is designed to be dumped as text or as JSON representation
 * @author sven
 *
 */
public class Reading implements Serializable
{
	
	static 	final String TAG = "Reading";
	String 	bbsVersion;
	String 	creationDate;
	Long 	readingTimeMs;
	String 	statType;
	String 	duration;
	long 	totalTime;
	String 	buildVersionRelease;
	String 	buildBrand;
	String 	buildDevice;
	String 	buildManufacturer;
	String 	buildModel;
	String 	osVersion;
	String 	buildBootloader;
	String 	buildHardware;
	String 	buildFingerprint;
	String 	buildId;
	String 	buildTags;
	String 	buildUser;
	String 	buildProduct;
	String 	buildRadio;
	boolean rootPermissions;
	boolean batteryStatsPermGranted;
	String 	batteryServiceState;
	String 	seLinuxPolicy;
	int 	batteryLevelLost;
	int 	batteryVoltageLost;
	String 	batteryLevelLostText;
	String 	batteryVoltageLostText;
	String 	note;
	
	
	ArrayList<StatElement> otherStats;
	ArrayList<StatElement> kernelWakelockStats;
	ArrayList<StatElement> partialWakelockStats;
	ArrayList<StatElement> alarmStats;
	ArrayList<StatElement> networkStats;
	ArrayList<StatElement> cpuStateStats;
	ArrayList<StatElement> processStats;
	ArrayList<StatElement> sensorUsage;
	
	@SuppressLint({ "InlinedApi", "NewApi" })
	public Reading(Context context, Reference refFrom, Reference refTo)
	{
		otherStats 				= new ArrayList<StatElement>();
		kernelWakelockStats 	= new ArrayList<StatElement>();
		partialWakelockStats 	= new ArrayList<StatElement>();
		alarmStats 				= new ArrayList<StatElement>();
		networkStats 			= new ArrayList<StatElement>();
		cpuStateStats 			= new ArrayList<StatElement>();
		processStats 			= new ArrayList<StatElement>();
		sensorUsage 			= new ArrayList<StatElement>();


		if ((refFrom == null) || (refTo == null))
		{
			Log.e(TAG, "Error: a Reading was instanciated with a null refFrom or refTo");
			return;
		}
		
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
		readingTimeMs       = refTo.m_creationTime;
		statType 			= refFrom.getLabel() + " to "+ refTo.getLabel();
		totalTime			= StatsProvider.getInstance().getSince(refFrom, refTo);
		duration 			= DateUtils.formatDuration(totalTime);
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

		SharedPreferences sharedPrefs 	= PreferenceManager.getDefaultSharedPreferences(context);
		
		batteryStatsPermGranted = SysUtils.hasBatteryStatsPermission(context);

		// Determine the status of the batteryinfo service
		BatteryStatsProxy stats = BatteryStatsProxy.getInstance(context);
		String status = "";
		if (stats.initFailed())
		{
			status = "Failed";
			if (!stats.getError().equals(""))
			{
				status = status + ": " + stats.getError();
			}

		}
		else
		{
			status = "Success";
			if (stats.isFallback())
			{
				status = status + " (fallback)";
			}
		}

		batteryServiceState = status;

		seLinuxPolicy = SysUtils.getSELinuxPolicy();
		
		batteryLevelLost 		= StatsProvider.getInstance().getBatteryLevelStat(refFrom, refTo);
		batteryVoltageLost 		= StatsProvider.getInstance().getBatteryVoltageStat(refFrom, refTo);
		batteryLevelLostText 	= StatsProvider.getInstance().getBatteryLevelFromTo(refFrom, refTo, false);
		batteryVoltageLostText 	= StatsProvider.getInstance().getBatteryVoltageFromTo(refFrom, refTo);

		// populate the stats
		
		boolean bFilterStats 			= sharedPrefs.getBoolean("filter_data", true);
		int iPctType 					= 0;
		int iSort 						= 0;
		
		try
		{
			ArrayList<StatElement> tempStats = StatsProvider.getInstance().getOtherUsageStatList(bFilterStats, refFrom, false, false, refTo);
			for (int i = 0; i < tempStats.size(); i++)
			{
				// make sure to load all data (even the lazy loaded one)
				tempStats.get(i).getFqn(UidNameResolver.getInstance());
				
				if (tempStats.get(i) instanceof Misc)
				{
					otherStats.add((Misc) tempStats.get(i));
				}
				else
				{
					Log.e(TAG, tempStats.get(i).toString() + " is not of type Misc");
				}
			}
			
			tempStats = StatsProvider.getInstance().getWakelockStatList(bFilterStats, refFrom, iPctType, iSort, refTo);
			for (int i = 0; i < tempStats.size(); i++)
			{
				// make sure to load all data (even the lazy loaded one)
				tempStats.get(i).getFqn(UidNameResolver.getInstance());

				if (tempStats.get(i) instanceof Wakelock)
				{
					partialWakelockStats.add((Wakelock) tempStats.get(i));
				}
				else
				{
					Log.e(TAG, tempStats.get(i).toString() + " is not of type Wakelock");
				}
			}

			tempStats = StatsProvider.getInstance().getKernelWakelockStatList(bFilterStats, refFrom, iPctType, iSort, refTo);
			for (int i = 0; i < tempStats.size(); i++)
			{
				// make sure to load all data (even the lazy loaded one)
				tempStats.get(i).getFqn(UidNameResolver.getInstance());

				if (tempStats.get(i) instanceof NativeKernelWakelock)
				{
					kernelWakelockStats.add((NativeKernelWakelock) tempStats.get(i));
				}
				else
				{
					Log.e(TAG, tempStats.get(i).toString() + " is not of type NativeKernelWakelock");
				}
			}
			
			tempStats = StatsProvider.getInstance().getProcessStatList(bFilterStats, refFrom, iSort, refTo);
			for (int i = 0; i < tempStats.size(); i++)
			{
				// make sure to load all data (even the lazy loaded one)
				tempStats.get(i).getFqn(UidNameResolver.getInstance());

				if (tempStats.get(i) instanceof Process)
				{
					processStats.add((Process) tempStats.get(i));
				}
				else
				{
					Log.e(TAG, tempStats.get(i).toString() + " is not of type Process");
				}
			}
			
			tempStats = StatsProvider.getInstance().getAlarmsStatList(bFilterStats, refFrom, refTo);
			for (int i = 0; i < tempStats.size(); i++)
			{
				// make sure to load all data (even the lazy loaded one)
				tempStats.get(i).getFqn(UidNameResolver.getInstance());

				if (tempStats.get(i) instanceof Alarm)
				{
					alarmStats.add((Alarm) tempStats.get(i));
				}
				else
				{
					Log.e(TAG, tempStats.get(i).toString() + " is not of type Alarm");
				}
			}
			
			tempStats = StatsProvider.getInstance().getNetworkUsageStatList(bFilterStats, refFrom, refTo);
			for (int i = 0; i < tempStats.size(); i++)
			{
				// make sure to load all data (even the lazy loaded one)
				tempStats.get(i).getFqn(UidNameResolver.getInstance());

				if (tempStats.get(i) instanceof NetworkUsage)
				{
					networkStats.add((NetworkUsage) tempStats.get(i));
				}
				else
				{
					Log.e(TAG, tempStats.get(i).toString() + " is not of type NetworkUsage");
				}
			}
			
			tempStats = StatsProvider.getInstance().getCpuStateList(refFrom, refTo, bFilterStats);
			for (int i = 0; i < tempStats.size(); i++)
			{
				// make sure to load all data (even the lazy loaded one)
				tempStats.get(i).getFqn(UidNameResolver.getInstance());

				if (tempStats.get(i) instanceof State)
				{
					cpuStateStats.add((State) tempStats.get(i));
				}
				else
				{
					Log.e(TAG, tempStats.get(i).toString() + " is not of type State");
				}
			}
			
			tempStats = StatsProvider.getInstance().getSensorStatList(false, refFrom, refTo);
			for (int i = 0; i < tempStats.size(); i++)
			{
				// make sure to load all data (even the lazy loaded one)
				tempStats.get(i).getFqn(UidNameResolver.getInstance());

				if (tempStats.get(i) instanceof SensorUsage)
				{
					sensorUsage.add((SensorUsage) tempStats.get(i));
				}
				else
				{
					Log.e(TAG, tempStats.get(i).toString() + " is not of type SensorUsage");
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
	

	public String toStringText(Context context, String note)
	{
		StringWriter out = new StringWriter();
		SharedPreferences sharedPrefs 	= PreferenceManager.getDefaultSharedPreferences(context);


		// write header
		out.write("===================\n");
		out.write("General Information\n");
		out.write("===================\n");
		out.write("BetterBatteryStats version: " + bbsVersion + "\n");
		out.write("Creation Date: " + creationDate + "\n");
		out.write("Statistic Type: " + statType + "\n");
		out.write("Since " + duration + "\n");
		out.write("Note: " + note + "\n");	
		out.write("VERSION.RELEASE: " + buildVersionRelease + "\n");
		out.write("BRAND: " + buildBrand + "\n");
		out.write("DEVICE: " + buildDevice + "\n");
		out.write("MANUFACTURER: " + buildManufacturer + "\n");
		out.write("MODEL: " + buildModel + "\n");
		out.write("OS.VERSION: " + osVersion + "\n");

		if (Build.VERSION.SDK_INT >= 8)
		{

			out.write("BOOTLOADER: " + buildBootloader + "\n");
			out.write("HARDWARE: " + buildHardware + "\n");
		}
		out.write("FINGERPRINT: " + buildFingerprint + "\n");
		out.write("ID: " + buildId + "\n");
		out.write("TAGS: " + buildTags + "\n");
		out.write("USER: " + buildUser + "\n");
		out.write("PRODUCT: " + buildProduct + "\n");
		out.write("RADIO: " + buildRadio + "\n");
		out.write("Root perms: " + rootPermissions + "\n");
		out.write("SELinux Policy: " + seLinuxPolicy + "\n");
		out.write("BATTERY_STATS permission granted: " + batteryStatsPermGranted + "\n");
		out.write("BATTERY_STATS status: " + batteryServiceState + "\n");

		out.write("============\n");
		out.write("Battery Info\n");
		out.write("============\n");
		out.write("Level lost [%]: " + batteryLevelLostText + "\n");
		out.write("Voltage lost [mV]: " + batteryVoltageLostText + "\n");

		// write timing info
		out.write("===========\n");
		out.write("Other Usage\n");
		out.write("===========\n");
		dumpList(context, otherStats, out);

		// write wakelock info
		out.write("======================================================\n");
		out.write("Wakelocks\n");
		out.write("======================================================\n");
		dumpList(context, partialWakelockStats, out);

		String addendumKwl = "";
		if (Wakelocks.isDiscreteKwlPatch())
		{
			addendumKwl = "!!! Discrete !!!";
		}

		// write kernel wakelock info
		out.write("================\n");
		out.write("Kernel Wakelocks " + addendumKwl + "\n");
		out.write("================\n");

		dumpList(context, kernelWakelockStats, out);

		// write process info
		out.write("=========\n");
		out.write("Processes\n");
		out.write("=========\n");
		dumpList(context, processStats, out);

		// write alarms info
		out.write("======================\n");
		out.write("Alarms\n");
		out.write("======================\n");
		dumpList(context, alarmStats, out);

		// write alarms info
		out.write("======================\n");
		out.write("Network\n");
		out.write("======================\n");
		dumpList(context, networkStats, out);

		// write cpu states info
		out.write("==========\n");
		out.write("CPU States\n");
		out.write("==========\n");
		dumpList(context, cpuStateStats, out);

		// write sensor info
		out.write("==========\n");
		out.write("Sensors\n");
		out.write("==========\n");
		dumpList(context, sensorUsage, out);

		out.write("========\n");
		out.write("Services\n");
		out.write("========\n");
		out.write("Active since: The time when the service was first made active, either by someone starting or binding to it.\n");
		out.write("Last activity: The time when there was last activity in the service (either explicit requests to start it or clients binding to it)\n");
		out.write("See http://developer.android.com/reference/android/app/ActivityManager.RunningServiceInfo.html\n");
		
		ActivityManager am = (ActivityManager) context
				.getSystemService(context.ACTIVITY_SERVICE);
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

		// add chapter for reference info
		out.write("==================\n");
		out.write("Reference overview\n");
		out.write("==================\n");
		
		for (int i = 0; i < ReferenceStore.getReferenceNames(null, context).size(); i++)
		{
			String name = ReferenceStore.getReferenceNames(null, context).get(i);
			Reference ref = ReferenceStore.getReferenceByName(name, context);
			out.write	(name + ": " + ref.whoAmI() + "\n");
		}

		return out.toString();
	}

	@SuppressLint("NewApi") 
	public Uri writeDumpfile(Context context, String note)
	{
		Uri fileUri = null;
		
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(context);

		if (!DataStorage.isExternalStorageWritable())
		{
			Log.e(TAG, "External storage can not be written");
			Toast.makeText(context, context.getString(R.string.message_external_storage_write_error),
					Toast.LENGTH_SHORT).show();
		}
		String path = StatsProvider.getWritableFilePath();
        try
        {

            if (!path.equals(""))
            {
                String strFilename = "BetterBatteryStats-"
                        + DateUtils.now("yyyy-MM-dd_HHmmssSSS") + ".txt";
                File dumpFile = new File(path + "/" + strFilename);
                fileUri = Uri.fromFile(dumpFile);
                FileWriter fw = new FileWriter(dumpFile);
                BufferedWriter out = new BufferedWriter(fw);
                out.write("/*\n");
                out.write(this.toStringText(context, note));
                out.write("------ human readable part end here\n");
                out.write("*/\n");
                out.write(this.toJson());
                out.close();

                // workaround: force mediascanner to run
                DataStorage.forceMediaScanner(context, fileUri);
            } else
            {
                Log.i(TAG, "Write error. *" + path + "* couldn't be written");
            }
        }
        catch (Exception e)
        {
            Log.e(TAG, "Exception: " + e.getMessage());
        }

		return fileUri;
	}

	@SuppressLint("NewApi")
	public boolean writeTimeSeries(Context context)
	{

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(context);

		boolean timeSeriesEnabled = sharedPrefs.getBoolean("flag_time_series", false);

		if (timeSeriesEnabled)
		{
			String uuid = sharedPrefs.getString("uuid", "guest");

			// don't bother trying to access this URL: it does not exist ;)
            String address = "https://influxdb.gke-dev.asksven.io";
            String login = "admin";
            String password = "qQZqEtD5yK";
            String dbName = "tests";
            String creationTime = DateUtils.format(readingTimeMs);

            Long readingTimeNs = readingTimeMs * 1000* 1000;
            Long now = System.currentTimeMillis();
            String nowTime = DateUtils.format(now);


            DatabaseConnection database = new DatabaseConnection(address, login, password, dbName);


            for (int i=0 ; i < otherStats.size(); i++)
            {
                Misc data = (Misc) otherStats.get(i);


                OtherStatsDataPoint dataPoint = new OtherStatsDataPoint(
                        uuid,
                        data.getName(),
                        data.getTimeOn(),
                        data.getTimeRunning(),
                        data.getValues()[0]/data.getTimeRunning()*100,
                        readingTimeNs
                );
                Log.i(TAG, "Pushing " + data.getName() + " to influxdb");
                database.sendMsg(dataPoint);

            }

            // push the data
            database.send();


		}

        return true;

	}

	@SuppressLint("NewApi") 
	public String toStringJson(Context context)
	{
		return this.toJson();
	}
	
	/**
	 * Dump the elements on one list
	 * 
	 * @param myList
	 *            a list of StatElement
	 */
	private void dumpList(Context context, List<StatElement> myList, Writer out)
	{
		try
		{
			if (myList != null)
			{
				for (int i = 0; i < myList.size(); i++)
				{
					out.write(myList.get(i).getDumpData(UidNameResolver.getInstance(), totalTime) + "\n");
		
				}
			}
		}
		catch (Exception e)
		{
			Log.e(TAG, "An error occured serializing list: " + e.getMessage());
		}
	}


}
