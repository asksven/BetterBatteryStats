/**
 * 
 */
package com.asksven.android.common.kernelutils;

import android.os.SystemClock;
import android.util.Log;

import com.asksven.android.common.NonRootShell;

import com.asksven.android.common.privateapiproxies.Misc;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.android.common.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import com.asksven.android.contrib.Shell;

/**
 * Parses the content of 'dumpsys battery'
 * @author sven
 */
public class OtherStatsDumpsys
{
	static final String TAG = "OtherStatsDumpsys";
	static final String PERMISSION_DENIED = "rights required to access stats are not available / were not granted";

	/**
	 * Returns a list of alarm value objects
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<StatElement> getOtherStats(boolean showWifi, boolean showBt)
	{
		final String START_PATTERN = "Statistics since last charge";
		final String STOP_PATTERN = "Statistics since last unplugged";
		
		ArrayList<StatElement> myOther = null;
		long nTotalCount = 0;
		List<String> res = null;
		boolean useRoot = false;

        res = NonRootShell.getInstance().run("dumpsys batterystats");

		//List<String> res = getTestData();
		
		if ((res != null) && (res.size() != 0))

		{
			if (res.contains("Permission Denial"))
			{
				Pattern begin = Pattern.compile(START_PATTERN);
				Pattern end = Pattern.compile(STOP_PATTERN);
				
				boolean bParsing = false;

				// we are looking for single line entries in the format
				//  Screen on: 29s 297ms (99.7%), Input events: 0, Active phone call: 0ms (0.0%)
				//  Screen brightnesses: dark 29s 297ms (100.0%)
				//  Signal levels: none 21s 595ms (73.5%) 0x, poor 4s 447ms (15.1%) 3x, moderate 3s 295ms (11.2%) 1x, good 36ms (0.1%) 1x
				//  Radio types: none 22s 610ms (77.0%) 0x, hsdpa 4s 635ms (15.8%) 2x, other 2s 128ms (7.2%) 1x
				//  Wifi on: 0ms (0.0%), Wifi running: 0ms (0.0%), Bluetooth on: 0ms (0.0%)

				Pattern patternScreenOn	= Pattern.compile("\\s\\sScreen on:\\s(.*) \\(.*\\sActive phone call:\\s(.*)\\s\\(.*");
				Pattern patternWifiOn	= Pattern.compile("\\s\\sWifi on:\\s(.*) \\(.*\\sWifi running:\\s(.*)\\s\\(.*\\sBluetooth on:\\s(.*)\\s\\(.*");
				
				myOther = new ArrayList<StatElement>();
				Misc myMisc = null;
				
				// process the file
				long total = 0;
				for (int i=0; i < res.size(); i++)
				{
					// skip till start mark found
					if (bParsing)
					{
						// look for end
						Matcher endMatcher = end.matcher(res.get(i));
						if (endMatcher.find())
						{
							break;
						}
						
						// parse the alarms by block 
						String line = res.get(i);
						Matcher screenOnMatcher = patternScreenOn.matcher(line);
						Matcher wifiOnMatcher = patternWifiOn.matcher(line);
						
						// screen on line
						if ( screenOnMatcher.find() )
						{
							try
							{
								long durationScreenOn 	= DateUtils.durationToLong(screenOnMatcher.group(1));
								long durationInCall 	= DateUtils.durationToLong(screenOnMatcher.group(2));
																
								myMisc = new Misc("Screen On", durationScreenOn, SystemClock.elapsedRealtime());
								myOther.add(myMisc);

								myMisc = new Misc("Phone On", durationInCall, SystemClock.elapsedRealtime());
								myOther.add(myMisc);

								Log.i(TAG, "Adding partial wakelock: " + myMisc.toString());
							}
							catch (Exception e)
							{
								Log.e(TAG, "Error: parsing error in package line (" + line + ")");
							}
						}
						
						// phone on line
						if ( wifiOnMatcher.find() )
						{
							try
							{
								long durationWifiOn 		= DateUtils.durationToLong(wifiOnMatcher.group(1));
								long durationWifiRunning 	= DateUtils.durationToLong(wifiOnMatcher.group(2));
								long durationBtRunning 		= DateUtils.durationToLong(wifiOnMatcher.group(3));
								

								if (showWifi)
								{
									myMisc = new Misc("Wifi On", durationWifiOn, SystemClock.elapsedRealtime());
									myOther.add(myMisc);
	
									myMisc = new Misc("Wifi Running", durationWifiRunning, SystemClock.elapsedRealtime());
									myOther.add(myMisc);
								}
								
								if (showBt)
								{
									myMisc = new Misc("Bluetooth On", durationBtRunning, SystemClock.elapsedRealtime());
									myOther.add(myMisc);
								}
								Log.i(TAG, "Adding partial wakelock: " + myMisc.toString());
							}
							catch (Exception e)
							{
								Log.e(TAG, "Error: parsing error in package line (" + line + ")");
							}
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
				
				// set the total
				for (int i=0; i < myOther.size(); i++)
				{
					myOther.get(i).setTotal(total);
				}
			}
			else
			{
				myOther = new ArrayList<StatElement>();
				Misc myWl = new Misc(PERMISSION_DENIED, 1, 1);
				myOther.add(myWl);
			}
		}
		else
		{
			myOther = new ArrayList<StatElement>();
			Misc myWl = new Misc(PERMISSION_DENIED, 1, 1);
			myOther.add(myWl);

		}
		
		return myOther;
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
}
