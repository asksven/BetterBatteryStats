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
import com.asksven.betterbatterystats.widgetproviders.AppWidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.preference.PreferenceManager;

/**
 * @author sven
 *
 */
public class WidgetSummary
{
	long m_awake 		= 0;
	long m_screenOn 	= 0;
	long m_deepSleep 	= 0;
	long m_duration		= 0;
	long m_kwl			= 0;
	long m_pwl			= 0;
	int m_bitmapSizePx 	= 56;
	
	static final String TAG = "WidgetSummary";
	
	
	static final float STROKE_WIDTH 		= 1;
	static final int BAR_WIDTH				= 5;
	static final int PADDING 				= 5;
	
	static RectF sOval = new RectF();
	static RectF sOvalKWL = new RectF();
	static RectF sOvalPWL = new RectF();
    
    static Paint sText = new Paint();
    static
    {
    	sText.setStyle(Paint.Style.FILL);
    	sText.setColor(Color.WHITE);
    	sText.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
    	sText.setTextSize(12);
    	sText.setAntiAlias(true);
//        sPaint.setStrokeWidth(STROKE_WIDTH + 2);
    }
	static Paint m_paintBackground 		= new Paint();
    static Paint m_paintScreenOn 		= new Paint();
    static Paint m_paintDeepSleep 		= new Paint();
    static Paint m_paintScreenOffAwake	= new Paint();
    static Paint m_paintKWL 			= new Paint();
    static Paint m_paintPWL 			= new Paint();
    void initPaints(Context ctx)
    {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		int opacity	= sharedPrefs.getInt("small_widget_opacity", 80);
		opacity = (255 * opacity) / 100; 

		int opacity_bg	= sharedPrefs.getInt("small_widget_bg_opacity", 20);
		opacity_bg = (255 * opacity_bg) / 100; 

    	m_paintBackground.setStyle(Paint.Style.STROKE);
    	m_paintBackground.setColor(Color.BLACK);
    	m_paintBackground.setStrokeWidth(STROKE_WIDTH);
    	m_paintBackground.setAlpha(opacity_bg);

    	m_paintScreenOn.setColor(ctx.getResources().getColor(R.color.screen_on));
    	m_paintScreenOn.setStrokeWidth(STROKE_WIDTH);
    	m_paintScreenOn.setStyle(Paint.Style.STROKE);
    	m_paintScreenOn.setAlpha(opacity);
    	m_paintScreenOn.setAntiAlias(true);

    	m_paintDeepSleep.setColor(ctx.getResources().getColor(R.color.deep_sleep));
    	m_paintDeepSleep.setStrokeWidth(STROKE_WIDTH);
    	m_paintDeepSleep.setStyle(Paint.Style.STROKE);
    	m_paintDeepSleep.setAlpha(opacity);
    	m_paintDeepSleep.setAntiAlias(true);

    	m_paintScreenOffAwake.setColor(ctx.getResources().getColor(R.color.awake));
    	m_paintScreenOffAwake.setStrokeWidth(STROKE_WIDTH);
    	m_paintScreenOffAwake.setStyle(Paint.Style.STROKE);
    	m_paintScreenOffAwake.setAlpha(opacity);
    	m_paintScreenOffAwake.setAntiAlias(true);
    	
    	m_paintKWL.setColor(ctx.getResources().getColor(R.color.kwl));
    	m_paintKWL.setStrokeWidth(2);
    	m_paintKWL.setStyle(Paint.Style.STROKE);
    	m_paintKWL.setAlpha(opacity);
    	m_paintKWL.setAntiAlias(true);
    	
    	m_paintPWL.setColor(ctx.getResources().getColor(R.color.pwl));
    	m_paintPWL.setStrokeWidth(2);
    	m_paintPWL.setStyle(Paint.Style.STROKE);
    	m_paintPWL.setAlpha(opacity);
    	m_paintPWL.setAntiAlias(true);
    }

    public void setAwake(long awake)
    {
    	m_awake = awake;
    }
    
    public void setBitmapSizePx(int size)
    {
    	if (size < 10)
    	{
    		m_bitmapSizePx = 10;
    	}
    	else
    	{
    		m_bitmapSizePx = size;
    	}
    }
    
    public void setScreenOn(long screenOn)
    {
    	m_screenOn = screenOn;
    }

    public void setDeepSleep(long deepSleep)
    {
    	m_deepSleep = deepSleep;
    }

    public void setDuration(long duration)
    {
    	m_duration = duration;
    }
    
