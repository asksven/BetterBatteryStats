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

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;





//import android.content.Context;
//import android.content.pm.PackageManager;
//import android.graphics.drawable.Drawable;
import android.util.Log;

import com.asksven.android.common.nameutils.UidNameResolver;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.google.gson.annotations.SerializedName;

/**
 * Value holder for alarm items
 * @author sven
 *
 */
public class AlarmItem implements Serializable
{
	/** 
	 * the tag for logging
	 */
	private static transient final String TAG = "Alarm";

	@SerializedName("number")
	@JsonProperty("number")
	long m_nNumber;
	
	@SerializedName("intent")
	@JsonProperty("intent")
	String m_strIntent;

	public AlarmItem()
	{
		
	}

	/**
	 * Default cctor
	 * @param nCount
	 * @param strIntent
	 */
	@JsonIgnore
	public AlarmItem(long nCount, String strIntent)
	{
		m_nNumber 	= nCount;
		m_strIntent = strIntent;
	}

	public AlarmItem clone()
	{
		AlarmItem clone = new AlarmItem(m_nNumber, m_strIntent);
		return clone;
	}

	/**
	 * Returns the intent name
	 * @return
	 */
	@JsonProperty("intent")
	public String getIntent()
	{
		return m_strIntent;
	}
	
	/**
	 * Returns the count
	 * @return
	 */
	@JsonIgnore
	public long getCount()
	{
		return m_nNumber;
	}
	/**
	 * Returns the data as a string
	 * @return
	 */
	@JsonIgnore
	public String getData()
	{
		return "Alarms: " + m_nNumber + ", Intent: " + m_strIntent;
	}
	/**
	 * Substracts the values from a previous object
	 * found in myList from the current Process
	 * in order to obtain an object containing only the data since a referenc
	 * @param myList
	 */
	public void substractFromRef(List<AlarmItem> myList )
	{
		if (myList != null)
		{
			for (int i = 0; i < myList.size(); i++)
			{
				try
				{
					AlarmItem myRef = (AlarmItem) myList.get(i);
					if (this.getIntent().equals(myRef.getIntent()))
					{
						// process main values
						this.m_nNumber		-= myRef.getCount();
						Log.i(TAG, "Result: " + this.toString());
					}
				}
				catch (ClassCastException e)
				{
					// just log as it is no error not to change the process
					// being substracted from to do nothing
					Log.e(TAG, "AlarmItem.substractFromRef was called with a wrong list type");
				}
			}
		}
	}
}
	
