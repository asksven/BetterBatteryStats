package com.asksven.android.common.privateapiproxies;
/*
 * Copyright (C) 2022 asksven
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


import android.os.Parcelable;

import java.io.Serializable;

/**
 * ICS specific Value holder for BatteryStats$HistoryItem
 * @author sven
 *
 */
public class HistoryItemAndroid13 extends HistoryItem implements Serializable, Parcelable
{

    public static final byte CMD_UPDATE = 0;        // These can be written as deltas
    public static final byte CMD_NULL = -1;
    public static final byte CMD_START = 4;
    public static final byte CMD_CURRENT_TIME = 5;
    public static final byte CMD_OVERFLOW = 6;
    public static final byte CMD_RESET = 7;
    public static final byte CMD_SHUTDOWN = 8;

    public byte cmd = CMD_NULL;

    // Constants from SCREEN_BRIGHTNESS_*
    public static final int STATE_BRIGHTNESS_SHIFT = 0;
    public static final int STATE_BRIGHTNESS_MASK = 0x7;
    // Constants from SIGNAL_STRENGTH_*
    public static final int STATE_PHONE_SIGNAL_STRENGTH_SHIFT = 3;
    public static final int STATE_PHONE_SIGNAL_STRENGTH_MASK = 0x7 << STATE_PHONE_SIGNAL_STRENGTH_SHIFT;
    // Constants from ServiceState.STATE_*
    public static final int STATE_PHONE_STATE_SHIFT = 6;
    public static final int STATE_PHONE_STATE_MASK = 0x7 << STATE_PHONE_STATE_SHIFT;
    // Constants from DATA_CONNECTION_*
    public static final int STATE_DATA_CONNECTION_SHIFT = 9;
    public static final int STATE_DATA_CONNECTION_MASK = 0x1f << STATE_DATA_CONNECTION_SHIFT;
    // These states always appear directly in the first int token
    // of a delta change; they should be ones that change relatively
    // frequently.
    public static final int STATE_CPU_RUNNING_FLAG = 1<<31;
    public static final int STATE_WAKE_LOCK_FLAG = 1<<30;
    public static final int STATE_GPS_ON_FLAG = 1<<29;
    public static final int STATE_WIFI_FULL_LOCK_FLAG = 1<<28;
    public static final int STATE_WIFI_SCAN_FLAG = 1<<27;
    public static final int STATE_WIFI_RADIO_ACTIVE_FLAG = 1<<26;
    public static final int STATE_MOBILE_RADIO_ACTIVE_FLAG = 1<<25;
    // Do not use, this is used for coulomb delta count.
    private static final int STATE_RESERVED_0 = 1<<24;
    // These are on the lower bits used for the command; if they change
    // we need to write another int of data.
    public static final int STATE_SENSOR_ON_FLAG = 1<<23;
    public static final int STATE_AUDIO_ON_FLAG = 1<<22;
    public static final int STATE_PHONE_SCANNING_FLAG = 1<<21;
    public static final int STATE_SCREEN_ON_FLAG = 1<<20;       // consider moving to states2
    public static final int STATE_BATTERY_PLUGGED_FLAG = 1<<19; // consider moving to states2
    public static final int STATE_SCREEN_DOZE_FLAG = 1 << 18;
    // empty slot
    public static final int STATE_WIFI_MULTICAST_ON_FLAG = 1<<16;

    // Constants from WIFI_SUPPL_STATE_*
    public static final int STATE2_WIFI_SUPPL_STATE_SHIFT = 0;
    public static final int STATE2_WIFI_SUPPL_STATE_MASK = 0xf;
    // Values for NUM_WIFI_SIGNAL_STRENGTH_BINS
    public static final int STATE2_WIFI_SIGNAL_STRENGTH_SHIFT = 4;
    public static final int STATE2_WIFI_SIGNAL_STRENGTH_MASK =
            0x7 << STATE2_WIFI_SIGNAL_STRENGTH_SHIFT;
    // Values for NUM_GPS_SIGNAL_QUALITY_LEVELS
    public static final int STATE2_GPS_SIGNAL_QUALITY_SHIFT = 7;
    public static final int STATE2_GPS_SIGNAL_QUALITY_MASK =
            0x1 << STATE2_GPS_SIGNAL_QUALITY_SHIFT;
    public static final int STATE2_POWER_SAVE_FLAG = 1<<31;
    public static final int STATE2_VIDEO_ON_FLAG = 1<<30;
    public static final int STATE2_WIFI_RUNNING_FLAG = 1<<29;
    public static final int STATE2_WIFI_ON_FLAG = 1<<28;
    public static final int STATE2_FLASHLIGHT_FLAG = 1<<27;
    public static final int STATE2_DEVICE_IDLE_SHIFT = 25;
    public static final int STATE2_DEVICE_IDLE_MASK = 0x3 << STATE2_DEVICE_IDLE_SHIFT;
    public static final int STATE2_CHARGING_FLAG = 1<<24;
    public static final int STATE2_PHONE_IN_CALL_FLAG = 1<<23;
    public static final int STATE2_BLUETOOTH_ON_FLAG = 1<<22;
    public static final int STATE2_CAMERA_FLAG = 1<<21;
    public static final int STATE2_BLUETOOTH_SCAN_FLAG = 1 << 20;
    public static final int STATE2_CELLULAR_HIGH_TX_POWER_FLAG = 1 << 19;
    public static final int STATE2_USB_DATA_LINK_FLAG = 1 << 18;


    public HistoryItemAndroid13(Long time, Byte cmd, Byte batteryLevel, Byte batteryStatusValue,
                                Byte batteryHealthValue, Byte batteryPlugTypeValue,
                                String batteryTemperatureValue, String batteryVoltageValue,
                                Integer	statesValue, Integer states2Value)
    {
        super(time, cmd, batteryLevel, batteryStatusValue,
                batteryHealthValue, batteryPlugTypeValue,
                batteryTemperatureValue, batteryVoltageValue,
                statesValue, states2Value);
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
        boolean bWifiRunning = (m_states2Value & STATE2_WIFI_RUNNING_FLAG) != 0;
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

        boolean bPhoneInCall = (m_states2Value & STATE2_PHONE_IN_CALL_FLAG) != 0;

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
        boolean bBluetoothOn = ((m_states2Value & STATE2_BLUETOOTH_ON_FLAG) != 0);

        return bBluetoothOn;
    }

}
