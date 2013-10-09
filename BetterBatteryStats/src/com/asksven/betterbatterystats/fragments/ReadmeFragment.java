/*
 * Copyright (C) 2011 asksven
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
import com.asksven.betterbatterystats.R.id;
import com.asksven.betterbatterystats.R.layout;
import com.asksven.betterbatterystats.R.string;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;

/**
 * 
 * @author sven
 *
 * Add following strings to your app
 *    <string name="dialog_apps">Other Apps</string>
 *    <string name="dialog_dismiss">Dismiss</string>
 *    <string name="dialog_follow_me">Follow me</string>
 *    <string name="twitter_link">https://twitter.com/#!/asksven</string>
 *    <string name="market_link">market://search?q=com.asksven</string>
 *    
 * and the activity to your manifest
 *    <activity android:name="com.asksven.android.common.ReadmeActivity" />
 */
public class ReadmeFragment extends SherlockFragment
{
	/**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.fragment_readme, container, false);
		
		WebView browser = (WebView)rootView.findViewById(R.id.webview);

	    WebSettings settings = browser.getSettings();
	    settings.setJavaScriptEnabled(true);
	    
	    // retrieve any passed data (filename)
	    Bundle b = getArguments();
	    String strFilename = b.getString("filename");
	    String strURL = b.getString("url");
	    
	    // if a URL is passed open it
	    // if not open a local file
	    if ( (strURL == null) || (strURL.equals("")) )
	    {
		    if (strFilename.equals(""))
		    {
		    	browser.loadUrl("file:///android_asset/help.html");
		    }
		    else
		    {
		    	browser.loadUrl("file:///android_asset/" + strFilename);
		    }
	    }
	    else
	    {
	    	browser.loadUrl(strURL);
	    }


        final Button buttonRate = (Button) rootView.findViewById(R.id.buttonMarket);
        buttonRate.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
            	openURL(ReadmeFragment.this.getString(R.string.market_link));
            }
        });
        final Button buttonFollow = (Button) rootView.findViewById(R.id.buttonTwitter);
        buttonFollow.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
            	openURL(ReadmeFragment.this.getString(R.string.twitter_link));
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
