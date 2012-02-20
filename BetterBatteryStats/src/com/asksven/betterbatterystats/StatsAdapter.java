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
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.asksven.android.common.kernelutils.Alarm;
import com.asksven.android.common.kernelutils.Alarm.AlarmItem;
import com.asksven.android.common.kernelutils.NativeKernelWakelock;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.betterbatterystats.data.KbData;
import com.asksven.betterbatterystats.data.KbEntry;
import com.asksven.betterbatterystats.widgets.GraphableButton;
import com.asksven.betterbatterystats.R;

public class StatsAdapter extends BaseAdapter
{
    private Context context;

    private List<StatElement> m_listData;

    private double m_maxValue = 0;
    
    /** The Knowlegde base */
    private KbData m_kb;

    public StatsAdapter(Context context, List<StatElement> listData)
    {
        this.context = context;
        this.m_listData = listData;

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.context);
        boolean bKbEnabled = sharedPrefs.getBoolean("enable_kb", true);
        
        if (bKbEnabled)
        {
        	// async read KB
        	new ReadKb().execute("");
        }
        
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

       	KbEntry kbentry = null;
        if (m_kb != null)
        {
        	 kbentry = m_kb.findByStatElement(entry.getName(), entry.getFqn(context));
        }
        
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.context);
        boolean bShowKb = sharedPrefs.getBoolean("enable_kb", true);
        ImageView iconKb = (ImageView) convertView.findViewById(R.id.imageKB);
        if ( (bShowKb) && (kbentry != null))
        {
        	iconKb.setVisibility(View.VISIBLE);
        }
        else
        {
        	iconKb.setVisibility(View.INVISIBLE);
        }
        TextView tvFqn = (TextView) convertView.findViewById(R.id.TextViewFqn);
        tvFqn.setText(entry.getFqn(context));

        TextView tvData = (TextView) convertView.findViewById(R.id.TextViewData);
        tvData.setText(entry.getData());
        
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
        }
        
        // add on click listener for the icon only if KB is enabled
        if (bShowKb)
        {
	        // set a click listener for the list
	        iconKb.setOnClickListener(new OnIconClickListener(position));
        }
        
        // add on click listener for the list entry if details are availble
        if ( (entry instanceof Alarm) || (entry instanceof NativeKernelWakelock) )
        {
        	convertView.setOnClickListener(new OnItemClickListener(position));
        }
        return convertView;
    }
    
    /**
     * Handler for on click of the KB icon
     * @author sven
     *
     */
    private class OnIconClickListener implements OnClickListener
    {           
        private int m_iPosition;
        OnIconClickListener(int position)
        {
                m_iPosition = position;
        }
        
        @Override
        public void onClick(View arg0)
        {
        	StatElement entry = (StatElement) getItem(m_iPosition);
            
        	// the timing may lead to m_kb not being initialized yet, it must be checked
        	if (m_kb == null)
        	{
        		return;
        	}
        	KbEntry kbentry = m_kb.findByStatElement(entry.getName(), entry.getFqn(StatsAdapter.this.context));
  	      	if (kbentry != null)
  	      	{
	  	      	SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(StatsAdapter.this.context);
	  	      	
	  	      	String url = kbentry.getUrl();
	  	      	
	  	        if (sharedPrefs.getBoolean("kb_ext_browser", true))
	  	        {
					
					Intent intent = new Intent("android.intent.action.VIEW",
							Uri.parse(url)); 
					StatsAdapter.this.context.startActivity(intent);
	  	        }
	  	        else
	  	        {
		  	      	Intent intentKB = new Intent(StatsAdapter.this.context,
		  	      			HelpActivity.class);
		  	      	intentKB.putExtra("url", url);
		  	        StatsAdapter.this.context.startActivity(intentKB);
	  	        }           
  	      	}
        }
    }
    
    /**
     * Handler for the on click of the list item
     * @author sven
     *
     */
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
        	StatElement entry = (StatElement) getItem(m_iPosition);

        	if (entry instanceof Alarm)
        	{
	        	Alarm alarmEntry = (Alarm) getItem(m_iPosition);
	            
	        	Dialog dialog = new Dialog(context);
	
	        	dialog.setContentView(R.layout.alarms_dialog);
	        	dialog.setTitle("Details");
	
	        	TextView title = (TextView) dialog.findViewById(R.id.title);
	        	TextView subtitle = (TextView) dialog.findViewById(R.id.subtitle);
	        	TextView text = (TextView) dialog.findViewById(R.id.text);
	        	title.setText(entry.getName());
	        	subtitle.setText(entry.getData());
	        	
	        	String strText = "";
	        	ArrayList<AlarmItem> myItems = alarmEntry.getItems();
	        	if (myItems != null)
	        	{
	        		for (int i=0; i<myItems.size(); i++)
	        		{
	        			if (myItems.get(i).getCount() > 0)
	        			{
	        				strText = strText + myItems.get(i).getData() + "\n";
	        			}
	        		}
	        	}
	        	text.setText(strText);
	        	dialog.show();
	        }
        	if (entry instanceof NativeKernelWakelock)
        	{
        		NativeKernelWakelock kernelWakelockEntry = (NativeKernelWakelock) getItem(m_iPosition);
                
            	Dialog dialog = new Dialog(context);

            	dialog.setContentView(R.layout.alarms_dialog);
            	dialog.setTitle("Details");

            	TextView title = (TextView) dialog.findViewById(R.id.title);
            	TextView subtitle = (TextView) dialog.findViewById(R.id.subtitle);
            	TextView text = (TextView) dialog.findViewById(R.id.text);
            	title.setText(kernelWakelockEntry.getName());
            	subtitle.setText(kernelWakelockEntry.getData());
            	
            	String strText = "";
            	strText += "Count: " + kernelWakelockEntry.getCount() + "\n";
            	strText += "Expire Count: " + kernelWakelockEntry.getExpireCount() + "\n";
            	strText += "Wake Count: " + kernelWakelockEntry.getWakeCount() + "\n";
            	strText += "Total Time: "+ kernelWakelockEntry.getTtlTime() + "\n";
            	strText += "Sleep Time: " + kernelWakelockEntry.getSleepTime() + "\n";
            	strText += "Max Time: " + kernelWakelockEntry.getMaxTime() + "\n";

            	text.setText(strText);
            	dialog.show();

        	}
        }
    }

    private class ReadKb extends AsyncTask
	{
		@Override
	    protected Object doInBackground(Object... params)
	    {
			// retrieve KB
			StatsAdapter.this.m_kb = KbReader.read(StatsAdapter.this.context);

	    	return true;
	    }

		@Override
		protected void onPostExecute(Object o)
	    {
			super.onPostExecute(o);
	        // update hourglass
	    }
	 }
}

