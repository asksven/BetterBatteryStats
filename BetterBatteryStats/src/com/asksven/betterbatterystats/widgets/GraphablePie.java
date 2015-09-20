/*
 * Copyright (C) 2011-12 asksven
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

import com.asksven.betterbatterystats.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ImageView;

/**
 * @author sven
 */
public class GraphablePie extends ImageView
{
    private static final String TAG = "GraphablePie";
    private static final String ASKSVENNS="http://asksven.net";
    private Context m_context;
    
    private static final int STROKE_WIDTH = 6;

    static RectF sOval = new RectF();
    
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
        sBackground.setColor(0x7795a5a6);
    }
    
    static Paint sText = new Paint();
    static
    {
    	sText.setStyle(Paint.Style.STROKE);
    	sText.setColor(Color.BLACK);
    	sText.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
    	sText.setTextSize(12);
    	sText.setAntiAlias(true);
//        sPaint.setStrokeWidth(STROKE_WIDTH + 2);
    }

    double mValue = 0.5;
    
    String mLabelPct = String.format("%.0f", mValue * 100) + "%";
    String m_name;
    
    public GraphablePie(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        m_context = context;
        
        int color = 0;
        try
        {
	        TypedValue tv = new TypedValue();
	        m_context.getTheme().resolveAttribute(android.R.attr.textColorPrimary, tv, true);
	        color = getResources().getColor(tv.resourceId);
        }
        catch (Exception e)
        {
        	color = Color.BLACK;
        }

        sPaint.setColor(m_context.getResources().getColor(R.color.peterriver)); 
    	//sText.setTextSize(m_context.getResources().getDimension(R.dimen.text_size_medium));
    	sText.setColor(color); 



    }
    
    public void setValue(double value, double maxValue)
    {
    	mLabelPct = String.format("%.0f", value) + "%";
        mValue = (double) ((float)value / (float)maxValue);
    	mLabelPct = String.format("%.0f", mValue * 100) + "%";

        // we must force onDraw by invalidating the View (see http://wing-linux.sourceforge.net/guide/topics/graphics/index.html)
        this.invalidate();
    }
    
    public void setName(String name)
    {
    	m_name = name;
    }
    
    @Override
    public void onDraw(Canvas canvas)
    {
    	float centerX, centerY, radius;
		centerX = getWidth() / 2;
		centerY = getHeight() / 2;
		radius = getWidth() / 2 - getPaddingLeft() -1;
        
        sOval.set(centerX - radius, 
				centerY - radius, 
				centerX + radius, 
				centerY + radius);

        sPaint.setStrokeWidth((getWidth() - (2 * getPaddingLeft())) / 10);
        sBackground.setStrokeWidth((getWidth() - (2 * getPaddingLeft())) / 10);
    	// draw bg        
        canvas.drawArc(sOval, 0, 360f, false, sBackground);
        // draw gauge
        canvas.drawArc(sOval, -90, (float)(360 * mValue), false, sPaint);

        // calculate a size that will not overlap with the circle, whatever the size is
        long textSize =  (getHeight()- (2 * getPaddingLeft())) / 3 ;
        sText.setTextSize(textSize);
        
        // when rendering in editor make sure the color is set to something
        if (sText.getColor() == 0)
        {
        	sText.setColor(Color.BLACK);
        }
        
        float width = sText.measureText(mLabelPct);
        canvas.drawText(mLabelPct, getWidth() / 2 - width / 2, getHeight() / 2 + textSize / 3, sText);
        //canvas.drawText("n/a%", getWidth() / 2 - width / 2, getHeight() / 2 + textSize / 3, sText);
        
        super.onDraw(canvas);
    }
}