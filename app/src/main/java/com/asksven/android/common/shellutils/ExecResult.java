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

package com.asksven.android.common.shellutils;

import java.util.ArrayList;

public class ExecResult
{
	protected boolean m_bSuccess;
	protected ArrayList<String> m_oResult;
	protected ArrayList<String> m_oError;
	
	public ExecResult()
	{
		m_oResult = new ArrayList<String>();
		m_oError = new ArrayList<String>();
		
	}
	
	public boolean getSuccess()
	{
		return m_bSuccess;
	}
	
	public ArrayList<String> getResult()
	{
		return m_oResult;
	}
	
	public String getResultLine()
	{
		String strRes = "";
		if (!m_oResult.isEmpty()) 
		{
			strRes = m_oResult.get(0);
		}
		
		return strRes;
	}
}
