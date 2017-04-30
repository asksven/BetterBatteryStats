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
 * A DTO for Wakelock
 * @author sven
 *
 */
public class WakelockDto implements Serializable
{

	// from StatElement
	@JsonProperty("uid") public int m_uid;

	@JsonProperty("total") public long m_total;	

	// from Wakelock
	@JsonProperty("wake_type") public int m_wakeType;
	
	@JsonProperty("name") public String m_name;
	
	@JsonProperty("duration_ms") public long m_duration;
	
	@JsonProperty("count") public int m_count;

}
