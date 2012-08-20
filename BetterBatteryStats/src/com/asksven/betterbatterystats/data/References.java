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

import android.util.Log;

import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.android.common.utils.DateUtils;

/**
 * A serializable value holder for stat references 
 * @author sven
 *
 */
class References implements Serializable
{
	
	private static final long serialVersionUID = 3L;
	
	private transient static final String TAG = "References";
	
	protected static final String CUSTOM_REF_FILENAME 			= "custom_ref";
	protected static final String SINCE_UNPLUGGED_REF_FILENAME 	= "since_unplugged_ref";
	protected static final String SINCE_CHARGED_REF_FILENAME 		= "since_charged_ref";
	protected static final String SINCE_SCREEN_OFF_REF_FILENAME	= "since_screen_off";
	protected static final String SINCE_BOOT_REF_FILENAME			= "since_boot";

	protected static final String NO_STATS_WHEN_CHARGING		= "Device is plugged in: no stats";
	protected static final String GENERIC_REF_ERR 			= "No reference set yet";
	protected static final String CUSTOM_REF_ERR 				= "No custom reference set yet";
	protected static final String SINCE_UNPLUGGED_REF_ERR 	= "No reference since unplugged set yet";
	protected static final String SINCE_CHARGED_REF_ERR 		= "No reference since charged set yet";
	protected static final String SINCE_SCREEN_OFF_REF_ERR	= "No reference since screen off set yet";
	protected static final String SINCE_BOOT_REF_ERR			= "No since boot reference set yet";

	/** storage of custom references */
	protected String m_fileName								= "";
	protected Date m_creationDate							= null;
    protected ArrayList<StatElement> m_refWakelocks 		= null;
    protected ArrayList<StatElement> m_refKernelWakelocks 	= null;
    protected ArrayList<StatElement> m_refNetworkStats	 	= null;
    protected ArrayList<StatElement> m_refAlarms		 	= null;
    protected ArrayList<StatElement> m_refProcesses 		= null;
    
    /** @todo unused, delete in 2.0 */
    protected ArrayList<StatElement> m_refNetwork	 		= null;
    protected ArrayList<StatElement> m_refOther	 			= null;
    protected ArrayList<StatElement> m_refCpuStates		 	= null;
    protected long m_refBatteryRealtime 					= 0;  
    protected int m_refBatteryLevel							= 0;
    protected int m_refBatteryVoltage						= 0;
    
    private References()
    {
    	
    }

    public References(String fileName)
    {
    	m_fileName = fileName;
    	m_creationDate = Calendar.getInstance().getTime();
    	Log.i(TAG, "Create ref " + m_fileName + " at " + DateUtils.format(m_creationDate));
    }
    
    public void setEmpty()
    {
    	m_creationDate.setTime(0);
    }
    
    public String getMissingRefError()
    {
    	if (m_fileName.equals(CUSTOM_REF_FILENAME))
    		return CUSTOM_REF_ERR;
    	else if (m_fileName.equals(SINCE_UNPLUGGED_REF_FILENAME))
    		return SINCE_UNPLUGGED_REF_ERR;
    	else if (m_fileName.equals(SINCE_CHARGED_REF_FILENAME))
    		return SINCE_CHARGED_REF_ERR;
    	else if (m_fileName.equals(SINCE_SCREEN_OFF_REF_FILENAME))
    		return SINCE_SCREEN_OFF_REF_ERR;
    	else if (m_fileName.equals(SINCE_BOOT_REF_FILENAME))
    		return SINCE_BOOT_REF_ERR;
    	else
    		return "No reference found";
    }
        	
        	
    public String whoAmI()
    {
    	return "Reference " + m_fileName + " created " + DateUtils.format(m_creationDate) + " " + elements();
    }
    
    private String elements()
    {
    	String wakelocks = (m_refWakelocks == null) ? "null" : m_refWakelocks.size() + " elements";
        String kernelWakelocks = (m_refKernelWakelocks == null) ? "null" : m_refKernelWakelocks.size() + "elements";
        String networkStats = (m_refNetworkStats == null) ? "null" : m_refNetworkStats.size() + " elements";
        String alarms = (m_refAlarms == null) ? "null" : m_refAlarms.size() + " elements";
        String processes = (m_refProcesses ==null) ? "null" : m_refAlarms + " elements";
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

}
