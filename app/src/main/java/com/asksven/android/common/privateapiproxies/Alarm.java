/*
 * Copyright (C) 2011-2012 asksven
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







//import android.content.Context;
//import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.asksven.android.common.CommonLogSettings;
import com.asksven.android.common.dto.AlarmDto;
import com.asksven.android.common.dto.AlarmItemDto;
import com.asksven.android.common.nameutils.UidInfo;
import com.asksven.android.common.nameutils.UidNameResolver;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.android.common.utils.DateUtils;
import com.asksven.android.common.utils.StringUtils;
import com.google.gson.annotations.SerializedName;

/**
 * @author sven
 * Value holder for alarms
 */
public class Alarm extends StatElement implements Comparable<Alarm>, Serializable
{
	/** 
	 * the tag for logging
	 */
	private static transient final String TAG = "Alarm";
	
	/** The name of the app responsible for the alarm */
	@SerializedName("package_name")
	String m_strPackageName;
	
	/**
	 * the details, if any
	 */
	
	@SerializedName("details")
	String m_details;
	
	/** The number od wakeups */
	@SerializedName("wakeups")
	long m_nWakeups;

	/** The duration of the sample */
	@SerializedName("time_runing_ms")
	long m_timeRunning;

	/** The total count */
	@SerializedName("total_count")
	long m_nTotalCount;
	
	
	/** The details */
	@SerializedName("items")
	ArrayList<AlarmItem> m_items;
	

	/**
	 * The default cctor
	 * @param strName
	 */
	public Alarm(String strName)
	{
		m_strPackageName = strName;
		m_details = "";
		m_items = new ArrayList<AlarmItem>();
		
	}

	public Alarm(String strName, String strDetails)
	{
		m_strPackageName = strName;
		m_details = strDetails;
		m_items = new ArrayList<AlarmItem>();
		
	}

	/**
	 * The default cctor
	 * @param strName
	 */
	public Alarm(String strName, String strDetails, long lWakeups, long lCount, long timeRunning, ArrayList<AlarmItem> items)
	{
		m_strPackageName 	= strName;
		m_details			= strDetails;
		m_nWakeups 			= lWakeups;
		m_nTotalCount 		= lCount;
		m_timeRunning		= timeRunning;
		setTotal(m_timeRunning);
		m_items = items;
		
	}

	public Alarm(AlarmDto source)
	{
		
		this.setUid(source.m_uid);
		this.m_strPackageName 	= source.m_strPackageName;
		this.m_details			= source.m_details;
		this.m_nWakeups 		= source.m_nWakeups;
		this.m_timeRunning	= source.m_timeRunning;
		this.m_nTotalCount 		= source.m_nTotalCount;
	
		if (source.m_items != null)
		{
			this.m_items = new ArrayList<AlarmItem>();
			for (int i=0; i < source.m_items.size(); i++)
			{
				AlarmItem item = new AlarmItem();
				item.m_nNumber = source.m_items.get(i).m_nNumber;
				item.m_strIntent = source.m_items.get(i).m_strIntent;
				this.m_items.add(item);
			}
		}
	}

	public Alarm clone()
	{
		Alarm clone = new Alarm(m_strPackageName, m_details);
		clone.setWakeups(getWakeups());
		clone.setTimeRunning(getTimeRunning());
		clone.setTotalCount(m_nTotalCount);
		for (int i=0; i < m_items.size(); i++)
		{
			clone.m_items.add(m_items.get(i).clone());
		}
		return clone;
	}
	/**
	 * Store the number of wakeups 
	 * @param nCount
	 */
	public void setWakeups(long nCount)
	{
		m_nWakeups = nCount;
	}
	
	/**
	 * Store the time running 
	 * @param nTimeRunning
	 */
	public void setTimeRunning(long nTimeRunning)
	{
		m_timeRunning = nTimeRunning;
	}
	/**
	 * Set the total wakeup count for the sum of all alarms
	 * @param nCount
	 */
	public void setTotalCount(long nCount)
	{
		m_nTotalCount = nCount;
	}
	
