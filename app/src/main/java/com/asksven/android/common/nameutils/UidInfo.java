/*
 * Copyright (C) 2011-2018 asksven
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

package com.asksven.android.common.nameutils;

import com.asksven.android.common.dto.UidInfoDto;

import java.io.Serializable;

/**
 * @author sven
 *
 */
public class UidInfo implements Serializable
{
	private int m_uid;
	private String m_uidName = "";
	private String m_uidNamePackage = "";
	private boolean m_uidUniqueName = false;
	private static final long serialVersionUID = 1L;

    public UidInfo()
    {
    }

    public UidInfo(UidInfoDto source)
    {
		this.m_uid				= source.m_uid;
		this.m_uidName 			= source.m_uidName;
		this.m_uidNamePackage	= source.m_uidNamePackage;
		this.m_uidUniqueName	= source.m_uidUniqueName;
    }
    
    public UidInfoDto toDto()
    {
    	UidInfoDto ret = new UidInfoDto();
		ret.m_uid 				= this.m_uid;
		ret.m_uidName 			= this.m_uidName;
		ret.m_uidNamePackage	= this.m_uidNamePackage;
		ret.m_uidUniqueName		= this.m_uidUniqueName;
		
    	return ret;
    }

	/**
	 * @return the m_uid
	 */
	public int getUid()
	{
		return m_uid;
	}

	/**
	 * @param m_uid the m_uid to set
	 */
	public void setUid(int m_uid)
	{
		this.m_uid = m_uid;
	}

	/**
	 * @return the uidName
	 */
	public String getName()
	{
		return m_uidName;
	}

	/**
	 * @param uidName the uidName to set
	 */
	public void setName(String uidName)
	{
		this.m_uidName = uidName;
	}

	/**
	 * @return the uidNamePackage
	 */
	public String getNamePackage()
	{
		return m_uidNamePackage;
	}

	/**
	 * @param uidNamePackage the uidNamePackage to set
	 */
	public void setNamePackage(String uidNamePackage)
	{
		this.m_uidNamePackage = uidNamePackage;
	}

	/**
	 * @return the uidUniqueName
	 */
	public boolean isUniqueName()
	{
		return m_uidUniqueName;
	}

	/**
	 * @param uidUniqueName the uidUniqueName to set
	 */
	public void setUniqueName(boolean uidUniqueName)
	{
		this.m_uidUniqueName = uidUniqueName;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "UidInfo [m_uid=" + m_uid + ", m_uidName=" + m_uidName
				+ ", m_uidNamePackage=" + m_uidNamePackage
				+ ", m_uidUniqueName=" + m_uidUniqueName + "]";
	}
	
	
}
