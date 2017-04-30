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

import com.asksven.android.common.dto.ProcessDto;
import com.asksven.android.common.dto.WakelockDto;
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
public class Wakelock extends StatElement implements Comparable<Wakelock>, Serializable
{	
	/** 
	 * the tag for logging
	 */
	private static transient final String TAG = "Wakelock";

	/**
	 * the wakelock type
	 */
	@SerializedName("wake_type")
	private int m_wakeType;
	
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
	 * Creates a wakelock instance
	 * @param wakeType the type of wakelock (partial or full)
	 * @param name the speaking name
	 * @param duration the duration the wakelock was held
	 * @param time the battery realtime 
	 * @param count the number of time the wakelock was active
	 */

	public Wakelock(int wakeType, String name, long duration, long time, int count)
	{
		m_wakeType	= wakeType;
		m_name		= StringUtils.maskAccountInfo(name);


		m_duration	= duration;
		setTotal(time);
		m_count		= count;
	}

	public Wakelock(WakelockDto source)
	{
		
		this.setUid(source.m_uid);
		this.m_name 		= source.m_name;
		this.m_count 		= source.m_count;
		this.m_duration		= source.m_duration;
		this.m_wakeType		= source.m_wakeType;
		this.setTotal(source.m_total);
	}

	public WakelockDto toDto()
	{
		WakelockDto ret = new WakelockDto();
		ret.m_uid			= this.getuid();
		ret.m_name 			= this.m_name;
		ret.m_count 		= this.m_count;
		ret.m_duration 		= this.m_duration;
		ret.m_wakeType		= this.m_wakeType;
		ret.m_total		 	= this.getTotal();

	
		return ret;
	}

	public Wakelock clone()
	{
		Wakelock clone = new Wakelock(m_wakeType, m_name, m_duration, getTotal(), m_count);
		
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
					Wakelock myRef = (Wakelock) myList.get(i);
					if ( (this.getName().equals(myRef.getName())) && (this.getuid() == myRef.getuid()) )
					{
						Log.i(TAG, "Substraction " + myRef.toString() + " from " + this.toString());
						this.m_duration	-= myRef.getDuration();
						this.setTotal( getTotal() - myRef.getTotal());
						this.m_count	-= myRef.getCount();
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

	/**
	 * @return the wakeType
	 */
	public int getWakeType() {
		return m_wakeType;
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
	 * @return the count
	 */
	public int getCount() {
		return m_count;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Wakelock [m_wakeType=" + m_wakeType + ", m_name=" + m_name
				+ ", m_duration=" + m_duration + ", m_total=" + getTotal() + "]";
	}
	
	 /**
     * Compare a given Wakelock with this object.
     * If the duration of this object is 
     * greater than the received object,
     * then this object is greater than the other.
     */
	public int compareTo(Wakelock o)
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
			+ " Count:" + getCount()
			+ " " + this.formatRatio(getDuration(), totalTime);
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
	
	public static class WakelockCountComparator implements Comparator<Wakelock>
	{
		public int compare(Wakelock a, Wakelock b)
		{
			return ((int)(b.getCount() - a.getCount()));
		}
	}
	
	public static class WakelockTimeComparator implements Comparator<Wakelock>
	{
		public int compare(Wakelock a, Wakelock b)
		{
			return ((int)(b.getDuration() - a.getDuration()));
		}
	}
	
	public Drawable getIcon(UidNameResolver resolver)
	{
		if (m_icon == null)
		{
			// retrieve and store the icon for that package
			if (m_uidInfo != null)
			{
				String myPackage = m_uidInfo.getNamePackage();
				m_icon = resolver.getIcon(myPackage);
			}
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



}
