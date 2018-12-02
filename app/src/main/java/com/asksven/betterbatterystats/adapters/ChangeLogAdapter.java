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

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.asksven.betterbatterystats.R;

import java.util.ArrayList;
import java.util.List;

import de.cketti.library.changelog.ChangeLog;

public class ChangeLogAdapter extends ArrayAdapter<String>
{
	private final Context m_context;

    private List<String> changeLog = null;

    private String releaseName = "";

	public ChangeLogAdapter(Context context)
	{
		super(context, R.layout.changelog_row);
		this.m_context = context;

		changeLog = new ArrayList<String>();
        ChangeLog cl = new ChangeLog(context);
        List<ChangeLog.ReleaseItem> releases = cl.getChangeLog(true);
        if (releases.size() > 0)
        {
            releaseName = releases.get(0).versionName;
            List<String> changes = releases.get(0).changes;

            for (int j=0; j < changes.size(); j++)
            {
                changeLog.add(changes.get(j));
            }
        }
        else
        {
            changeLog.add("n/a");
        }
    }

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		LayoutInflater inflater = (LayoutInflater) m_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.changelog_row, parent, false);
		TextView textViewChange = (TextView) rowView.findViewById(R.id.textViewChange);

        textViewChange.setText(changeLog.get(position));

		return rowView;
	}
	
	public int getCount()
    {
    	if (changeLog != null)
    	{
    		return changeLog.size();
    	}
    	else
    	{
    		return 0;
    	}
    }

    public String getItem(int position)
    {
        return changeLog.get(position);
    }

    public String getReleaseName()
    {
        return releaseName;
    }

    public long getItemId(int position)
    {
        return position;
    }

}