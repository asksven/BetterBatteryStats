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

package com.asksven.android.common.dto;

import java.io.Serializable;
import java.util.ArrayList;

import org.codehaus.jackson.annotate.JsonProperty;

import com.asksven.android.common.nameutils.UidInfo;

/**
 * A DTO class for Alarms
 * @author sven
 *
 */
public class AlarmDto implements Serializable
{

	// from StatElement
	@JsonProperty("uid") public int m_uid = -1;
	
	@JsonProperty("total") public long m_total;	

	// from Alarm
	@JsonProperty("package_name") public String m_strPackageName;

	// from Alarm
	@JsonProperty("details") public String m_details;

	@JsonProperty("wakeups") public long m_nWakeups;
	
	@JsonProperty("total_count") public long m_nTotalCount;
	
	@JsonProperty("time_running_ms") public long m_timeRunning;
	
	@JsonProperty("items") public ArrayList<AlarmItemDto> m_items;
}
