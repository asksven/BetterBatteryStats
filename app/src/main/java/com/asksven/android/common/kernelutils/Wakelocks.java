/**
 * 
 */
package com.asksven.android.common.kernelutils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.asksven.android.contrib.Shell;
import com.asksven.android.contrib.Util;
import com.asksven.android.common.CommonLogSettings;
import com.asksven.android.common.privateapiproxies.NativeKernelWakelock;
import com.asksven.android.common.privateapiproxies.StatElement;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.util.Log;


/**
 * @author sven
 * 
 * Parsing of /proc/wakelocks
 * 
 * Fields:
 * total_time: accumulates the total amount of time that the corresponding suspend blocker has been held.
 * active_since: tracks how long a suspend blocker has been held since it was last acquired, or (presumably) zero if it is not currently held.
 * count: the number of times that the suspend blocker has been acquired. This is useful in combination with total_time, as it allows you to calculate the average hold time for the suspend blocker.
 * expire_count: the number of times that the suspend blocker has timed out. This indicates that some application has an input device open, but is not reading from it, which is a bug, as noted earlier.
 * max_time: the longest hold time for the suspend blocker. This allows finding cases where suspend blockers are held for too long, but are eventually released. (In contrast, active_since is more useful in the held-forever case.)
 * sleep_time: the total time that the suspend blocker was held while the display was powered off.
 * wake_count: the number of times that the suspend blocker was the first to be acquired in the resume path.
 *
 */
public class Wakelocks
{
    private final static String TAG ="Wakelocks";
    private static String FILE_PATH = "/proc/wakelocks";
    
    public static ArrayList<StatElement> parseProcWakelocks(Context context)
    {
    	if (CommonLogSettings.DEBUG)
    	{
    		Log.i(TAG, "Parsing " + FILE_PATH);
    	}
    	
    	String delimiter = String.valueOf('\t');
    	ArrayList<StatElement> myRet = new ArrayList<StatElement>();
    	// format 
    	// [name, count, expire_count, wake_count, active_since, total_time, sleep_time, max_time, last_change]
    	ArrayList<String[]> rows = parseDelimitedFile(FILE_PATH, delimiter);

    	long msSinceBoot = SystemClock.elapsedRealtime();

		// list the running processes
		ActivityManager actvityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> procInfos = actvityManager.getRunningAppProcesses();
		PackageManager pack=context.getPackageManager();

    	// start with 1
    	for (int i=1; i < rows.size(); i++ )
    	{
    		try
    		{
    			// times in file are microseconds
    			String[] data = (String[]) rows.get(i);
    			String name = data[0];
    			int count = Integer.valueOf(data[1]);
    			int expire_count = Integer.valueOf(data[2]);
    			int wake_count = Integer.valueOf(data[3]);
    			long active_since = Long.valueOf(data[4]);
    			long total_time = Long.valueOf(data[5]) / 1000000;
    			long sleep_time = Long.valueOf(data[6]) / 1000000;
    			long max_time = Long.valueOf(data[7]) / 1000000;
    			long last_change = Long.valueOf(data[8]);
    			
				// post-processing of eventX-YYYY processes
				String details = "";
//				name = "event3-30240";
				// we start with a " here as that is the way the data comes from /proc
				if (name.startsWith("\"event"))
				{
					String process = name.replaceAll("\"", "");
					if (CommonLogSettings.DEBUG)
					{
						Log.d(TAG, "Pattern 'event' found in " + process);
					}
					
					int proc = 0;
					String[] parts = process.split("-");
					if (parts.length == 2)
					{
						try
						{
							proc = Integer.valueOf(parts[1]);
							if (CommonLogSettings.DEBUG)
							{
								Log.d(TAG, "Resolving proc name for 'event' " + proc);
							}
						}
						catch (Exception e)
						{
							Log.e(TAG, "Cound not split process name " + process);
						}
					}
					
					if (proc != 0)
					{
						// search for the process in the task list
						for (int psCount = 0; psCount < procInfos.size(); psCount++)
						{
							int id = procInfos.get(psCount).pid; 
							if ( id == proc)
							{
								String processName = procInfos.get(psCount).processName;

								details= processName; 
								String appName = "";

								String[] pkgList = procInfos.get(count).pkgList;
								for (int j=0; j < pkgList.length; j++)
								{
									if (details.length() > 0)
									{
										details += ", ";
									}
									details += pkgList[j];
								}

								if (CommonLogSettings.DEBUG)
								{
									Log.d(TAG, "Pattern 'event' resolved to " + details);
								}
							}
						}
					}
				}

    			NativeKernelWakelock wl = new NativeKernelWakelock(
    					name, details, count, expire_count, wake_count,
    					active_since, total_time, sleep_time, max_time,
    					last_change, msSinceBoot);
    			myRet.add(wl);
    		}
    		catch (Exception e)
    		{
    			// go on
    		}
    	}
    	return myRet;
    }
    protected static ArrayList<String[]> parseDelimitedFile(String filePath, String delimiter)
    {
		ArrayList<String[]> rows = new ArrayList<String[]>();
    	try
    	{
			FileReader fr = new FileReader(filePath);
			BufferedReader br = new BufferedReader(fr);
			String currentRecord;
			while ((currentRecord = br.readLine()) != null)
				rows.add(currentRecord.split(delimiter));
			br.close();
    	}
    	catch (Exception e)
    	{
    		Log.i(TAG, "An error occured while parsing " + filePath + ": " + e.getMessage() + ". Retrying with root");
    		
    		// retry with root

			List<String> res = Shell.SU.run("cat " + filePath);
			for (int i=0; i < res.size(); i++)
			{
				rows.add(res.get(i).split(delimiter));
    		}
			
			if (res.isEmpty())
			{
				Log.i(TAG, "Wakelocks could not be read from " + filePath + ", even with root");
			}
    	}
		return rows;
    }

