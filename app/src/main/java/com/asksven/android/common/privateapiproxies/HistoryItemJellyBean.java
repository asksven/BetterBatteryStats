package com.asksven.android.common.privateapiproxies;
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


import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

import com.asksven.android.common.privateapiproxies.HistoryItem.BitDescription;
import com.asksven.android.common.utils.DateUtils;

/**
 * ICS specific Value holder for BatteryStats$HistoryItem
 * @author sven
 *
 */
public class HistoryItemJellyBean extends HistoryItem implements Serializable, Parcelable
{
    
    public static final byte CMD_NULL = 0;
    public static final byte CMD_UPDATE = 1;
    public static final byte CMD_START = 2;
    public static final byte CMD_OVERFLOW = 3;
    
    public byte cmd = CMD_NULL;
	  
	public byte batteryLevel;
	public byte batteryStatus;
	public byte batteryHealth;
	public byte batteryPlugType;
	  
	public short batteryTemperature;
	public char batteryVoltage;
	 
    // Constants from SCREEN_BRIGHTNESS_*
    public static final int STATE_BRIGHTNESS_MASK = 0x0000000f;
    public static final int STATE_BRIGHTNESS_SHIFT = 0;
    // Constants from SIGNAL_STRENGTH_*
    public static final int STATE_SIGNAL_STRENGTH_MASK = 0x000000f0;
    public static final int STATE_SIGNAL_STRENGTH_SHIFT = 4;
    // Constants from ServiceState.STATE_*
    public static final int STATE_PHONE_STATE_MASK = 0x00000f00;
    public static final int STATE_PHONE_STATE_SHIFT = 8;
    // Constants from DATA_CONNECTION_*
    public static final int STATE_DATA_CONNECTION_MASK = 0x0000f000;
    public static final int STATE_DATA_CONNECTION_SHIFT = 12;
    
    // These states always appear directly in the first int token
    // of a delta change; they should be ones that change relatively
    // frequently.
    public static final int STATE_WAKE_LOCK_FLAG = 1<<30;
    public static final int STATE_SENSOR_ON_FLAG = 1<<29;
    public static final int STATE_GPS_ON_FLAG = 1<<28;
    public static final int STATE_PHONE_SCANNING_FLAG = 1<<27;
    public static final int STATE_WIFI_RUNNING_FLAG = 1<<26;
    public static final int STATE_WIFI_FULL_LOCK_FLAG = 1<<25;
    public static final int STATE_WIFI_SCAN_LOCK_FLAG = 1<<24;
    public static final int STATE_WIFI_MULTICAST_ON_FLAG = 1<<23;
    // These are on the lower bits used for the command; if they change
    // we need to write another int of data.
    public static final int STATE_AUDIO_ON_FLAG = 1<<22;
    public static final int STATE_VIDEO_ON_FLAG = 1<<21;
    public static final int STATE_SCREEN_ON_FLAG = 1<<20;
    public static final int STATE_BATTERY_PLUGGED_FLAG = 1<<19;
    public static final int STATE_PHONE_IN_CALL_FLAG = 1<<18;
    public static final int STATE_WIFI_ON_FLAG = 1<<17;
    public static final int STATE_BLUETOOTH_ON_FLAG = 1<<16;
    
    public static final int MOST_INTERESTING_STATES =
        STATE_BATTERY_PLUGGED_FLAG | STATE_SCREEN_ON_FLAG
        | STATE_GPS_ON_FLAG | STATE_PHONE_IN_CALL_FLAG;
    	
    public HistoryItemJellyBean(Long time, Byte cmd, Byte batteryLevel, Byte batteryStatusValue,
    		Byte batteryHealthValue, Byte batteryPlugTypeValue,
    		String batteryTemperatureValue,	String batteryVoltageValue,
    		Integer	statesValue)
    {
    	super(time, cmd, batteryLevel, batteryStatusValue,
    		batteryHealthValue, batteryPlugTypeValue,
    		batteryTemperatureValue, batteryVoltageValue,
    		statesValue);
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
	
	

}
