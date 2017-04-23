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

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import com.asksven.betterbatterystats.R;
import com.asksven.betterbatterystats.data.StatsProvider;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import de.cketti.library.changelog.ChangeLog;

public class AboutActivity extends BaseActivity
{

    private static final String TAG = "AboutStatsActivity";
    public static final String MARKET_LINK ="market://details?id=com.asksven.betterbatterystats";
    public static final String TWITTER_LINK ="https://twitter.com/#!/asksven";
    

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		toolbar.setTitle(getString(R.string.label_about));

	    setSupportActionBar(toolbar);
	    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	    getSupportActionBar().setDisplayUseLogoEnabled(false);

        
        // retrieve the version name and display it
        try
        {
        	PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        	TextView versionTextView = (TextView) findViewById(R.id.textViewVersion);
        	versionTextView.setText(pinfo.versionName);
        	
        	TextView editionTextView = (TextView) findViewById(R.id.textViewEdition);
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
        
        final Button buttonRate = (Button) findViewById(R.id.buttonRate);
        buttonRate.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                openURL(MARKET_LINK);
            }
        });
        final Button buttonFollow = (Button) findViewById(R.id.buttonTwitter);
        buttonFollow.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                openURL(TWITTER_LINK);
            }
        });
        
    }
    
    public void openURL( String inURL )
    {
        Intent browse = new Intent( Intent.ACTION_VIEW , Uri.parse( inURL ) );

        startActivity( browse );
    }

    public void showChangeLog(View view) {
        ChangeLog cl = new ChangeLog(this);
        cl.getFullLogDialog().show();
    }

    public void showCredits(View view) {
        Intent intentCredits = new Intent(this, CreditsActivity.class);
        this.startActivity(intentCredits);
    }
    
	private static final X500Principal DEBUG_DN = new X500Principal("CN=Android Debug,O=Android,C=US");
	private boolean isDebuggable(Context ctx)
	{
	    boolean debuggable = false;

	    try
	    {
	        PackageInfo pinfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(),PackageManager.GET_SIGNATURES);
	        Signature signatures[] = pinfo.signatures;

	        CertificateFactory cf = CertificateFactory.getInstance("X.509");

	        for ( int i = 0; i < signatures.length;i++)
	        {   
	            ByteArrayInputStream stream = new ByteArrayInputStream(signatures[i].toByteArray());
	            X509Certificate cert = (X509Certificate) cf.generateCertificate(stream);       
	            debuggable = cert.getSubjectX500Principal().equals(DEBUG_DN);
	            if (debuggable)
	                break;
	        }
	    }
	    catch (NameNotFoundException e)
	    {
	        //debuggable variable will remain false
	    }
	    catch (CertificateException e)
	    {
	        //debuggable variable will remain false
	    }
	    return debuggable;
	}

}
