/*
 * Copyright (C) 2011-2014 asksven
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
package com.asksven.betterbatterystats.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import android.os.SystemClock;
import android.util.Log;

import com.asksven.android.common.dto.AlarmDto;
import com.asksven.android.common.dto.MiscDto;
import com.asksven.android.common.dto.NativeKernelWakelockDto;
import com.asksven.android.common.dto.NetworkUsageDto;
import com.asksven.android.common.dto.ProcessDto;
import com.asksven.android.common.dto.SensorUsageDto;
import com.asksven.android.common.dto.StateDto;
import com.asksven.android.common.dto.WakelockDto;
import com.asksven.android.common.privateapiproxies.NativeKernelWakelock;
import com.asksven.android.common.kernelutils.State;
import com.asksven.android.common.privateapiproxies.Alarm;
import com.asksven.android.common.privateapiproxies.Misc;
import com.asksven.android.common.privateapiproxies.NetworkUsage;
import com.asksven.android.common.privateapiproxies.SensorUsage;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.android.common.privateapiproxies.Process;
import com.asksven.android.common.utils.DateUtils;
import com.asksven.betterbatterystats.LogSettings;
import com.asksven.android.common.privateapiproxies.Wakelock;

/**
 * A serializable value holder for stat references 
 * @author sven
 *
 */
@JsonSerialize(include=JsonSerialize.Inclusion.ALWAYS)
@JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.ANY, getterVisibility=JsonAutoDetect.Visibility.NONE, setterVisibility=JsonAutoDetect.Visibility.NONE)
public class Reference implements Serializable
{
	
	private static final long serialVersionUID = 3L;
	
	private transient static final String TAG = "References";
	
	public static final String EXTRA_REF_NAME					= "com.asksven.betterbatterystats.REF_NAME";
	
	public static final String CUSTOM_REF_FILENAME 				= "ref_custom";
	public static final String TIMER_REF_FILENAME 				= "Timer ";
	public static final String UNPLUGGED_REF_FILENAME 			= "ref_unplugged";
	public static final String CHARGED_REF_FILENAME 			= "ref_charged";
	public static final String SCREEN_OFF_REF_FILENAME			= "ref_screen_off";
	public static final String SCREEN_ON_REF_FILENAME			= "ref_screen_on";
	public static final String BOOT_REF_FILENAME				= "ref_boot";
	public static final String CURRENT_REF_FILENAME 			= "ref_current";

	public static final int TYPE_CUSTOM 	= 1;
	public static final int TYPE_EVENT 		= 2;
	public static final int TYPE_CURRENT 	= 3;
	public static final int TYPE_TIMER	 	= 4;
	
	
	
	protected static final String[] FILES = {CUSTOM_REF_FILENAME, CURRENT_REF_FILENAME, UNPLUGGED_REF_FILENAME, CHARGED_REF_FILENAME, SCREEN_OFF_REF_FILENAME, SCREEN_ON_REF_FILENAME, BOOT_REF_FILENAME};

	protected static final String CUSTOM_REF_ERR 				= "No custom reference was set. Please use the menu to do so";
	protected static final String UNPLUGGED_REF_ERR 			= "No reference unplugged was saved yet, plug/unplug you phone";
	protected static final String CHARGED_REF_ERR 				= "No reference charged was saved yet, it will the next time you charge to 100%";
	protected static final String SCREEN_OFF_REF_ERR			= "Screen off event was not registered yet. Make sure to activate the watchdog.";
	protected static final String SCREEN_ON_REF_ERR				= "Screen on event was not registered yet. Make sure to activate the watchdog.";
	protected static final String BOOT_REF_ERR					= "Boot event was not registered yet, it will at next reboot";

	protected static final String CUSTOM_REF_NAME 				= "Custom";
	protected static final String CURRENT_REF_NAME 				= "Current";
	protected static final String UNPLUGGED_REF_NAME 			= "Unplugged";
	protected static final String CHARGED_REF_NAME 				= "Charged";
	protected static final String SCREEN_OFF_REF_NAME			= "Screen Off";
	protected static final String SCREEN_ON_REF_NAME			= "Screen On";
	protected static final String BOOT_REF_NAME					= "Boot";
	protected static final String TIMER_REF_NAME				= "Timer";

