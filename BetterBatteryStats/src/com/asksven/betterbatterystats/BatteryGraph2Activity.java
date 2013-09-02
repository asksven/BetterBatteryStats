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

import android.app.ProgressDialog;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.androidplot.xy.*;
import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.android.common.privateapiproxies.HistoryItem;
import com.asksven.betterbatterystats.R;

import java.util.ArrayList;


/**
 * The battery graph rendered by AndroidPlot
 * @author sven
 *
 */
public class BatteryGraph2Activity extends BatteryGraphActivity // implements OnTouchListener
{
	/**
	 * a progess dialog to be used for long running tasks
	 */
	ProgressDialog m_progressDialog;
	
	private static final String TAG = "BatteryGraphActivity";
    private static final int FONT_LABEL_SIZE = 13;
    private XYPlot m_plotWakelock;
    private XYPlot m_plotScreenOn;
    private XYPlot m_plotWifi;
    private XYPlot m_plotCharging;
    private XYPlot m_plotGps;
    private XYPlot m_plotBt;

//    private Viewport m_viewPort; 


    private ArrayList<HistoryItem> m_histList;
	    
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.batterygraph2);
        
		ActionBar ab = getSupportActionBar();
		ab.setDisplayHomeAsUpEnabled(true);

        m_plotWakelock 	= (XYPlot) findViewById(R.id.wakelockPlot);
        m_plotScreenOn 	= (XYPlot) findViewById(R.id.screenOnPlot);
        m_plotCharging 	= (XYPlot) findViewById(R.id.chargingPlot);
        m_plotWifi	 	= (XYPlot) findViewById(R.id.wifiPlot);
        m_plotGps	 	= (XYPlot) findViewById(R.id.gpsPlot);
        m_plotBt	 	= (XYPlot) findViewById(R.id.btPlot);

        
        if (m_histList == null)
        {
        	m_histList = BatteryGraphActivity.m_histList;
        }
        seriesSetup();
      
        makePlotPretty(m_plotWakelock);
        makePlotPretty(m_plotCharging);
        makePlotPretty(m_plotScreenOn);
        makePlotPretty(m_plotWifi);
        makePlotPretty(m_plotGps);
        makePlotPretty(m_plotBt);
      
        refreshPlot(m_plotWakelock);
        refreshPlot(m_plotCharging);
        refreshPlot(m_plotScreenOn);
        refreshPlot(m_plotWifi);
        refreshPlot(m_plotGps);
        refreshPlot(m_plotBt);

//		m_viewPort = new Viewport(m_plotCharge);

//		m_plotCharge.setOnTouchListener(this);
    }
	 
	/* Request updates at startup */
	@Override
	protected void onResume()
	{
		super.onResume();

	}


    /** 
     * Add menu items
     * 
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    public boolean onCreateOptionsMenu(Menu menu)
    {  
    	MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.history2_menu, menu);
        return true;
    }  

	
    /** 
     * Define menu action
     * 
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    public boolean onOptionsItemSelected(MenuItem item)
    {  
        switch (item.getItemId())
        {  

			case R.id.refresh:
				BatteryStatsProxy.getInstance(this).invalidate();
				m_histList = getHistList();
				refreshPlot(m_plotBt);
				refreshPlot(m_plotCharging);
				refreshPlot(m_plotGps);
				refreshPlot(m_plotScreenOn);
				refreshPlot(m_plotWakelock);
				refreshPlot(m_plotWifi);

				break;
	

        }
        
        return true;
    }
			

	    
    private void seriesSetup()
    {
        
        // SERIES #2:
        BatteryGraphSeries mySerie2 = new BatteryGraphSeries(
        		m_histList,
        		BatteryGraphSeries.SERIE_WAKELOCK,
        		"Wakelock");
        BarFormatter formater2 = new BarFormatter(
        		Color.rgb(0, 0, 200),
        		Color.rgb(0, 0, 80));
        formater2.getFillPaint().setAlpha(220);

        m_plotWakelock.addSeries(mySerie2, formater2);	        
        configBinPlot(m_plotWakelock);
        
        // SERIES #3:
		BatteryGraphSeries mySerie3 = new BatteryGraphSeries(
				m_histList,
				BatteryGraphSeries.SERIE_SCREENON,
				"Screen On");
		m_plotScreenOn.addSeries(mySerie3, formater2);	        
		configBinPlot(m_plotScreenOn);

        // SERIES #4:
		BatteryGraphSeries mySerie4 = new BatteryGraphSeries(
				m_histList,
				BatteryGraphSeries.SERIE_CHARGING,
				"Charging");
		m_plotCharging.addSeries(mySerie4, formater2);	        
		configBinPlot(m_plotCharging);

        // SERIES #5:
		BatteryGraphSeries mySerie5 = new BatteryGraphSeries(
				m_histList,
				BatteryGraphSeries.SERIE_WIFI,
				"Wifi");
		m_plotWifi.addSeries(mySerie5, formater2);	        
		configBinPlot(m_plotWifi);

        // SERIES #6:
		BatteryGraphSeries mySerie6 = new BatteryGraphSeries(
				m_histList,
				BatteryGraphSeries.SERIE_GPS,
				"GPS");
		m_plotGps.addSeries(mySerie6, formater2);	        
		configBinPlot(m_plotGps);

        // SERIES #7:
		BatteryGraphSeries mySerie7 = new BatteryGraphSeries(
				m_histList,
				BatteryGraphSeries.SERIE_BT,
				"Bluetooth");
		m_plotBt.addSeries(mySerie7, formater2);	        
		configBinPlot(m_plotBt);

    }
}