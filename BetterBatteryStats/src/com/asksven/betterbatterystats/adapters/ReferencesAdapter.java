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
import com.asksven.betterbatterystats.data.ReferenceStore;

public class ReferencesAdapter extends ArrayAdapter<String>
{

    private List<String> m_listData;
    private static final String TAG = "ReferencesAdapter";

    /**
	 * @param context
	 * @param textViewResourceId
	 */
	public ReferencesAdapter(Context context, int textViewResourceId)
	{
		super(context, textViewResourceId);
		m_listData = ReferenceStore.getReferences(null, context);
	}


    public int getCount()
    {
        return m_listData.size();
    }

    public String getItem(int position)
    {
        return m_listData.get(position);
    }

    public int getPosition(String name)
    {
        return m_listData.indexOf(name);
    }

    public long getItemId(int position)
    {
        return position;
    }

}

