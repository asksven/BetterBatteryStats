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

import com.androidplot.series.XYSeries;
import com.asksven.android.common.privateapiproxies.HistoryItem;
import com.asksven.betterbatterystats.R;

/**
 * The data source for all series to plot
 * @author sven
 */
public class BatteryGraphSeries implements XYSeries
{	
	ArrayList<HistoryItem> m_dataSource;
	private int m_iSerie;
    private String m_title;
    
    public static final int SERIE_CHARGE	= 1;
    public static final int SERIE_WAKELOCK 	= 2;
    public static final int SERIE_SCREENON 	= 3;
    public static final int SERIE_CHARGING 	= 4;
    public static final int SERIE_WIFI	 	= 5;
    

    public BatteryGraphSeries(ArrayList<HistoryItem> datasource, int iSerie, String title)
    {
    	m_iSerie	 	= iSerie;
        m_dataSource 	= datasource;
        m_title 		= title;
    }
    
    @Override
    public String getTitle()
    {
        return m_title;
    }
 
    @Override
    public int size()
    {
        return m_dataSource.size();
    }
 
    @Override
    public Number getX(int index)
    {
        return m_dataSource.get(index).getNormalizedTimeLong();
    }
 
    @Override
    public Number getY(int index)
    {
    	// return the Y data depending on the m_iSerie 
    	switch (m_iSerie)
    	{
    		case SERIE_CHARGE:
    			return m_dataSource.get(index).getBatteryLevelInt();
    		case SERIE_WAKELOCK:
    			return m_dataSource.get(index).getWakelockInt();
    		case SERIE_SCREENON:	
    			return m_dataSource.get(index).getScreenOnInt();
    		case SERIE_CHARGING:
    			return m_dataSource.get(index).getChargingInt();
    		case SERIE_WIFI:
    			return m_dataSource.get(index).getWifiRunningInt();	
    					
    		default: // unexisting
    			return 0;
    	}
    }	

}
