/*
 * Copyright (C) 2011-2014 asksven
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
package com.asksven.betterbatterystats.adapters;

import java.util.ArrayList;

import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.android.common.privateapiproxies.HistoryItem;
import com.asksven.android.system.AndroidVersion;
import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.data.BatteryGraphSeries;
import com.asksven.betterbatterystats.widgets.GraphableBarsTimeline;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class GraphsAdapter extends ArrayAdapter<String>
{
	private static final String TAG = "GraphsAdapter";
	private final Context m_context;
	private ArrayList<BatteryGraphSeries> m_graphs = new ArrayList<BatteryGraphSeries>();

	protected static ArrayList<HistoryItem> m_histList;

	public GraphsAdapter(Context context, ArrayList<HistoryItem> history)
	{
		super(context, R.layout.credits_row);
		this.m_context = context;
		m_histList = history;
		this.seriesSetup();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
        if (convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) m_context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            
        	convertView = inflater.inflate(R.layout.timeline, null);
        }

		TextView textViewTitle = (TextView) convertView.findViewById(R.id.textViewTitle);
		textViewTitle.setText(m_graphs.get(position).getTitle());
		
        GraphableBarsTimeline bars = (GraphableBarsTimeline) convertView.findViewById(R.id.Timeline);	
        bars.setValues(m_graphs.get(position).getValues());


		return convertView;
	}
	
	public int getCount()
    {
    	if (m_graphs != null)
    	{
    		return m_graphs.size();
    	}
    	else
    	{
    		return 0;
    	}
    }

    public String getItem(int position)
    {
        return m_graphs.get(position).getTitle();
    }

    public long getItemId(int position)
    {
        return position;
    }
    
	private void seriesSetup()
    {
        // SERIES #2:
        BatteryGraphSeries mySerie1 = new BatteryGraphSeries(
        		m_histList,
        		BatteryGraphSeries.SERIE_WAKELOCK,
        		m_context.getString(R.string.label_graph_wakelock));

        m_graphs.add(mySerie1);	        

        
        // SERIES #3:
		BatteryGraphSeries mySerie2 = new BatteryGraphSeries(
				m_histList,
				BatteryGraphSeries.SERIE_SCREENON,
				m_context.getString(R.string.label_graph_screen));
		m_graphs.add(mySerie2);	        


        // SERIES #4:
		BatteryGraphSeries mySerie3 = new BatteryGraphSeries(
				m_histList,
				BatteryGraphSeries.SERIE_WIFI,
				m_context.getString(R.string.label_graph_wifi));
		m_graphs.add(mySerie3);	        


        // SERIES #4:
		BatteryGraphSeries mySerie4 = new BatteryGraphSeries(
				m_histList,
				BatteryGraphSeries.SERIE_CHARGING,
				m_context.getString(R.string.label_graph_power));
		m_graphs.add(mySerie4);	                

        // SERIES #6:
		BatteryGraphSeries mySerie6 = new BatteryGraphSeries(
				m_histList,
				BatteryGraphSeries.SERIE_GPS,
				m_context.getString(R.string.label_graph_gps));
		m_graphs.add(mySerie6);	        


        // SERIES #7:
		BatteryGraphSeries mySerie7 = new BatteryGraphSeries(
				m_histList,
				BatteryGraphSeries.SERIE_BT,
				m_context.getString(R.string.label_graph_bluetooth));
		m_graphs.add(mySerie7);	        



    }

	
}