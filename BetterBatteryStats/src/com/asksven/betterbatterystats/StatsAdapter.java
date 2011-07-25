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

import com.asksven.android.common.privateapiproxies.StatElement;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class StatsAdapter extends BaseAdapter
{
    private Context context;

    private List<StatElement> m_listData;

    public StatsAdapter(Context context, List<StatElement> listData)
    {
        this.context = context;
        this.m_listData = listData;
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

        return convertView;
    }

    private void showDialog(StatElement entry)
    {
        // Create and show your dialog
        // Depending on the Dialogs button clicks delete it or do nothing
    }

}
