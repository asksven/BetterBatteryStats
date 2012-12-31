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

import java.util.ArrayList;

import com.asksven.betterbatterystats.LogSettings;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * @author sven
 *
 */
public class WidgetBars
{
	static final String TAG = "WidgetBars";
	static final float STROKE_WIDTH = 1;
	static final int BAR_WIDTH = 6;
	
	static final int BITMAP_WIDTH = 256;
	static final int BITMAP_HEIGHT = 70;
	
	
    static Paint[] m_paint = new Paint[6];
	static Paint m_paintBackground = new Paint();

    
    void initPaints(Context ctx)
    {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		int opacity	= sharedPrefs.getInt("small_widget_opacity", 80);
		opacity = (255 * opacity) / 100; 

    	m_paintBackground.setStyle(Paint.Style.FILL);
    	m_paintBackground.setColor(Color.BLACK);
    	m_paintBackground.setStrokeWidth(STROKE_WIDTH);
    	m_paintBackground.setAlpha(opacity);

    	m_paint[0] = new Paint();
    	m_paint[0].setStyle(Paint.Style.FILL);
    	m_paint[0].setColor(Color.BLUE);
    	m_paint[0].setStrokeWidth(STROKE_WIDTH);
//    	m_paint[0].setStyle(Paint.Style.STROKE);
    	m_paint[0].setAlpha(opacity);
        
    	m_paint[1] = new Paint();
    	m_paint[1].setStyle(Paint.Style.FILL);
    	m_paint[1].setColor(Color.GREEN);
    	m_paint[1].setStrokeWidth(STROKE_WIDTH);
//    	m_paint[1].setStyle(Paint.Style.STROKE);
    	m_paint[1].setAlpha(opacity);
        
    	m_paint[2] = new Paint();
    	m_paint[2].setStyle(Paint.Style.FILL);
    	m_paint[2].setColor(Color.YELLOW);
    	m_paint[2].setStrokeWidth(STROKE_WIDTH);
//    	m_paint[2].setStyle(Paint.Style.STROKE);
    	m_paint[2].setAlpha(opacity);

    	m_paint[3] = new Paint();
    	m_paint[3].setStyle(Paint.Style.FILL);
    	m_paint[3].setColor(Color.WHITE);
    	m_paint[3].setStrokeWidth(STROKE_WIDTH);
//    	m_paint[3].setStyle(Paint.Style.STROKE);
    	m_paint[3].setAlpha(opacity);

    	m_paint[4] = new Paint();
    	m_paint[4].setStyle(Paint.Style.FILL);
    	m_paint[4].setColor(Color.MAGENTA);
    	m_paint[4].setStrokeWidth(STROKE_WIDTH);
//    	m_paint[4].setStyle(Paint.Style.STROKE);
    	m_paint[4].setAlpha(opacity);

    	m_paint[5] = new Paint();
    	m_paint[5].setStyle(Paint.Style.FILL);
    	m_paint[5].setColor(Color.CYAN);
    	m_paint[5].setStrokeWidth(STROKE_WIDTH);
//    	m_paint[5].setStyle(Paint.Style.STROKE);
    	m_paint[5].setAlpha(opacity);

    }

    public Bitmap getBitmap(Context ctx, ArrayList<Long> values)
    {
    	this.initPaints(ctx);

    	long max = 0;
    	for (int i=0; i < values.size(); i++)
    	{
    		if (values.get(i) > max)
    		{
    			max = values.get(i);
    		}
    	}
    	
    	
        Bitmap bitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        canvas.drawColor(Color.TRANSPARENT);

        
    	for (int i=0; i < values.size(); i++)
    	{
    		float ratio = (float)values.get(i) / (float)max;
    		int len = (int) (BITMAP_WIDTH*ratio);
    		if (LogSettings.DEBUG)
    		{
    			Log.d(TAG, "Drawing line for value " + len + ",ratio is " + ratio +  ", value is " + values.get(i));
    		}
    		int pos = i*10+10;
//    		canvas.drawLine(0, pos, len, pos, m_paint[i]);
    		canvas.drawRect(0, pos, len, pos + BAR_WIDTH, m_paint[i]);
    	}
    	
    	
        return bitmap;
    }
}
