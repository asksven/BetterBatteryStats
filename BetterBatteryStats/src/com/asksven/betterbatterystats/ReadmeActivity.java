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
package com.asksven.betterbatterystats;

import com.asksven.betterbatterystats.R;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;

public class ReadmeActivity extends Activity
{
	/**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.readmewebview);
		
		WebView browser = (WebView)findViewById(R.id.webview);

	    WebSettings settings = browser.getSettings();
	    settings.setJavaScriptEnabled(true);
	    
	    // retrieve any passed data (filename)
	    String strFilename = getIntent().getStringExtra("filename");
	    String strURL = getIntent().getStringExtra("url");
	    
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

        final Button buttonClose = (Button) findViewById(R.id.buttonClose);
        buttonClose.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
            	finish();
            }
        });

        final Button buttonRate = (Button) findViewById(R.id.buttonMarket);
        buttonRate.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
            	openURL(AboutActivity.MARKET_LINK);
            	finish();
            }
        });
        final Button buttonFollow = (Button) findViewById(R.id.buttonTwitter);
        buttonFollow.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
            	openURL(AboutActivity.TWITTER_LINK);
            	finish();
            }
        });

	}
	
    public void openURL( String inURL )
    {
        Intent browse = new Intent( Intent.ACTION_VIEW , Uri.parse( inURL ) );

        startActivity( browse );
    }

}
