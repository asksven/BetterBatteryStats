/**
 * 
 */
package com.asksven.android.common.kernelutils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.asksven.android.common.NonRootShell;
import com.asksven.android.common.privateapiproxies.Process;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.android.common.utils.DateUtils;
import com.asksven.android.common.utils.SysUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the content of 'dumpsys battery'
 * @author sven
 */
public class ProcessStatsDumpsys
{
	static final String TAG = "OtherStatsDumpsys";
	static final String PERMISSION_DENIED = "su rights required to access alarms are not available / were not granted";

	/**
	 * Returns a list of alarm value objects
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<StatElement> getProcesses(Context ctx)
	{
		ArrayList myProcesses = new ArrayList<Process>();
		
		// get the list of all installed packages
		PackageManager pm = ctx.getPackageManager();
		List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
//		List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_META_DATA);
		
		HashMap<String, Integer> xrefPackages = new HashMap<String, Integer>();
		
		for (int i=0; i < apps.size(); i++)
		{
			xrefPackages.put(apps.get(i).packageName, apps.get(i).uid);
		}
		
		List<String> res = null;
		
		res = NonRootShell.getInstance().run("dumpsys batterystats");

		HashMap<String, List<Process>> xrefUserNames = getProcesses(res);

		// go through the processes and set the proper uid
		Iterator<String> userNames = xrefUserNames.keySet().iterator();
		while (userNames.hasNext())
		{
			String userName = userNames.next();
			List<Process> procs = xrefUserNames.get(userName);
			int uid = -1;
			if (!userName.equals(""))
			{
				if (userName.startsWith("u0a"))
				{
					// resolve though xrefPackages
					uid = -1;
				}
				else
				{
					uid = Integer.valueOf(userName);
				}
			}
			
			for (int i=0; i < procs.size(); i++)
			{
				Process proc = procs.get(i);
				if (uid == -1)
				{
					String packageName = proc.getName();
					if ((packageName != null) && (xrefPackages != null))
					{
						try
						{
							Integer lookupUid = xrefPackages.get(packageName);
							if (lookupUid != null)
							{
								uid = lookupUid;
							}
							else
							{
								Log.d(TAG, "Package " + packageName + " was not found in xref");
							}
						}
						catch (Exception e)
						{
							Log.e(TAG, "An error occured when retrieving uid=" + uid + " for package=" + packageName);
						}
					}
				}
				proc.setUid(uid);
				myProcesses.add(proc);
			}
		}
		
		return myProcesses;
	}


	static ArrayList<String> getTestData()
	{
		ArrayList<String> myRet = new ArrayList<String>()
				{{
					add("Alarm Stats:");
					add("  All partial wake locks:");
					add("  Wake lock 1001 RILJ: 1h 8m 23s 575ms (930 times) realtime");
					add("  Wake lock 1013 AudioMix: 26m 33s 343ms (10 times) realtime");
					add("  Wake lock u0a203 android.media.MediaPlayer: 26m 20s 380ms (3 times) realtime");
					add("  Wake lock u0a203 pocketcasts_wake_lock: 26m 19s 956ms (3 times) realtime");
					add("  Wake lock u0a18 NlpCollectorWakeLock: 5m 1s 608ms (347 times) realtime");
					add("  Wake lock u0a18 NlpWakeLock: 1m 58s 440ms (1473 times) realtime");
					add("  Wake lock u0a18 Checkin Service: 1m 36s 820ms (47 times) realtime");
					add("  Wake lock u0a203 pocketcasts_update_wake_lock: 44s 69ms (5 times) realtime");
					add("  Wake lock 1000 ActivityManager-Launch: 27s 214ms (72 times) realtime");
					add("  Wake lock u0a18 WakefulIntentService[GCoreUlr-LocationReportingService]: 27s 108ms (11 times) realtime");
					add("  Wake lock u0a47 StartingAlertService: 23s 785ms (15 times) realtime");
					add("  Wake lock u0a59 *sync*/gmail-ls/com.google/sven.knispel@gmail.com: 17s 777ms (6 times) realtime");
					add("  Wake lock 1000 AlarmManager: 17s 235ms (193 times) realtime");
					add("  Wake lock u0a18 Icing: 14s 250ms (45 times) realtime");
					add("  Wake lock u0a18 GCM_CONN_ALARM: 13s 467ms (25 times) realtime");
					add("  Wake lock u0a18 ezk: 11s 653ms (136 times) realtime");
					add("  Wake lock u0a178 AlarmManager: 10s 671ms (162 times) realtime");

				}};

		return myRet;
	}
	
	protected static HashMap<String, List<Process>> getProcesses(List<String> res)
	{
		HashMap<String, List<Process>> xref = new HashMap<String, List<Process>>();
				
		final String START_PATTERN = "Statistics since last charge";
		final String STOP_PATTERN = "Statistics since last unplugged";
		
		if ((res != null) && (res.size() != 0))

		{
			Pattern begin = Pattern.compile(START_PATTERN);
			Pattern end = Pattern.compile(STOP_PATTERN);
			
			boolean bParsing = false;

			Pattern patternUser		= Pattern.compile("\\s\\s((u0a)?\\d+):");
			Pattern patternProcess	= Pattern.compile("\\s\\s\\s\\sProc\\s(.*):");
			Pattern patternCpu		= Pattern.compile("\\s\\s\\s\\s\\s\\sCPU:\\s(.*) usr \\+ (.*) krn.*");
			Pattern patternStarts	= Pattern.compile("\\s\\s\\s\\s\\s\\s(\\d+) proc starts");

			String user = "";
			String process = "";
			long userCpu = 0;
			long systemCpu = 0;
			int starts = 0;
			
			for (int i=0; i < res.size(); i++)
			{
				// skip till start mark found
				if (bParsing)
				{

					// look for end
					Matcher endMatcher = end.matcher(res.get(i));
					if (endMatcher.find())
					{
						// add whatever was not saved yet
						if (!user.equals("") && !process.equals(""))
						{
							Process myProc = new Process(process, userCpu, systemCpu, starts);
							List<Process> myList = xref.get(user);
							if (myList == null)
							{
								myList = new ArrayList<Process>();
								xref.put(user, myList);
							}
								myList.add(myProc);
						}

						break;
					}

					String line = res.get(i);
					Matcher mUser 		= patternUser.matcher(line);
					Matcher mProcess 	= patternProcess.matcher(line);
					Matcher mCpu 		= patternCpu.matcher(line);
					Matcher mStarts 	= patternStarts.matcher(line);
					
					if ( mUser.find() )
					{
						// check if we had detected something previously
						if (!user.equals("") && !process.equals(""))
						{
							Process myProc = new Process(process, userCpu, systemCpu, starts);
							List<Process> myList = xref.get(user);
							if (myList == null)
							{
								myList = new ArrayList<Process>();
								xref.put(user, myList);
							}
								myList.add(myProc);
						}
						user = mUser.group(1);
						process = "";
						userCpu = 0;
						systemCpu = 0;
						starts = 0;

					}
					if ( mProcess.find() )
					{
						// check if we had detected something previously
						if (!user.equals("") && !process.equals(""))
						{
							Process myProc = new Process(process, userCpu, systemCpu, starts);
							List<Process> myList = xref.get(user);
							if (myList == null)
							{
								myList = new ArrayList<Process>();
								xref.put(user, myList);
							}
								myList.add(myProc);
						}
						process = mProcess.group(1);
						userCpu = 0;
						systemCpu = 0;
						starts = 0;

					}
					if ( mCpu.find() )
					{
						userCpu = DateUtils.durationToLong(mCpu.group(1));
						systemCpu = DateUtils.durationToLong(mCpu.group(2));

					}
					if ( mStarts.find() )
					{
						starts = Integer.valueOf(mStarts.group(1));
					}
					
				}
				else
				{
					// look for beginning
					Matcher line = begin.matcher(res.get(i));
					if (line.find())
					{
						bParsing = true;
					}
				}
					
			}
		}
		
		return xref;
	}

}
