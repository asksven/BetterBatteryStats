/*
 * Copyright (C) 2011-2015 asksven
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

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class HelpActivity extends BaseActivity
{
	/**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		String strTitle = getIntent().getStringExtra("title");
		
		setContentView(R.layout.helpwebview);
		
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		toolbar.setTitle(strTitle);

	    setSupportActionBar(toolbar);
	    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	    getSupportActionBar().setDisplayUseLogoEnabled(false);
	    
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
	}
}