	/** storage of custom references */
	public String m_fileName								= "";
	protected long m_creationTime							= 0;
	protected int m_refType									= 0;
	protected String m_refLabel								= "";
	@JsonDeserialize(contentAs = Wakelock.class)
    protected ArrayList<StatElement> m_refWakelocks 		= null;
	
	@JsonDeserialize(contentAs = NativeKernelWakelock.class)
    protected ArrayList<StatElement> m_refKernelWakelocks 	= null;
	
	@JsonDeserialize(contentAs = NetworkUsage.class)
    protected ArrayList<StatElement> m_refNetworkStats	 	= null;
	
	@JsonDeserialize(contentAs = Alarm.class)
    protected ArrayList<StatElement> m_refAlarms		 	= null;
	
	@JsonDeserialize(contentAs = Process.class)
    protected ArrayList<StatElement> m_refProcesses 		= null;

	@JsonDeserialize(contentAs = Misc.class)
    protected ArrayList<StatElement> m_refOther	 			= null;
	
	@JsonDeserialize(contentAs = State.class)
    protected ArrayList<StatElement> m_refCpuStates			= null;

	@JsonDeserialize(contentAs = SensorUsage.class)
    protected ArrayList<StatElement> m_refSensorUsage		= null;

    protected long m_refBatteryRealtime 					= 0;
    protected int m_refBatteryLevel							= 0;
    protected int m_refBatteryVoltage						= 0;
    
    private Reference()
    {
    	
    }

    public Reference(ReferenceDto source)
    {		
    	if (source == null)
    	{
    		return;
    	}
    	
		this.m_creationTime 		= source.m_creationTime;
		this.m_fileName 			= source.m_fileName;
		this.m_refBatteryLevel 		= source.m_refBatteryLevel;
		this.m_refBatteryRealtime 	= source.m_refBatteryRealtime;
		this.m_refBatteryVoltage 	= source.m_refBatteryVoltage;
		this.m_refLabel 			= source.m_refLabel;
		this.m_refType 				= source.m_refType;
		
		if (source.m_refAlarms != null)
		{
			this.m_refAlarms = new ArrayList<StatElement>();
			for (int i=0; i < source.m_refAlarms.size(); i++)
			{
				this.m_refAlarms.add(new Alarm(source.m_refAlarms.get(i)));
			}
		}

		if (source.m_refCpuStates != null)
		{
			this.m_refCpuStates = new ArrayList<StatElement>();
			for (int i=0; i < source.m_refCpuStates.size(); i++)
			{
				this.m_refCpuStates.add(new State(source.m_refCpuStates.get(i)));
			}
		}

		if (source.m_refKernelWakelocks != null)
		{
			this.m_refKernelWakelocks = new ArrayList<StatElement>();
			for (int i=0; i < source.m_refKernelWakelocks.size(); i++)
			{
				this.m_refKernelWakelocks.add(new NativeKernelWakelock(source.m_refKernelWakelocks.get(i)));
			}
		}

		if (source.m_refNetworkStats != null)
		{
			this.m_refNetworkStats = new ArrayList<StatElement>();
			for (int i=0; i < source.m_refNetworkStats.size(); i++)
			{
				this.m_refNetworkStats.add(new NetworkUsage(source.m_refNetworkStats.get(i)));
			}
		}

		if (source.m_refOther != null)
		{
			this.m_refOther = new ArrayList<StatElement>();
			for (int i=0; i < source.m_refOther.size(); i++)
			{
				this.m_refOther.add(new Misc(source.m_refOther.get(i)));
			}
		}
		
		if (source.m_refProcesses != null)
		{
			this.m_refProcesses = new ArrayList<StatElement>();
			for (int i=0; i < source.m_refProcesses.size(); i++)
			{
				this.m_refProcesses.add(new Process(source.m_refProcesses.get(i)));
			}
		}

		if (source.m_refWakelocks != null)
		{
			this.m_refWakelocks = new ArrayList<StatElement>();
			for (int i=0; i < source.m_refWakelocks.size(); i++)
			{
				this.m_refWakelocks.add(new Wakelock(source.m_refWakelocks.get(i)));
			}
		}

		if (source.m_refSensorUsage != null)
		{
			this.m_refSensorUsage = new ArrayList<StatElement>();
			for (int i=0; i < source.m_refSensorUsage.size(); i++)
			{
				this.m_refSensorUsage.add(new SensorUsage(source.m_refSensorUsage.get(i)));
			}
		}

		
    }

