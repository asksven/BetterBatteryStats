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
package com.asksven.android.common.kernelutils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Build;
import android.util.Log;


import com.asksven.android.common.CommonLogSettings;
import com.asksven.android.common.NonRootShell;
import com.asksven.android.common.privateapiproxies.Alarm;
import com.asksven.android.common.privateapiproxies.StatElement;

/**
 * Parses the content of 'dumpsys alarm'
 * processes the result of 'dumpsys alarm' as explained in KB article
 * https://github.com/asksven/BetterBatteryStats-Knowledge-Base/wiki/AlarmManager
 * @author sven
 */
public class AlarmsDumpsys
{
	static final String TAG = "AlarmsDumpsys";
	static final String PERMISSION_DENIED = "rights required to access stats are not available / were not granted";
	static final String SERVICE_NOT_ACCESSIBLE = "Can't find service: alarm";

	public static ArrayList<StatElement> getAlarms()
	{
		String release = Build.VERSION.RELEASE;
		int sdk = Build.VERSION.SDK_INT;
		Log.i(TAG, "getAlarms: SDK=" + sdk + ", RELEASE=" + release);
		
		List<String> res = null;

		res = NonRootShell.getInstance().run("dumpsys alarm");

		if (sdk < 17) // Build.VERSION_CODES.JELLY_BEAN_MR1)
		{
			return getAlarmsPriorTo_4_2_2(res);
		}
		else if (sdk == Build.VERSION_CODES.JELLY_BEAN_MR1)
		{
			if (release.equals("4.2.2"))
			{
				return getAlarmsFrom_4_2_2(res);
			}
			else
			{
				return getAlarmsPriorTo_4_2_2(res);
			}
		}

		else if (sdk <= 19)
		{
			return getAlarmsFrom_4_3(res);
		}
		else if (sdk < 23 )
		{
			return getAlarmsFrom_5(res);
		}
		else
		{
			return getAlarmsFrom_6(res);
		}
	}
	/**
	 * Returns a list of alarm value objects
	 * @return
	 * @throws Exception
	 */
	protected static ArrayList<StatElement> getAlarmsPriorTo_4_2_2(List<String> res)
	{
		ArrayList<StatElement> myAlarms = null;
		long nTotalCount = 0;

//		if (res.getSuccess())
		if ((res != null) && (res.size() != 0))

		{
//			String strRes = res.getResultLine(); 
			if (!res.contains("Permission Denial"))
			{
				Pattern begin = Pattern.compile("Alarm Stats");
				boolean bParsing = false;
//				ArrayList<String> myRes = res.getResult(); // getTestData();

				// we are looking for multiline entries in the format
				// ' <package name>
				// '  <time> ms running, <number> wakeups
				// '  <number> alarms: act=<intent name> flg=<flag> (repeating 1..n times)
				Pattern packagePattern 	= Pattern.compile("\\s\\s([a-z][a-zA-Z0-9\\.]+)");
				Pattern timePattern 	= Pattern.compile("\\s\\s(\\d+)ms running, (\\d+) wakeups");
				Pattern numberPattern	= Pattern.compile("\\s\\s(\\d+) alarms: (flg=[a-z0-9]+\\s){0,1}(act|cmp)=([A-Za-z0-9\\-\\_\\.\\{\\}\\/\\{\\}\\$]+)");
				
				myAlarms = new ArrayList<StatElement>();
				Alarm myAlarm = null;
				
				// process the file
				for (int i=0; i < res.size(); i++)
				{
					// skip till start mark found
					if (bParsing)
					{
						// parse the alarms by block 
						String line = res.get(i);
						Matcher mPackage 	= packagePattern.matcher(line);
						Matcher mTime 		= timePattern.matcher(line);
						Matcher mNumber 	= numberPattern.matcher(line);
						
						// first line
						if ( mPackage.find() )
						{
							try
							{
								// if there was a previous Alarm populated store it
								if (myAlarm != null)
								{
									myAlarms.add(myAlarm);
								}
								// we are interested in the first token 
								String strPackageName = mPackage.group(1);
								myAlarm = new Alarm(strPackageName);
							}
							catch (Exception e)
							{
								Log.e(TAG, "Error: parsing error in package line (" + line + ")");
							}
						}

						// second line
						if ( mTime.find() )
						{
							try
							{
								// we are interested in the second token
								String strWakeups = mTime.group(2);
								long nWakeups = Long.parseLong(strWakeups);
	
								if (myAlarm == null)
								{
									Log.e(TAG, "Error: time line found but without alarm object (" + line + ")");
								}
								else
								{
									myAlarm.setWakeups(nWakeups);
									nTotalCount += nWakeups;
								}
							}
							catch (Exception e)
							{
								Log.e(TAG, "Error: parsing error in time line (" + line + ")");
							}
						}

						// third line (and following till next package
						if ( mNumber.find() )
						{
							try
							{
								// we are interested in the first and second token
								String strNumber = mNumber.group(1);
								String strIntent = mNumber.group(4);
								long nNumber = Long.parseLong(strNumber);
	
								if (myAlarm == null)
								{
									Log.e(TAG, "Error: number line found but without alarm object (" + line + ")");
								}
								else
								{
									myAlarm.addItem(nNumber, strIntent);
								}
							}
							catch (Exception e)
							{
								Log.e(TAG, "Error: parsing error in number line (" + line + ")");
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
				// the last populated alarms has not been added to the list yet
				myAlarms.add(myAlarm);
				
			}
			else
			{
				myAlarms = new ArrayList<StatElement>();
				Alarm myAlarm = new Alarm(PERMISSION_DENIED);
				myAlarm.setWakeups(1);
				myAlarms.add(myAlarm);
			}
		}
		else
		{
			myAlarms = new ArrayList<StatElement>();
			Alarm myAlarm = new Alarm(PERMISSION_DENIED);
			myAlarm.setWakeups(1);
			myAlarms.add(myAlarm);

		}
		
		
		for (int i=0; i < myAlarms.size(); i++)
		{
			((Alarm)myAlarms.get(i)).setTotalCount(nTotalCount);
		}
		return myAlarms;
	}

	protected static ArrayList<StatElement> getAlarmsFrom_4_2_2(List<String> res)
	{
		ArrayList<StatElement> myAlarms = null;
		long nTotalCount = 0;
		
		if ((res != null) && (res.size() != 0))

		{
			Pattern begin = Pattern.compile("Alarm Stats");
			boolean bParsing = false;

			// we are looking for multiline entries in the format
			// ' <package name> +<time>ms running, <number> wakeups
			// '  +<time>ms <number> wakes <number> alarms: act=<intern> (repeating 1..n times)
			Pattern packagePattern 	= Pattern.compile("\\s\\s([a-z][a-zA-Z0-9\\.]+)\\s\\+(.*), (\\d+) wakeups:");
			Pattern numberPattern	= Pattern.compile("\\s\\s\\s\\s\\+([0-9a-z]+)ms (\\d+) wakes (\\d+) alarms: (act|cmp)=([A-Za-z0-9\\-\\_\\.\\$\\{\\}]+)");
			
			myAlarms = new ArrayList<StatElement>();
			Alarm myAlarm = null;
			
			// process the file
			for (int i=0; i < res.size(); i++)
			{
				// skip till start mark found
				if (bParsing)
				{
					// parse the alarms by block 
					String line = res.get(i);
					Matcher mPackage 	= packagePattern.matcher(line);
					Matcher mNumber 	= numberPattern.matcher(line);
					
					// first line
					if ( mPackage.find() )
					{
						try
						{
							// if there was a previous Alarm populated store it
							if (myAlarm != null)
							{
								myAlarms.add(myAlarm);
							}
							// we are interested in the first token 
							String strPackageName = mPackage.group(1);
							myAlarm = new Alarm(strPackageName);

							String strWakeups = mPackage.group(3);
							long nWakeups = Long.parseLong(strWakeups);
							myAlarm.setWakeups(nWakeups);
							nTotalCount += nWakeups;

						}
						catch (Exception e)
						{
							Log.e(TAG, "Error: parsing error in package line (" + line + ")");
						}
					}

					// second line (and following till next package)
					if ( mNumber.find() )
					{
						try
						{
							// we are interested in the first and second token
							String strNumber = mNumber.group(2);
							String strIntent = mNumber.group(5);
							long nNumber = Long.parseLong(strNumber);

							if (myAlarm == null)
							{
								Log.e(TAG, "Error: number line found but without alarm object (" + line + ")");
							}
							else
							{
								myAlarm.addItem(nNumber, strIntent);
							}
						}
						catch (Exception e)
						{
							Log.e(TAG, "Error: parsing error in number line (" + line + ")");
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
			// the last populated alarms has not been added to the list yet
			myAlarms.add(myAlarm);
			
		}
		else
		{
			myAlarms = new ArrayList<StatElement>();
			Alarm myAlarm = new Alarm(PERMISSION_DENIED);
			myAlarm.setWakeups(1);
			myAlarms.add(myAlarm);

		}
		
		
		for (int i=0; i < myAlarms.size(); i++)
		{
			Alarm myAlarm = (Alarm)myAlarms.get(i);
			if (myAlarm != null)
			{
				myAlarm.setTotalCount(nTotalCount);
			}
		}
		return myAlarms;
	}
	protected static ArrayList<StatElement> getAlarmsFrom_4_3(List<String> res)
	{
		ArrayList<StatElement> myAlarms = null;
		long nTotalCount = 0;
		
		if ((res != null) && (res.size() != 0))

		{
			Pattern begin = Pattern.compile("Alarm Stats");
			boolean bParsing = false;

			// we are looking for multiline entries in the format
			// ' <package name> +<time>ms running, <number> wakeups
			// '  +<time>ms <number> wakes <number> alarms: act=<intern> (repeating 1..n times)
			Pattern packagePattern 	= Pattern.compile("\\s\\s([a-z][a-zA-Z0-9\\.]+)\\s\\+(.*), (\\d+) wakeups:");
			Pattern numberPattern	= Pattern.compile("\\s\\s\\s\\s\\+([0-9a-z]+)ms (\\d+) wakes (\\d+) alarms: (act|cmp)=(.*)");
			
			myAlarms = new ArrayList<StatElement>();
			Alarm myAlarm = null;
			
			// process the file
			for (int i=0; i < res.size(); i++)
			{
				// skip till start mark found
				if (bParsing)
				{
					// parse the alarms by block 
					String line = res.get(i);
					Matcher mPackage 	= packagePattern.matcher(line);
					Matcher mNumber 	= numberPattern.matcher(line);
					
					// first line
					if ( mPackage.find() )
					{
						try
						{
							// if there was a previous Alarm populated store it
							if (myAlarm != null)
							{
								myAlarms.add(myAlarm);
							}
							// we are interested in the first token 
							String strPackageName = mPackage.group(1);
							myAlarm = new Alarm(strPackageName);

							String strWakeups = mPackage.group(3);
							long nWakeups = Long.parseLong(strWakeups);
							myAlarm.setWakeups(nWakeups);
							nTotalCount += nWakeups;

						}
						catch (Exception e)
						{
							Log.e(TAG, "Error: parsing error in package line (" + line + ")");
						}
					}

					// second line (and following till next package)
					if ( mNumber.find() )
					{
						try
						{
							// we are interested in the first and second token
							String strNumber = mNumber.group(2);
							String strIntent = mNumber.group(5);
							long nNumber = Long.parseLong(strNumber);

							if (myAlarm == null)
							{
								Log.e(TAG, "Error: number line found but without alarm object (" + line + ")");
							}
							else
							{
								myAlarm.addItem(nNumber, strIntent);
							}
						}
						catch (Exception e)
						{
							Log.e(TAG, "Error: parsing error in number line (" + line + ")");
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
			// the last populated alarms has not been added to the list yet
			myAlarms.add(myAlarm);
			
		}
		else
		{
			myAlarms = new ArrayList<StatElement>();
			Alarm myAlarm = new Alarm(PERMISSION_DENIED);
			myAlarm.setWakeups(1);
			myAlarms.add(myAlarm);

		}
		
		
		for (int i=0; i < myAlarms.size(); i++)
		{
			Alarm myAlarm = (Alarm)myAlarms.get(i);
			if (myAlarm != null)
			{
				myAlarm.setTotalCount(nTotalCount);
			}
		}
		return myAlarms;
	}

	protected static ArrayList<StatElement> getAlarmsFrom_5(List<String> res)
	{
		ArrayList<StatElement> myAlarms = null;
		long nTotalCount = 0;
		
		if ((res != null) && (res.size() != 0))

		{
			Pattern begin = Pattern.compile("Alarm Stats");
			boolean bParsing = false;

			// we are looking for multiline entries in the format
			// ' <package name> +<time>ms running, <number> wakeups
			// '  +<time>ms <number> wakes <number> alarms: act=<intern> (repeating 1..n times)
			Pattern packagePattern 	= Pattern.compile("\\s\\s.*:([a-z][a-zA-Z0-9\\.]+)\\s\\+(.*), (\\d+) wakeups:");
			Pattern numberPattern	= Pattern.compile("\\s\\s\\s\\s\\+([0-9a-z]+)ms (\\d+) wakes (\\d+) alarms: (\\*alarm\\*|\\*walarm\\*):(.*)");
			
			myAlarms = new ArrayList<StatElement>();
			Alarm myAlarm = null;
			
			// process the file
			for (int i=0; i < res.size(); i++)
			{
				// skip till start mark found
				if (bParsing)
				{
					// parse the alarms by block 
					String line = res.get(i);
					Matcher mPackage 	= packagePattern.matcher(line);
					Matcher mNumber 	= numberPattern.matcher(line);
					
					// first line
					if ( mPackage.find() )
					{
						try
						{
							// if there was a previous Alarm populated store it
							if (myAlarm != null)
							{
								myAlarms.add(myAlarm);
							}
							// we are interested in the first token 
							String strPackageName = mPackage.group(1);
							myAlarm = new Alarm(strPackageName);

							String strWakeups = mPackage.group(3);
							long nWakeups = Long.parseLong(strWakeups);
							myAlarm.setWakeups(nWakeups);
							nTotalCount += nWakeups;

						}
						catch (Exception e)
						{
							Log.e(TAG, "Error: parsing error in package line (" + line + ")");
						}
					}

					// second line (and following till next package)
					if ( mNumber.find() )
					{
						try
						{
							// we are interested in the first and second token
							String strNumber = mNumber.group(2);
							String strIntent = mNumber.group(5);
							long nNumber = Long.parseLong(strNumber);

							if (myAlarm == null)
							{
								Log.e(TAG, "Error: number line found but without alarm object (" + line + ")");
							}
							else
							{
								myAlarm.addItem(nNumber, strIntent);
							}
						}
						catch (Exception e)
						{
							Log.e(TAG, "Error: parsing error in number line (" + line + ")");
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
			// the last populated alarms has not been added to the list yet
			myAlarms.add(myAlarm);
			
		}
		else
		{
			myAlarms = new ArrayList<StatElement>();
			Alarm myAlarm = new Alarm(PERMISSION_DENIED);
			myAlarm.setWakeups(1);
			myAlarms.add(myAlarm);

		}
		
		
		for (int i=0; i < myAlarms.size(); i++)
		{
			Alarm myAlarm = (Alarm)myAlarms.get(i);
			if (myAlarm != null)
			{
				myAlarm.setTotalCount(nTotalCount);
			}
		}
		return myAlarms;
	}
	
	protected static ArrayList<StatElement> getAlarmsFrom_6(List<String> res)
	{
		ArrayList<StatElement> myAlarms = null;
		long nTotalCount = 0;
		
		if ((res != null) && (res.size() != 0))

		{
			Pattern begin = Pattern.compile("Alarm Stats");
			boolean bParsing = false;

			// we are looking for multiline entries in the format
			// ' <package name> +<time>ms running, <number> wakeups
			// '  +<time>ms <number> wakes <number> alarms: act=<intern> (repeating 1..n times)
			Pattern packagePattern 	= Pattern.compile("\\s\\s.*:([a-z][a-zA-Z0-9\\.]+)\\s\\+(.*), (\\d+) wakeups:");
			Pattern numberPattern	= Pattern.compile("\\s\\s\\s\\s\\+([0-9a-z]+)ms (\\d+) wakes (\\d+) alarms(.*)");
			Pattern detailsPattern	= Pattern.compile("\\s\\s\\s\\s\\s\\s(\\*alarm\\*|\\*walarm\\*):(.*)");
			
			myAlarms = new ArrayList<StatElement>();
			Alarm myAlarm = null;
			long nNumber = 0;
			
			// process the file
			for (int i=0; i < res.size(); i++)
			{
				// skip till start mark found
				if (bParsing)
				{
					// parse the alarms by block 
					String line = res.get(i);
					
					Matcher mPackage 	= packagePattern.matcher(line);
					Matcher mNumber 	= numberPattern.matcher(line);
					Matcher mDetails	= detailsPattern.matcher(line);
					
					// first line
					if ( mPackage.find() )
					{
						try
						{
							// if there was a previous Alarm populated store it
							if (myAlarm != null)
							{
								myAlarms.add(myAlarm);
							}
							// we are interested in the first token 
							String strPackageName = mPackage.group(1);
							myAlarm = new Alarm(strPackageName);

							String strWakeups = mPackage.group(3);
							long nWakeups = Long.parseLong(strWakeups);
							myAlarm.setWakeups(nWakeups);
							nTotalCount += nWakeups;

						}
						catch (Exception e)
						{
							Log.e(TAG, "Error: parsing error in package line (" + line + ")");
						}
					}

					// second line
					if ( mNumber.find() )
					{
						try
						{
							// we are interested in the first and second token
							String strNumber = mNumber.group(2);
							nNumber = Long.parseLong(strNumber);

							if (myAlarm == null)
							{
								Log.e(TAG, "Error: number line found but without alarm object (" + line + ")");
							}

						}
						catch (Exception e)
						{
							Log.e(TAG, "Error: parsing error in number line (" + line + ")");
						}
					}
					// third line
					if ( mDetails.find() )
					{
						try
						{
							// we are interested in the first and second token
							String strIntent = mDetails.group(2);
							
							if (myAlarm == null)
							{
								Log.e(TAG, "Error: number line found but without alarm object (" + line + ")");
							}
							else
							{
								if (CommonLogSettings.DEBUG)
								{
									Log.i(TAG, "Added: " + strIntent + "(" + nNumber + ")");
								}
								myAlarm.addItem(nNumber, strIntent);
							}
						}
						catch (Exception e)
						{
							Log.e(TAG, "Error: parsing error in number line (" + line + ")");
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
			// the last populated alarms has not been added to the list yet
			if (myAlarm != null)
			{
				myAlarms.add(myAlarm);
			}
			
		}
		else
		{
			myAlarms = new ArrayList<StatElement>();
			Alarm myAlarm = new Alarm(PERMISSION_DENIED);
			myAlarm.setWakeups(1);
			myAlarms.add(myAlarm);

		}
		
		
		for (int i=0; i < myAlarms.size(); i++)
		{
			Alarm myAlarm = (Alarm)myAlarms.get(i);
			if (myAlarm != null)
			{
				myAlarm.setTotalCount(nTotalCount);
			}
		}
		return myAlarms;
	}

}
