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

import com.asksven.betterbatterystats.data.StatsProvider;

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
public class WidgetBattery
{
	long m_awake 		= 0;
	long m_screenOn 	= 0;
	long m_deepSleep 	= 0;
	
	static final String TAG = "WidgetBattery";
	static final float STROKE_WIDTH 		= 1;
	static final int BITMAP_WIDTH 			= 40;
	static final int BITMAP_HEIGHT 			= 40;
	static final int BATTERY_WIDTH 			= BITMAP_WIDTH - 25;
	static final int BATTERY_HEIGHT 		= BITMAP_HEIGHT - 10;
	static final int BATTERY_MARGIN_BOTTOM 	= 5;
	static final int BAR_WIDTH				= 5;

	
	static Paint m_paintBackground = new Paint();
    static Paint m_paintContour = new Paint();
    static Paint m_paintAwake = new Paint();
    static Paint m_paintDeepSleep = new Paint();
    static Paint m_paintScreenOffAwake = new Paint();

    void initPaints(Context ctx)
    {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		int opacity	= sharedPrefs.getInt("small_widget_opacity", 80);
		opacity = (255 * opacity) / 100; 

		int opacity_bg	= sharedPrefs.getInt("small_widget_bg_opacity", 20);
		opacity_bg = (255 * opacity_bg) / 100; 

    	m_paintBackground.setStyle(Paint.Style.FILL);
    	m_paintBackground.setColor(Color.BLACK);
    	m_paintBackground.setStrokeWidth(STROKE_WIDTH);
    	m_paintBackground.setAlpha(opacity_bg);

    	m_paintContour.setStyle(Paint.Style.FILL);
    	m_paintContour.setColor(Color.WHITE);
    	m_paintContour.setStrokeWidth(STROKE_WIDTH);
    	m_paintContour.setStyle(Paint.Style.STROKE);
        
    	m_paintAwake.setColor(Color.YELLOW);
    	m_paintAwake.setStrokeWidth(STROKE_WIDTH);
    	m_paintAwake.setStyle(Paint.Style.FILL);
    	m_paintAwake.setAlpha(opacity);

    	m_paintDeepSleep.setColor(Color.GREEN);
    	m_paintDeepSleep.setStrokeWidth(STROKE_WIDTH);
    	m_paintDeepSleep.setStyle(Paint.Style.FILL);
    	m_paintDeepSleep.setAlpha(opacity);

    	m_paintScreenOffAwake.setColor(Color.RED);
    	m_paintScreenOffAwake.setStrokeWidth(STROKE_WIDTH);
    	m_paintScreenOffAwake.setStyle(Paint.Style.FILL);
    	m_paintScreenOffAwake.setAlpha(opacity);

    }

    public void setAwake(long awake)
    {
    	m_awake = awake;
    }
    public void setScreenOn(long screenOn)
    {
    	m_screenOn = screenOn;
    }
    public void setDeepSleep(long deepSleep)
    {
    	m_deepSleep = deepSleep;
    }

    public Bitmap getBitmap(Context ctx)
    {
    	this.initPaints(ctx);
    	
        Bitmap bitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

//        canvas.drawPaint(m_paintBackground); //
//        drawColor(Color.TRANSPARENT);
    	
    	// draw the battery container
    	float left = BITMAP_WIDTH/2 - BATTERY_WIDTH / 2 + BAR_WIDTH;
    	float top = BITMAP_HEIGHT - BATTERY_MARGIN_BOTTOM - BATTERY_HEIGHT;
    	float right = BITMAP_WIDTH/2 + BATTERY_WIDTH / 2 + BAR_WIDTH;
    	float bottom = BITMAP_HEIGHT - BATTERY_MARGIN_BOTTOM;
    	canvas.drawRect(left, top, right, bottom, m_paintContour);
    	canvas.drawRect(left + 5, top - 3, right - 5, top, m_paintContour);
    	
    	// draw the gauges
    	float pctAwake = (float) ((float)m_screenOn / (float)m_awake);
    	
    	
    	if (pctAwake > 1f)
    	{
    		pctAwake = 1f;
    	}
    	pctAwake = 1 - pctAwake;


    	// draw battery
//    	canvas.drawRect(left+1, top+1, right, bottom, m_paintScreenOn);
    	
    	float topRed = bottom - ((bottom - top) * pctAwake);
    	canvas.drawRect(left+1, topRed, right, bottom, m_paintScreenOffAwake);

    	// draw deep sleep / awake
    	right = left - 3;
    	left = right - BAR_WIDTH;
    	
    	pctAwake = (float) ((float)m_awake / ((float)m_awake + (float) m_deepSleep));

    	canvas.drawRect(left, top, right, bottom+1, m_paintDeepSleep);
    	
    	topRed = bottom - ((bottom - top) * pctAwake);
    	canvas.drawRect(left, topRed, right, bottom+1, m_paintAwake);

    	return bitmap;
    }
}
