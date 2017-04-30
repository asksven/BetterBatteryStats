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

import org.codehaus.jackson.annotate.JsonProperty;

import com.asksven.android.common.nameutils.UidInfo;

/**
 * A DTO for NativeKernelWakelock
 * @author sven
 *
 */
public class NativeKernelWakelockDto implements Serializable
{

	// from StatElement
	@JsonProperty("uid") public int m_uid = -1;

	@JsonProperty("total") public long m_total;	

	// from NativeKernelWakelock
	@JsonProperty("name") public String m_name;
	
	@JsonProperty("details") public String m_details;

	@JsonProperty("count") public int m_count;

	@JsonProperty("expire_count") public int m_expireCount;

	@JsonProperty("wake_count") public int m_wakeCount;

	@JsonProperty("active_since") public long m_activeSince;

	@JsonProperty("total_time") public long m_ttlTime;
	
	@JsonProperty("sleep_time") public  long m_sleepTime;
	
	@JsonProperty("max_time") public long m_maxTime;

	@JsonProperty("last_change") public long m_lastChange;

}