	/**
	 * Return the max of all alarms (wakeups) 
	 */
	public double getMaxValue()
	{
		return m_nTotalCount;
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
	 * Returns the number of wakeups
	 * @return
	 */
	public long getWakeups()
	{
		return m_nWakeups;
	}
	
	/**
	 * @see getWakeups
	 * @return
	 */
	long getCount()
	{
		return getWakeups();
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
	public long getTimeRunning()
	{
		return m_timeRunning;
	}
	/** 
	 * returns the values of the data
	 */	
	public double[] getValues()
	{
		double[] retVal = new double[2];
		retVal[0] = getCount();
		return retVal;
	}
	
	/**
	 * returns a string representation of the data
	 */
	public String getData(long totalTime)
	{
		String strRet = "";
		strRet = "Wakeups: " + getCount();
		if ((getCount() > 0) && (getTimeRunning() > 0))
		{
			strRet += " (" + DateUtils.formatFrequency(getCount(), totalTime) + ")";
		}
		
		return strRet;
	}

	
	/**
	 * returns a string representation of the detailed data (including children)  
	 */
	public String getDetailedData()
	{
		String strRet = "";
		strRet = "Wakeups: " + getCount();
		
		if ((getCount() > 0) && (getTimeRunning() > 0))
		{
			strRet += " (" + DateUtils.formatFrequency(getCount(), getTimeRunning()) + ")";
		}
		
		strRet += "\n";
		
		for (int i=0; i < m_items.size(); i++)
		{
			strRet += "  " + m_items.get(i).getData() + "\n";
		}
		
		return strRet;
	}

	/**
	 * Returns the full qualified name (default, can be overwritten)
	 * @return the full qualified name
	 */
	public String getFqn(UidNameResolver resolver)
	{
		return m_details;
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
	public void addItem(long nCount, String strIntent)
	{
		m_items.add(new AlarmItem(nCount, strIntent));
	}
	
	/**
	 * Retrieve the list of items
	 * @return
	 */
	public ArrayList<AlarmItem> getItems()
	{
		return m_items;
	}
	
	public void setItems(ArrayList<AlarmItem> items)
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
					Alarm myRef = (Alarm) myList.get(i);
					if (this.getName().equals(myRef.getName()))
					{
						// process main values
						if (CommonLogSettings.DEBUG)
							Log.i(TAG, "Substracting " + myRef.toString() + " from " + this.toString());
						
						this.m_nWakeups		-= myRef.getCount();
						this.m_timeRunning	-= myRef.getTimeRunning();
						this.m_nTotalCount  -= myRef.getMaxValue();
						Log.i(TAG, "Result: " + this.toString());
						
						// and process items
						for (int j=0; j < this.m_items.size(); j++)
						{
							AlarmItem myItem = this.m_items.get(j);
							myItem.substractFromRef(myRef.getItems());
						}

						if (this.getCount() < 0)
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
			String myPackage = m_strPackageName;
			if (!myPackage.equals(""))
			{
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
	public int compareTo(Alarm o)
	{
		// we want to sort in descending order
		return ((int)(o.getWakeups() - this.getWakeups()));
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
	
	public AlarmDto toDto()
	{
		AlarmDto ret = new AlarmDto();
		
		ret.m_uid 				= this.getuid();
		ret.m_strPackageName 	= this.m_strPackageName;
		ret.m_details			= this.m_details;
		ret.m_nWakeups 			= this.m_nWakeups;
		ret.m_nTotalCount 		= this.m_nTotalCount;
		ret.m_timeRunning 		= this.m_timeRunning;
		ret.m_total		 	= this.getTotal();

	
		if (m_items != null)
		{
			ret.m_items = new ArrayList<AlarmItemDto>();
			for (int i=0; i < m_items.size(); i++)
			{
				AlarmItemDto item = new AlarmItemDto();
				item.m_nNumber = m_items.get(i).m_nNumber;
				item.m_strIntent = m_items.get(i).m_strIntent;
				ret.m_items.add(item);
			}
		}
		return ret;
	}


	
	public static class CountComparator implements Comparator<Alarm>
	{
		public int compare(Alarm a, Alarm b)
		{
			return ((int)(b.getCount() - a.getCount()));
		}
	}
	
	public static class TimeComparator implements Comparator<Alarm>
	{
		public int compare(Alarm a, Alarm b)
		{
			return ((int)(b.getDuration() - a.getDuration()));
		}
	}
	
	
}
