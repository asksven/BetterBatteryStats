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
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.Settings;

import com.asksven.android.common.kernelutils.NativeKernelWakelock;
import com.asksven.android.common.kernelutils.State;
import com.asksven.android.common.privateapiproxies.Alarm;
import com.asksven.android.common.privateapiproxies.Alarm.AlarmItem;
import com.asksven.android.common.privateapiproxies.Misc;
import com.asksven.android.common.privateapiproxies.NetworkUsage;
import com.asksven.android.common.privateapiproxies.Process;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.betterbatterystats.data.GoogleAnalytics;
import com.asksven.betterbatterystats.data.KbData;
import com.asksven.betterbatterystats.data.KbEntry;
import com.asksven.betterbatterystats.data.KbReader;
import com.asksven.betterbatterystats.widgets.GraphableBars;
import com.asksven.betterbatterystats.HelpActivity;
import com.asksven.betterbatterystats.PackageInfoTabsPager;
import com.asksven.betterbatterystats.R;

public class StatsAdapter extends BaseAdapter
{
    private Context m_context;

    private List<StatElement> m_listData;
    private static final String TAG = "StatsAdapter";

    private double m_maxValue = 0;
    
    /** The Knowlegde base */
    private KbData m_kb;

    public StatsAdapter(Context context, List<StatElement> listData)
    {
        this.m_context = context;
        this.m_listData = listData;

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.m_context);
        boolean bKbEnabled = sharedPrefs.getBoolean("enable_kb", true);
        
        if (bKbEnabled && ((m_kb == null) || (m_kb.getEntries().isEmpty())))
        {
        	// async read KB
        	new ReadKb().execute("");
        }
        
        if (m_listData != null)
        {
	        for (int i = 0; i < m_listData.size(); i++)
	        {
	        	StatElement g = m_listData.get(i);
	        	double[] values = g.getValues();
	        	m_maxValue = Math.max(m_maxValue, values[values.length - 1]);
	            m_maxValue = Math.max(m_maxValue, g.getMaxValue());
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

    public long getItemId(int position)
    {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup viewGroup)
    {
    	StatElement entry = m_listData.get(position);
        if (convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) m_context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.stat_row, null);
        }
        TextView tvName = (TextView) convertView.findViewById(R.id.TextViewName);
       	tvName.setText(entry.getName());

       	KbEntry kbentry = null;
        if (m_kb != null)
        {
        	 kbentry = m_kb.findByStatElement(entry.getName(), entry.getFqn(m_context));
        }
        
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.m_context);
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
        tvFqn.setText(entry.getFqn(m_context));

        TextView tvData = (TextView) convertView.findViewById(R.id.TextViewData);
        tvData.setText(entry.getData());
        
        LinearLayout myLayout = (LinearLayout) convertView.findViewById(R.id.LinearLayoutBar);
        LinearLayout myFqnLayout = (LinearLayout) convertView.findViewById(R.id.LinearLayoutFqn);
        LinearLayout myRow = (LinearLayout) convertView.findViewById(R.id.LinearLayoutEntry);
        
        // long press for "copy to clipboard"
        //myRow.setOnLongClickListener(new OnItemLongClickListener(position));

        
        GraphableBars buttonBar = (GraphableBars) convertView.findViewById(R.id.ButtonBar);
        
        ImageView iconView = (ImageView) convertView.findViewById(R.id.icon);
        
        if (sharedPrefs.getBoolean("hide_bars", false))
        {
        	myLayout.setVisibility(View.GONE);
        	
        }
        else
        {
        	myLayout.setVisibility(View.VISIBLE);
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
        	iconView.setImageDrawable(entry.getIcon(m_context));
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
            
        	// the timing may lead to m_kb not being initialized yet, it must be checked
        	if (m_kb == null)
        	{
        		return;
        	}
        	KbEntry kbentry = m_kb.findByStatElement(entry.getName(), entry.getFqn(StatsAdapter.this.m_context));
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
        	if (entry.getIcon(ctx) == null)
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
	        	title.setText(entry.getData());
	        	
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
            	title.setText(kernelWakelockEntry.getData());
            	
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

    private class ReadKb extends AsyncTask
	{
		@Override
	    protected Object doInBackground(Object... params)
	    {
			// retrieve KB
			StatsAdapter.this.m_kb = KbReader.read(StatsAdapter.this.m_context);

	    	return true;
	    }

		@Override
		protected void onPostExecute(Object o)
	    {
			super.onPostExecute(o);
	        // update hourglass
	    }
	 }
    


    public static void showInstalledPackageDetails(Context context, String packageName)
    {
    	Intent intentPerms = new Intent(context, PackageInfoTabsPager.class); //Activity.class);
    	intentPerms.putExtra("package", packageName);
    	GoogleAnalytics.getInstance(context).trackPage(GoogleAnalytics.ACTIVITY_PERMS);
        context.startActivity(intentPerms);
    }

    
}

