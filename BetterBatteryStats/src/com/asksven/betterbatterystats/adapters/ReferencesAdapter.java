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

import java.util.ArrayList;
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
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.data.Permission;
import com.asksven.betterbatterystats.data.Reference;
import com.asksven.betterbatterystats.data.ReferenceStore;

public class ReferencesAdapter extends ArrayAdapter<String>
{

    private List<String> m_listLabels;
    private List<String> m_listNames;
    private static final String TAG = "ReferencesAdapter";

    /**
	 * @param context
	 * @param textViewResourceId
	 */
	public ReferencesAdapter(Context context, int textViewResourceId)
	{
		super(context, textViewResourceId);
		refresh(context);
		m_listNames = ReferenceStore.getReferenceNames(null, context);
		m_listLabels = ReferenceStore.getReferenceLabels(null, context);
	}


	public void refreshFromSpinner(Context context)
	{
		this.refresh(context);
		
		int posCurrent = m_listNames.indexOf(Reference.CURRENT_REF_FILENAME);
		
		if (posCurrent != -1)
		{	
			m_listNames.remove(posCurrent);
			m_listLabels.remove(posCurrent);		
		}
		this.notifyDataSetChanged();
	}

	private void refresh(Context context)
	{
		m_listNames = ReferenceStore.getReferenceNames(null, context);
		m_listLabels = ReferenceStore.getReferenceLabels(null, context);		
	}

	
	public void filterToSpinner(String refName, Context context)
	{
		m_listNames = ReferenceStore.getReferenceNames(refName, context);
		m_listLabels = ReferenceStore.getReferenceLabels(refName, context);
		
		// remove "charged" and "unplugged" from to spinner as those make no sense
		int posCurrent = m_listNames.indexOf(Reference.CHARGED_REF_FILENAME);
		
		if (posCurrent != -1)
		{	
			m_listNames.remove(posCurrent);
			m_listLabels.remove(posCurrent);		
		}

		posCurrent = m_listNames.indexOf(Reference.UNPLUGGED_REF_FILENAME);
		
		if (posCurrent != -1)
		{	
			m_listNames.remove(posCurrent);
			m_listLabels.remove(posCurrent);		
		}

		this.notifyDataSetChanged();
		
	}
    public int getCount()
    {
        return m_listNames.size();
    }

    public String getItem(int position)
    {
    	if (position < m_listLabels.size())
    	{
    		return m_listLabels.get(position);
    	}
    	else
    	{
    		return "";
    	}
    }

    public String getItemName(int position)
    {
        return m_listNames.get(position);
    }

    public int getPosition(String name)
    {
    	int ret = m_listNames.indexOf(name);
        return ret;
    }

    
    public int getPositionForRefName(String name)
    {
    	int ret = m_listLabels.indexOf(name);
        return ret;
    }

    public long getItemId(int position)
    {
        return position;
    }

}

