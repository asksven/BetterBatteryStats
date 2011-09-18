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
package com.asksven.betterbatterystats;

/**
 * @author sven
 *
 */

import com.google.gson.Gson;
import com.asksven.betterbatterystats.data.KbData;
import com.asksven.betterbatterystats.data.SampleKbData;

public class KbReader
{
    private static KbData m_kb = null;
    
    public static KbData read()
    {
    	if (m_kb != null)
    	{
    		return m_kb;
    	}
    	else
    	{
	    	KbData data = null;
	    	try
	    	{
		        // Now do the magic.
		        data = new Gson().fromJson(SampleKbData.json, KbData.class);
		
		        // Show it.
		        System.out.println(data);
	    	}
	    	catch (Exception e)
	    	{
	    		e.printStackTrace();
	    	}
	    	m_kb = data;
    	}
    	return m_kb;
    }

}