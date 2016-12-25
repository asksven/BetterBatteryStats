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

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.asksven.android.common.dto.AlarmDto;
import com.asksven.android.common.dto.MiscDto;
import com.asksven.android.common.dto.NativeKernelWakelockDto;
import com.asksven.android.common.dto.NetworkUsageDto;
import com.asksven.android.common.dto.ProcessDto;
import com.asksven.android.common.dto.SensorUsageDto;
import com.asksven.android.common.dto.StateDto;
import com.asksven.android.common.dto.WakelockDto;

/**
 * A DTO for Reference 
 * @author sven
 *
 */
@JsonSerialize(include=JsonSerialize.Inclusion.ALWAYS)
@JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.ANY, getterVisibility=JsonAutoDetect.Visibility.NONE, setterVisibility=JsonAutoDetect.Visibility.NONE)
public class ReferenceDto implements Serializable
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

	@JsonProperty("sensor_usage") public ArrayList<SensorUsageDto> m_refSensorUsage						= null;

    
	/**
	 * Deserialize
	 * @param serializedReference
	 * @return
	 */
    protected static ReferenceDto unmarshall(byte[] serializedReference)
    {
    	//return ReferenceDto.fromJson(serializedReference);
    	return ReferenceDto.deserialize(serializedReference);
    }

    /**
     * Serialize
     * @return
     */
	protected byte[] marshall()
    {
    	//return toJson();
		return serialize();
    }
	
	/** 
	 * Deserialize from JSON
	 * @param serializedReference
	 * @return
	 */
    private static ReferenceDto fromJson(byte[] serializedReference)
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

    /**
     * Serialize to JSON
     * @return
     */
	private byte[] toJson()
    {
    	byte[] ret = null;
    	StringWriter buffer = new StringWriter();
    	ObjectMapper mapper = new ObjectMapper();
    	
//    	mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
    	try
		{
    		ret = mapper.writeValueAsBytes(this);
//			mapper.writeValue(buffer, this);
//    		ret = buffer.toString();
			
		}
		catch (JsonGenerationException e)
		{
			e.printStackTrace();
		}
		catch (JsonMappingException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    	return ret;
    }
    
	/**
	 * Deserialize from Java serialization
	 * @param serializedReference
	 * @return
	 */
    private static ReferenceDto deserialize (byte[] serializedReference)
    {
		ByteArrayInputStream bis = null;
		ObjectInput in = null;
		ReferenceDto ret = null;
		
		try
		{
			bis = new ByteArrayInputStream(serializedReference);
			in = null;	
			in = new ObjectInputStream(bis);
			Object o = in.readObject();
			ret = (ReferenceDto) o;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				bis.close();
				in.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			catch (NullPointerException e)
			{
				// nothing went wrong
			}
		}
		
		return ret;
    }
    
    /**
     * Serialize using Java serialization
     * @return
     */
    private byte[] serialize()
    {
    	byte[] ret = null;

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		try
		{
			out = new ObjectOutputStream(bos);
			out.writeObject(this);
			ret = bos.toByteArray();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				out.close();
				bos.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			
		}
		
    	return ret;
    }

}
