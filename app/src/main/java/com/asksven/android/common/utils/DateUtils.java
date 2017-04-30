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

/**
 * @author sven
 *
 */
import java.util.Calendar;
import java.util.Date;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

public class DateUtils
{
	public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
	public static final String DATE_FORMAT_SHORT = "HH:mm:ss";
	private static final Calendar m_cal = Calendar.getInstance();

	/**
	 * Returns the current date in the default format.
	 * @return the current formatted date/time
	 */
	public static String now()
	{
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
		long now = System.currentTimeMillis();
		return sdf.format(new Date(now));
	}
	
	/**
	 * Returns the current date in a given format.
	 * DateUtils.now("dd MMMMM yyyy")
     * DateUtils.now("yyyyMMdd")
     * DateUtils.now("dd.MM.yy")
     * DateUtils.now("MM/dd/yy")
	 * @param dateFormat a date format (See examples)
	 * @return the current formatted date/time
	 */
	public static String now(String dateFormat)
	{
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		long now = System.currentTimeMillis();
		return sdf.format(new Date(now));
	}
	
	public static String format(String dateFormat, Date time)
	{

		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		return sdf.format(time);
	}

	public static String format(String dateFormat, Long time)
	{

		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		return sdf.format(time);
	}

	public static String format(Date time)
	{
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
		return sdf.format(time);
	}

	public static String format(long timeMs)
	{
		return format(timeMs, DATE_FORMAT_NOW);
	}

	public static String formatShort(long timeMs)
	{
		return format(timeMs, DATE_FORMAT_SHORT);
	}

	public static String format(long timeMs, String format)
	{
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(timeMs);
	}

	/**
	 * Formats milliseconds to a friendly form 
	 * @param millis
	 * @return the formated string
	 */
	public static String formatDuration(long millis)
	{		
        int seconds = (int) Math.floor(millis / 1000);
        
        int days = 0, hours = 0, minutes = 0;
        if (seconds > (60*60*24)) {
            days = seconds / (60*60*24);
            seconds -= days * (60*60*24);
        }
        if (seconds > (60 * 60)) {
            hours = seconds / (60 * 60);
            seconds -= hours * (60 * 60);
        }
        if (seconds > 60) {
            minutes = seconds / 60;
            seconds -= minutes * 60;
        }

        // use StringBuilder for better performance
        StringBuilder builder = new StringBuilder();
        if (days > 0)
        {
            builder.append(days + " d ");
        }
        
        if (hours > 0)
        {
        	builder.append(hours + " h ");
        }
        
        if (minutes > 0)
        { 
        	builder.append(minutes + " m ");
        }
        if (seconds > 0)
        {
        	builder.append(seconds + " s ");
        }
        
        String ret = builder.toString();
        if (ret.equals(""))
        {
        	ret = "0 s";
        }
        return ret;
	}

	/**
	 * Formats milliseconds to a friendly form 
	 * @param millis
	 * @return the formated string
	 */
	public static String formatFrequency(long occurences, long duration_ms)
	{
		String ret = "";
		DecimalFormat f = new DecimalFormat("#0.0");
        int minutes = (int) Math.floor(duration_ms / 1000 / 60);
        
        double perMinute = ((double) occurences) / ((double) minutes);
        
        if (perMinute >= 1)
        {
        	// we keep this value
        	ret = f.format(perMinute) + " / min";
        }
        else
        {
        	// we try to switch to hours
        	ret = f.format(perMinute * 60) + " / h";
        }
        
        return ret;
	}

	/**
	 * Formats milliseconds to a friendly form. Short means that seconds are truncated if value > 1 Day 
	 * @param millis
	 * @return the formated string
	 */
	public static String formatDurationShort(long millis)
	{
		String ret = "";
		
        int seconds = (int) Math.floor(millis / 1000);
        
        int days = 0, hours = 0, minutes = 0;
        if (seconds > (60*60*24)) {
            days = seconds / (60*60*24);
            seconds -= days * (60*60*24);
        }
        if (seconds > (60 * 60)) {
            hours = seconds / (60 * 60);
            seconds -= hours * (60 * 60);
        }
        if (seconds > 60) {
            minutes = seconds / 60;
            seconds -= minutes * 60;
        }
        ret = "";
        if (days > 0)
        {
            ret += days + "d ";
        }
        
        if (hours > 0)
        {
        	ret += hours + "h ";
        }
        
        if (minutes > 0)
        { 
        	ret += minutes + "m ";
        }
        if ( (seconds > 0) && (days == 0) )
        {
        	// only show seconds when value < 1 day
        	ret += seconds + "s";
        }
        
        if (ret.equals(""))
        {
        	ret = "0s";
        }
        return ret;
	}

