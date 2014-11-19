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
        	mMinX = mValues.get(0).mX.longValue();
        }
        
        for (int i = 0; i < values.size(); i++)
        {
        	if (mValues.get(i).mX.longValue() > mMaxX)
        	{
        		mMaxX = mValues.get(i).mX.longValue();
        	}
        	if (mValues.get(i).mY.longValue() < mMinY)
        	{
        		mMinY = mValues.get(i).mY.longValue();
        	}

        	if (mValues.get(i).mY.longValue() > mMaxY)
        	{
        		mMaxY = mValues.get(i).mY.longValue();
        	}
            
        }
        // we must force onDraw by invalidating the View (see http://wing-linux.sourceforge.net/guide/topics/graphics/index.html)
        this.invalidate();
    }
    
    @Override
    public void onDraw(Canvas canvas)
    {
    	// set some values for the gauge to get painted in the res editor
    	if (mValues == null)
    	{
    		mValues = new ArrayList<Datapoint>();
    		
    		mValues.add(new Datapoint(10, 3));
    		mValues.add(new Datapoint(30, 1));
    		mValues.add(new Datapoint(31, 1));
    		mValues.add(new Datapoint(50, 1));
    		mMinX = 10;
    		mMaxX = 50;
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
        
        for (int i = 0; i < mValues.size(); i++)
        {
        	float ratioX = ( (float)mValues.get(i).mX.longValue() - (float)mMinX ) / ( (float)mMaxX - (float)mMinX );
        	float ratioY = ( (float)mValues.get(i).mY.longValue() - (float)mMinY ) / ( (float)mMaxY - (float)mMinY );
        	float posX = (ratioX * (xmax - xmin)) + xmin;
        	float posY = (ratioY * (ymax - ymin)) + ymin;
            canvas.drawLine(posX, ymax, posX, ymax - posY, sPaint);
        }
        super.onDraw(canvas);
    }
}