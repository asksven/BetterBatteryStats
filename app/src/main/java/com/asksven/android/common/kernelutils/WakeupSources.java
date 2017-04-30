/**
 * 
 */
package com.asksven.android.common.kernelutils;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.asksven.android.contrib.Shell;
import com.asksven.android.common.CommonLogSettings;
import com.asksven.android.common.privateapiproxies.NativeKernelWakelock;
import com.asksven.android.common.privateapiproxies.StatElement;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;


/**
 * @author sven
 * Parser for wakeup_sources
 * 
 ********************************** 
 * From pm_wakeup.h
 * struct wakeup_source - Representation of wakeup sources
 *
 * @total_time: Total time this wakeup source has been active.
 * @max_time: Maximum time this wakeup source has been continuously active.
 * @last_time: Monotonic clock when the wakeup source's was touched last time.
 * @prevent_sleep_time: Total time this source has been preventing autosleep.
 * @event_count: Number of signaled wakeup events.
 * @active_count: Number of times the wakeup sorce was activated.
 * @relax_count: Number of times the wakeup sorce was deactivated.
 * @expire_count: Number of times the wakeup source's timeout has expired.
 * @wakeup_count: Number of times the wakeup source might abort suspend.
 * @active: Status of the wakeup source.
 * @has_timeout: The wakeup source has been activated with a timeout.
 *	struct wakeup_source {
 *		const char 		*name;
 *		struct list_head	entry;
 *		spinlock_t		lock;
 *		struct timer_list	timer;
 *		unsigned long		timer_expires;
 *		ktime_t total_time;
 *		ktime_t max_time;
 *		ktime_t last_time;
 *		ktime_t start_prevent_time;
 *		ktime_t prevent_sleep_time;
 *		unsigned long		event_count;
 *		unsigned long		active_count;
 *		unsigned long		relax_count;
 *		unsigned long		expire_count;
 *		unsigned long		wakeup_count;
 *		bool			active:1;
 *		bool			autosleep_enabled:1;
 *	};
 *
 */
public class WakeupSources extends Wakelocks
{
    private final static String TAG ="WakeupSources";
    private static String FILE_PATH = "/sys/kernel/debug/wakeup_sources";
    //private static String FILE_PATH = "/sdcard/wakeup_sources.txt";
    
    public static ArrayList<StatElement> parseWakeupSources(Context context)
    {
    	Log.i(TAG, "Parsing " + FILE_PATH);
       	
    	String delimiter = String.valueOf('\t');
    	delimiter = delimiter + "+";
    	ArrayList<StatElement> myRet = new ArrayList<StatElement>();
    	// format 
    	// new [name	active_count	event_count		wakeup_count	expire_count	active_since	total_time	max_time	last_change	prevent_suspend_time]
    	ArrayList<String[]> rows = parseDelimitedFile(FILE_PATH, delimiter);

    	long msSinceBoot = SystemClock.elapsedRealtime();

		// list the running processes
		ActivityManager actvityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> procInfos = actvityManager.getRunningAppProcesses();

    	// start with 1
    	for (int i=1; i < rows.size(); i++ )
    	{
    		try
    		{
    			// times in file are milliseconds
    			String[] data = (String[]) rows.get(i);
    			String name = data[0].trim(); 						// name
    			int count = Integer.valueOf(data[1]);				// active_count
    			int expire_count = Integer.valueOf(data[4]);		// expire_count
    			int wake_count = Integer.valueOf(data[3]);			// wakeup_count
    			long active_since = Long.valueOf(data[5]);			// active_since
    			long total_time = Long.valueOf(data[6]);			// total_time
    			long sleep_time = Long.valueOf(data[9]);			// prevent_suspend_time
    			long max_time = Long.valueOf(data[7]);				// max_time
    			long last_change = Long.valueOf(data[8]);			// last_change
    			
				// post-processing of eventX-YYYY processes
				String details = "";

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
            	if (CommonLogSettings.DEBUG)
            	{
	            	Log.d(TAG, "Native Kernel wakelock parsed"  
	            		+ " name=" + name
	            		+ " details=" + details
	            		+ " count=" + count
	            		+ " expire_count=" + expire_count
	            		+ " wake_count=" + wake_count
    					+ " active_since=" + active_since
    					+ " total_time=" + total_time
    					+ " sleep_time=" + sleep_time
    					+ " max_time=" + max_time
    					+ "last_change=" + last_change
    					+ "ms_since_boot=" + msSinceBoot);
            	}
            	NativeKernelWakelock wl = null;
            	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            	{
            		// on L sleep time is always 0 so use total time instead
            		wl = new NativeKernelWakelock(
	    					name, details, count, expire_count, wake_count,
	    					active_since, total_time, total_time, max_time,
	    					last_change, msSinceBoot);
            	}
            	else
            	{
	    			wl = new NativeKernelWakelock(
	    					name, details, count, expire_count, wake_count,
	    					active_since, total_time, sleep_time, max_time,
	    					last_change, msSinceBoot);
            	}
    			myRet.add(wl);
    		}
    		catch (Exception e)
    		{
    			// go on
    		}
    	}
    	return myRet;
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
    		List<String> res = Shell.SU.run("cat " + FILE_PATH);
    		
    		if ((res == null)||(res.size()==0))
    		{
    			exists = false;
    		}
    		else
    		{
    			exists = true;
    		}
    	}
    	finally
    	{
    		if (fr != null && exists)
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
}