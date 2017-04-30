/*
 * Copyright (C) 2011-2016 asksven
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
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import android.util.Log;

import com.asksven.android.common.nameutils.UidNameResolver;
import com.asksven.android.common.utils.DateUtils;
import com.google.gson.annotations.SerializedName;

/**
 * Value holder for alarm items
 * @author sven
 *
 */
public class SensorUsageItem implements Serializable
{
	/** 
	 * the tag for logging
	 */
	private static transient final String TAG = "SensorItem";

	@SerializedName("time")
	@JsonProperty("time")
	long m_nTime;
	
	@SerializedName("sensor")
	@JsonProperty("sensor")
	String m_strSensor;

	@SerializedName("handle")
	@JsonProperty("handle")
	int m_nHandle;

	public SensorUsageItem()
	{
		
	}

	/**
	 * Default cctor
	 * @param nCount
	 * @param strIntent
	 */
	@JsonIgnore
	public SensorUsageItem(long nTime, String strSensor, int nHandle)
	{
		m_nTime 	= nTime;
		m_strSensor = strSensor;
		m_nHandle	= nHandle;
	}

	public SensorUsageItem clone()
	{
		SensorUsageItem clone = new SensorUsageItem(m_nTime, m_strSensor, m_nHandle);
		return clone;
	}

	/**
	 * Returns the sensor name
	 * @return
	 */
	@JsonProperty("sensor")
	public String getSensor()
	{
		return m_strSensor;
	}
	
	/**
	 * Returns the time
	 * @return
	 */
	@JsonProperty("time")
	public long getTime()
	{
		return m_nTime;
	}

	/**
	 * Returns the handle
	 * @return
	 */
	@JsonProperty("handle")
	public int getHandle()
	{
		return m_nHandle;
	}

	
	/**
	 * Returns the data as a string
	 * @return
	 */
	@JsonIgnore
	public String getData()
	{
		return "Sensor: " + m_strSensor + ", Time: " + DateUtils.formatDuration(m_nTime);
	}
	/**
	 * Substracts the values from a previous object
	 * found in myList from the current Process
	 * in order to obtain an object containing only the data since a referenc
	 * @param myList
	 */
	public void substractFromRef(List<SensorUsageItem> myList )
	{
		if (myList != null)
		{
			for (int i = 0; i < myList.size(); i++)
			{
				try
				{
					SensorUsageItem myRef = (SensorUsageItem) myList.get(i);
					if (this.getSensor().equals(myRef.getSensor()))
					{
						// process main values
						this.m_nTime		-= myRef.getTime();
						Log.i(TAG, "Result: " + this.toString());
					}
				}
				catch (ClassCastException e)
				{
					// just log as it is no error not to change the process
					// being substracted from to do nothing
					Log.e(TAG, "SensorItem.substractFromRef was called with a wrong list type");
				}
			}
		}
	}
}
	
