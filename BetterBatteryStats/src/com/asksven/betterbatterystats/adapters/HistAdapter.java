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
package com.asksven.betterbatterystats.adapters;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.asksven.android.common.privateapiproxies.HistoryItem;
import com.asksven.betterbatterystats.R;

public class HistAdapter extends BaseAdapter
{
    private Context context;

    private List<HistoryItem> m_listData;
    

    public HistAdapter(Context context, List<HistoryItem> listData)
    {
        this.context = context;
        this.m_listData = listData;        
    }

    public int getCount()
    {
    	if (m_listData != null)
    	{
    		return m_listData.size();
    	}
    	else
    	{
    		return 0;
    	}
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
    	HistoryItem entry = m_listData.get(position);
        if (convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.hist_row, null);
        }
        TextView tvTime = (TextView) convertView.findViewById(R.id.TextViewTime);
       	tvTime.setText(entry.getNormalizedTime());
        
        TextView tvBatteryPct = (TextView) convertView.findViewById(R.id.TextViewBatteryPct);
        tvBatteryPct.setText(entry.getBatteryLevel());
        
        TextView tvCharging = (TextView) convertView.findViewById(R.id.TextViewCharing);
        tvCharging.setText(entry.getCharging());
        
        TextView tvScreenOn = (TextView) convertView.findViewById(R.id.TextViewScreenOn);
        tvScreenOn.setText(entry.getScreenOn());

        TextView tvGpsOn = (TextView) convertView.findViewById(R.id.TextViewGpsOn);
        tvGpsOn.setText(entry.getGpsOn());
        
        TextView tvWifiOn = (TextView) convertView.findViewById(R.id.TextViewWifiOn);
        tvWifiOn.setText(entry.getWifiRunning());

        TextView tvWakelock = (TextView) convertView.findViewById(R.id.TextViewWakelock);
        tvWakelock.setText(entry.getWakelock());

        TextView tvBtOn = (TextView) convertView.findViewById(R.id.TextViewBtOn);
        tvBtOn.setText(entry.getBluetoothOn());

        TextView tvInCall = (TextView) convertView.findViewById(R.id.TextViewPhoneInCall);
        tvInCall.setText(entry.getPhoneInCall());

        TextView tvPhoneScanning = (TextView) convertView.findViewById(R.id.TextViewPhoneScanning);
        tvPhoneScanning.setText(entry.getPhoneScanning());

        return convertView;
    }
    
    @Override
    public void notifyDataSetChanged()
    {
      super.notifyDataSetChanged();
    }
}

