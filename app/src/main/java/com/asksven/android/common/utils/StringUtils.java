/*
 * Copyright (C) 2011 asksven
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

package com.asksven.android.common.utils;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.provider.Settings;
import android.util.Log;

/**
 * @author sven
 * 
 */
public class StringUtils
{
	
	private static String TAG = "StringUtils";

	static Pattern emailPattern			= Pattern.compile("(.*/)([A-Za-z0-9._%-+]+)@([a-z0-9.-]+\\.[a-z]{2,4})(.*)");
	static Pattern accountnamePattern	= Pattern.compile("(.*\\{name\\=)(.*)(\\,.*)");
	
	public static final String formatRatio(long num, long den)
	{
		StringBuilder mFormatBuilder = new StringBuilder(8);
		if (den == 0L)
		{
			return "---%";
		}

		Formatter mFormatter = new Formatter(mFormatBuilder);
		float perc = ((float) num) / ((float) den) * 100;
		mFormatBuilder.setLength(0);
		mFormatter.format("%.1f%%", perc);
		mFormatter.close();
		return mFormatBuilder.toString();
	}

	public static String join(String[] array, String sep, boolean merge)
	{
		String ret = "";
		for (int i = 0; i < array.length; i++)
		{
			if (ret.equals(""))
			{
				ret = array[i];
			} else
			{
				if (merge)
				{
					// check if the string is alread present
					if (ret.indexOf(array[i]) == -1)
					{
						// add
						ret += sep + array[i];
					}
				} else
				{
					ret += sep + array[i];
				}
			}
		}
		return ret;
	}

	public static void splitLine(String line, ArrayList<String> outSplit)
	{
		outSplit.clear();
		final StringTokenizer t = new StringTokenizer(line, " \t\n\r\f:");
		while (t.hasMoreTokens())
		{
			outSplit.add(t.nextToken());
		}
	}

	public static void splitLine(String line, ArrayList<String> outSplit, String sep)
	{
		outSplit.clear();
		final StringTokenizer t = new StringTokenizer(line, sep);
		while (t.hasMoreTokens())
		{
			outSplit.add(t.nextToken());
		}
	}

	public static void parseLine(ArrayList<String> keys, ArrayList<String> values, HashMap<String, String> outParsed)
	{
		outParsed.clear();
		final int size = Math.min(keys.size(), values.size());
		for (int i = 0; i < size; i++)
		{
			outParsed.put(keys.get(i), values.get(i));
		}
	}

	public static int getParsedInt(HashMap<String, String> parsed, String key)
	{
		final String value = parsed.get(key);
		return value != null ? Integer.parseInt(value) : 0;
	}

	public static long getParsedLong(HashMap<String, String> parsed, String key)
	{
		final String value = parsed.get(key);
		return value != null ? Long.parseLong(value) : 0;
	}

	public static String stripLeadingAndTrailingQuotes(String str)
	{
		if (str == null)
		{
			return str;
		}
		
		if (str.startsWith("\""))
		{
			str = str.substring(1, str.length());
		}
		if (str.endsWith("\""))
		{
			str = str.substring(0, str.length() - 1);
		}
		return str;
	}
	
	public static String maskAccountInfo(String str)
	{
		String ret = str;
		
		String serial = ""; 

		try
		{
		    Class<?> c = Class.forName("android.os.SystemProperties");
		    Method get = c.getMethod("get", String.class);
		    serial = (String) get.invoke(c, "ro.serialno");
		}
		catch (Exception ignored)
		{
		}
		
		Matcher email		 	= emailPattern.matcher(str);
		if ( email.find() )
		{
			String strName = email.group(2);
			try
			{
				// generate some long noise
				byte[] bytesOfSerial = serial.getBytes("UTF-8");
				MessageDigest mdSha = MessageDigest.getInstance("SHA-256");
				byte[] theShaDigest = mdSha.digest(bytesOfSerial);
				StringBuffer sb = new StringBuffer();
		        for (int i = 0; i < theShaDigest.length; ++i)
		        {
		          sb.append(Integer.toHexString((theShaDigest[i] & 0xFF) | 0x100).substring(1,3));
		        }
		        serial = sb.toString();
				
				byte[] bytesOfMessage = strName.concat(serial).getBytes("UTF-8");
	
				MessageDigest md = MessageDigest.getInstance("MD5");
				byte[] thedigest = md.digest(bytesOfMessage);
				sb = new StringBuffer();
		        for (int i = 0; i < thedigest.length; ++i)
		        {
		          sb.append(Integer.toHexString((thedigest[i] & 0xFF) | 0x100).substring(1,3));
		        }
		        ret = email.group(1) + sb.toString() + "@" + email.group(3) + email.group(4); 
			}
			catch (Exception e)
			{
				Log.e(TAG, "An error occured: " + e.getMessage());
			}
		        
		}
		else
		{
			Matcher account		 	= accountnamePattern.matcher(str);
			if ( account.find() )
			{
				String strName = account.group(2);
				try
				{
					byte[] bytesOfMessage = strName.getBytes("UTF-8");
		
					MessageDigest md = MessageDigest.getInstance("MD5");
					byte[] thedigest = md.digest(bytesOfMessage);
					StringBuffer sb = new StringBuffer();
			        for (int i = 0; i < thedigest.length; ++i)
			        {
			          sb.append(Integer.toHexString((thedigest[i] & 0xFF) | 0x100).substring(1,3));
			        }
			        ret = account.group(1) +  sb.toString() + account.group(3); 
				}
				catch (Exception e)
				{
					Log.e(TAG, "An error occured: " + e.getMessage());
				}
			}
		}
		return ret;
	}

}
