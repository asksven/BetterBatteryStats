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

import android.util.Log;

import java.util.ArrayList;

/**
 * The data source for all series to plot
 *
 * @author sven
 */
public class GraphSerie
{

	private String m_title;
	private ArrayList<Datapoint> m_serie;

	static final String TAG = "GraphSerie";

	public GraphSerie(String title, ArrayList<Datapoint> serie)
	{
		if (serie != null)
		{
			m_serie = serie;
		} else
		{
			m_serie = new ArrayList<Datapoint>();
		}

		m_title = title;

		if (m_serie != null)
		{
			Log.i(TAG, "Added Serie " + m_title + " with " + m_serie.size() + " entries");
		} else
		{
			Log.i(TAG, "Added Serie was null");
		}
	}

	public String getTitle()
	{
		return m_title;
	}

	public int size()
	{
		return m_serie.size();
	}


	public ArrayList<Datapoint> getValues()
	{
		return m_serie;
	}


}
