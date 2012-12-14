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
package com.asksven.betterbatterystats.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.os.SystemClock;
import android.util.Log;

import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.android.common.utils.DateUtils;

/**
 * A serializable value holder for stat references 
 * @author sven
 *
 */
public class Reference implements Serializable
{
	
	private static final long serialVersionUID = 3L;
	
	private transient static final String TAG = "References";
	
	public static final String CUSTOM_REF_FILENAME 				= "ref_custom";
	public static final String UNPLUGGED_REF_FILENAME 			= "ref_unplugged";
	public static final String CHARGED_REF_FILENAME 			= "ref_charged";
	public static final String SCREEN_OFF_REF_FILENAME			= "ref_screen_off";
	public static final String SCREEN_ON_REF_FILENAME			= "ref_screen_on";
	public static final String BOOT_REF_FILENAME				= "ref_boot";
	public static final String CURRENT_REF_FILENAME 			= "ref_current";

	public static final int TYPE_CUSTOM 	= 1;
	public static final int TYPE_EVENT 		= 2;
	public static final int TYPE_CURRENT 	= 3;
	
	
	protected static final String[] FILES = {CUSTOM_REF_FILENAME, CURRENT_REF_FILENAME, UNPLUGGED_REF_FILENAME, CHARGED_REF_FILENAME, SCREEN_OFF_REF_FILENAME, SCREEN_ON_REF_FILENAME, BOOT_REF_FILENAME};

	protected static final String NO_STATS_WHEN_CHARGING		= "Device is plugged in: no stats";
	protected static final String GENERIC_REF_ERR 				= "No reference set yet";
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

	/** storage of custom references */
	public String m_fileName								= "";
	protected long m_creationTime							= 0;
	protected int m_refType									= 0;
	protected String m_refLabel								= "";
    protected ArrayList<StatElement> m_refWakelocks 		= null;
    protected ArrayList<StatElement> m_refKernelWakelocks 	= null;
    protected ArrayList<StatElement> m_refNetworkStats	 	= null;
    protected ArrayList<StatElement> m_refAlarms		 	= null;
    protected ArrayList<StatElement> m_refProcesses 		= null;
    protected ArrayList<StatElement> m_refOther	 			= null;
    protected ArrayList<StatElement> m_refCpuStates			= null;
    protected long m_refBatteryRealtime 					= 0;  
    protected int m_refBatteryLevel							= 0;
    protected int m_refBatteryVoltage						= 0;
    
    private Reference()
    {
    	
    }

    public Reference(String fileName, int type)
    {
    	m_fileName 		= fileName;
    	m_creationTime 	= System.currentTimeMillis(); //SystemClock.elapsedRealtime();
    	m_refType 		= type;
    	m_refLabel		= getLabel(fileName);
//    	+ " " + DateUtils.format(m_creationTime);			
    	Log.i(TAG, "Create ref " + m_fileName + " at " + DateUtils.formatDuration(m_creationTime));
    }
    
    public void setEmpty()
    {
    	m_creationTime = 0;
    }
    public void setTimestamp()
    {
    	m_creationTime = SystemClock.elapsedRealtime();
    }
    
    
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
        
        wakelocks = "Wl: " + wakelocks;
        kernelWakelocks = "KWl: " + kernelWakelocks;
        networkStats = "NetS: " + networkStats;
        alarms = "Alrm: " + alarms;
        processes = "Proc: " + processes;
        other = "Oth: " + other;
        cpuStates = "CPU: " + cpuStates;
        
        return "(" + wakelocks + "; " + kernelWakelocks + "; " + networkStats + "; " + alarms + "; "
        		+ processes + "; " + other + "; " + cpuStates + ")";

    }

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
    	
    	return ret;
    }

    public String getLabel()
    {
    	return Reference.getLabel(m_fileName);
    }

}
