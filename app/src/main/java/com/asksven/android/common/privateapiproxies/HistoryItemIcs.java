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

import com.asksven.android.common.utils.DateUtils;

/**
 * ICS specific Value holder for BatteryStats$HistoryItem
 * @author sven
 *
 */
public class HistoryItemIcs extends HistoryItem implements Serializable, Parcelable
{
	static final long serialVersionUID = 1L;
	private static final byte CMD_NULL = 0;
    public static final byte CMD_UPDATE = 1;
    private static final byte CMD_START = 2;
    private static final byte CMD_OVERFLOW = 3;

    private byte cmd = CMD_NULL;
    // Constants from SCREEN_BRIGHTNESS_*
    private static final int STATE_BRIGHTNESS_MASK = 0x0000000f;
    private static final int STATE_BRIGHTNESS_SHIFT = 0;
    // Constants from SIGNAL_STRENGTH_*
    private static final int STATE_SIGNAL_STRENGTH_MASK = 0x000000f0;
    private static final int STATE_SIGNAL_STRENGTH_SHIFT = 4;
    // Constants from ServiceState.STATE_*
    private static final int STATE_PHONE_STATE_MASK = 0x00000f00;
    private static final int STATE_PHONE_STATE_SHIFT = 8;
    // Constants from DATA_CONNECTION_*
    private static final int STATE_DATA_CONNECTION_MASK = 0x0000f000;
    private static final int STATE_DATA_CONNECTION_SHIFT = 12;
    
    // These states always appear directly in the first int token
    // of a delta change; they should be ones that change relatively
    // frequently.
    private static final int STATE_WAKE_LOCK_FLAG = 1<<30;
    private static final int STATE_SENSOR_ON_FLAG = 1<<29;
    private static final int STATE_GPS_ON_FLAG = 1<<28;
    private static final int STATE_PHONE_SCANNING_FLAG = 1<<27;
    private static final int STATE_WIFI_RUNNING_FLAG = 1<<26;
    private static final int STATE_WIFI_FULL_LOCK_FLAG = 1<<25;
    private static final int STATE_WIFI_SCAN_LOCK_FLAG = 1<<24;
    private static final int STATE_WIFI_MULTICAST_ON_FLAG = 1<<23;
    // These are on the lower bits used for the command; if they change
    // we need to write another int of data.
    private static final int STATE_AUDIO_ON_FLAG = 1<<22;
    private static final int STATE_VIDEO_ON_FLAG = 1<<21;
    private static final int STATE_SCREEN_ON_FLAG = 1<<20;
    private static final int STATE_BATTERY_PLUGGED_FLAG = 1<<19;
    private static final int STATE_PHONE_IN_CALL_FLAG = 1<<18;
    private static final int STATE_WIFI_ON_FLAG = 1<<17;
    private static final int STATE_BLUETOOTH_ON_FLAG = 1<<16;


    public HistoryItemIcs(Long time, Byte cmd, Byte batteryLevel, Byte batteryStatusValue,
    		Byte batteryHealthValue, Byte batteryPlugTypeValue,
    		String batteryTemperatureValue,	String batteryVoltageValue,
    		Integer	statesValue, Integer states2Value)
    {
    	super(time, cmd, batteryLevel, batteryStatusValue,
    		batteryHealthValue, batteryPlugTypeValue,
    		batteryTemperatureValue, batteryVoltageValue,
    		statesValue, states2Value);
    }
    
}
