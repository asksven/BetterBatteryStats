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
import com.google.gson.annotations.SerializedName;

import android.util.Log;


/**
 * @author sven
 *
 */
public class KernelWakelock extends StatElement implements Comparable<KernelWakelock>, Serializable
{	
	/** 
	 * the tag for logging
	 */
	private static transient final String TAG = "KernelWakelock";

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
	public KernelWakelock(String name, long duration, long time, int count)
	{
		m_name		= name;
		m_duration	= duration;
		setTotal(time);
		m_count		= count;
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
					KernelWakelock myRef = (KernelWakelock) myList.get(i);
					if ( (this.getName().equals(myRef.getName())) && (this.getuid() == myRef.getuid()) )
					{
						this.m_duration	-= myRef.getDuration();
						this.setTotal( this.getTotal() - myRef.getTotal());
						this.m_count	-= myRef.getCount();

						if ((m_count < 0) || (m_duration < 0) || (this.getTotal() < 0))
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
	 * @return the name
	 */
	public String getName()
	{
		return m_name;
	}

	/**
	 * @return the duration
	 */
	public long getDuration()
	{
		return m_duration;
	}

	/**
	 * @return the count
	 */
	public int getCount()
	{
		return m_count;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() 
	{
		return "Kernel Wakelock [m_name=" + m_name
				+ ", m_duration=" + m_duration
				+ ", m_count=" + m_count+ "]";
	}
	
	 /**
     * Compare a given Wakelock with this object.
     * If the duration of this object is 
     * greater than the received object,
     * then this object is greater than the other.
     */
	public int compareTo(KernelWakelock o)
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
			+ " (" + getDuration()/1000 + " s)"
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
	
	public static class CountComparator implements Comparator<KernelWakelock>
	{
		public int compare(KernelWakelock a, KernelWakelock b)
		{
			return ((int)(b.getCount() - a.getCount()));
		}
	}
	
	public static class TimeComparator implements Comparator<KernelWakelock>
	{
		public int compare(KernelWakelock a, KernelWakelock b)
		{
			return ((int)(b.getDuration() - a.getDuration()));
		}
	}

}
