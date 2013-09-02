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
package com.asksven.betterbatterystats.data;

/**
 * @author sven
 *
 */
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.asksven.betterbatterystats.R;
import com.fima.cardsui.objects.Card;

public class GooglePlayCard extends Card
{

	public GooglePlayCard(String titlePlay, String description, String color, String titleColor, Boolean hasOverflow,
			Boolean isClickable)
	{
		super(titlePlay, description, color, titleColor, hasOverflow, isClickable);
	}

	@Override
	public View getCardContent(Context context)
	{
		View v = LayoutInflater.from(context).inflate(R.layout.card_play, null);

		((TextView) v.findViewById(R.id.title)).setText(titlePlay);
		((TextView) v.findViewById(R.id.title)).setTextColor(Color.parseColor(titleColor));
		((TextView) v.findViewById(R.id.description)).setText(description);
		((ImageView) v.findViewById(R.id.stripe)).setBackgroundColor(Color.parseColor(color));

		if (isClickable == true)
			((LinearLayout) v.findViewById(R.id.contentLayout))
					.setBackgroundResource(R.drawable.selectable_background_cardbank);

		if (hasOverflow == true)
			((ImageView) v.findViewById(R.id.overflow)).setVisibility(View.VISIBLE);
		else
			((ImageView) v.findViewById(R.id.overflow)).setVisibility(View.GONE);

		return v;
	}

}
