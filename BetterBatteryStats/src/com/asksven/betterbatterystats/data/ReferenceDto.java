/*
 * Copyright (C) 2014 asksven
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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import android.os.SystemClock;
import android.util.Log;

import com.asksven.android.common.dto.AlarmDto;
import com.asksven.android.common.dto.MiscDto;
import com.asksven.android.common.dto.NativeKernelWakelockDto;
import com.asksven.android.common.dto.NetworkUsageDto;
import com.asksven.android.common.dto.ProcessDto;
import com.asksven.android.common.dto.StateDto;
import com.asksven.android.common.dto.WakelockDto;
import com.asksven.android.common.kernelutils.NativeKernelWakelock;
import com.asksven.android.common.kernelutils.State;
import com.asksven.android.common.privateapiproxies.Alarm;
import com.asksven.android.common.privateapiproxies.KernelWakelock;
import com.asksven.android.common.privateapiproxies.Misc;
import com.asksven.android.common.privateapiproxies.NetworkUsage;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.android.common.privateapiproxies.Process;
import com.asksven.android.common.utils.DateUtils;
import com.asksven.betterbatterystats.LogSettings;
import com.asksven.android.common.privateapiproxies.Wakelock;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * A DTO for Reference 
 * @author sven
 *
 */
@JsonSerialize(include=JsonSerialize.Inclusion.ALWAYS)
@JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.ANY, getterVisibility=JsonAutoDetect.Visibility.NONE, setterVisibility=JsonAutoDetect.Visibility.NONE)
public class ReferenceDto
{
	@JsonProperty("filename") public String m_fileName													= "";
	@JsonProperty("creation_time") public long m_creationTime											= 0;
	@JsonProperty("type") public int m_refType															= 0;
	@JsonProperty("label") public String m_refLabel														= "";
	@JsonProperty("battery_realtime") public long m_refBatteryRealtime 									= 0;
	@JsonProperty("battery_level") public int m_refBatteryLevel											= 0;
	@JsonProperty("battery_voltage") public int m_refBatteryVoltage										= 0;
	
	@JsonProperty("partial_wakelocks") public ArrayList<WakelockDto> m_refWakelocks 					= null;
	
	@JsonProperty("kernel_wakelocks") public ArrayList<NativeKernelWakelockDto> m_refKernelWakelocks 	= null;
	
	@JsonProperty("network_stats") public ArrayList<NetworkUsageDto> m_refNetworkStats	 				= null;
	
	@JsonProperty("alarms") public ArrayList<AlarmDto> m_refAlarms		 								= null;
	
	@JsonProperty("processes") public ArrayList<ProcessDto> m_refProcesses 								= null;

	@JsonProperty("other_stats") public ArrayList<MiscDto> m_refOther	 								= null;
	
	@JsonProperty("cpu_states") public ArrayList<StateDto> m_refCpuStates								= null;
	
    
    
    protected static ReferenceDto fromJson(byte[] serializedReference)
    {
    	ReferenceDto ret = null;
    	ObjectMapper objectMapper = new ObjectMapper();
    	try
		{
			ret = objectMapper.readValue(serializedReference, ReferenceDto.class);
		}
		catch (JsonParseException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (JsonMappingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return ret;
    }

	protected String toJson()
    {
    	String ret = "";
    	StringWriter buffer = new StringWriter();
    	ObjectMapper mapper = new ObjectMapper();
    	
//    	mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
    	try
		{
    		ret = mapper.writeValueAsString(this);
			mapper.writeValue(buffer, this);
//    		ret = buffer.toString();
			
		}
		catch (JsonGenerationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (JsonMappingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    	return ret;
    }
    

}
