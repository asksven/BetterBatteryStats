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

import java.util.List;
import java.util.Vector;
import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;

public class WakelockStatsActivity extends ListActivity
{
	/**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wakelock_stats);

        setListAdapter(new ArrayAdapter<String>(this,
                       android.R.layout.simple_list_item_1,  
                       getStatList()));
	}
	
	List<String> getStatList()
	{
		List<String> myStats = new Vector<String>();
		myStats.add("value 1");
		myStats.add("value 2");
		
		return myStats;
	}
}