    public void setKWL(long kwl)
    {
    	m_kwl = kwl;
    }
    
    public void setPWL(long pwl)
    {
    	m_pwl = pwl;
    }
    
    public Bitmap getBitmap(Context ctx)
    {
    	this.initPaints(ctx);

        Bitmap bitmap = Bitmap.createBitmap(m_bitmapSizePx, m_bitmapSizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

    	//////
       	float centerX, centerY, radius;
		centerX = m_bitmapSizePx / 2;
		centerY = m_bitmapSizePx / 2;
		radius = m_bitmapSizePx / 2 - (m_bitmapSizePx / 10);
        
		float strokeWidth, strokeWidthSmall;
		strokeWidth = m_bitmapSizePx / 10;
		strokeWidthSmall = m_bitmapSizePx / 10 / 4;
				
        sOval.set(centerX - radius, 
				centerY - radius, 
				centerX + radius, 
				centerY + radius);
        float offset = (m_bitmapSizePx / 10 / 2) + (m_bitmapSizePx / 10 / 8) + (m_bitmapSizePx / 10 / 16) + (m_bitmapSizePx / 10 / 32);
         		
        sOvalKWL.set(centerX - (radius + offset), 
				centerY - (radius + offset), 
				centerX + (radius + offset), 
				centerY + (radius + offset));
        
        sOvalPWL.set(centerX - (radius - offset), 
				centerY - (radius - offset), 
				centerX + (radius - offset), 
				centerY + (radius - offset));

        // draw gauge
        float angleDeepSleep 		= (float)m_deepSleep / (float)m_duration;
        float angleAwakeScreenOff	= (float)m_awake / (float)m_duration;
        float angleScreenOn			= (float)m_screenOn / (float)m_duration;
        float angleKWL				= (float)m_kwl / (float)m_duration;
        float anglePWL				= (float)m_pwl / (float)m_duration;
        
        m_paintDeepSleep.setStrokeWidth(strokeWidth);
        m_paintScreenOn.setStrokeWidth(strokeWidth);
        m_paintScreenOffAwake.setStrokeWidth(strokeWidth);
        m_paintKWL.setStrokeWidth(strokeWidthSmall);
        m_paintPWL.setStrokeWidth(strokeWidthSmall);
        
        canvas.drawArc(sOval, -90, 360f * angleDeepSleep, false, m_paintDeepSleep);
        canvas.drawArc(sOval, -90 + (360f * angleDeepSleep), 360f * angleScreenOn, false, m_paintScreenOn);
        canvas.drawArc(sOval, -90 + (360f * (angleDeepSleep + angleScreenOn)), 360f * angleAwakeScreenOff, false, m_paintScreenOffAwake);
        
        canvas.drawArc(sOvalKWL, -90 - (360f * angleKWL), 360f * angleKWL, false, m_paintKWL);
        canvas.drawArc(sOvalPWL, -90 -(360f * anglePWL), 360f * anglePWL, false, m_paintPWL);
        

        // calculate a size that will not overlap with the circle, whatever the size is
        String labelDuration = AppWidget.formatDuration(m_duration);

        long textSize =  (m_bitmapSizePx - (2 * PADDING)) / 3 ;
        sText.setTextSize(textSize);
        
        // calculate the containing rectangle size
        Rect bounds = new Rect();
        // ask the paint for the bounding rect if it were to draw this
        // text.
        // if the text is short, e.g. 12m then calculate with a longer text: 1h12m
        if (labelDuration.length() < 5)
        {
        	sText.getTextBounds("1h12m", 0, 5, bounds);
        }
        {
        	sText.getTextBounds(labelDuration, 0, labelDuration.length(), bounds);
        }
        // determine the width and height
        int w = bounds.right - bounds.left;
        int h = bounds.bottom - bounds.top;
        // calculate the baseline to use so that the
        // entire text is visible including the descenders

        // determine how much to scale the width to fit the view
    	float xscaleW = ((float) (m_bitmapSizePx-PADDING) / w);
    	float xscaleH = ((float) (m_bitmapSizePx/2-PADDING) / h);
        
        sText.setTextSize(textSize * Math.min(xscaleW, xscaleH));
        // when rendering in editor make sure the color is set to something
        if (sText.getColor() == 0)
        {
        	sText.setColor(Color.WHITE);
        }
        
        
        float width = sText.measureText(labelDuration);
        canvas.drawText(labelDuration, m_bitmapSizePx / 2 - width / 2, m_bitmapSizePx / 2 + textSize / 3, sText);

    	return bitmap;
    }
}