    public static boolean fileExists()
    {
    	boolean exists = false;
    	FileReader fr = null;
    	try
    	{
			fr = new FileReader(FILE_PATH);
			exists = true;
    	}
    	catch (Exception e)
    	{
    		exists = false;
    	}
    	finally
    	{
    		if (exists)
    		{
    			try
    			{
					fr.close();
				}
    			catch (IOException e)
    			{
					// do nothing
				}
    		}
    	}
		return exists;
    }

	public static boolean isDiscreteKwlPatch()
	{
		boolean ret = false;
		
		String filePath = "/sys/module/wakelock/parameters/default_stats";
    	try
    	{
			FileReader fr = new FileReader(filePath);
			BufferedReader br = new BufferedReader(fr);

			// read first line
			String currentRecord = br.readLine();
			
			if (!currentRecord.equals("0"))
			{
				ret = true;
			}
			br.close();
    	}
    	catch (Exception e)
    	{
    		
    	}
		return ret;
	}
	
    public static boolean hasWakelocks(Context context)
    {
    	boolean myRet = false;
       	String filePath = "/sys/power/wake_lock";
		ArrayList<String> res = Util.run("su", "cat " + filePath);
		// Format: 0 /sys/power/wake_lock
		final ArrayList<String> values = new ArrayList<String>();
		if (res.size() != 0)
		{
			String line = res.get(0);
			try
			{
				myRet = line.contains("PowerManagerService");
				if (myRet)
				{
					Log.i(TAG, "Detected userland wakelock in line " + line);
				}
				else
				{
					Log.i(TAG, "No userland wakelock detected in line " + line);
				}
			}
			catch (Exception e)
			{
				// something went wrong
				Log.e(TAG, "Exeception processsing " + filePath + ": " + e.getMessage());
				myRet = false;
			}
		}
    	return myRet;
    }

}