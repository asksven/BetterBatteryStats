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

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Window;
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
import com.asksven.android.common.privateapiproxies.Process;
import com.asksven.android.common.privateapiproxies.SensorUsage;
import com.asksven.android.common.privateapiproxies.SensorUsageItem;
import com.asksven.android.common.privateapiproxies.StatElement;
import com.asksven.betterbatterystats.widgets.GraphableBars;
import com.asksven.betterbatterystats.widgets.GraphablePie;
import com.asksven.betterbatterystats.LogSettings;
import com.asksven.betterbatterystats.PackageInfoActivity;
import com.asksven.betterbatterystats.R;

public class StatsAdapter extends BaseAdapter
{
    private Context m_context;

    private List<StatElement> m_listData;
    private static final String TAG = "StatsAdapter";
    public static final String TRANSITION_NAME = "icon_transition";

    private double m_maxValue = 0;
    private long m_timeSince = 0; 
    private Activity m_parent = null;
    
    public StatsAdapter(Context context, List<StatElement> listData, Activity parent)
    {
        this.m_context = context;
        this.m_listData = listData;
        this.m_parent = parent;

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.m_context);
        boolean bKbEnabled = sharedPrefs.getBoolean("enable_kb", true);
        
        if ((m_listData != null) && (!m_listData.isEmpty()))
        {
        	if (m_listData.get(0) instanceof NetworkUsage)
        	{
		        for (int i = 0; i < m_listData.size(); i++)
		        {
		        	StatElement g = m_listData.get(i);
		        	
	        		double[] values = g.getValues();
		        	m_maxValue = Math.max(m_maxValue, values[values.length - 1]);
		            m_maxValue = Math.max(m_maxValue, g.getMaxValue());
		        }
        	}
        	else if (m_listData.get(0) instanceof Alarm)
        	{
		        for (int i = 0; i < m_listData.size(); i++)
		        {
		        	StatElement g = m_listData.get(i);
		        	
	        		double[] values = g.getValues();
		        	m_maxValue += values[0];
		        	Log.i(TAG, "Summing up " + values[0] + ", max is now " + m_maxValue);
		        }
        	}
        	else if (m_listData.get(0) instanceof SensorUsage)
        	{
		        for (int i = 0; i < m_listData.size(); i++)
		        {
		        	StatElement g = m_listData.get(i);
		        	
	        		double[] values = g.getValues();
		        	m_maxValue += values[0];
		        	Log.i(TAG, "Summing up " + values[0] + ", max is now " + m_maxValue);
		        }
        	}

        	else if (m_listData.get(0) instanceof Process)
        	{
	        	StatElement g = m_listData.get(0);
	        	
	        	m_maxValue = g.getTotal();
	        	Log.i(TAG, "Total is " + m_maxValue);
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
    	
    	if (!((m_listData.get(0) instanceof Process) || (m_listData.get(0) instanceof NetworkUsage) || (m_listData.get(0) instanceof Alarm)))
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
        
        if (LogSettings.DEBUG)
        {
        	Log.i(TAG, "Values: " +entry.getVals());
        }
        
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
        
        final float scale = this.m_context.getResources().getDisplayMetrics().density;
        
        
        TextView tvName = (TextView) convertView.findViewById(R.id.TextViewName);
        
        /////////////////////////////////////////
		// we do some stuff here to handle settings about font size and thumbnail size
		String fontSize = sharedPrefs.getString("medium_font_size", "16");
		int mediumFontSize = Integer.parseInt(fontSize);

		//we need to change "since" fontsize
		tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, mediumFontSize);

		// We need to handle an exception here: Sensors do not have a name so we use the fqn instead
        if (entry instanceof SensorUsage)
        {
        	tvName.setText(entry.getFqn(UidNameResolver.getInstance(m_context)));
        	
        }
        else
        {
        	tvName.setText(entry.getName());
        }

        boolean bShowKb = sharedPrefs.getBoolean("enable_kb", true);
        ImageView iconKb = (ImageView) convertView.findViewById(R.id.imageKB);
        
        iconKb.setVisibility(View.INVISIBLE);

        TextView tvFqn = (TextView) convertView.findViewById(R.id.TextViewFqn);
        tvFqn.setText(entry.getFqn(UidNameResolver.getInstance(m_context)));

        TextView tvData = (TextView) convertView.findViewById(R.id.TextViewData);

        // for alarms the values is wakeups per hour so we need to take the time as reference for the text
        if (entry instanceof Alarm)
        {
        	tvData.setText(entry.getData((long)m_timeSince));
        }
        else
        {
        	tvData.setText(entry.getData((long)m_maxValue));
        }
        
        //LinearLayout myLayout = (LinearLayout) convertView.findViewById(R.id.LinearLayoutBar);
        LinearLayout myFqnLayout = (LinearLayout) convertView.findViewById(R.id.LinearLayoutFqn);
        LinearLayout myRow = (LinearLayout) convertView.findViewById(R.id.LinearLayoutEntry);
        
        // long press for "copy to clipboard"
        convertView.setOnLongClickListener(new OnItemLongClickListener(position));

        if (!bShowBars)
        {
	        GraphablePie gauge = (GraphablePie) convertView.findViewById(R.id.Gauge);
	        
	        /////////////////////////////////////////
			// we do some stuff here to handle settings about font size and thumbnail size
			String iconDim = sharedPrefs.getString("thumbnail_size", "56");
			int iconSize = Integer.parseInt(iconDim);
			int pixels = (int) (iconSize * scale + 0.5f);
			//we need to change "since" fontsize
			gauge.getLayoutParams().height = pixels;
			gauge.getLayoutParams().width = pixels;
			gauge.requestLayout();
			
			////////////////////////////////////////////////////////////////////////////////////
	        if (entry instanceof NetworkUsage)
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
	        		Log.i(TAG, "Upping gauge max to " + max);
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
        LinearLayout iconLayout = (LinearLayout) convertView.findViewById(R.id.LayoutIcon);
        /////////////////////////////////////////
		// we do some stuff here to handle settings about font size and thumbnail size
		String iconDim = sharedPrefs.getString("thumbnail_size", "56");
		int iconSize = Integer.parseInt(iconDim);
		int pixels = (int) (iconSize * scale + 0.5f);
		//we need to change "since" fontsize
		iconView.getLayoutParams().width = pixels;
		iconView.getLayoutParams().height = pixels;
		iconView.requestLayout();
		//n 20;setLay.setTextSize(TypedValue.COMPLEX_UNIT_DIP, iconSize);
		
		////////////////////////////////////////////////////////////////////////////////////
                
        // add on click listener for the icon only if KB is enabled
//        if (bShowKb)
//        {
//	        // set a click listener for the list
//	        iconKb.setOnClickListener(new OnIconClickListener(position));
//        }

        // show / hide fqn text
        if ((entry instanceof Process) || (entry instanceof State) || (entry instanceof Misc)
        		|| (entry instanceof NativeKernelWakelock) || (entry instanceof Alarm) || (entry instanceof SensorUsage))
        {
        	myFqnLayout.setVisibility(View.GONE);
        }
        else
        {
        	myFqnLayout.setVisibility(View.VISIBLE);
        }

        // show / hide package icons (we show / hide the whole layout as it contains a margin that must be hidded as well
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
        if ( (entry instanceof Alarm) || (entry instanceof NativeKernelWakelock) || (entry instanceof SensorUsage))
        {
        	convertView.setOnClickListener(new OnItemClickListener(position));
        }
        
//        // show / hide set dividers
//        ListView myList = (ListView) convertView.getListView(); //findViewById(R.id.id.list);
//        myList.setDivider(new ColorDrawable(0x99F10529));
//        myList.setDividerHeight(1);
        return convertView;
    }
    
//    /**
//     * Handler for on click of the KB icon
//     * @author sven
//     *
//     */
//    private class OnIconClickListener implements OnClickListener
//    {           
//        private int m_iPosition;
//        OnIconClickListener(int position)
//        {
//                m_iPosition = position;
//        }
//        
//        @Override
//        public void onClick(View arg0)
//        {
//        	StatElement entry = (StatElement) getItem(m_iPosition);
//            KbData kb = KbReader.getInstance().read(m_context);
//        	// the timing may lead to m_kb not being initialized yet, it must be checked
//        	if (kb == null)
//        	{
//        		return;
//        	}
//        	KbEntry kbentry = kb.findByStatElement(entry.getName(), entry.getFqn(UidNameResolver.getInstance(StatsAdapter.this.m_context)));
//  	      	if (kbentry != null)
//  	      	{
//	  	      	SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(StatsAdapter.this.m_context);
//	  	      	
//	  	      	String url = kbentry.getUrl();
//	  	      	
//	  	        if (sharedPrefs.getBoolean("kb_ext_browser", true))
//	  	        {
//					
//					Intent intent = new Intent("android.intent.action.VIEW",
//							Uri.parse(url)); 
//					StatsAdapter.this.m_context.startActivity(intent);
//	  	        }
//	  	        else
//	  	        {
//		  	      	Intent intentKB = new Intent(StatsAdapter.this.m_context,
//		  	      			HelpActivity.class);
//		  	      	intentKB.putExtra("url", url);
//		  	        StatsAdapter.this.m_context.startActivity(intentKB);
//	  	        }           
//  	      	}
//        }
//    }

    /**
     * Handler for on click of the icon
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
        	showInstalledPackageDetails(ctx, packageName, arg0);
        	
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
        
        @SuppressLint("NewApi")
		@Override
        public void onClick(View arg0)
        {
        	
        	StatElement entry = (StatElement) getItem(m_iPosition);

        	if (entry instanceof SensorUsage)
        	{
        		SensorUsage sensorEntry = (SensorUsage) getItem(m_iPosition);
	            
	        	Dialog dialog = new Dialog(m_context);
	        	dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
	        	dialog.setContentView(R.layout.details_dialog);
	        	
	        	TextView dialogTitle = (TextView) dialog.findViewById(R.id.dialog_title);
	        	dialogTitle.setText(entry.getFqn(UidNameResolver.getInstance(m_context)));
	        	TextView title = (TextView) dialog.findViewById(R.id.title);
	        	TextView text = (TextView) dialog.findViewById(R.id.text);
	        	title.setText(entry.getData((long)m_timeSince));
	        	
	        	String strText = "";
	        	ArrayList<SensorUsageItem> myItems = sensorEntry.getItems();
	        	if (myItems != null)
	        	{
	        		for (int i=0; i<myItems.size(); i++)
	        		{
	        			if (myItems.get(i).getTime() > 0)
	        			{
	        				strText = strText + myItems.get(i).getData() + "\n\n";
	        			}
	        		}
	        	}
	        	text.setText(strText);
	        	dialog.show();       	
	        }

        	if (entry instanceof Alarm)
        	{
	        	Alarm alarmEntry = (Alarm) getItem(m_iPosition);
	            
	        	Dialog dialog = new Dialog(m_context);
	        	dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
	        	dialog.setContentView(R.layout.details_dialog);
	        	
	        	TextView dialogTitle = (TextView) dialog.findViewById(R.id.dialog_title);
	        	dialogTitle.setText(entry.getName());
	        	TextView title = (TextView) dialog.findViewById(R.id.title);
	        	TextView text = (TextView) dialog.findViewById(R.id.text);
	        	title.setText(entry.getData((long)m_timeSince));
	        	
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
            	dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            	dialog.setContentView(R.layout.details_dialog);
            	dialog.setTitle(kernelWakelockEntry.getName());

            	TextView dialogTitle = (TextView) dialog.findViewById(R.id.dialog_title);
	        	dialogTitle.setText(kernelWakelockEntry.getName());
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
        	try
        	{
        		Toast.makeText(m_context, m_context.getString(R.string.message_copied_to_clipboard, entry.getName()), Toast.LENGTH_LONG).show();
        	}
        	catch (Exception e)
        	{
        		// can normally not fail, if it still does it's only the toast
        	}
        	return true;
        }
    }

    public void showInstalledPackageDetails(Context context, String packageName, View view)
    {       
    	Intent intentPerms = new Intent(context, PackageInfoActivity.class);
    	intentPerms.putExtra("package", packageName);
        //context.startActivity(intentPerms);
        View source_icon = view.findViewById(R.id.icon);
        
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                m_parent, source_icon, TRANSITION_NAME);
        ActivityCompat.startActivity(m_parent, intentPerms, options.toBundle());
        
    }

    
}

