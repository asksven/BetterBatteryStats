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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.asksven.android.common.privateapiproxies.NativeKernelWakelock;
import com.asksven.android.common.kernelutils.State;
import com.asksven.android.common.nameutils.UidNameResolver;
import com.asksven.android.common.privateapiproxies.Alarm;
import com.asksven.android.common.privateapiproxies.AlarmItem;
import com.asksven.android.common.privateapiproxies.Misc;
import com.asksven.android.common.privateapiproxies.NetworkUsage;
import com.asksven.android.common.privateapiproxies.Notification;
import com.asksven.android.common.privateapiproxies.Process;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.android.common.utils.DateUtils;
import com.asksven.android.common.utils.MathUtils;
import com.asksven.betterbatterystats.data.KbData;
import com.asksven.betterbatterystats.data.KbEntry;
import com.asksven.betterbatterystats.data.KbReader;
import com.asksven.betterbatterystats.widgets.GraphableBars;
import com.asksven.betterbatterystats.widgets.GraphablePie;
import com.asksven.betterbatterystats.HelpActivity;
import com.asksven.betterbatterystats.PackageInfoTabsPager;
import com.asksven.betterbatterystats.R;

public class StatsAdapter extends BaseAdapter
{
    private Context m_context;

    private List<StatElement> m_listData;
    private static final String TAG = "StatsAdapter";

    private double m_maxValue = 0;
    private long m_timeSince = 0; 
    
    public StatsAdapter(Context context, List<StatElement> listData)
    {
        this.m_context = context;
        this.m_listData = listData;

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.m_context);
        boolean bKbEnabled = sharedPrefs.getBoolean("enable_kb", true);
        
        if ((m_listData != null) && (!m_listData.isEmpty()))
        {
        	// handle notification
        	if (m_listData.get(0) instanceof Notification)
			{
        		// show notification panel
			}
        	else
        	{
        		// hide notifcation panel
        	}
        	if ((m_listData.get(0) instanceof Process) || (m_listData.get(0) instanceof NetworkUsage))
        	{
		        for (int i = 0; i < m_listData.size(); i++)
		        {
		        	StatElement g = m_listData.get(i);
		        	
	        		double[] values = g.getValues();
		        	m_maxValue = Math.max(m_maxValue, values[values.length - 1]);
		            m_maxValue = Math.max(m_maxValue, g.getMaxValue());
		        }
        	}
        	else
        	{
        		m_maxValue = m_timeSince;
        	}
        	
        	
        }
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

    public void setTotalTime(long sinceMs)
    {
    	m_timeSince = sinceMs;
    	if ((m_listData == null) || (m_listData.isEmpty())) return;
    	
    	if (!((m_listData.get(0) instanceof Process) || (m_listData.get(0) instanceof NetworkUsage)))
    	{
    		m_maxValue = m_timeSince;
    	}
    }
    
    public long getItemId(int position)
    {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup viewGroup)
    {
    	StatElement entry = m_listData.get(position);
    	
    	SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.m_context);
        boolean bShowBars = sharedPrefs.getBoolean("show_gauge", false);
        
