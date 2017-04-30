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
package com.asksven.android.common.utils;

import java.util.Formatter;

import android.location.Location;

/**
 * A collection of math functions
 * @author sven
 *
 */
public class MathUtils
{
	/**
	 * calculate great circle distance in meters
	 * @param pos1 a Location object
	 * @param pos2 a Location object
	 * @return the distance between the two locations in meters 
	 */
	public static double getDistanceGreatCircle(Location pos1, Location pos2)
	{
		double lat1 	= pos1.getLatitude();
		double long1 	= pos1.getLongitude();
		double lat2 	= pos2.getLatitude();
		double long2 	= pos2.getLongitude();

	    double earthRadius = 3958.75;
	    double dLat = Math.toRadians(lat2-lat1);
	    double dLng = Math.toRadians(long2-long1);
	    double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
	               Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
	               Math.sin(dLng/2) * Math.sin(dLng/2);
	    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
	    double dist = earthRadius * c;

	    int meterConversion = 1609;

	    return dist * meterConversion;
	}
	
	public static String formatRatio(long num, long den)
	{
		StringBuilder mFormatBuilder = new StringBuilder(8);
        if (den == 0L)
        {
            return "---%";
        }
        
	    Formatter mFormatter = new Formatter(mFormatBuilder);        
        float perc = ((float)num) / ((float)den) * 100;
        mFormatBuilder.setLength(0);
        mFormatter.format("%.1f%%", perc);
        mFormatter.close();
        return mFormatBuilder.toString();
    }

	public static String formatRatio(double num, double den)
	{
		StringBuilder mFormatBuilder = new StringBuilder(8);
        if (den == 0L)
        {
            return "---%";
        }
        
	    Formatter mFormatter = new Formatter(mFormatBuilder);        
        float perc = ((float)num) / ((float)den) * 100;
        mFormatBuilder.setLength(0);
        mFormatter.format("%.1f%%", perc);
        mFormatter.close();
        return mFormatBuilder.toString();
    }

}
