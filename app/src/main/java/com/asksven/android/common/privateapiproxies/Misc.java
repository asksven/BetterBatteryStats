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
import java.util.List;

import com.asksven.android.common.dto.MiscDto;
import com.asksven.android.common.nameutils.UidInfo;
import com.google.gson.annotations.SerializedName;

import android.util.Log;

/**
 * @author sven
 *
 */
public class Misc extends StatElement implements Comparable<Misc>, Serializable
{
	
	/** 
	 * the tag for logging
	 */
	private static transient final String TAG = "Misc";
	
	/**
	 * the name of the object
	 */
	@SerializedName("name")
	private String m_name;
	
	/**
	 * the time on in ms
	 */
	@SerializedName("time_on_ms")
	private long m_timeOn;
	
	/**
	 * the time running in ms
	 */
	@SerializedName("time_runing_ms")
	private long m_timeRunning;

	/**
	 * Constructor
	 * @param name
	 * @param timeOn
	 * @param ratio
	 */
	public Misc(String name, long timeOn, long timeRunning)
	{

		m_name			= name;
		m_timeOn		= timeOn;
		m_timeRunning	= timeRunning;
	}
	
	public Misc(MiscDto source)
	{
		
		this.setUid(source.m_uid);
		this.m_name 		= source.m_name;
		this.m_timeOn 		= source.m_timeOn;
		this.m_timeRunning	= source.m_timeRunning;
		this.setTotal(source.m_total);
	}

	public MiscDto toDto()
	{
		MiscDto ret = new MiscDto();
		ret.m_uid			= this.getuid();
		ret.m_timeOn 		= this.m_timeOn;
		ret.m_timeRunning 	= this.m_timeRunning;
		ret.m_total		 	= this.getTotal();
		ret.m_name			= this.m_name;
	
		return ret;
	}

	public Misc clone()
	{
		Misc clone = new Misc(m_name, m_timeOn, m_timeRunning);
		clone.setTotal(this.getTotal());
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
					Misc myRef = (Misc) myList.get(i);
					if ( (this.getName().equals(myRef.getName())) && (this.getuid() == myRef.getuid()) )
					{
//						Log.d(TAG, "Substracting " + myRef.getName() + " " + myRef.getVals()
//								+ " from " + this.getName() + " " + this.getVals());
						
						this.m_timeOn		-= myRef.getTimeOn();
						this.m_timeRunning	-= myRef.getTimeRunning();
						this.setTotal(this.m_timeRunning);
						
						if (this.m_timeOn > this.m_timeRunning)
						{
							Log.i(TAG, "Fixed rounding difference: " + this.m_timeOn + " -> " + this.m_timeRunning);
							this.m_timeOn = this.m_timeRunning;
						}

						if ((m_timeOn < 0) || (m_timeRunning < 0))
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
	 * @return the time on
	 */
	public long getTimeOn()
	{
		return m_timeOn;
	}

	/**
	 * @return the time running
	 */
	public long getTimeRunning()
	{
		return m_timeRunning;
	}

	/**
     * Compare a given Wakelock with this object.
     * If the duration of this object is 
     * greater than the received object,
     * then this object is greater than the other.
     */
	public int compareTo(Misc o)
	{
		// we want to sort in descending order
		return (int)( o.getTimeOn() - this.getTimeOn());
	}

	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Misc [m_name=" + m_name + ", m_timeOn=" + formatDuration(getTimeOn())
				+ ", m_timeRunning=" + formatDuration(getTimeRunning()) + "]";
	}

	/**
	 * returns a string representation of the data
	 */
	public String getData(long totalTime)
	{
		
		if (totalTime < getTimeOn())
		{
			totalTime = getTimeOn();
		}
		return this.formatDuration(getTimeOn())
		+ " " + formatRatio(getTimeOn(), totalTime);
	}

	/**
	 * returns a string representation of the data
	 */
	public String getVals()
	{
		
		return m_name + " " + this.formatDuration(getTimeOn()) + " (" + getTimeOn()/1000 + " s)"
				+ " in " + this.formatDuration(getTimeRunning()) + " (" + getTimeRunning()/1000 + " s)"
		+ " Ratio: " + formatRatio(getTimeOn(), getTimeRunning());
	}

	/** 
	 * returns the values of the data
	 */	
	public double[] getValues()
	{
		double[] retVal = new double[2];
		retVal[0] = getTimeOn();
		return retVal;
	}
	
	public double getMaxValue()
	{
		return getTimeOn();            
    }
	
}

