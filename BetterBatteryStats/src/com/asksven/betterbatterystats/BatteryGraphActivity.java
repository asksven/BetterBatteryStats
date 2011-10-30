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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.FloatMath;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.androidplot.LineRegion;
import com.androidplot.ui.*;
import com.androidplot.xy.*;
import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.android.common.privateapiproxies.HistoryItem;
import com.asksven.betterbatterystats.ZoomScrollGraphActivity.Viewport;
import com.asksven.betterbatterystats.R;
 
import java.sql.Date;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


/**
 * The battery graph rendered by AndroidPlot
 * @author sven
 *
 */
public class BatteryGraphActivity extends Activity implements OnTouchListener
{
	/**
	 * a progess dialog to be used for long running tasks
	 */
	ProgressDialog m_progressDialog;
	
	private static final String TAG = "BatteryGraphActivity";
    private static final int FONT_LABEL_SIZE = 13;
    private XYPlot m_plotCharge;
    private XYPlot m_plotWakelock;
    private XYPlot m_plotScreenOn;
    private XYPlot m_plotWifi;

    private Viewport m_viewPort; 

//    private XYPlot m_plotCharging;

    private ArrayList<HistoryItem> m_histList;
	    
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.batterygraph);
        m_plotCharge 	= (XYPlot) findViewById(R.id.myBatteryXYPlot);
        m_plotWakelock 	= (XYPlot) findViewById(R.id.wakelockPlot);
        m_plotScreenOn 	= (XYPlot) findViewById(R.id.screenOnPlot);
//        m_plotCharging 	= (XYPlot) findViewById(R.id.chargingPlot);
        m_plotWifi	 	= (XYPlot) findViewById(R.id.wifiPlot);
        
		//new LoadStatData().execute(this);
		m_histList = this.getHistList();
        seriesSetup();
      
        makePlotPretty(m_plotCharge);
        makePlotPretty(m_plotWakelock);
//        makePlotPretty(m_plotCharging);
        makePlotPretty(m_plotScreenOn);
        makePlotPretty(m_plotWifi);
        
        refreshPlot(m_plotCharge);
        refreshPlot(m_plotWakelock);
