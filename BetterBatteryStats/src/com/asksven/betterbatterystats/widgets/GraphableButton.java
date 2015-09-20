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
package com.asksven.betterbatterystats.widgets;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.widget.Button;

/**
 * @author sven
 * Adapted from http://gitorious.org/0xdroid/packages_apps_settings/blobs/99f095dfde9dd7c95fd0b9b555b02b85594495b2/src/com/android/settings/battery_history/GraphableButton.java
 */
public class GraphableButton extends Button
{
    private static final String TAG = "GraphableButton";
    private Context m_context;
    
    static Paint[] sPaint = new Paint[2];
    static
    {
        sPaint[0] = new Paint();
        sPaint[0].setStyle(Paint.Style.FILL);
        sPaint[0].setColor(0xFF0080FF);
        
        sPaint[1] = new Paint();
        sPaint[1].setStyle(Paint.Style.FILL);
        sPaint[1].setColor(0xFFFF6060);
    }
    
    double[] mValues;
    
    public GraphableButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        m_context = context;
    }
    
    public void setValues(double[] values, double maxValue)
    {
        mValues = values.clone();
        for (int i = 0; i < values.length; i++)
        {
            mValues[i] /= maxValue;
        }
    }
    
    @Override
    public void onDraw(Canvas canvas)
    {
//        Log.d(TAG, "onDraw: w = " + getWidth() + ", h = " + getHeight());
        
        int xmin = getPaddingLeft();
        int xmax = getWidth() - getPaddingRight();
        int ymin = getPaddingTop();
        int ymax = getHeight() - getPaddingBottom();

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(m_context);
		int iHeight = 0;
		
		try
		{
			iHeight = Integer.valueOf(sharedPrefs.getString("graph_bar_height", "0"));
		}
		catch (Exception e)
		{
			// nop
		}
		
		if (iHeight != 0)
        {
        	int iMiddle = ymax - ymin;
        	
        	ymin = ymin - (iHeight / 2);
        	ymax = ymax + (iHeight / 2);
        }
        
        int startx = xmin;
        for (int i = 0; i < mValues.length; i++)
        {
            int endx = xmin + (int) (mValues[i] * (xmax - xmin));
//            Log.d(TAG, "onDraw: canvas (" + startx + ", " + ymin + ", " + endx + ", " + ymax +")");
            canvas.drawRect(startx, ymin, endx, ymax, sPaint[i]);
            startx = endx;
        }
        super.onDraw(canvas);
    }
}