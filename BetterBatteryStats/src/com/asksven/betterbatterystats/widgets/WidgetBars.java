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

import com.asksven.betterbatterystats.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
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
	static final int BAR_WIDTH = 10;
	
	static final int BITMAP_WIDTH = 256;
	static final int BITMAP_HEIGHT = 110;
	
	
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
    	
    	Resources res = ctx.getResources();
    	int since = res.getColor(R.color.since);
    	int deepSleep = res.getColor(R.color.deepSleep);
    	int awake = res.getColor(R.color.awake);
    	int screenOn = res.getColor(R.color.screenOn);
    	int kWl = res.getColor(R.color.kWl);
    	int pWl = res.getColor(R.color.pWl);

    	//Paint for "Since:"
    	m_paint[0] = new Paint();
    	m_paint[0].setStyle(Paint.Style.FILL);
    	m_paint[0].setColor(since);
    	m_paint[0].setStrokeWidth(STROKE_WIDTH);
//    	m_paint[0].setStyle(Paint.Style.STROKE);
    	m_paint[0].setAlpha(opacity);
        
    	//Paint for "Deep Sleep":
    	m_paint[1] = new Paint();
    	m_paint[1].setStyle(Paint.Style.FILL);
    	m_paint[1].setColor(deepSleep);
    	m_paint[1].setStrokeWidth(STROKE_WIDTH);
//    	m_paint[1].setStyle(Paint.Style.STROKE);
    	m_paint[1].setAlpha(opacity);
        
    	//Paint for "Awake:"
    	m_paint[2] = new Paint();
    	m_paint[2].setStyle(Paint.Style.FILL);
    	m_paint[2].setColor(awake);
    	m_paint[2].setStrokeWidth(STROKE_WIDTH);
//    	m_paint[2].setStyle(Paint.Style.STROKE);
    	m_paint[2].setAlpha(opacity);

    	//Paint for "Screen on:"
    	m_paint[3] = new Paint();
    	m_paint[3].setStyle(Paint.Style.FILL);
    	m_paint[3].setColor(screenOn);
    	m_paint[3].setStrokeWidth(STROKE_WIDTH);
//    	m_paint[3].setStyle(Paint.Style.STROKE);
    	m_paint[3].setAlpha(opacity);

    	//Paint for "Kernel WL:"
    	m_paint[4] = new Paint();
    	m_paint[4].setStyle(Paint.Style.FILL);
    	m_paint[4].setColor(kWl);
    	m_paint[4].setStrokeWidth(STROKE_WIDTH);
//    	m_paint[4].setStyle(Paint.Style.STROKE);
    	m_paint[4].setAlpha(opacity);

    	//Paint for "Partial WL:"
    	m_paint[5] = new Paint();
    	m_paint[5].setStyle(Paint.Style.FILL);
    	m_paint[5].setColor(pWl);
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

        int pos = 0;
    	for (int i=0; i < values.size(); i++)
    	{
    		float ratio = (float)values.get(i) / (float)max;
    		int len = (int) (BITMAP_WIDTH*ratio);
    		Log.d(TAG, "Drawing line for value " + len + ", ratio is " + ratio +  ", value is " + values.get(i));
    		canvas.drawRect(0, pos, len, pos + BAR_WIDTH, m_paint[i]);
    		pos += 20;
    	}
    	
    	
        return bitmap;
    }
}
