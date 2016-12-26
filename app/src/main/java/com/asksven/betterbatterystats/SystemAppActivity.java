/*
 * Copyright (C) 2014-2015 asksven
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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;

import com.asksven.android.common.utils.SysUtils;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

public class SystemAppActivity extends BaseActivity
{

	final static String TAG = "SystemAppActivity";
	final static String BBS_SIGNED_APK = "com.asksven.betterbatterystats_signed.apk";
	final static String BBS_DEBUG_APK 	= "com.asksven.betterbatterystats_debug.apk";
	final static String BBS_XDA_APK		= "com.asksven.betterbatterystats_xdaedition.apk";

	String systemAPKName = "";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_systemapp);
		
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		toolbar.setTitle(getString(R.string.label_system_app));
		
	    setSupportActionBar(toolbar);
	    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	    getSupportActionBar().setDisplayUseLogoEnabled(false);

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		// package name is either com.asksven.betterbatterystats or com.asksven.betterbatterystats_xdaedition 
		String packageName = getPackageName();
		
		// now we also need to find out if this build was signed with the debug key
		boolean debug = isDebuggable(this);
		
		// determine the name of the APK to install from assets
		// if package name does not contain xdaedition
		//   if package is signed with a debug key
		//     use BBS_SIGNED_APK
		//   else
		//     use BBS_DEBUG_APK
		// else
		//    use BBS_XDA_APK
		if (!packageName.contains("xdaedition"))
		{
			if (!debug)
			{
				systemAPKName = BBS_SIGNED_APK;
			}
			else
			{
				systemAPKName = BBS_DEBUG_APK;
			}
		}
		else
		{
			systemAPKName = BBS_XDA_APK;
		}
		
		Log.i(TAG, "SystemAPKName = " + systemAPKName);
		
		final TextView permBattery = (TextView) findViewById(R.id.textViewPermBATTERY_STATS);
		if (SysUtils.hasBatteryStatsPermission(this))
		{
			permBattery.setText("BATTERY_STATS " + getString(R.string.label_granted));
		}
		else
		{
			permBattery.setText("BATTERY_STATS  " + getString(R.string.label_not_granted));
		}

		final TextView seLinux = (TextView) findViewById(R.id.textViewSELinux);
		seLinux.setText("SELinux: " + SysUtils.getSELinuxPolicy());
		

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
