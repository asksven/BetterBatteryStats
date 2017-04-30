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
import java.util.Comparator;
import java.util.List;

import android.graphics.drawable.Drawable;
import android.util.Log;

import com.asksven.android.common.CommonLogSettings;
import com.asksven.android.common.dto.SensorUsageDto;
import com.asksven.android.common.dto.SensorUsageItemDto;
import com.asksven.android.common.nameutils.UidNameResolver;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.android.common.utils.DateUtils;
import com.google.gson.annotations.SerializedName;

/**
 * @author sven
 * Value holder for alarms
 */
public class SensorUsage extends StatElement implements Comparable<SensorUsage>, Serializable
{
	/** 
	 * the tag for logging
	 */
	private static transient final String TAG = "Sensor";
	
	/** The name of the app responsible for the sensor */
	@SerializedName("package_name")
	String m_strPackageName;
	
	/**
	 * the details, if any
	 */
	
	@SerializedName("details")
	String m_details;
	
	/** The total time */
	@SerializedName("total_time")
	long m_nTotalTime;
	
	
	/** The details */
	@SerializedName("items")
	ArrayList<SensorUsageItem> m_items;
	

	/**
	 * The default cctor
	 * @param strName
	 */
	public SensorUsage(long nTime)
	{
		m_strPackageName = "";
		m_details = "";
		m_nTotalTime = nTime;
		m_items = new ArrayList<SensorUsageItem>();
		
	}

	public SensorUsage(SensorUsageDto source)
	{
		this.setUid(source.m_uid);
		this.m_strPackageName 	= source.m_strPackageName;
		this.m_details			= source.m_details;
		this.m_nTotalTime 		= source.m_totalTime;
	
		if (source.m_items != null)
		{
			this.m_items = new ArrayList<SensorUsageItem>();
			for (int i=0; i < source.m_items.size(); i++)
			{
				SensorUsageItem item = new SensorUsageItem();
				item.m_nTime = source.m_items.get(i).m_nTime;
				item.m_strSensor = source.m_items.get(i).m_strSensor;
				item.m_nHandle = source.m_items.get(i).m_nHandle;
				
				this.m_items.add(item);
			}
		}
	}

	public SensorUsage clone()
	{
		SensorUsage clone = new SensorUsage(m_nTotalTime);
		clone.setUidInfo(this.getUidInfo());
		clone.m_strPackageName 	= this.m_strPackageName;
		clone.m_details			= this.m_details;
		clone.setTotalTime(m_nTotalTime);
		for (int i=0; i < m_items.size(); i++)
		{
			clone.m_items.add(m_items.get(i).clone());
		}
		return clone;
	}

	/**
	 * Set the total wakeup count for the sum of all alarms
	 * @param nCount
	 */
	public void setTotalTime(long nTime)
	{
		m_nTotalTime = nTime;
	}
	
	/**
	 * Return the max of all alarms (wakeups) 
	 */
	public double getMaxValue()
	{
		return m_nTotalTime;
	}
	
	/* (non-Javadoc)
	 * @see com.asksven.android.common.privateapiproxies.StatElement#getName()
	 */
	public String getName()
	{
		return m_strPackageName;
	}

	public String getDetails()
	{
		return m_details;
	}

	
	/** 
	 * Not supported for alarms
	 * @return 0
	 */
	long getDuration()
	{
		return 0;
	}
	
	/**
	 * @return the time running
	 */
	public long getTotal()
	{
		return m_nTotalTime;
	}
	/** 
	 * returns the values of the data
	 */	
	public double[] getValues()
	{
		double[] retVal = new double[2];
		retVal[0] = getTotal();
		return retVal;
	}
	
	/**
	 * returns a string representation of the data
	 */
	public String getData(long totalTime)
	{
		String strRet = DateUtils.formatDuration(getTotal());
		
		return strRet;
	}

	
	/**
	 * returns a string representation of the detailed data (including children)  
	 */
	public String getDetailedData()
	{
		String strRet = "";
		
		for (int i=0; i < m_items.size(); i++)
		{
			strRet += "  " + m_items.get(i).getData() + "\n";
		}
		
		return strRet;
	}

	/** 
	 * returns the representation of the data for file dump
	 */	
	public String getDumpData(UidNameResolver nameResolver, long totalTime)
	{
		return this.getName() + " (" + this.getFqn(nameResolver) + "): " + this.getDetailedData();
	}

	
	/**
	 * Adds an item
	 * @param nCount
	 * @param strIntent
	 */
	public void addItem(long nTime, String strSensor, int nHandle)
	{
		m_items.add(new SensorUsageItem(nTime, strSensor, nHandle));
	}
	
	/**
	 * Retrieve the list of items
	 * @return
	 */
	public ArrayList<SensorUsageItem> getItems()
	{
		return m_items;
	}
	
	public void setItems(ArrayList<SensorUsageItem> items)
	{
		m_items = items;
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
					SensorUsage myRef = (SensorUsage) myList.get(i);
					if (this.getName().equals(myRef.getName()))
					{
						// process main values
						if (CommonLogSettings.DEBUG)
							Log.i(TAG, "Substracting " + myRef.toString() + " from " + this.toString());
						
						this.m_nTotalTime  -= myRef.getTotal();
						Log.i(TAG, "Result: " + this.toString());
						
						// and process items
						for (int j=0; j < this.m_items.size(); j++)
						{
							SensorUsageItem myItem = this.m_items.get(j);
							myItem.substractFromRef(myRef.getItems());
						}

						if (this.getTotal() < 0)
						{
							Log.e(TAG, "substractFromRef generated negative values (" + this.toString() + " - " + myRef.toString() + ")");
						}
						if (this.getItems().size() < myRef.getItems().size())
						{
							Log.e(TAG, "substractFromRef error processing alarm items: ref can not have less items");
						}
							
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
	
	public String getPackageName()
	{
		return m_strPackageName;
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

	/**
	 * Compare a given Wakelock with this object.
	 * If the duration of this object is 
	 * greater than the received object,
	 * then this object is greater than the other.
	 */
	public int compareTo(SensorUsage o)
	{
		// we want to sort in descending order
		return ((int)(o.getTotal() - this.getTotal()));
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() 
	{
		return getName() + " ["
				+ getData(0)
				+ "]";
	}
	
	public SensorUsageDto toDto()
	{
		SensorUsageDto ret = new SensorUsageDto();
		
		ret.m_uid 				= this.getuid();
		ret.m_strPackageName 	= this.m_strPackageName;
		ret.m_details			= this.m_details;
		ret.m_totalTime 		= this.m_nTotalTime;
	
		if (m_items != null)
		{
			ret.m_items = new ArrayList<SensorUsageItemDto>();
			for (int i=0; i < m_items.size(); i++)
			{
				SensorUsageItemDto item = new SensorUsageItemDto();
				item.m_nTime = m_items.get(i).m_nTime;
				item.m_strSensor = m_items.get(i).m_strSensor;
				item.m_nHandle = m_items.get(i).m_nHandle;

				ret.m_items.add(item);
			}
		}
		return ret;
	}
	
	public static class TimeComparator implements Comparator<SensorUsage>
	{
		public int compare(SensorUsage a, SensorUsage b)
		{
			return ((int)(b.getTotal() - a.getTotal()));
		}
	}
	
	
}
