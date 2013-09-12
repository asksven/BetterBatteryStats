/*
 * Copyright (C) 2012 asksven
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
 * 
 * This file was contributed by two forty four a.m. LLC <http://www.twofortyfouram.com>
 * unter the terms of the Apache License, Version 2.0
 */

package com.asksven.betterbatterystats.widgets;

import java.util.ArrayList;

import com.echo.holographlibrary.PieGraph;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

/**
 * Extends Daniel Nadeau's PieGraph and adds a title text
 * @author sven
 *
 */
public class MyPieGraph extends PieGraph
{
	String m_title = "";    
	float m_textHeightRatio = 0.3f;
	
	static Paint sText = new Paint();
    static
    {
    	sText.setStyle(Paint.Style.STROKE);
    	sText.setColor(Color.WHITE);
//        sPaint.setStrokeWidth(STROKE_WIDTH + 2);
    }
	
	
	
	public MyPieGraph(Context context)
	{
		super(context);
	}

	public MyPieGraph(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);
		if (m_title.equals(""))
		{
			return;
		}
		
		float midX, midY;

		midX = getWidth() / 2;
		midY = getHeight() / 2;

		float textSize = getHeight() * m_textHeightRatio;
        sText.setTextSize( (float) textSize);
        float width = sText.measureText(m_title);
        canvas.drawText(m_title, getWidth() / 2 - width / 2, getHeight() / 2 + textSize / 3, sText);

	}
	
	public void setTitle(String title)
	{
		m_title = title;
	}
	
	public void setTextHeightRatio(float ratio)
	{
		m_textHeightRatio = ratio;
	}
	

}