//        refreshPlot(m_plotCharging);
        refreshPlot(m_plotScreenOn);
        refreshPlot(m_plotWifi);

		//Set of internal variables for keeping track of the boundaries
		m_plotCharge.calculateMinMaxVals();
		m_viewPort = new Viewport(m_plotCharge);

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
    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.history_menu, menu);
        return true;
    }  

    @Override
	public boolean onPrepareOptionsMenu(Menu menu)
    {
//    	boolean bSortingEnabled = true;
    	    	
//    	MenuItem zoomIn = menu.findItem(R.id.zoom_plus);
//    	MenuItem zoomOut = menu.findItem(R.id.zoom_minus);
    	
//    	if (m_iSorting == 0)
//    	{
//    		// sorting is by time
//    		sortTime.setEnabled(false);
//    		sortCount.setEnabled(true);
//    	}
//    	else
//    	{
//    		// sorting is by count
//    		sortTime.setEnabled(true);
//    		sortCount.setEnabled(false);
//    	}
//    			
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

			case R.id.view_serie:
				Intent intentHist = new Intent(this, HistActivity.class);
			    this.startActivity(intentHist);
			    break;
//			case R.id.zoom_plus:
//				m_viewPort.zoom(2);
//				checkBoundaries();
//				m_plotCharge.redraw();
//				break;
//
//			case R.id.zoom_minus:
//				m_viewPort.zoom(0.5f);
//				checkBoundaries();
//				m_plotCharge.redraw();
//				break;
			case R.id.refresh:
				checkBoundaries();
				m_plotCharge.redraw();
			break;
	

        }
        
        return true;
    }
			

	    
     /**
     * Cleans up the plot's general layout and color scheme
     */
    private void makePlotPretty(XYPlot plot)
    {	 
        // hide legend
        plot.getLegendWidget().setVisible(false);
        // make our domain and range labels invisible:
        plot.getDomainLabelWidget().setVisible(false);
        plot.getRangeLabelWidget().setVisible(false);
 
//	        plot.getGraphWidget().setRangeLabelMargin(-1);
        plot.getGraphWidget().setRangeLabelWidth(25);
        plot.getGraphWidget().setDomainLabelWidth(10);
//	        plot.getGraphWidget().setDomainLabelMargin(-6);
        plot.setBackgroundPaint(null);
        plot.getGraphWidget().setBackgroundPaint(null);
        plot.setBorderPaint(null);
        
        plot.getGraphWidget().getGridBackgroundPaint().setColor(Color.BLACK);
        plot.getGraphWidget().getDomainLabelPaint().setTextSize(FONT_LABEL_SIZE);
        plot.getGraphWidget().getDomainOriginLabelPaint().setTextSize(FONT_LABEL_SIZE);
        plot.getGraphWidget().getRangeLabelPaint().setTextSize(FONT_LABEL_SIZE);
        plot.getGraphWidget().getRangeOriginLabelPaint().setTextSize(FONT_LABEL_SIZE);
        plot.getGraphWidget().getGridLinePaint().setPathEffect(new DashPathEffect(new float[]{1, 2, 1, 2}, 0));
        plot.getTitleWidget().getLabelPaint().setTextSize(FONT_LABEL_SIZE);
        plot.getTitleWidget().pack();
        plot.disableAllMarkup();
    }
	 
    private void seriesSetup()
    {
        // SERIES #1:
        BatteryGraphSeries mySerie = new BatteryGraphSeries(
        		m_histList,
        		BatteryGraphSeries.SERIE_CHARGE,
        		"Charge");
        
        LineAndPointFormatter formater = new LineAndPointFormatter(
        		Color.rgb(0, 0, 200),
        		null,
        		Color.rgb(0, 0, 80));
        formater.getFillPaint().setAlpha(220);
        
        m_plotCharge.addSeries(mySerie, formater);
        
        m_plotCharge.setTicksPerDomainLabel(2);
        m_plotCharge.setTicksPerRangeLabel(1);
        m_plotCharge.disableAllMarkup();
        m_plotCharge.setDomainLabel("Time");
        m_plotCharge.setRangeLabel("%");
        
        m_plotCharge.setRangeBoundaries(0, 100, BoundaryMode.FIXED);
        
        m_plotCharge.setRangeValueFormat(new DecimalFormat("0"));
        m_plotCharge.setDomainValueFormat(new MyDateFormat());
        
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

//        // SERIES #4:
//		BatteryGraphSeries mySerie4 = new BatteryGraphSeries(
//				m_histList,
//				BatteryGraphSeries.SERIE_CHARGING,
//				"Charging");
//		m_plotCharging.addSeries(mySerie4, formater);	        
//		configBinPlot(m_plotCharging);

        // SERIES #5:
		BatteryGraphSeries mySerie5 = new BatteryGraphSeries(
				m_histList,
				BatteryGraphSeries.SERIE_WIFI,
				"Wifi");
		m_plotScreenOn.addSeries(mySerie5, formater2);	        
		configBinPlot(m_plotWifi);

    }

    /**
     * Set common attributes for binary (0/1) plots
     * @param plot the plot to be configured
     */
    private void configBinPlot(XYPlot plot)
    {
        plot.setTicksPerDomainLabel(2);
        plot.setTicksPerRangeLabel(1);
        plot.setRangeBoundaries(0, 1, BoundaryMode.FIXED);
        plot.disableAllMarkup();
        plot.setDomainLabel("Time");
        plot.setRangeLabel("");
        
        plot.setRangeValueFormat(new DecimalFormat("0"));
        plot.setDomainValueFormat(new MyDateFormat());
        
        // remove ticks
        plot.getGraphWidget().getDomainLabelPaint().setAlpha(0);
        plot.getGraphWidget().getDomainOriginLabelPaint().setAlpha(0);
        plot.getGraphWidget().getRangeLabelPaint().setAlpha(0);
        plot.getGraphWidget().getRangeOriginLabelPaint().setAlpha(0);
        
        /*
         * for future version 0.44
         */
//        plot.getGraphWidget().setDomainLabelPaint(null);
//        plot.getGraphWidget().setDomainOriginLabelPaint(null);
//        plot.getGraphWidget().setRangeLabelPaint(null);
//        plot.getGraphWidget().setRangeOriginLabelPaint(null);
    }
    
    /**
     * refresh a plot
     * @param plot the plot to be refreshed
     */
    private void refreshPlot(XYPlot plot)
    {
    	plot.redraw();
    }
	/**
	 * Get the Stat to be displayed
	 * @return a List of StatElements sorted (descending)
	 */
	private ArrayList<HistoryItem> getHistList()
	{
		ArrayList<HistoryItem> myRet = new ArrayList<HistoryItem>();
		
		
		BatteryStatsProxy mStats = new BatteryStatsProxy(this);
		try
		{
			myRet = mStats.getHistory(this);
		}
		catch (Exception e)
		{
			Log.e(TAG, "An error occured while retrieving history. No result");
		}
		return myRet;
	}

		
	private class MyDateFormat extends Format 
	{
        // create a simple date format that draws on the year portion of our timestamp.
        // see http://download.oracle.com/javase/1.4.2/docs/api/java/text/SimpleDateFormat.html
        // for a full description of SimpleDateFormat.
        private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
 
 
        @Override
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos)
        {
            long timestamp = ((Number) obj).longValue();
            Date date = new Date(timestamp);
            return dateFormat.format(date, toAppendTo, pos);
        }
 
		@Override
		public Object parseObject(String source, ParsePosition pos)
		{
			return null;
		}
 
	}
		
	// @see http://code.google.com/p/makemachine/source/browse/trunk/android/examples/async_task/src/makemachine/android/examples/async/AsyncTaskExample.java
	// for more details
	private class LoadStatData extends AsyncTask<Context, Integer, Integer>
	{
		@Override
	    protected Integer doInBackground(Context... params)
	    {
			//super.doInBackground(params);
			m_histList = getHistList();
	    	//StatsActivity.this.setListAdapter(m_listViewAdapter);
	        // getStatList();
	        return 1;
	    }
		
		@Override
		protected void onPostExecute(Integer i)
	    {
			super.onPostExecute(i);
	        // update hourglass
	    	if (m_progressDialog != null)
	    	{
	    		m_progressDialog.hide();
	    		m_progressDialog = null;
	    	}
	    }
	    @Override
	    protected void onPreExecute()
	    {
	        // update hourglass
	    	// @todo this code is only there because onItemSelected is called twice
	    	if (m_progressDialog == null)
	    	{
		    	m_progressDialog = new ProgressDialog(BatteryGraphActivity.this);
		    	m_progressDialog.setMessage("Computing...");
		    	m_progressDialog.setIndeterminate(true);
		    	m_progressDialog.setCancelable(false);
		    	m_progressDialog.show();
	    	}
	    }
	}

	/** handle multitouch
	 * 
	 */
	// Definition of the touch states
	static final private int NONE = 0;
	static final private int ONE_FINGER_DRAG = 1;
	static final private int TWO_FINGERS_DRAG = 2;
	private int mode = NONE;
 
	private PointF firstFinger;
	private float lastScrolling;
	private float distBetweenFingers;
	private float lastZooming;
 
	@Override
	public boolean onTouch(View arg0, MotionEvent event)
	{
		switch(event.getAction() & MotionEvent.ACTION_MASK)
		{
			case MotionEvent.ACTION_DOWN: // Start gesture
				firstFinger = new PointF(event.getX(), event.getY());
				mode = ONE_FINGER_DRAG;
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
				//When the gesture ends, a thread is created to give inertia to the scrolling and zoom 
				final Timer t = new Timer();
				t.schedule(new TimerTask()
				{
					@Override
					public void run()
					{
						while(Math.abs(lastScrolling) > 1f
								|| Math.abs(lastZooming - 1) < 1.01)
						{
							lastScrolling *= .8;	//speed of scrolling damping
							m_viewPort.scroll(lastScrolling);
							lastZooming += (1 - lastZooming) * .2;	//speed of zooming damping
//							m_viewPort.zoom(lastZooming);
							checkBoundaries();
							try
							{
								m_plotCharge.postRedraw();
							}
							catch (final InterruptedException e)
							{
								e.printStackTrace();
							}
							// the thread lives until the scrolling and zooming are imperceptible
						}
					}
				}, 0);
 
			case MotionEvent.ACTION_POINTER_DOWN: // second finger
				distBetweenFingers = spacing(event);
				// the distance check is done to avoid false alarms
				if (distBetweenFingers > 5f)
				{
					mode = TWO_FINGERS_DRAG;
				}
				break;
			case MotionEvent.ACTION_MOVE:
				if (mode == ONE_FINGER_DRAG)
				{
					final PointF oldFirstFinger = firstFinger;
					firstFinger = new PointF(event.getX(), event.getY());
					lastScrolling = oldFirstFinger.x - firstFinger.x;
					m_viewPort.scroll(lastScrolling);
					lastZooming = (firstFinger.y - oldFirstFinger.y) / m_plotCharge.getHeight();
					if (lastZooming < 0)
					{
						lastZooming = 1 / (1 - lastZooming);
					}
					else
					{
						lastZooming += 1;
					}
//					m_viewPort.zoom(lastZooming);
					checkBoundaries();
					m_plotCharge.redraw();
 
				}
//				else if (mode == TWO_FINGERS_DRAG) {
//					final float oldDist = distBetweenFingers;
//					distBetweenFingers = spacing(event);
//					lastZooming = oldDist / distBetweenFingers;
//					m_viewPort.zoom(lastZooming);
//					checkBoundaries();
//					mySimpleXYPlot.redraw();
//				}
				break;
		}
		return true;
	}
 
 
	private float spacing(MotionEvent event)
	{
		final float x = event.getX(0) - event.getX(1);
		final float y = event.getY(0) - event.getY(1);
		return FloatMath.sqrt(x * x + y * y);
	}
 
	private void checkBoundaries()
	{
		//Make sure the proposed domain boundaries will not cause plotting issues
		m_viewPort.updateBoudaries();
		m_plotCharge.setDomainBoundaries(m_viewPort.minXY.x, m_viewPort.maxXY.x, BoundaryMode.AUTO);
	}

	class Viewport
	{
		public PointF minXY;
		public PointF maxXY;
		public float absMinX;
		public float absMaxX;
		public float minNoError;
		public float maxNoError;
//		public double minDif;
		protected double MIN_DIFF;

		public Viewport(XYPlot aPlot)
		{
			minXY = new PointF(m_plotCharge.getCalculatedMinX().floatValue(),
					m_plotCharge.getCalculatedMinY().floatValue()); //initial minimum data point
			absMinX = minXY.x; //absolute minimum data point
			//absolute minimum value for the domain boundary maximum
			minNoError = Math.round(m_histList.get(1).getNormalizedTimeLong().floatValue() + 2);
			maxXY = new PointF(m_plotCharge.getCalculatedMaxX().floatValue(),
					m_plotCharge.getCalculatedMaxY().floatValue()); //initial maximum data point
			absMaxX = maxXY.x; //absolute maximum data point
			//absolute maximum value for the domain boundary minimum
			maxNoError = (float) Math.round(m_histList.get(m_histList.size() - 1).getNormalizedTimeLong().floatValue()) - 2;
			MIN_DIFF = (absMaxX - absMinX) / 10;

		}
		private void zoom(float scale)
		{
			final float domainSpan = maxXY.x - minXY.x;
			final float domainMidPoint = maxXY.x - domainSpan / 2.0f;
			final float offset = domainSpan * scale / 2.0f;
			minXY.x = domainMidPoint - offset;
			maxXY.x = domainMidPoint + offset;
		}
	 
		private void scroll(float pan)
		{
			final float domainSpan = maxXY.x - minXY.x;
			final float step = domainSpan / m_plotCharge.getWidth();
			final float offset = pan * step;
			minXY.x += offset;
			maxXY.x += offset;
		}
		
		private void updateBoudaries()
		{
			if (minXY.x < absMinX)
				minXY.x = absMinX;
			else if (minXY.x > maxNoError)
				minXY.x = maxNoError;
			if (maxXY.x > absMaxX)
				maxXY.x = absMaxX;
			else if (maxXY.x < minNoError)
				maxXY.x = minNoError;
			if (maxXY.x - minXY.x < MIN_DIFF)
				maxXY.x = maxXY.x + (float) (MIN_DIFF - (maxXY.x - minXY.x));
		}
	}

}