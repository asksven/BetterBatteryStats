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
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
 
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.LineAndPointRenderer;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.SimpleXYSeries.ArrayFormat;
import com.androidplot.xy.XYPlot;
import com.asksven.betterbatterystats.R;
 
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

/**
 * @author sven
 *
 */
public class ZoomScrollGraphActivity extends Activity implements OnTouchListener {
	 

		private XYPlot mySimpleXYPlot;
		private SimpleXYSeries mySeries;
		
		private Viewport m_viewPort; 
		public class Viewport
		{
			public PointF minXY;
			public PointF maxXY;
			public float absMinX;
			public float absMaxX;
			public float minNoError;
			public float maxNoError;
//			public double minDif;
			protected double MIN_DIFF;

			public Viewport(XYPlot aPlot)
			{
				minXY = new PointF(mySimpleXYPlot.getCalculatedMinX().floatValue(),
						mySimpleXYPlot.getCalculatedMinY().floatValue()); //initial minimum data point
				absMinX = minXY.x; //absolute minimum data point
				//absolute minimum value for the domain boundary maximum
				minNoError = Math.round(mySeries.getX(1).floatValue() + 2);
				maxXY = new PointF(mySimpleXYPlot.getCalculatedMaxX().floatValue(),
						mySimpleXYPlot.getCalculatedMaxY().floatValue()); //initial maximum data point
				absMaxX = maxXY.x; //absolute maximum data point
				//absolute maximum value for the domain boundary minimum
				maxNoError = (float) Math.round(mySeries.getX(mySeries.size() - 1).floatValue()) - 2;
				MIN_DIFF = (absMaxX - absMinX) / 10;

			}
			private void zoom(float scale) {
				final float domainSpan = maxXY.x - minXY.x;
				final float domainMidPoint = maxXY.x - domainSpan / 2.0f;
				final float offset = domainSpan * scale / 2.0f;
				minXY.x = domainMidPoint - offset;
				maxXY.x = domainMidPoint + offset;
			}
		 
			private void scroll(float pan) {
				final float domainSpan = maxXY.x - minXY.x;
				final float step = domainSpan / mySimpleXYPlot.getWidth();
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
		
	 
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			
			setContentView(R.layout.zoomscrollgraph);
			mySimpleXYPlot = (XYPlot) findViewById(R.id.mySimpleXYPlot);
			mySimpleXYPlot.setOnTouchListener(this);
	 
			//Plot layout configurations
			mySimpleXYPlot.getGraphWidget().setTicksPerRangeLabel(1);
			mySimpleXYPlot.getGraphWidget().setTicksPerDomainLabel(1);
			mySimpleXYPlot.getGraphWidget().setRangeValueFormat(
					new DecimalFormat("#####.##"));
			mySimpleXYPlot.getGraphWidget().setDomainValueFormat(
					new DecimalFormat("#####.##"));
			mySimpleXYPlot.getGraphWidget().setRangeLabelWidth(25);
			mySimpleXYPlot.setRangeLabel("");
			mySimpleXYPlot.setDomainLabel("");
			mySimpleXYPlot.disableAllMarkup();
	 
			//Creation of the series
			final Vector<Double> vector = new Vector<Double>();
			for (double x = 0.0; x < Math.PI * 5; x += Math.PI / 20) {
				vector.add(x);
				vector.add(Math.sin(x));
			}
			mySeries = new SimpleXYSeries(vector, ArrayFormat.Y_VALS_ONLY, "Series Name");
	 
			//colors: (line, vertex, fill)
			mySimpleXYPlot.addSeries(mySeries, LineAndPointRenderer.class,
					new LineAndPointFormatter(Color.rgb(0, 200, 0), Color.rgb(200, 0, 0), Color.argb(100, 0, 0, 100)));
	 
			//Enact all changes
			mySimpleXYPlot.redraw();
	 
			//Set of internal variables for keeping track of the boundaries
			mySimpleXYPlot.calculateMinMaxVals();
			m_viewPort = new Viewport(mySimpleXYPlot);
	 
		}
	 
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
		public boolean onTouch(View arg0, MotionEvent event) {
			switch(event.getAction() & MotionEvent.ACTION_MASK) {
				case MotionEvent.ACTION_DOWN: // Start gesture
					firstFinger = new PointF(event.getX(), event.getY());
					mode = ONE_FINGER_DRAG;
					break;
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_POINTER_UP:
					//When the gesture ends, a thread is created to give inertia to the scrolling and zoom 
					final Timer t = new Timer();
					t.schedule(new TimerTask() {
						@Override
						public void run() {
							while(Math.abs(lastScrolling) > 1f || Math.abs(lastZooming - 1) < 1.01) {
								lastScrolling *= .8;	//speed of scrolling damping
								m_viewPort.scroll(lastScrolling);
								lastZooming += (1 - lastZooming) * .2;	//speed of zooming damping
								m_viewPort.zoom(lastZooming);
								checkBoundaries();
								try {
									mySimpleXYPlot.postRedraw();
								} catch (final InterruptedException e) {
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
						mode = TWO_FINGERS_DRAG;
					break;
				case MotionEvent.ACTION_MOVE:
					if (mode == ONE_FINGER_DRAG) {
						final PointF oldFirstFinger = firstFinger;
						firstFinger = new PointF(event.getX(), event.getY());
						lastScrolling = oldFirstFinger.x - firstFinger.x;
						m_viewPort.scroll(lastScrolling);
						lastZooming = (firstFinger.y - oldFirstFinger.y) / mySimpleXYPlot.getHeight();
						if (lastZooming < 0)
							lastZooming = 1 / (1 - lastZooming);
						else
							lastZooming += 1;
						m_viewPort.zoom(lastZooming);
						checkBoundaries();
						mySimpleXYPlot.redraw();
	 
					}
//					else if (mode == TWO_FINGERS_DRAG) {
//						final float oldDist = distBetweenFingers;
//						distBetweenFingers = spacing(event);
//						lastZooming = oldDist / distBetweenFingers;
//						m_viewPort.zoom(lastZooming);
//						checkBoundaries();
//						mySimpleXYPlot.redraw();
//					}
					break;
			}
			return true;
		}
	 
	 
		private float spacing(MotionEvent event) {
			final float x = event.getX(0) - event.getX(1);
			final float y = event.getY(0) - event.getY(1);
			return FloatMath.sqrt(x * x + y * y);
		}
	 
		private void checkBoundaries() {
			//Make sure the proposed domain boundaries will not cause plotting issues
			m_viewPort.updateBoudaries();
			mySimpleXYPlot.setDomainBoundaries(m_viewPort.minXY.x, m_viewPort.maxXY.x, BoundaryMode.AUTO);
		}
	}

