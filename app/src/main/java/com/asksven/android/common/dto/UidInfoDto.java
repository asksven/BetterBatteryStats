/*
 * Copyright (C) 2011 asksven
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

/**
 * @author sven
 *
 */
public class UidInfoDto implements Serializable
{
	@JsonProperty("uid") public int m_uid;
	@JsonProperty("name") public String m_uidName = "";
	@JsonProperty("package")public String m_uidNamePackage = "";
	@JsonProperty("unique") public boolean m_uidUniqueName = false;
	
}
