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

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.asksven.android.common.kernelutils.CpuStates;
import com.asksven.android.common.privateapiproxies.NativeKernelWakelock;
import com.asksven.android.common.kernelutils.State;
import com.asksven.android.common.privateapiproxies.Alarm;
import com.asksven.android.common.privateapiproxies.KernelWakelock;
import com.asksven.android.common.privateapiproxies.Misc;
import com.asksven.android.common.privateapiproxies.NetworkUsage;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.android.common.privateapiproxies.Wakelock;
import com.asksven.android.common.privateapiproxies.Process;
import com.asksven.betterbatterystats.data.Reference;

import junit.framework.TestCase;

/**
 * @author sven
 *
 */
public class ReferenceTests extends TestCase
{



	Reference getTestReference()
	{
		Reference testData = new Reference("test", 1);

		testData.m_refWakelocks = new ArrayList<StatElement>();
		testData.m_refWakelocks.add(new Wakelock(1, "name", 2, 3, 4));
		testData.m_refAlarms = new ArrayList<StatElement>();
		Alarm testAlarm = new Alarm("name");
		testAlarm.addItem(2, "intent");
		testData.m_refAlarms.add(testAlarm);
		testData.m_refKernelWakelocks = new ArrayList<StatElement>();
		testData.m_refKernelWakelocks.add(new NativeKernelWakelock("name", "details", 1, 2, 3, 0, 0, 0, 0, 0, 0));
		testData.m_refNetworkStats = new ArrayList<StatElement>();
		testData.m_refNetworkStats.add(new NetworkUsage(1, "name", 2, 3));
		testData.m_refProcesses = new ArrayList<StatElement>();
		testData.m_refProcesses.add(new Process("name", 1, 2, 3));
		testData.m_refOther = new ArrayList<StatElement>();
		testData.m_refOther.add(new Misc("name", 1, 2));
		testData.m_refCpuStates = new ArrayList<StatElement>();
		testData.m_refCpuStates.add(new State(12, 13));

		return testData;
	}
	/**
	 * Test method for {@link com.asksven.betterbatterystats.data.Reference#toJson()}.
	 */
	public void testDummyToJson()
	{
		DummyData testData = new DummyData();
		
    	String ret = "";
    	StringWriter buffer = new StringWriter();
    	ObjectMapper mapper = new ObjectMapper();
    	
//	    	mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
    	try
		{
    		ret = mapper.writeValueAsString(testData);
			
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


		assertTrue(ret.length() != 0);
	}

}
