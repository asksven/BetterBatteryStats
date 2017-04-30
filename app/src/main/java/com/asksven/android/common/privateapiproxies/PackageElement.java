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

package com.asksven.android.common.privateapiproxies;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

import com.asksven.android.common.nameutils.UidInfo;
import com.asksven.android.common.nameutils.UidNameResolver;
import com.asksven.android.common.utils.StringUtils;
import com.google.gson.annotations.SerializedName;


//import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;

/**
 * @author sven
 *
 */
public class PackageElement extends StatElement implements Comparable<PackageElement>, Serializable
{	
	/** 
	 * the tag for logging
	 */
	private static transient final String TAG = "Package";

	/**
	 * the package name of the wakelock holder
	 */
	@SerializedName("package_name")
	private String m_packageName;

	/**
	 * the name of the wakelock holder
	 */
	@SerializedName("name")
	private String m_name;
	
	/**
	 * the duration in ms
	 */
	@SerializedName("duration_ms")
	private long m_duration;
	


	/**
	 * the count
	 */
	@SerializedName("count")
	private int m_count;

	/**
	 * the number of wakeups
	 */
	@SerializedName("wakeups")
	private long m_wakeups;

	/**
	 * the total data transfer
	 */
	@SerializedName("rxtx")
	private long m_rxtx;

	/**
	 * Creates a package instance
	 * @param name the speaking name
	 * @param duration the duration the wakelock was held
	 * @param time the battery realtime 
	 * @param count the number of time the wakelock was active
	 */
	public PackageElement(String packageName, String name, int uid, long duration, long time, int count, long wakeups, long rxtx)
	{
		m_packageName = packageName;
		m_name 		= name;
		m_duration	= duration;
		setTotal(time);
		m_count		= count;
		m_wakeups	= wakeups;
		m_rxtx		= rxtx;
		super.setUid(uid);
	}
	

	public PackageElement clone()
	{
		PackageElement clone = new PackageElement(m_packageName, m_name, getuid(), m_duration, getTotal(), m_count, m_wakeups, m_rxtx);
		
		// Overwrite name to avoid multiple hashes
		clone.m_name	= m_name;

		clone.m_icon = m_icon;
		clone.m_uidInfo = m_uidInfo;
		clone.setUid(getuid());

		return clone;
	}

	/**
	 * Substracts the values from a previous object
	 * found in myList from the current Process
	 * in order to obtain an object containing only the data since a referenc
	 * @param myList
	 */
	public void substractFromRef(List<StatElement> myList )
	{
		if (myList != null)
		{
			for (int i = 0; i < myList.size(); i++)
			{
				try
				{
					PackageElement myRef = (PackageElement) myList.get(i);
					if ( (this.getName().equals(myRef.getName())) && (this.getuid() == myRef.getuid()) )
					{
						Log.i(TAG, "Substraction " + myRef.toString() + " from " + this.toString());
						this.m_duration	-= myRef.getDuration();
						this.setTotal( getTotal() - myRef.getTotal());
						this.m_count	-= myRef.getCount();
						this.m_wakeups -= myRef.m_wakeups;
						this.m_rxtx -= myRef.m_rxtx;
						Log.i(TAG, "Result: " + this.toString());
						if ((m_count < 0) || (m_duration < 0) || (getTotal() < 0))
						{
							Log.e(TAG, "substractFromRef generated negative values (" + this.toString() + " - " + myRef.toString() + ")");
						}
						break;
					}
				}
				catch (ClassCastException e)
				{
					// just log as it is no error not to change the process
					// being substracted from to do nothing
					Log.e(TAG, "substractFromRef was called with a wrong list type");
				}
				
			}
		}
	}


	public void add(StatElement element)
	{
		if (element instanceof Wakelock)
		{
			m_duration += ((Wakelock) element).getDuration();
			m_count += ((Wakelock) element).getCount();
		}
		else if (element instanceof Alarm)
		{
			m_wakeups += ((Alarm) element).getWakeups();
		}
		else if (element instanceof NetworkUsage)
		{
			m_rxtx += ((NetworkUsage) element).getTotalBytes();
		}

		else
		{
			Log.d(TAG, "element "+ element.toString() +  " was not added.");
		}
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return m_name;
	}

	/**
	 * @return the duration
	 */
	public long getDuration() {
		return m_duration;
	}

	/**
	 * @return the number of wakeups
	 */
	public long getWakeups() {
		return m_wakeups;
	}

	/**
	 * @return the data volume
	 */
	public long getDataVolume() {
		return m_rxtx;
	}

	/**
	 * @return the count
	 */
	public int getCount() {
		return m_count;
	}
	
	 /**
     * Compare a given Wakelock with this object.
     * If the duration of this object is 
     * greater than the received object,
     * then this object is greater than the other.
     */
	public int compareTo(PackageElement o)
	{
		// we want to sort in descending order
		return ((int)(o.getDuration() - this.getDuration()));
	}
	
	/**
	 * returns a string representation of the data
	 */
	public String getData(long totalTime)
	{
		return this.formatDuration(getDuration()) 
			+ " Wakeups:" + m_wakeups
			+ " Data:" + formatVolume(m_rxtx)
			+ " " + this.formatRatio(getDuration(), totalTime);
	}

	/**
	 * Formats data volumes 
	 * @param bytes
	 * @return the formated string
	 */
	public static String formatVolume(double bytes)
	{
		String ret = "";
		
		double kB = Math.floor(bytes / 1024);
		double mB = Math.floor(bytes / 1024 / 1024);
		double gB = Math.floor(bytes / 1024 / 1024 / 1024);
		double tB = Math.floor(bytes / 1024 / 1024 / 1024 / 1024);
        
        if (tB > 0)
        {
            ret = tB + " TBytes";
        }
        else if ( gB > 0)
        {
            ret = gB + " GBytes";
        }
        else if ( mB > 0)
        {
            ret = mB + " MBytes";
        }
        else if ( kB > 0)
        {
            ret = kB + " KBytes";
        }
        else
        {
            ret = bytes + " Bytes";
        }
        return ret;
	}

	/** 
	 * returns the values of the data
	 */	
	public double[] getValues()
	{
		double[] retVal = new double[2];
		retVal[0] = getDuration();
		return retVal;
	}
	
	public static class WakelockCountComparator implements Comparator<PackageElement>
	{
		public int compare(PackageElement a, PackageElement b)
		{
			return ((int)(b.getCount() - a.getCount()));
		}
	}
	
	public static class WakelockTimeComparator implements Comparator<PackageElement>
	{
		public int compare(PackageElement a, PackageElement b)
		{
			return ((int)(b.getDuration() - a.getDuration()));
		}
	}
	
	public Drawable getIcon(UidNameResolver resolver)
	{
		if (m_icon == null)
		{
			// retrieve and store the icon for that package
				String myPackage = m_packageName;
				m_icon = resolver.getIcon(m_packageName);
		}
		return m_icon;
	}
	
	public String getPackageName()
	{
		if (m_uidInfo != null)
		{
			return m_uidInfo.getNamePackage();
		}
		else
		{
			return "";
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "PackageElement [m_name=" + m_name + ", m_packageName=" + m_packageName + ", m_uid=" + getuid() + ", m_duration=" + m_duration
				+ ", m_count=" + m_count + ", m_rxtx=" + m_rxtx + ", m_wakeups=" + m_wakeups  +"]";
	}



}