    public Reference(String fileName, int type)
    {
    	m_fileName 		= fileName;
    	m_creationTime 	= System.currentTimeMillis(); //SystemClock.elapsedRealtime();
    	m_refType 		= type;
    	m_refLabel		= getLabel(fileName);
//    	+ " " + DateUtils.format(m_creationTime);		
		if (LogSettings.DEBUG)
		{
			Log.i(TAG, "Create ref " + m_fileName + " at " + DateUtils.formatDuration(m_creationTime));
		}
    }
    
	public ReferenceDto toReferenceDto()
	{
		ReferenceDto ret = new ReferenceDto();
		
		ret.m_creationTime 			= this.m_creationTime;
		ret.m_fileName 				= this.m_fileName;
		ret.m_refBatteryLevel 		= this.m_refBatteryLevel;
		ret.m_refBatteryRealtime 	= this.m_refBatteryRealtime;
		ret.m_refBatteryVoltage 	= this.m_refBatteryVoltage;
		ret.m_refLabel 				= this.m_refLabel;
		ret.m_refType 				= this.m_refType;
		
		if (this.m_refAlarms != null)
		{
			ret.m_refAlarms = new ArrayList<AlarmDto>();
			for (int i=0; i < this.m_refAlarms.size(); i++)
			{
				ret.m_refAlarms.add(((Alarm) this.m_refAlarms.get(i)).toDto());
			}
		}

		if (this.m_refCpuStates != null)
		{
			ret.m_refCpuStates = new ArrayList<StateDto>();
			for (int i=0; i < this.m_refCpuStates.size(); i++)
			{
				ret.m_refCpuStates.add(((State) this.m_refCpuStates.get(i)).toDto());
			}
		}

		if (this.m_refKernelWakelocks != null)
		{
			ret.m_refKernelWakelocks = new ArrayList<NativeKernelWakelockDto>();
			for (int i=0; i < this.m_refKernelWakelocks.size(); i++)
			{
				ret.m_refKernelWakelocks.add(((NativeKernelWakelock) this.m_refKernelWakelocks.get(i)).toDto());
			}
		}

		if (this.m_refNetworkStats != null)
		{
			ret.m_refNetworkStats = new ArrayList<NetworkUsageDto>();
			for (int i=0; i < this.m_refNetworkStats.size(); i++)
			{
				ret.m_refNetworkStats.add(((NetworkUsage) this.m_refNetworkStats.get(i)).toDto());
			}
		}
		
		if (this.m_refOther != null)
		{
			ret.m_refOther = new ArrayList<MiscDto>();
			for (int i=0; i < this.m_refOther.size(); i++)
			{
				ret.m_refOther.add(((Misc) this.m_refOther.get(i)).toDto());
			}
		}
		
		if (this.m_refProcesses != null)
		{
			ret.m_refProcesses = new ArrayList<ProcessDto>();
			for (int i=0; i < this.m_refProcesses.size(); i++)
			{
				ret.m_refProcesses.add(((Process) this.m_refProcesses.get(i)).toDto());
			}
		}

		if (this.m_refWakelocks != null)
		{
			ret.m_refWakelocks = new ArrayList<WakelockDto>();
			for (int i=0; i < this.m_refWakelocks.size(); i++)
			{
				ret.m_refWakelocks.add(((Wakelock) this.m_refWakelocks.get(i)).toDto());
			}
		}

		if (this.m_refSensorUsage != null)
		{
			ret.m_refSensorUsage = new ArrayList<SensorUsageDto>();
			for (int i=0; i < this.m_refSensorUsage.size(); i++)
			{
				ret.m_refSensorUsage.add(((SensorUsage) this.m_refSensorUsage.get(i)).toDto());
			}
		}

		return ret;
	}

