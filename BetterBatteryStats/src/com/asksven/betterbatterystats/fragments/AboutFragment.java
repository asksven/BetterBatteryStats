/*
 * Copyright (C) 2011-2012 asksven
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

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.R.id;
import com.asksven.betterbatterystats.R.layout;

public class AboutFragment extends SherlockFragment
{

    private static final String TAG = "AboutFragment";
    public static final String MARKET_LINK ="market://details?id=com.asksven.betterbatterystats";
    public static final String TWITTER_LINK ="https://twitter.com/#!/asksven";
    

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.fragment_about, container, false);
        
        // retrieve the version name and display it
        try
        {
        	PackageInfo pinfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
        	TextView versionTextView = (TextView) rootView.findViewById(R.id.textViewVersion);
        	versionTextView.setText(pinfo.versionName);
        	
        	TextView editionTextView = (TextView) rootView.findViewById(R.id.textViewEdition);
        	if (pinfo.packageName.endsWith("_xdaedition"))
        	{
        		editionTextView.setText("xda edition");
        	}
        	else
        	{
        		editionTextView.setText("");
        	}
        }
        catch (Exception e)
        {
        	Log.e(TAG, "An error occured retrieveing the version info: " + e.getMessage());
        }
        
        final Button buttonRate = (Button) rootView.findViewById(R.id.buttonRate);
        buttonRate.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                openURL(MARKET_LINK);
            }
        });
        final Button buttonFollow = (Button) rootView.findViewById(R.id.buttonTwitter);
        buttonFollow.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                openURL(TWITTER_LINK);
            }
        });

        return rootView;
        
    }   
    
    public void openURL( String inURL )
    {
        Intent browse = new Intent( Intent.ACTION_VIEW , Uri.parse( inURL ) );

        startActivity( browse );
    }
}