/*
 * Copyright (C) 2011-14 asksven
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
package com.asksven.betterbatterystats.widgets;

import java.util.ArrayList;

import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.data.Datapoint;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

/**
 * @author sven
 */
public class GraphableBarsTimeline extends ImageView
{
	private static final String TAG = "GraphableBarsTimeline";
	private Context m_context;

	private static final int STROKE_WIDTH = 1;

	static Paint sPaint = new Paint();
	static
	{
		sPaint.setStyle(Paint.Style.STROKE);
		sPaint.setAntiAlias(true);
		sPaint.setColor(0xFF0080FF);
		sPaint.setStrokeWidth(STROKE_WIDTH);
	}
	static Paint sBackground = new Paint();
	static
	{
		sBackground.setStyle(Paint.Style.STROKE);
		sBackground.setAntiAlias(true);
		sBackground.setStrokeWidth(STROKE_WIDTH);
		sBackground.setColor(0x778B7B8B);
	}

	ArrayList<Datapoint> mValues;
	long mMinX = 0;
	long mMaxX = 0;
	long mMinY = 0;
	long mMaxY = 0;

	public GraphableBarsTimeline(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		m_context = context;
		sPaint.setColor(m_context.getResources().getColor(R.color.peterriver));
	}

	public void setValues(ArrayList<Datapoint> values)
	{
		mValues = (ArrayList<Datapoint>) values.clone();
		if (values.size() > 0)
		{
			mMinX = mValues.get(0).mX;
		}

		for (int i = 0; i < values.size(); i++)
		{
			if (mValues.get(i).mX > mMaxX)
			{
				mMaxX = mValues.get(i).mX;
			}
			if (mValues.get(i).mY < mMinY)
			{
				mMinY = mValues.get(i).mY;
			}

			if (mValues.get(i).mY > mMaxY)
			{
				mMaxY = mValues.get(i).mY;
			}

		}
		Log.i(TAG, "Read values and tetermined mMinX=" + mMinX + ", mMaxX=" + mMaxX + ", mMinY=" + mMinY + ", mMaxY="
				+ mMaxY);
		// we must force onDraw by invalidating the View (see
		// http://wing-linux.sourceforge.net/guide/topics/graphics/index.html)
		this.invalidate();
	}

	@Override
    public void onDraw(Canvas canvas)
    {
    	// set some values for the gauge to get painted in the res editor
    	if (mValues == null)
    	{
    		mValues = new ArrayList<Datapoint>();
    		

    		mValues.add(new Datapoint(1416405215261L, 100));
    		mValues.add(new Datapoint(1416405215465L, 100));
			mValues.add(new Datapoint(1416405224514L, 100));
			mValues.add(new Datapoint(1416405240194L, 100));
			mValues.add(new Datapoint(1416405242746L, 100));
			mValues.add(new Datapoint(1416405255284L, 100));
			mValues.add(new Datapoint(1416405258300L, 100));
			mValues.add(new Datapoint(1416405261313L, 100));
			mValues.add(new Datapoint(1416405265069L, 100));
			mValues.add(new Datapoint(1416405270363L, 100));
			mValues.add(new Datapoint(1416405287955L, 100));
			mValues.add(new Datapoint(1416405288170L, 100));
			mValues.add(new Datapoint(1416405288968L, 100));
			mValues.add(new Datapoint(1416405294829L, 100));
			mValues.add(new Datapoint(1416405295026L, 100));
			mValues.add(new Datapoint(1416405297529L, 100));
			mValues.add(new Datapoint(1416405298158L, 100));
			mValues.add(new Datapoint(1416405300541L, 100));
			mValues.add(new Datapoint(1416405345550L, 100));

    		mMinX = 1416405215261L;
    		mMaxX = 1416405345550L;
    		mMinY = 0;
    		mMaxY = 3;
    	}
//        Log.d(TAG, "onDraw: w = " + getWidth() + ", h = " + getHeight());
        
        int xmin = getPaddingLeft();
        int xmax = getWidth() - getPaddingRight();
        int ymin = 0;
        int ymax = getHeight();

        
        // draw bg
        //canvas.drawRect(getPaddingLeft(), 0, xmax, getHeight(), sBackground);
        long rangeX = ( mMaxX - mMinX );
        long rangeY = ( mMaxY - mMinY );
        for (int i = 0; i < mValues.size(); i++)
        {
        	Log.i(TAG, "Rendering value [" + i + "]: x=" + mValues.get(i).mX + ", y=" + mValues.get(i).mY);
        	float ratioX = (float)( mValues.get(i).mX - mMinX ) / rangeX;
        	float ratioY = (float)( mValues.get(i).mY - mMinY ) / rangeY;
        	Log.i(TAG, "Ratio values [" + i + "]: rX=" + ratioX + ", rY=" + ratioY);
        	float posX = (ratioX * (xmax - xmin)) + xmin;
        	float posY = (ratioY * (ymax - ymin)) + ymin;
        	Log.i(TAG, "Translated to posX=" + posX + ", posY=" + posY);
            canvas.drawLine(posX, ymax, posX, ymax - posY, sPaint);
        }
        super.onDraw(canvas);
    }
}