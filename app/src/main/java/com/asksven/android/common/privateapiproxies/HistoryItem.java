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

import android.os.Parcel;
import android.os.Parcelable;

import com.asksven.android.common.utils.DateUtils;

/**
 * Value holder for BatteryStats$HistoryItem
 * @author sven
 *
 */
public class HistoryItem implements Serializable, Parcelable
{
	static final long serialVersionUID = 1L;
    static final byte CMD_UPDATE = 0;
    static final byte CMD_START = 1;
    static final byte CMD_OVERFLOW = 2;
    // Constants from SCREEN_BRIGHTNESS_*
    static final int STATE_BRIGHTNESS_MASK = 0x000000f;
    static final int STATE_BRIGHTNESS_SHIFT = 0;
    // Constants from SIGNAL_STRENGTH_*
    static final int STATE_SIGNAL_STRENGTH_MASK = 0x00000f0;
    static final int STATE_SIGNAL_STRENGTH_SHIFT = 4;
    // Constants from ServiceState.STATE_*
    static final int STATE_PHONE_STATE_MASK = 0x0000f00;
    static final int STATE_PHONE_STATE_SHIFT = 8;
    // Constants from DATA_CONNECTION_*
    static final int STATE_DATA_CONNECTION_MASK = 0x000f000;
    static final int STATE_DATA_CONNECTION_SHIFT = 12;
    
    static final int STATE_BATTERY_PLUGGED_FLAG = 1<<30;
    static final int STATE_SCREEN_ON_FLAG = 1<<29;
    static final int STATE_GPS_ON_FLAG = 1<<28;
    static final int STATE_PHONE_IN_CALL_FLAG = 1<<27;
    static final int STATE_PHONE_SCANNING_FLAG = 1<<26;
    static final int STATE_WIFI_ON_FLAG = 1<<25;
    static final int STATE_WIFI_RUNNING_FLAG = 1<<24;
    static final int STATE_WIFI_FULL_LOCK_FLAG = 1<<23;
    static final int STATE_WIFI_SCAN_LOCK_FLAG = 1<<22;
    static final int STATE_WIFI_MULTICAST_ON_FLAG = 1<<21;
    static final int STATE_BLUETOOTH_ON_FLAG = 1<<20;
    static final int STATE_AUDIO_ON_FLAG = 1<<19;
    static final int STATE_VIDEO_ON_FLAG = 1<<18;
    static final int STATE_WAKE_LOCK_FLAG = 1<<17;
    static final int STATE_SENSOR_ON_FLAG = 1<<16;
    
    static final int MOST_INTERESTING_STATES =
        STATE_BATTERY_PLUGGED_FLAG | STATE_SCREEN_ON_FLAG
        | STATE_GPS_ON_FLAG | STATE_PHONE_IN_CALL_FLAG;
    
    protected Long m_time;
    protected Long m_offset;
    protected Byte m_cmd;
    protected Byte m_batteryLevel;
    protected Byte m_batteryStatusValue;
    protected Byte m_batteryHealthValue;
    protected Byte m_batteryPlugTypeValue;
    protected String m_batteryTemperatureValue;
    protected String m_batteryVoltageValue;
    protected Integer m_statesValue;
    
    public HistoryItem(Long time, Byte cmd, Byte batteryLevel, Byte batteryStatusValue,
    		Byte batteryHealthValue, Byte batteryPlugTypeValue,
    		String batteryTemperatureValue,	String batteryVoltageValue,
    		Integer	statesValue)
    {
    	m_time						= time;
    	m_offset					= Long.valueOf(0);
    	m_cmd 						= cmd;
    	m_batteryLevel 				= batteryLevel;
    	m_batteryStatusValue 		= batteryStatusValue;
		m_batteryHealthValue 		= batteryHealthValue;
		m_batteryPlugTypeValue 		= batteryPlugTypeValue;
		m_batteryTemperatureValue 	= batteryTemperatureValue;
		m_batteryVoltageValue 		= batteryVoltageValue;
		m_statesValue 				= statesValue;
    }

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		String strTime = DateUtils.format("HH:mm:ss", m_time);
		
