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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.betterbatterystats.widgets.GraphableButton;
import com.asksven.betterbatterystats.R;

public class StatsAdapter extends BaseAdapter
{
    private Context context;

    private List<StatElement> m_listData;
    private double m_maxValue = 0;

    public StatsAdapter(Context context, List<StatElement> listData)
    {
        this.context = context;
        this.m_listData = listData;
        for (int i = 0; i < m_listData.size(); i++)
        {
        	StatElement g = m_listData.get(i);
        	double[] values = g.getValues();
        	m_maxValue = Math.max(m_maxValue, values[values.length - 1]);
            m_maxValue = Math.max(m_maxValue, g.getMaxValue());
        }
    }

    public int getCount()
    {
        return m_listData.size();
    }

    public Object getItem(int position)
    {
        return m_listData.get(position);
    }

    public long getItemId(int position)
    {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup viewGroup)
    {
    	StatElement entry = m_listData.get(position);
        if (convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.stat_row, null);
        }
        TextView tvName = (TextView) convertView.findViewById(R.id.TextViewName);
        tvName.setText(entry.getName());

        TextView tvFqn = (TextView) convertView.findViewById(R.id.TextViewFqn);
        tvFqn.setText(entry.getFqn(context));

        TextView tvData = (TextView) convertView.findViewById(R.id.TextViewData);
        tvData.setText(entry.getData());

        // Only do if prefs are set accordingly
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.context);
        
        LinearLayout myLayout = (LinearLayout) convertView.findViewById(R.id.LinearLayoutBar);
		
        GraphableButton buttonBar = (GraphableButton) convertView.findViewById(R.id.ButtonBar);
        if (sharedPrefs.getBoolean("hide_bars", false))
        {
        	myLayout.setVisibility(View.INVISIBLE);
        	buttonBar.setHeight(0);
        	
        }
        else
        {
        	myLayout.setVisibility(View.VISIBLE);
        	buttonBar.setValues(entry.getValues(), m_maxValue);
        	buttonBar.setHeight(20);
//        	double[] test = new double[2];
//        	test[0] = 50;
//        	double max = 100;
//        	buttonBar.setValues(test, max);
        	
        }
        return convertView;
    }

    private void showDialog(StatElement entry)
    {
        // Create and show your dialog
        // Depending on the Dialogs button clicks delete it or do nothing
    }

}