    	Log.i(TAG, "Values: " +entry.getVals());
        if (convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) m_context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            
            // depending on settings show new pie gauge or old bar gauge
            if (!bShowBars)
            {
            	convertView = inflater.inflate(R.layout.stat_row, null);
            }
            else
            {
            	convertView = inflater.inflate(R.layout.stat_row_gauge, null);
            }
        }
        
        
        
        TextView tvName = (TextView) convertView.findViewById(R.id.TextViewName);
       	tvName.setText(entry.getName());

       	KbData kb = KbReader.getInstance().read(m_context);
       	KbEntry kbentry = null;
        if (kb != null)
        {
        	 kbentry = kb.findByStatElement(entry.getName(), entry.getFqn(UidNameResolver.getInstance(m_context)));
        }
        
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
        tvFqn.setText(entry.getFqn(UidNameResolver.getInstance(m_context)));

        TextView tvData = (TextView) convertView.findViewById(R.id.TextViewData);
        tvData.setText(entry.getData((long)m_maxValue));
        
        //LinearLayout myLayout = (LinearLayout) convertView.findViewById(R.id.LinearLayoutBar);
        LinearLayout myFqnLayout = (LinearLayout) convertView.findViewById(R.id.LinearLayoutFqn);
        LinearLayout myRow = (LinearLayout) convertView.findViewById(R.id.LinearLayoutEntry);
        
        // long press for "copy to clipboard"
        convertView.setOnLongClickListener(new OnItemLongClickListener(position));

        if (!bShowBars)
        {
	        GraphablePie gauge = (GraphablePie) convertView.findViewById(R.id.Gauge);
	        if (entry instanceof Alarm)
	        {
	        	gauge.setValue(entry.getValues()[0], ((Alarm) entry).getMaxValue());
	        }
	        else if (entry instanceof NetworkUsage)
	        {
	        	gauge.setValue(entry.getValues()[0], ((NetworkUsage) entry).getTotal());
	        	
	        }
	        else
	        {
	        	double max = m_maxValue;
	        	// avoid rounding errors leading to values > 100 %
	        	if (entry.getValues()[0] > max)
	        	{
	        		max = entry.getValues()[0];
	        	}
	        	gauge.setValue(entry.getValues()[0], max);
	        }
        }
        else
        {
        	GraphableBars buttonBar = (GraphableBars) convertView.findViewById(R.id.ButtonBar);
        	int iHeight = 10;
        	try
    		{
    			iHeight = Integer.valueOf(sharedPrefs.getString("graph_bar_height", "10"));
    		}
    		catch (Exception e)
    		{
    			iHeight = 10;
    		}    		
        	if (iHeight == 0)
        	{
        		iHeight = 10;
        	}

   			buttonBar.setMinimumHeight(iHeight);
   			buttonBar.setName(entry.getName());
        	buttonBar.setValues(entry.getValues(), m_maxValue);        	
        }
        ImageView iconView = (ImageView) convertView.findViewById(R.id.icon);
                
        // add on click listener for the icon only if KB is enabled
        if (bShowKb)
        {
	        // set a click listener for the list
	        iconKb.setOnClickListener(new OnIconClickListener(position));
        }

        // show / hide fqn text
        if ((entry instanceof Process) || (entry instanceof Alarm) || (entry instanceof NativeKernelWakelock) || (entry instanceof State) || (entry instanceof Misc))
        {
        	myFqnLayout.setVisibility(View.GONE);
        }
        else
        {
        	myFqnLayout.setVisibility(View.VISIBLE);
        }

        // show / hide package icons
        if ((entry instanceof NativeKernelWakelock) || (entry instanceof State) || (entry instanceof Misc))
        {

        	iconView.setVisibility(View.GONE);

        }
        else
        {
        	iconView.setVisibility(View.VISIBLE); 
        	iconView.setImageDrawable(entry.getIcon(UidNameResolver.getInstance(m_context)));
	        // set a click listener for the list
	        iconView.setOnClickListener(new OnPackageClickListener(position));

        }
        
        // add on click listener for the list entry if details are availble
        if ( (entry instanceof Alarm) || (entry instanceof NativeKernelWakelock) )
        {
        	convertView.setOnClickListener(new OnItemClickListener(position));
        }
        
