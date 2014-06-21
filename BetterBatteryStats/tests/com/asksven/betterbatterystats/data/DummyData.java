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
package com.asksven.betterbatterystats.data;

import java.util.ArrayList;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * @author sven
 *
 */
@JsonSerialize(include=JsonSerialize.Inclusion.ALWAYS)
@JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.ANY)
public class DummyData
{
	int m_int = 12;
	String mString = "abc";
	long m_Long = 122222L;
	ArrayList<String> m_Tags = null;
	
	DummyData()
	{
		m_Tags = new ArrayList<String>();
	}

}
