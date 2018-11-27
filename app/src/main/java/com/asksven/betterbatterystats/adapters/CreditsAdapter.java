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

import com.asksven.betterbatterystats.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class CreditsAdapter extends ArrayAdapter<String>
{
	private final Context m_context;

    private ArrayList<Credit> credits = null;

	public CreditsAdapter(Context context)
	{
		super(context, R.layout.credits_row);
		this.m_context = context;
        // special credits
        credits = new ArrayList<Credit>();
        credits.add(new Credit(context.getString(R.string.label_about_courtesy), "", ""));
        credits.add(new Credit(context.getString(R.string.label_about_translation1), "", ""));
        credits.add(new Credit(context.getString(R.string.label_about_translation2), "", ""));
        credits.add(new Credit(context.getString(R.string.label_about_translation3), "", ""));
        credits.add(new Credit(context.getString(R.string.label_about_translation4), "", ""));
        credits.add(new Credit(context.getString(R.string.label_about_translation5), "", ""));
        credits.add(new Credit(context.getString(R.string.label_about_translation6), "", ""));
        credits.add(new Credit(context.getString(R.string.label_about_translation7), "", ""));
        credits.add(new Credit(context.getString(R.string.label_about_translation8), "", ""));

        // libs
        credits.add(new Credit("libsuperuser", "Chainfire", "Apache 2.0"));
        credits.add(new Credit("Android Common", "asksven", "Apache 2.0"));
        credits.add(new Credit("Google gson", "", "Apache 2.0"));
        credits.add(new Credit("DashClock", "Roman Nurik", "Apache 2.0"));
        credits.add(new Credit("RootTools", "Stericson", "Apache 2.0"));
        credits.add(new Credit("ckChangeLog", "cketti", "Apache 2.0"));

    }

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		LayoutInflater inflater = (LayoutInflater) m_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.credits_row, parent, false);
		TextView textViewName = (TextView) rowView.findViewById(R.id.textViewName);
		TextView textViewAuthor = (TextView) rowView.findViewById(R.id.textViewAuthor);
		TextView textViewLicense = (TextView) rowView.findViewById(R.id.textViewLicense);

		textViewName.setText(credits.get(position).mName);
		textViewAuthor.setText(credits.get(position).mAuthor);
		textViewLicense.setText(credits.get(position).mLicense);

		return rowView;
	}
	
	public int getCount()
    {
    	if (credits != null)
    	{
    		return credits.size();
    	}
    	else
    	{
    		return 0;
    	}
    }

    public String getItem(int position)
    {
        return credits.get(position).mName;
    }

    public long getItemId(int position)
    {
        return position;
    }

    private class Credit
    {
        String mName ="";
        String mAuthor="";
        String mLicense="";

        Credit(String name, String author, String license)
        {
            mName = name;
            mAuthor = author;
            mLicense = license;

        }

    }

	
}