		return "HistoryItem [m_time=" + strTime + ", m_cmd=" + m_cmd
				+ ", m_batteryLevel=" + m_batteryLevel
				+ ", m_batteryStatusValue=" + m_batteryStatusValue
				+ ", m_batteryHealthValue=" + m_batteryHealthValue
				+ ", m_batteryPlugTypeValue=" + m_batteryPlugTypeValue
				+ ", m_statesValue=" + m_statesValue + "]";
	}

	/**
	 * @return the m_cmd
	 */
	public Byte getCmd()
	{
		return m_cmd;
	}
	
	/**
	 * Returns the raw time of the HistoryItem as tring
	 * @return the RAW time as sting
	 */
	public String getTime()
	{
		return DateUtils.format("HH:mm:ss", m_time);
		
	}
	
	/**
	 * returns the "real" time of the HistoryItem by applying the offset
	 * calculated as the difference of the most recent
	 * sample from the stats and the current time at the point of reading
	 * the stats. This norms the last sample to the time the data is queried 
	 * @return the normalized time
	 */
	public String getNormalizedTime()
	{
		return DateUtils.format("dd.MM.yyyy HH:mm:ss S", m_time + m_offset);
		
	}

	public Long getNormalizedTimeLong()
	{
		return m_time + m_offset;
		
	}
	/**
	 * @return the m_batteryLevel
	 */
	public String getBatteryLevel()
	{
		return String.valueOf(m_batteryLevel);
	}

	/**
	 * @return the m_batteryLevel
	 */
	public int getBatteryLevelInt()
	{
		return m_batteryLevel;
	}

	/**
	 * @return the m_statesValue
	 */
	public Integer getStatesValue()
	{
		return m_statesValue;
	}


	/**
	 * @return the m_bCharging as "0" or "1"
	 */
	public String getCharging()
	{
		return getBooleanAsString(isCharging());
	}

	/**
	 * @return the m_bCharging as "0" or "1"
	 */
	public int getChargingInt()
	{
		return getBooleanAsInt(isCharging());
	}

	/**
	 * @return the m_bScreenOn as "0" or "1"
	 */
	public String getScreenOn()
	{
		
		return getBooleanAsString(isScreenOn());
	}

	/**
	 * @return the m_bScreenOn as "0" or "1"
	 */
	public int getScreenOnInt()
	{
		
		return getBooleanAsInt(isScreenOn());
	}

	
	/**
	 * @return the m_bWakelock as "0" or "1"
	 */
	public String getWakelock()
	{
		return getBooleanAsString(isWakeLock());
	}

	/**
	 * @return the m_bWakelock as "0" or "1"
	 */
	public int getWakelockInt()
	{
		return getBooleanAsInt(isWakeLock());
	}

	/**
	 * @return the m_bWifiRunning as "0" or "1"
	 */
	public String getWifiRunning()
	{
		return getBooleanAsString(isWifiRunning());
	}

	/**
	 * @return the m_bWifiRunning as "0" or "1"
	 */
	public int getWifiRunningInt()
	{
		return getBooleanAsInt(isWifiRunning());
	}

	/**
	 * @return the m_bGpsOn as "0" or "1"
	 */
	public String getGpsOn()
	{
		
		return getBooleanAsString(isGpsOn());
	}

	/**
	 * @return the m_bGpsOn as "0" or "1"
	 */
	public int getGpsOnInt()
	{
		
		return getBooleanAsInt(isGpsOn());
	}

	/**
	 * @return the m_bPhoneInCall as "0" or "1"
	 */
	public String getPhoneInCall()
	{
		
		return getBooleanAsString(isPhoneInCall());
	}

	/**
	 * @return the m_bPhoneScanning as "0" or "1"
	 */
	public String getPhoneScanning()
	{
		return getBooleanAsString(isPhoneScanning());
	}

	/**
	 * @return the m_bBluetoothOn as "0" or "1"
	 */
	public String getBluetoothOn()
	{
		return getBooleanAsString(isBluetoothOn());
	}

	/**
	 * @return the m_bBluetoothInt as integer
	 */
	public int getBluetoothOnInt()
	{

		return getBooleanAsInt(isBluetoothOn());
	}

	
	/**
	 * @return the m_bCharging as "0" or "1"
	 */
	protected String getBooleanAsString(boolean bVal)
	{
		if (bVal)
		{
			return "1";
		}
		else
		{
			return "0";
		}
	}

	/**
	 * @return the boolean as 0 or 1
	 */
	protected int getBooleanAsInt(boolean bVal)
	{
		if (bVal)
		{
			return 1;
		}
		else
		{
			return 0;
		}
	}

	/**
	 * @return true is phone is charging
	 */
	public boolean isCharging()
	{
		boolean bCharging = (m_statesValue & STATE_BATTERY_PLUGGED_FLAG) != 0;
		
		return bCharging;
	}

	/**
	 * @return true if screen is on
	 */
	public boolean isScreenOn()
	{
		boolean bScreenOn = (m_statesValue & STATE_SCREEN_ON_FLAG) != 0;
		return bScreenOn;
	}

	/**
	 * @return true is GPS is on
	 */
	public boolean isGpsOn()
	{
		boolean bGpsOn = (m_statesValue & STATE_GPS_ON_FLAG) != 0;
		return bGpsOn;
	}
	
	/**
	 * @return true is wifi is running
	 */
	public boolean isWifiRunning()
	{
		boolean bWifiRunning = (m_statesValue & STATE_WIFI_RUNNING_FLAG) != 0;
		return bWifiRunning;
	}

	/**
	 * @return true is a wakelock is present
	 */
	public boolean isWakeLock()
	{
		boolean bWakeLock = (m_statesValue & STATE_WAKE_LOCK_FLAG) != 0;
		return bWakeLock;
	}

	/**
	 * @return true  if Phone is in Call
	 */
	public boolean isPhoneInCall()
	{
		
		boolean bPhoneInCall = (m_statesValue & STATE_PHONE_IN_CALL_FLAG) != 0;

		return bPhoneInCall;
	}

	/**
	 * @return true if Phone is Scanning
	 */
	public boolean isPhoneScanning()
	{
		boolean bPhoneScanning = (m_statesValue & STATE_PHONE_SCANNING_FLAG) != 0;

		return bPhoneScanning;
	}

	/**
	 * @return the true if bluetooth is on
	 */
	public boolean isBluetoothOn()
	{
		boolean bBluetoothOn = (m_statesValue & STATE_BLUETOOTH_ON_FLAG) != 0;

		return bBluetoothOn;
	}

	
	public void setOffset(Long offset)
	{
		m_offset = offset;
	}

	/* (non-Javadoc)
	 * @see android.os.Parcelable#describeContents()
	 */
	@Override
	public int describeContents()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		// TODO Auto-generated method stub
	    dest.writeLong(m_time);
	    dest.writeLong(m_offset);
	    dest.writeByte(m_cmd);
	    dest.writeByte(m_batteryLevel);
	    dest.writeByte(m_batteryStatusValue);
	    dest.writeByte(m_batteryHealthValue);
	    dest.writeByte(m_batteryPlugTypeValue);
	    dest.writeString(m_batteryTemperatureValue);
	    dest.writeString(m_batteryVoltageValue);
	    dest.writeInt(m_statesValue);
	}
	
	public static final class  BitDescription
	{
		public final int mask;
		public final int shift;
		public final String name;
		public final String[] values;
		
        public BitDescription(int mask, String name)
        {
        	this.mask = mask;
        	this.shift = -1;
        	this.name = name;
        	this.values = null;
        }
  
		public BitDescription(int mask, int shift, String name, String[] values)
		{
			this.mask = mask;
			this.shift = shift;
			this.name = name;
			this.values = values;
		}
	}
	
	 static final String[] SCREEN_BRIGHTNESS_NAMES = {"dark", "dim", "medium", "light", "bright"};
	 
	 public static final String[] SIGNAL_STRENGTH_NAMES = {"none", "poor", "moderate", "good", "great"};
	 
	static final String[] DATA_CONNECTION_NAMES = {
		"none", "gprs", "edge", "umts", "cdma", "evdo_0", "evdo_A",
		"1xrtt", "hsdpa", "hsupa", "hspa", "iden", "evdo_b", "lte",
		"ehrpd", "hspap", "other"};
	

	
	String printBitDescriptions(int oldval, int newval)
	{
		String ret = "";

		
		return ret;
	}
}
