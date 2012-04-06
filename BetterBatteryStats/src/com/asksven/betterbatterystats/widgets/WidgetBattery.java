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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

/**
 * @author sven
 *
 */
public class WidgetBattery
{
	long m_awake 	= 0;
	long m_screenOn = 0;
	
	static final String TAG = "WidgetBattery";
	static final float STROKE_WIDTH 		= 1;
	static final int BITMAP_WIDTH 			= 72;
	static final int BITMAP_HEIGHT 			= 72;
	static final int BATTERY_WIDTH 			= 25;
	static final int BATTERY_HEIGHT 		= 55;
	static final int BATTERY_MARGIN_BOTTOM 	= 5;

	
	
    static Paint sPaintContour = new Paint();
    static Paint sPaintScreenOn = new Paint();
    static Paint sPaintScreenOffAwake = new Paint();

    static
    {
    	sPaintContour.setStyle(Paint.Style.FILL);
    	sPaintContour.setColor(Color.WHITE);
    	sPaintContour.setStrokeWidth(STROKE_WIDTH);
    	sPaintContour.setStyle(Paint.Style.STROKE);
        
    	sPaintScreenOn.setColor(Color.GREEN);
    	sPaintScreenOn.setStrokeWidth(STROKE_WIDTH);
    	sPaintScreenOn.setStyle(Paint.Style.FILL);

    	sPaintScreenOffAwake.setColor(Color.RED);
    	sPaintScreenOffAwake.setStrokeWidth(STROKE_WIDTH);
    	sPaintScreenOffAwake.setStyle(Paint.Style.FILL);

    }

    public void setAwake(long awake)
    {
    	m_awake = awake;
    }
    public void setScreenOn(long screenOn)
    {
    	m_screenOn = screenOn;
    }
    public Bitmap getBitmap()
    {
    	
        Bitmap bitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        canvas.drawColor(Color.TRANSPARENT);
    	
    	// draw the battery container
    	float left = BITMAP_WIDTH/2 - BATTERY_WIDTH / 2;
    	float top = BITMAP_HEIGHT - BATTERY_MARGIN_BOTTOM - BATTERY_HEIGHT;
    	float right = BITMAP_WIDTH/2 + BATTERY_WIDTH / 2;
    	float bottom = BITMAP_HEIGHT - BATTERY_MARGIN_BOTTOM;
    	canvas.drawRect(left, top, right, bottom, sPaintContour);
    	canvas.drawRect(left + 5, top - 3, right - 5, top, sPaintContour);
    	
    	// draw the gauges
    	float pctAwake = (float) ((float)m_screenOn / (float)m_awake);
    	if (pctAwake > 1f)
    	{
    		pctAwake = 1f;
    	}

    	canvas.drawRect(left+1, top+1, right, bottom, sPaintScreenOn);
    	
    	float topRed = bottom - ((bottom - top) * pctAwake);
    	canvas.drawRect(left+1, topRed, right, bottom, sPaintScreenOffAwake);

    	return bitmap;
    }
}