//        // show / hide set dividers
//        ListView myList = (ListView) convertView.getListView(); //findViewById(R.id.id.list);
//        myList.setDivider(new ColorDrawable(0x99F10529));
//        myList.setDividerHeight(1);
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
            KbData kb = KbReader.getInstance().read(m_context);
        	// the timing may lead to m_kb not being initialized yet, it must be checked
        	if (kb == null)
        	{
        		return;
        	}
        	KbEntry kbentry = kb.findByStatElement(entry.getName(), entry.getFqn(UidNameResolver.getInstance(StatsAdapter.this.m_context)));
  	      	if (kbentry != null)
  	      	{
	  	      	SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(StatsAdapter.this.m_context);
	  	      	
	  	      	String url = kbentry.getUrl();
	  	      	
	  	        if (sharedPrefs.getBoolean("kb_ext_browser", true))
	  	        {
					
					Intent intent = new Intent("android.intent.action.VIEW",
							Uri.parse(url)); 
					StatsAdapter.this.m_context.startActivity(intent);
	  	        }
	  	        else
	  	        {
		  	      	Intent intentKB = new Intent(StatsAdapter.this.m_context,
		  	      			HelpActivity.class);
		  	      	intentKB.putExtra("url", url);
		  	        StatsAdapter.this.m_context.startActivity(intentKB);
	  	        }           
  	      	}
        }
    }

    /**
     * Handler for on click of the KB icon
     * @author sven
     *
     */
    private class OnPackageClickListener implements OnClickListener
    {           
        private int m_iPosition;
        OnPackageClickListener(int position)
        {
                m_iPosition = position;
        }
        
        @Override
        public void onClick(View arg0)
        {
        	StatElement entry = (StatElement) getItem(m_iPosition);
        	
        	Context ctx = arg0.getContext();
        	if (entry.getIcon(UidNameResolver.getInstance(m_context)) == null)
        	{
        		return;
        	}
        	
//        	ctx.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS));
        	String packageName = entry.getPackageName();
        	showInstalledPackageDetails(ctx, packageName);
        	
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
	            
	        	Dialog dialog = new Dialog(m_context);
	
	        	dialog.setContentView(R.layout.alarms_dialog);
	        	dialog.setTitle(entry.getName());
	
	        	TextView title = (TextView) dialog.findViewById(R.id.title);
//	        	TextView subtitle = (TextView) dialog.findViewById(R.id.subtitle);
	        	TextView text = (TextView) dialog.findViewById(R.id.text);
	        	title.setText(entry.getData((long)m_maxValue));
	        	
	        	String strText = "";
	        	ArrayList<AlarmItem> myItems = alarmEntry.getItems();
	        	if (myItems != null)
	        	{
	        		for (int i=0; i<myItems.size(); i++)
	        		{
	        			if (myItems.get(i).getCount() > 0)
	        			{
	        				strText = strText + myItems.get(i).getData() + "\n\n";
	        			}
	        		}
	        	}
	        	text.setText(strText);
	        	dialog.show();
	        }
        	if (entry instanceof NativeKernelWakelock)
        	{
        		NativeKernelWakelock kernelWakelockEntry = (NativeKernelWakelock) getItem(m_iPosition);
                
            	Dialog dialog = new Dialog(m_context);

            	dialog.setContentView(R.layout.alarms_dialog);
            	dialog.setTitle(kernelWakelockEntry.getName());

            	TextView title = (TextView) dialog.findViewById(R.id.title);
//            	TextView subtitle = (TextView) dialog.findViewById(R.id.subtitle);
            	TextView text = (TextView) dialog.findViewById(R.id.text);
            	title.setText(kernelWakelockEntry.getData((long)m_maxValue));
            	
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

    /**
     * Handler for the on click of the list item
     * @author sven
     *
     */
    private class OnItemLongClickListener implements OnLongClickListener
    {           
        private int m_iPosition;
        OnItemLongClickListener(int position)
        {
                m_iPosition = position;
        }
        
        @SuppressLint("NewApi")
		@Override
        public boolean onLongClick(View arg0)
        {
        	StatElement entry = (StatElement) getItem(m_iPosition);
        	if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB)
        	{
        	    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) m_context.getSystemService(Context.CLIPBOARD_SERVICE);
        	    clipboard.setText(entry.getName());
        	}
        	else
        	{
        	    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) m_context.getSystemService(Context.CLIPBOARD_SERVICE);
        	    android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", entry.getName());
        	            clipboard.setPrimaryClip(clip);
        	}
			Toast.makeText(m_context, entry.getName() + " was copied to the clipboard", Toast.LENGTH_LONG).show();
        	
        	return true;
        }
    }

    public static void showInstalledPackageDetails(Context context, String packageName)
    {
    	Intent intentPerms = new Intent(context, PackageInfoTabsPager.class); //Activity.class);
    	intentPerms.putExtra("package", packageName);
        context.startActivity(intentPerms);
    }

    
}