    public void setEmpty()
    {
    	m_creationTime = 0;
    }
    public void setTimestamp()
    {
    	m_creationTime = System.currentTimeMillis(); //SystemClock.elapsedRealtime();
    }
    
    public long getCreationTime()
    {
    	return m_creationTime;
    }
    
    @JsonIgnore
    public String getMissingRefError()
    {
    	if (m_fileName.equals(CUSTOM_REF_FILENAME))
    		return CUSTOM_REF_ERR;
    	else if (m_fileName.equals(UNPLUGGED_REF_FILENAME))
    		return UNPLUGGED_REF_ERR;
    	else if (m_fileName.equals(CHARGED_REF_FILENAME))
    		return CHARGED_REF_ERR;
    	else if (m_fileName.equals(SCREEN_OFF_REF_FILENAME))
    		return SCREEN_OFF_REF_ERR;
    	else if (m_fileName.equals(SCREEN_ON_REF_FILENAME))
    		return SCREEN_ON_REF_ERR;
    	else if (m_fileName.equals(BOOT_REF_FILENAME))
    		return BOOT_REF_ERR;
    	else
    		return "No reference found";
    }
        	
    public String whoAmI()
    {
    	return "Reference " + m_fileName + " created " + DateUtils.formatDuration(m_creationTime) + " " + elements();
    }
    
    private String elements()
    {
    	String wakelocks = (m_refWakelocks == null) ? "null" : m_refWakelocks.size() + " elements";
        String kernelWakelocks = (m_refKernelWakelocks == null) ? "null" : m_refKernelWakelocks.size() + "elements";
        String networkStats = (m_refNetworkStats == null) ? "null" : m_refNetworkStats.size() + " elements";
        String alarms = (m_refAlarms == null) ? "null" : m_refAlarms.size() + " elements";
        String processes = (m_refProcesses ==null) ? "null" : m_refProcesses.size() + " elements";
        String other = (m_refOther == null) ? "null" : m_refOther.size() + " elements";
        String cpuStates = (m_refCpuStates == null) ? "null" : m_refCpuStates.size() + " elements";
        String sensorUsage = (m_refSensorUsage == null) ? "null" : m_refSensorUsage.size() + " elements";
        
        wakelocks = "Wl: " + wakelocks;
        kernelWakelocks = "KWl: " + kernelWakelocks;
        networkStats = "NetS: " + networkStats;
        alarms = "Alrm: " + alarms;
        processes = "Proc: " + processes;
        other = "Oth: " + other;
        cpuStates = "CPU: " + cpuStates;
        sensorUsage = "Sensors: " + sensorUsage;
        
        
        return "(" + wakelocks + "; " + kernelWakelocks + "; " + networkStats + "; " + alarms + "; "
        		+ processes + "; " + other + "; " + cpuStates + "; " + sensorUsage + ")";

    }

    @JsonIgnore
    public static String getLabel(String refName)
    {
    	String ret = "";
    	
    	if (refName.equals(CUSTOM_REF_FILENAME))
    		ret = CUSTOM_REF_NAME;
    	else if (refName.equals(UNPLUGGED_REF_FILENAME))
    		ret = UNPLUGGED_REF_NAME;
    	else if (refName.equals(CHARGED_REF_FILENAME))
    		ret = CHARGED_REF_NAME;
    	else if (refName.equals(SCREEN_OFF_REF_FILENAME))
    		ret = SCREEN_OFF_REF_NAME;
    	else if (refName.equals(SCREEN_ON_REF_FILENAME))
    		ret = SCREEN_ON_REF_NAME;
    	else if (refName.equals(BOOT_REF_FILENAME))
    		ret = BOOT_REF_NAME;
    	else if (refName.equals(CURRENT_REF_FILENAME))
    		ret = CURRENT_REF_NAME;
    	else if (refName.startsWith(TIMER_REF_FILENAME))
    		ret = refName;
    	
    	return ret;
    }

    @JsonIgnore
    public String getLabel()
    {
    	return Reference.getLabel(m_fileName);
    }

}