	/**
	 * Parses  string of the format 26m 33s 343ms and returns the number of ms
	 * @param duration
	 * @return
	 */
	public static long durationToLong(String duration)
	{
		long time = 0;
		
		String[] parts = duration.split(" ");
		for (int i=0; i < parts.length; i++)
		{
			if (parts[i].endsWith("ms"))
			{
				String val = parts[i].substring(0, parts[i].length()-2);
				long dur = Long.valueOf(val);
				time += dur * 1;
			}
			else if (parts[i].endsWith("s"))
			{
				String val = parts[i].substring(0, parts[i].length()-1);
				long dur = Long.valueOf(val);
				time += dur * 1000;
			}
			else if (parts[i].endsWith("m"))
			{
				String val = parts[i].substring(0, parts[i].length()-1);
				long dur = Long.valueOf(val);
				time += dur * 1000 * 60;
			}
			else if (parts[i].endsWith("h"))
			{
				String val = parts[i].substring(0, parts[i].length()-1);
				long dur = Long.valueOf(val);
				time += dur * 1000 * 60 * 60;
			}
			else if (parts[i].endsWith("d"))
			{
				String val = parts[i].substring(0, parts[i].length()-1);
				long dur = Long.valueOf(val);
				time += dur * 1000 * 60 * 60 * 24;
			}
		}
		
		
		return time;
	}
	/**
	 * Formats milliseconds to a friendly non abbreviated form (days, hrs, min, sec) 
	 * @param millis
	 * @return the formated string
	 */
	public static String formatDurationLong(long millis)
	{
		String ret = "";
		
        int seconds = (int) Math.floor(millis / 1000);
        
        int days = 0, hours = 0, minutes = 0;
        if (seconds > (60*60*24)) {
            days = seconds / (60*60*24);
            seconds -= days * (60*60*24);
        }
        if (seconds > (60 * 60)) {
            hours = seconds / (60 * 60);
            seconds -= hours * (60 * 60);
        }
        if (seconds > 60) {
            minutes = seconds / 60;
            seconds -= minutes * 60;
        }
        ret = "";
        if (days > 0)
        {
        	if (days <= 1)
        	{
        		ret += days + " day ";
        	}
        	else
        	{
        		ret += days + " days ";
        	}
        }
        
        if (hours > 0)
        {
        	if (hours <= 1)
        	{
        		ret += hours + " hr ";
        	}
        	else
        	{
        		ret += hours + " hrs ";
        	}
        }
        
        if (minutes > 0)
        { 
        	ret += minutes + " min ";
        }
        if ( (seconds > 0) && (days == 0) )
        {
        	// only show seconds when value < 1 day
        	ret += seconds + " s ";
        }
        
        if (ret.equals(""))
        {
        	ret = "0 s";
        }
        return ret;
	}
	/**
	 * Formats milliseconds to a friendly form. Compressed means that seconds are truncated if value > 1 Hour 
	 * @param millis
	 * @return the formated string
	 */
	public static String formatDurationCompressed(long millis)
	{
		String ret = "";
		
        int seconds = (int) Math.floor(millis / 1000);
        
        int days = 0, hours = 0, minutes = 0;
        if (seconds > (60*60*24)) {
            days = seconds / (60*60*24);
            seconds -= days * (60*60*24);
        }
        if (seconds > (60 * 60)) {
            hours = seconds / (60 * 60);
            seconds -= hours * (60 * 60);
        }
        if (seconds > 60) {
            minutes = seconds / 60;
            seconds -= minutes * 60;
        }
        ret = "";
        if (days > 0)
        {
            ret += days + "d";
        }
        
        if (hours > 0)
        {
        	ret += hours + "h";
        }
        
        if (minutes > 0)
        { 
        	ret += minutes + "m";
        }
        if ( (seconds > 0) && (hours == 0) )
        {
        	// only show seconds when value < 1 day
        	ret += seconds + "s";
        }
        
        if (ret.equals(""))
        {
        	ret = "0s";
        }
        return ret;
	}

}