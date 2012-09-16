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
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PermissionInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.Contacts;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.data.Permission;


public class PermissionsAdapter extends BaseAdapter
{
    private Context m_context;

    private List<String> m_listData;
    private Map<String, Permission> m_dictionary;
    private static final String TAG = "PermissionsAdapter";
	int m_selectedPosition = 0;
	boolean m_expanded = false;


    public PermissionsAdapter(Context context, List<String> listData, Map<String, Permission> dictionary)
    {
        this.m_context = context;
        this.m_listData = listData;
        this.m_dictionary = dictionary;

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

        Permission permData = m_dictionary.get(entry);
        if (permData != null)
        {
	        TextView tvDescription = (TextView) convertView.findViewById(R.id.TextViewDescription);
	        tvDescription.setText(permData.description);
		        
	        int color;
	       	switch(permData.level)
	       	{
			case PermissionInfo.PROTECTION_DANGEROUS:
				color = R.color.dangerous_color;
				break;
			case PermissionInfo.PROTECTION_SIGNATURE:
			case PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM:
				color = R.color.signature_color;
				break;
			default:
				color = android.R.color.primary_text_light;
				break;
			}
	    	LinearLayout nameLayout = (LinearLayout) convertView.findViewById(R.id.LayoutName);

			nameLayout.setBackgroundColor(m_context.getResources().getColor(color));

        }
        
    	LinearLayout descriptionLayout = (LinearLayout) convertView.findViewById(R.id.LayoutDescription);

       	if (m_selectedPosition == position)
       	{
       		if (m_expanded)
       		{
        		descriptionLayout.setVisibility(View.VISIBLE);
       		}
       		else
       		{
        		descriptionLayout.setVisibility(View.GONE);
       		}
       	}
       	else
       	{
    		descriptionLayout.setVisibility(View.GONE);
       	}



        return convertView;
    }
}

