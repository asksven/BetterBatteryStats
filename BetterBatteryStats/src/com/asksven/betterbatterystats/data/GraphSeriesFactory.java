/*
 * Copyright (C) 2014 asksven
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
package com.asksven.betterbatterystats.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;

import com.asksven.android.common.privateapiproxies.HistoryItem;

/**
 * The data source for all series to plot
 * @author sven
 */
public class GraphSeriesFactory
{	
	ArrayList<HistoryItem> m_dataSource;
    static final String TAG = "GraphSeriesFactory";
    
    private static  Map<Integer, ArrayList<Datapoint>> m_serieStore = new HashMap<Integer, ArrayList<Datapoint>>();
    
    public static final int SERIE_CHARGE	= 1;
    public static final int SERIE_WAKELOCK 	= 2;
    public static final int SERIE_SCREENON 	= 3;
    public static final int SERIE_CHARGING 	= 4;
    public static final int SERIE_WIFI	 	= 5;
    public static final int SERIE_GPS	 	= 6;
    public static final int SERIE_BT	 	= 7;
    
    

    public GraphSeriesFactory(ArrayList<HistoryItem> datasource)
    {
        m_dataSource 	= datasource;
        m_serieStore.clear();
        if (m_dataSource != null)
        {

        	long[] prev_my = {-1, -1, -1, -1, -1, -1, -1};
        	long[] prev_mx = {-1, -1, -1, -1, -1, -1, -1};
        	
        	for (int index = 0; index < m_dataSource.size(); index++)
        	{
        		Datapoint data = new Datapoint();
        		HistoryItem item = m_dataSource.get(index);
        		data.mX = getX(index);
        		for (int iSerie = 1; iSerie <= 7; iSerie++)
        		{
	            	switch (iSerie)
	            	{
	            		case SERIE_CHARGE:
	            			data.mY = item.getBatteryLevelInt();
	            			//data.mY = 100;
	            			//Log.i(TAG, "Charge: x=" + data.mX + ", y=" + data.mY);
	            			break;
	            		case SERIE_WAKELOCK:
	            			data.mY = item.getWakelockInt();
	            			//Log.i(TAG, "Awake: x=" + data.mX + ", y=" + data.mY);
	            			break;
	            		case SERIE_SCREENON:	
	            			data.mY = item.getScreenOnInt();
	            			//Log.i(TAG, "Screen on: x=" + data.mX + ", y=" + data.mY);
	            			break;
	            		case SERIE_CHARGING:
	            			data.mY = item.getChargingInt();
	            			//Log.i(TAG, "Charging: x=" + data.mX + ", y=" + data.mY);
	            			break;
	            		case SERIE_WIFI:
	            			data.mY = item.getWifiRunningInt();
	            			//Log.i(TAG, "Wifi: x=" + data.mX + ", y=" + data.mY);
	            			break;
	            		case SERIE_GPS:
	            			data.mY = item.getGpsOnInt(); 
	            			//Log.i(TAG, "GPS: x=" + data.mX + ", y=" + data.mY);
	            			break;
	            		case SERIE_BT:
	            			data.mY = item.getBluetoothOnInt();
	            			//Log.i(TAG, "BT: x=" + data.mX + ", y=" + data.mY);
	            			break;
	            		default:
	            			Log.e(TAG, "No serie found for " + iSerie);
	            	}
	            	
	            	// add only datapoints to series if the last datapoint does not have the same Y
	            	ArrayList<Datapoint> serie = m_serieStore.get(iSerie);
	            	if (serie == null)
	            	{
	            		// do we have data in the serie yet?
	            		m_serieStore.put(iSerie, new ArrayList<Datapoint>());
	            		m_serieStore.get(iSerie).add(new Datapoint(data.mX, data.mY));
	            		prev_my[iSerie-1] = data.mY;
	            		prev_mx[iSerie-1] = data.mX;
	            		//Log.i(TAG, "added to serie " + data);
	            	}
	            	else
	            	{
	            		// does the last stored value have a different Y or is the last of the serie?
	            		//Datapoint lastValue = m_serieStore.get(iSerie).get(m_serieStore.get(iSerie).size()-1);
	            		if ((prev_my[iSerie-1] != data.mY) || (index == m_dataSource.size() - 1)) 
	            		{
	                		Datapoint prev = new Datapoint();
	                		prev.mX = prev_mx[iSerie-1];
	                		prev.mY = prev_my[iSerie-1];	
	                		if (prev.mY != -1)
	                		{
	                			m_serieStore.get(iSerie).add(new Datapoint(prev.mX, prev.mY));
	                			
	                			// we need to consider a special case for when we represent binary (0|1) values
	                			if ((prev.mY == 1) || (prev.mY == 0))
	                			{
	                				// the binary representation can not connect datapoint with anything else than 
	                				// vertical or horizontal lines. In this case as Y varies
	                				// we need to add a datapoint to make sure that we do not draw 45° lines:
	                				// descending: on the previous datapoint
	                				// ascending: on the current datapoint
	                				if (prev.mY == 1)
	                				{
	                					m_serieStore.get(iSerie).add(new Datapoint(prev.mX, 0));		
	                				}
	                				else
	                				{
	                					m_serieStore.get(iSerie).add(new Datapoint(data.mX, 0));
	                				}
	                					
	                			}
	                		}
	            			m_serieStore.get(iSerie).add(new Datapoint(data.mX, data.mY));
	            			prev_my[iSerie-1] = data.mY;
		            		prev_mx[iSerie-1] = data.mX;
	            			//Log.i(TAG, "added to serie" + data);
	            		}
	            		else
	            		{
	            			//Log.i(TAG, "not added to serie" + data);
	            			prev_my[iSerie-1] = data.mY;
		            		prev_mx[iSerie-1] = data.mX;
	            		}
	            		
	            	}
        		}
        	}
        }
        
//        Object[] array = m_serieStore.keySet().toArray();
//        Log.i(TAG, "Retrieved " + array.length + " series ot of " + m_dataSource.size() + " history items");
//        for (int i=0; i < array.length; i++)
//        {
//        	Log.i(TAG, "Serie " + (Integer)array[i] + " contains " + m_serieStore.get((Integer)array[i]).size() + " elements");	
//        	Log.i(TAG, m_serieStore.get((Integer)array[i]).toString() );
//        }
        
        
    }
 
    private long getX(int index)
    {
        return m_dataSource.get(index).getNormalizedTimeLong();
    }
 
    
    
    
    public ArrayList<Datapoint> getValues(int serie)
    {
    	ArrayList<Datapoint> ret = m_serieStore.get(serie);
//    	if (ret != null)
//    	{
//    		Log.i(TAG, "getValues for serie " + serie + " returns " + ret.size() + " values: " + ret.toString());
//    	}
//    	else
//    	{
//    		Log.i(TAG, "getValues for serie " + serie + " returns null");
//    	}
    	return ret;
    }
    


}
