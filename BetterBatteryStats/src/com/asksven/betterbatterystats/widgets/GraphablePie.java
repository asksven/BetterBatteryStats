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
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

/**
 * @author sven
 */
public class GraphablePie extends ImageView
{
    private static final String TAG = "GraphablePie";
    private Context m_context;
    
    private static final int STROKE_WIDTH = 8;

    static RectF sOval = new RectF();
    
    static Paint sPaint = new Paint();
    static
    {
        sPaint.setStyle(Paint.Style.STROKE);
        sPaint.setColor(0xFF0080FF);
        sPaint.setStrokeWidth(STROKE_WIDTH);
    }
    static Paint sBackground = new Paint();
    static
    {
        sBackground.setStyle(Paint.Style.STROKE);
        sBackground.setStrokeWidth(STROKE_WIDTH);
        sBackground.setColor(0x778B7B8B);
    }

    static Paint sText = new Paint();
    static
    {
    	sText.setStyle(Paint.Style.STROKE);
    	sText.setColor(Color.WHITE);
//        sPaint.setStrokeWidth(STROKE_WIDTH + 2);
    }

    double mValue = 0.5;
    String mLabelPct = String.format("%.0f", mValue * 100) + "%";
    String m_name;
    
    public GraphablePie(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        m_context = context;
        
        sPaint.setColor(m_context.getResources().getColor(R.color.peterriver)); 

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
		radius = getWidth() / 2 - getPaddingLeft();
        
        sOval.set(centerX - radius, 
				centerY - radius, 
				centerX + radius, 
				centerY + radius);

        sPaint.setStrokeWidth(getWidth() / 10);
        sBackground.setStrokeWidth(getWidth() / 10);
    	// draw bg        
        canvas.drawArc(sOval, 0, 360f, false, sBackground);
        // draw gauge
        canvas.drawArc(sOval, -90, (float)(360 * mValue), false, sPaint);

        long textSize = getHeight() / 3;
        sText.setTextSize(textSize);
        float width = sText.measureText(mLabelPct);
        canvas.drawText(mLabelPct, getWidth() / 2 - width / 2, getHeight() / 2 + textSize / 3, sText);
        super.onDraw(canvas);
    }
}