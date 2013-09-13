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

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.asksven.betterbatterystats.data.Reference;
import com.asksven.betterbatterystats.data.ReferenceStore;

/**
 * @author sven The Application class holds the application-wide settings
 */

public class BbsApplication extends Application
{
	int m_iStat = 0;
	private String m_refFromName = "";
	private String m_refToName = Reference.CURRENT_REF_FILENAME;

	@Override
	public void onCreate()
	{
		super.onCreate();
		
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    	m_iStat		= Integer.valueOf(sharedPrefs.getString("default_stat", "0"));
		m_refFromName	= sharedPrefs.getString("default_stat_type", Reference.UNPLUGGED_REF_FILENAME);

		if (!ReferenceStore.hasReferenceByName(m_refFromName, this))
		{
			if (sharedPrefs.getBoolean("fallback_to_since_boot", false))
			{
				m_refFromName = Reference.BOOT_REF_FILENAME;
			}
		}

	}

	public int getStat()
	{
		return m_iStat;
	}
	
	public String getRefFromName()
	{
		return m_refFromName;
	}

	public String getRefToName()
	{
		return m_refToName;
	}

	public void setStat(int stat)
	{
		m_iStat = stat;
	}
	
	public void setRefFromName(String name)
	{
		m_refFromName = name;
	}

	public void setRefToName(String name)
	{
		m_refToName = name;
	}

}