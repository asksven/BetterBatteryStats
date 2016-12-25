/*
 * Copyright (C) 2011-2012 asksven
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
import android.widget.LinearLayout;
import android.widget.TextView;
import com.asksven.betterbatterystats.R;

public class ServicesAdapter extends BaseAdapter
{
    private Context m_context;

    private List<String> m_listData;
    private static final String TAG = "ServicesAdapter";
	int m_selectedPosition = 0;
	boolean m_expanded = false;


    public ServicesAdapter(Context context, List<String> listData)
    {
        this.m_context = context;
        this.m_listData = listData;

    }

    public void setSelectedPosition(int position)
    {
    	m_selectedPosition = position;
    	notifyDataSetChanged();
    }
    
    public void toggleExpand()
    {
    	m_expanded = !m_expanded;
    	notifyDataSetChanged();
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
    	String entry = m_listData.get(position);
        if (convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) m_context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.package_info_row, null);
        }
        TextView tvName = (TextView) convertView.findViewById(R.id.TextViewName);
       	tvName.setText(entry);
       	LinearLayout descriptionLayout = (LinearLayout) convertView.findViewById(R.id.LayoutDescription);

  		descriptionLayout.setVisibility(View.GONE);
        return convertView;
    }
}

