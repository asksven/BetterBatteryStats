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
import org.codehaus.jackson.map.ser.std.TimeZoneSerializer;

import com.asksven.android.common.kernelutils.CpuStates;
import com.asksven.android.common.privateapiproxies.NativeKernelWakelock;
import com.asksven.android.common.kernelutils.State;
import com.asksven.android.common.privateapiproxies.Alarm;
import com.asksven.android.common.privateapiproxies.HistoryItem;
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
public class GraphSeriesFactoryTests extends TestCase
{



	ArrayList<HistoryItem> getTestBatteryData()
	{
		ArrayList<HistoryItem> testData = new ArrayList<HistoryItem>();
		Long[] timestamps = {1416405215261L, 1416405215465L, 1416405224514L, 1416405240194L, 1416405242746L,
				1416405255284L, 1416405258300L, 1416405261313L, 1416405265069L, 1416405270363L,
				1416405287955L, 1416405288170L, 1416405288968L,	1416405294829L, 1416405295026L};
		Integer[] values = { 100, 100, 100, 100, 100,
				100, 100, 100, 100, 100,
				100, 100, 100, 100, 100};
		
		Byte z = 0;

		for (int i = 0; i < timestamps.length; i++)
		{
			HistoryItem item = new HistoryItem(timestamps[i], z, values[i].byteValue(), z, z, z, "0", "0", 0);
			testData.add(item);
		}
		return testData;
	}

	public void testFactory()
	{
		ArrayList<HistoryItem> hist = getTestBatteryData();
		GraphSeriesFactory fact = new GraphSeriesFactory(hist);
		
		assertFalse(fact.getValues(GraphSeriesFactory.SERIE_CHARGE).isEmpty());
		assertTrue(fact.getValues(GraphSeriesFactory.SERIE_CHARGE).size() == 2);
			}

}
