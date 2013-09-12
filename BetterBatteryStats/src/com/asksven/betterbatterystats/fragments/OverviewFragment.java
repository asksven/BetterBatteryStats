/*
 * Copyright (C) 2013 asksven
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

package com.asksven.betterbatterystats.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.R.id;
import com.asksven.betterbatterystats.R.layout;
import com.asksven.betterbatterystats.widgets.MyPieGraph;
import com.echo.holographlibrary.PieGraph;
import com.echo.holographlibrary.PieGraph.OnSliceClickedListener;
import com.echo.holographlibrary.PieSlice;

public class OverviewFragment extends SherlockFragment
{

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		final View v = inflater.inflate(R.layout.overview, container, false);
		MyPieGraph pg = (MyPieGraph) v.findViewById(R.id.piegraph);

		pg.setTitle("12h 32m");
		pg.setTextHeightRatio(0.2f);
		
		PieSlice slice = new PieSlice();
		slice.setColor(getResources().getColor(R.color.state_green)); // Color.GREEN); //parseColor("#99CC00"));
		slice.setValue(2);
		slice.setTitle("Deep Sleep");
		pg.addSlice(slice);
		slice = new PieSlice();
		slice.setColor(getResources().getColor(R.color.state_red)); //Color.RED); //parseColor("#FFBB33"));
		slice.setValue(3);
		slice.setTitle("Awake");
		pg.addSlice(slice);
		slice = new PieSlice();
		slice.setColor(getResources().getColor(R.color.state_yellow)); //Color.YELLOW); //Color.parseColor("#AA66CC"));
		slice.setValue(8);
		slice.setTitle("Screen On");
		pg.addSlice(slice);

		pg.setOnSliceClickedListener(new OnSliceClickedListener()
		{

			@Override
			public void onClick(int index)
			{

			}

		});

		return v;
	}
}
