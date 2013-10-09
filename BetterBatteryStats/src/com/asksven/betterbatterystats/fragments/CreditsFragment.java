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

import com.actionbarsherlock.app.SherlockFragment;
import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.data.GooglePlayCard;
import com.fima.cardsui.views.CardUI;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class CreditsFragment extends SherlockFragment
{
	
	private CardUI mCardView;
	
	private final String[] m_libs = new String[]
	{ "ActionBarSherlock", "libsuperuser",
			"Android Common", "Google gson",
			"AchartEngine", "AndroidPlot",
			"DashClock", "RootTools",
			"HoloGraphLibrary", "Acra"};
	
	private final String[] m_authors = new String[]
	{ "Jake Wharton", "Chainfire",
			"asksven", "",
			"AchartEngine",	"AndroidPlot",
			"Roman Nurik", "Stericson",
			"Daniel Nadeau", "acra.ch"};
	
	private final String[] m_licenses = new String[]
	{ "Apache 2.0", "Apache 2.0",
			"Apache 2.0", "Apache 2.0",
			"Apache 2.0", "Apache 2.0",
			"Apache 2.0", "Apache 2.0",
			"Apache 2.0", "Apache 2.0"};
	
	private final String[] m_urls = new String[]
	{ "Apache 2.0", "Apache 2.0",
			"Apache 2.0", "https://code.google.com/p/google-gson/",
			"http://www.achartengine.org/index.html", "http://androidplot.com/",
			"https://code.google.com/p/dashclock/",	"https://code.google.com/p/roottools/",
			"https://bitbucket.org/danielnadeau/holographlibrary", "http://acra.ch"};

	private final String[] m_colors = new String[]
	{ "#33b6ea", "#e00707",
			"#f2a400", "#9d36d0",
			"#4ac925", "#222222",
			"#33b6ea", "#e00707",
			"#33b6ea", "#e00707"};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
//		// create ContextThemeWrapper from the original Activity Context with the custom theme
//		Context context = new ContextThemeWrapper(getActivity(), R.style.Theme_Sherlock_Light);
//		// clone the inflater using the ContextThemeWrapper
//		LayoutInflater localInflater = inflater.cloneInContext(context);
//		// inflate using the cloned inflater, not the passed in default	
//		return localInflater.inflate(R.layout.my_layout, container, false);
		View rootView = inflater.inflate(R.layout.fragment_credits, container, false);
		// init CardView
		mCardView = (CardUI) rootView.findViewById(R.id.cardsview);
		mCardView.setSwipeable(false);

		for (int i = 0; i < m_libs.length; i++)
		{
			mCardView.addCard(new GooglePlayCard(m_libs[i],
			m_authors[i], m_colors[i],
			m_colors[i], false, false));
		}

		// draw cards
		mCardView.refresh();

		return rootView;
	}

}