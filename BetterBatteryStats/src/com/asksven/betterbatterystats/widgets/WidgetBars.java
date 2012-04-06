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
public class WidgetBars
{
	static final String TAG = "WidgetBars";
	static final float STROKE_WIDTH = 5;
	static final int BITMAP_WIDTH = 256;
	static final int BITMAP_HEIGHT = 70;
	
	
    static Paint[] sPaint = new Paint[5];
    static
    {
        sPaint[0] = new Paint();
        sPaint[0].setStyle(Paint.Style.FILL);
        sPaint[0].setColor(Color.BLUE);
        sPaint[0].setStrokeWidth(STROKE_WIDTH);
        sPaint[0].setStyle(Paint.Style.STROKE);
        
        sPaint[1] = new Paint();
        sPaint[1].setStyle(Paint.Style.FILL);
        sPaint[1].setColor(Color.YELLOW);
        sPaint[1].setStrokeWidth(STROKE_WIDTH);
        sPaint[1].setStyle(Paint.Style.STROKE);
        
        sPaint[2] = new Paint();
        sPaint[2].setStyle(Paint.Style.FILL);
        sPaint[2].setColor(Color.GREEN);
        sPaint[2].setStrokeWidth(STROKE_WIDTH);
        sPaint[2].setStyle(Paint.Style.STROKE);

        sPaint[3] = new Paint();
        sPaint[3].setStyle(Paint.Style.FILL);
        sPaint[3].setColor(Color.RED);
        sPaint[3].setStrokeWidth(STROKE_WIDTH);
        sPaint[3].setStyle(Paint.Style.STROKE);

        sPaint[4] = new Paint();
        sPaint[4].setStyle(Paint.Style.FILL);
        sPaint[4].setColor(Color.MAGENTA);
        sPaint[4].setStrokeWidth(STROKE_WIDTH);
        sPaint[4].setStyle(Paint.Style.STROKE);

    }

    public Bitmap getBitmap(ArrayList<Long> values)
    {
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

        Paint paint = new Paint();

        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        
        paint.setColor(Color.RED);
        paint.setStrokeWidth(10);
        
    	for (int i=0; i < values.size(); i++)
    	{
    		float ratio = (float)values.get(i) / (float)max;
    		int len = (int) (BITMAP_WIDTH*ratio);
    		Log.d(TAG, "Drawing line for value " + len + ",ratio is " + ratio +  ", value is " + values.get(i));
    		int pos = i*10+10;
    		canvas.drawLine(0, pos, len, pos, sPaint[i]);
    	}
    	
//    	canvas.drawLine(0, 1, (128), 1, paint);
//    	canvas.drawLine(0, 11, (64), 11, paint);
//    	canvas.drawLine(0, 21, (32), 21, paint);
//    	canvas.drawLine(0, 31, (16), 31, paint);
    	
    	
    	
        return bitmap;
    }
}
