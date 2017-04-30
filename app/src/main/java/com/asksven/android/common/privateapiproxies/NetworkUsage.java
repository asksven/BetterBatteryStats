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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonFilter;

import com.asksven.android.common.dto.AlarmDto;
import com.asksven.android.common.dto.NativeKernelWakelockDto;
import com.asksven.android.common.dto.NetworkUsageDto;
import com.asksven.android.common.nameutils.UidInfo;
import com.asksven.android.common.nameutils.UidNameResolver;
import com.google.gson.annotations.SerializedName;

import android.app.Application;
//import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
//import android.graphics.drawable.Drawable;
import android.util.Log;

public class NetworkUsage extends StatElement implements Comparable<NetworkUsage>, Serializable
{
	/** 
	 * the tag for logging
	 */
	private static transient final String TAG = "NetworkUsage";
	
	
	/**
	 * tcpBytes received by the program
	 */
	@SerializedName("bytes_received")
	@JsonProperty("bytes_received")
	private long m_bytesReceived=0;
	
	/**
	 * tcpBytes sent by the program
	 */
	@SerializedName("bytes_sent")
	@JsonProperty("bytes_sent")
	private long m_bytesSent=0;

	/**
	 * the interface
	 */
	@SerializedName("iface")
	@JsonProperty("iface")
	private String m_iface = "";


	public NetworkUsage()
	{
		
	}
	
	/**
	 * 
	 * @param uid
	 * @param iface
	 * @param bytesReceived
	 * @param bytesSent
	 */
	public NetworkUsage(int uid, String iface, long bytesReceived, long bytesSent)
	{
		super.setUid(uid);
		m_iface			= iface;
		m_bytesReceived	= bytesReceived;
		m_bytesSent		= bytesSent;
	}
	
	public NetworkUsage clone()
	{
		NetworkUsage clone = new NetworkUsage(getuid(), m_iface, m_bytesReceived, m_bytesSent);
		return clone;
	}

	public NetworkUsage(NetworkUsageDto source)
	{
		
		this.setUid(source.m_uid);
		this.m_bytesReceived 	= source.m_bytesReceived;
		this.m_bytesSent 		= source.m_bytesSent;
		this.m_iface 		= source.m_iface;

	}

	public NetworkUsageDto toDto()
	{
		NetworkUsageDto ret = new NetworkUsageDto();
		ret.m_uid			= this.getuid();
		ret.m_bytesReceived 	= this.m_bytesReceived;
		ret.m_bytesSent 		= this.m_bytesSent;
		ret.m_iface 		= this.m_iface;
	
		return ret;
	}

	/**
	 * 
	 * @param uid
	 * @param bytesReceived
	 * @param bytesSent
	 */
	public NetworkUsage(int uid, long bytesReceived, long bytesSent)
	{
		super.setUid(uid);
		m_iface			= "";
		m_bytesReceived	= bytesReceived;
		m_bytesSent		= bytesSent;
	}

	/**
	 * Creates an Object by passing its name
	 * @param name
	 * @param uid
	 * @param bytesReceived
	 * @param bytesSent
	 */
	public NetworkUsage(String name, int uid, long bytesReceived, long bytesSent)
	{
		m_uidInfo = new UidInfo();
		m_uidInfo.setName(name);
		m_uidInfo.setUid(uid);
		super.setUid(uid);
		m_bytesReceived	= bytesReceived;
		m_bytesSent		= bytesSent;
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
					NetworkUsage myRef = (NetworkUsage) myList.get(i);
					if ( (this.getInterface().equals(myRef.getInterface())) 
							&& (this.getuid() == myRef.getuid()) )
					{
						this.m_bytesReceived	-= myRef.getBytesReceived();
						this.m_bytesSent		-= myRef.getBytesSent();
					
						if ((m_bytesReceived < 0) || (m_bytesSent < 0))
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
	 * @return the interface
	 */
	@JsonProperty("iface")
	public String getInterface()
	{
		String ret = "";
		if (m_iface.startsWith("wlan"))
		{
			ret = "Wifi";
		}
		else if (m_iface.startsWith("rmnet"))
		{
			ret = "Mobile";
		}
		else if (m_iface.startsWith("lo"))
		{
			ret = "Mobile";
		}
		else if (m_iface.startsWith("bnep"))
		{
			ret = "Bluetooth";
		}
		else
		{
			ret = m_iface;
		}

		return ret;
	}


	/**
	 * @return the bytes received
	 */
	@JsonProperty("bytes_received")
	public long getBytesReceived()
	{
		return m_bytesReceived;
	}

	public void addBytesReceived(long bytes)
	{
		m_bytesReceived += bytes;
	}

	public void addBytesSent(long bytes)
	{
		m_bytesSent += bytes;
	}
	/**
	 * @return the bytes sent
	 */
	@JsonProperty("bytes_sent")
	public long getBytesSent()
	{
		return m_bytesSent;
	}
	
	/**
	 * @return the total bytes sent and received
	 */
	@JsonIgnore
	public long getTotalBytes()
	{
		return m_bytesSent + m_bytesReceived;
	}

	/**
     * Compare a given NetworkUsage with this object.
     * If the duration of this object is 
     * greater than the received object,
     * then this object is greater than the other.
     */
	public int compareTo(NetworkUsage o)
	{
		// we want to sort in descending order
		return ((int)((o.getBytesReceived() + o.getBytesSent()) - (this.getBytesReceived() + this.getBytesSent())));
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "NetworkUsage [m_uid=" + super.getuid() + ", m_bytesReceived="
				+ m_bytesReceived + ", m_bytesSent=" + m_bytesSent + "]";
	}
	
	/**
	 * Network Stats do not have a speaking name, just the UID
	 */
	@JsonIgnore
	public String getName()
	{
		return String.valueOf(super.getuid() + " (" + getInterface() + ")");
	}
	
	/**
	 * returns a string representation of the data
	 */
	@JsonIgnore
	public String getData(long totalTime)
	{
		return formatVolume(getTotalBytes()) + " " + this.formatRatio(getTotalBytes(), getTotal());
	}

	/** 
	 * returns the values of the data
	 */
	@JsonIgnore
	public double[] getValues()
	{
		double[] retVal = new double[2];
		retVal[0] = getBytesReceived() + getBytesSent();
//		retVal[1] = getBytesReceived() + getBytesSent();
		return retVal;
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

	@JsonIgnore
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
	
	public Drawable getIcon(UidNameResolver resolver)
	{
		if (m_icon == null)
		{
			// retrieve and store the icon for that package
			String myPackage = m_uidInfo.getNamePackage();
			if (!myPackage.equals(""))
			{
				m_icon = resolver.getIcon(myPackage);
			}
		}
		return m_icon;
	}

}
