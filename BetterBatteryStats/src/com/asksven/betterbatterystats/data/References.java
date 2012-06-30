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

import com.asksven.android.common.privateapiproxies.StatElement;

/**
 * A serializable value holder for stat references 
 * @author sven
 *
 */
class References implements Serializable
{
	
	private static final long serialVersionUID = 3L;
	
	protected static final String CUSTOM_REF_FILENAME 			= "custom_ref";
	protected static final String SINCE_UNPLUGGED_REF_FILENAME 	= "since_unplugged_ref";
	protected static final String SINCE_CHARGED_REF_FILENAME 	= "since_charged_ref";
	protected static final String SINCE_SCREEN_OFF_REF_FILENAME	= "since_screen_off";
	/** storage of custom references */
	protected String m_fileName								= "";
    protected ArrayList<StatElement> m_refWakelocks 		= null;
    protected ArrayList<StatElement> m_refKernelWakelocks 	= null;
    protected ArrayList<StatElement> m_refNetworkStats	 	= null;
    protected ArrayList<StatElement> m_refAlarms		 	= null;
    protected ArrayList<StatElement> m_refProcesses 		= null;
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
    }

}
