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

import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.asksven.android.common.kernelutils.Alarm;
import com.asksven.android.common.kernelutils.Alarm.AlarmItem;
import com.asksven.betterbatterystats.R;

public class AlarmsAdapter extends BaseAdapter
{
    private Context context;

    private List<Alarm> m_listData;
    

    public AlarmsAdapter(Context context, List<Alarm> listData)
    {
        this.context = context;
        this.m_listData = listData;        
    }

    public int getCount()
    {
    	int ret = 0;
    	if (m_listData != null)
    	{
    		ret = m_listData.size();
    	}
    	return ret;
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
    	Alarm entry = m_listData.get(position);
        if (convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.alarm_row, null);
        }
        TextView tvName = (TextView) convertView.findViewById(R.id.TextViewName);
       	tvName.setText(entry.getName());
        
        TextView tvWakeups = (TextView) convertView.findViewById(R.id.TextViewWakeups);
        tvWakeups.setText(entry.getData());
        
        convertView.setOnClickListener(new OnItemClickListener(position));
        return convertView;
    }
    
    @Override
    public void notifyDataSetChanged()
    {
      super.notifyDataSetChanged();
    }
    
    private class OnItemClickListener implements OnClickListener
    {           
        private int m_iPosition;
        OnItemClickListener(int position)
        {
                m_iPosition = position;
        }
        
        @Override
        public void onClick(View arg0)
        {
        	Alarm entry = (Alarm) getItem(m_iPosition);
            
        	Dialog dialog = new Dialog(context);

        	dialog.setContentView(R.layout.alarms_dialog);
        	dialog.setTitle("Details");

        	TextView title = (TextView) dialog.findViewById(R.id.title);
        	TextView subtitle = (TextView) dialog.findViewById(R.id.subtitle);
        	TextView text = (TextView) dialog.findViewById(R.id.text);
        	title.setText(entry.getName());
        	subtitle.setText(entry.getData());
        	
        	String strText = "";
        	ArrayList<AlarmItem> myItems = entry.getItems();
        	if (myItems != null)
        	{
        		for (int i=0; i<myItems.size(); i++)
        		{
        			strText = strText + myItems.get(i).getData() + "\n";
        		}
        	}
        	text.setText(strText);
//        	ImageView image = (ImageView) dialog.findViewById(R.id.image);
//        	image.setImageResource(R.drawable.icon_kb);
        	dialog.show();
        }
    }
}

