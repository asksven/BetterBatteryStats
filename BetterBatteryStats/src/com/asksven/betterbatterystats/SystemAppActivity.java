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

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import com.asksven.android.common.RootShell;
import com.asksven.android.common.utils.SysUtils;
import com.asksven.android.common.utils.SystemAppInstaller;
import com.asksven.android.common.utils.SystemAppInstaller.Status;
import com.asksven.betterbatterystats.R;

import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class SystemAppActivity extends BaseActivity
{

	final static String TAG = "BetteryInfoTest.MainActivity";
//	final static String APK = "com.asksven.betterbatterystats_debug.apk";
	final static String BBS_SIGNED_APK 	= "com.asksven.betterbatterystats_signed.apk"; 
	final static String BBS_DEBUG_APK 	= "com.asksven.betterbatterystats_debug.apk";
	final static String BBS_XDA_APK		= "com.asksven.betterbatterystats_xdaedition.apk";
	
	final static String RECOVERY_GPLAY = "http://better.asksven.org/bbs-systemapp/";
	final static String RECOVERY_XDA = "http://forum.xda-developers.com/showpost.php?p=15869904&postcount=3";
	
	String systemAPKName = "";

	final static String PACKAGE = "com.asksven.betterbatterystats";

	
	Object m_stats = null;

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

		if ( !RootShell.getInstance().hasRootPermissions() )
		{
			// show message that data is not available
			// prepare the alert box
            AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
 
            // set the message to display
            alertbox.setMessage(getString(R.string.info_root_required));
 
            // add a neutral button to the alert box and assign a click listener
            alertbox.setNeutralButton(getString(R.string.label_button_ok), new DialogInterface.OnClickListener()
            {
 
                // click listener on the alert box
                public void onClick(DialogInterface arg0, int arg1)
                {
                	setResult(RESULT_OK);
                	finish();
                }
            });
 
            // show it
            alertbox.show();

		}

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
		if (hasBatteryStatsPermission(this))
		{
			permBattery.setText("BATTERY_STATS " + getString(R.string.label_granted));
		}
		else
		{
			permBattery.setText("BATTERY_STATS  " + getString(R.string.label_not_granted));
		}

		final TextView permDump = (TextView) findViewById(R.id.textViewPermDUMP);
		if (hasDumpPermission(this))
		{
			permDump.setText("DUMP " + getString(R.string.label_granted));
		}
		else
		{
			permDump.setText("DUMP  " + getString(R.string.label_not_granted));
		}

		final TextView seLinux = (TextView) findViewById(R.id.textViewSELinux);
		seLinux.setText("SELinux: " + SysUtils.getSELinuxPolicy());
		
		final Button buttonRemount = (Button) findViewById(R.id.button2);
		setButtonText(buttonRemount);

		buttonRemount.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				Status status;
				try
				{
					boolean install = !SystemAppInstaller.isSystemApp(systemAPKName); 
					if (install)
					{
						status = SystemAppInstaller.install(SystemAppActivity.this, systemAPKName);
					}
					else
					{
						status = SystemAppInstaller.uninstall(PACKAGE);
					}
						
					setButtonText(buttonRemount);
				
					if (status.getSuccess())
					{
						Toast.makeText(SystemAppActivity.this, getString(R.string.info_succeeded), Toast.LENGTH_LONG).show();
						// prepare the alert box
			            AlertDialog.Builder alertbox = new AlertDialog.Builder(SystemAppActivity.this);
			 
			            // set the message to display
			            if (install)
			            {
			            	alertbox.setMessage(getString(R.string.info_installed_system_app));
			            }
			            else
			            {
			            	alertbox.setMessage(getString(R.string.info_uninstalled_system_app));
			   			 
			            }
			            // add a neutral button to the alert box and assign a click listener
			            alertbox.setNeutralButton("Ok", new DialogInterface.OnClickListener()
			            {
			 
			                // click listener on the alert box
			                public void onClick(DialogInterface arg0, int arg1)
			                {
			                }
			            });
			 
			            // show it
			            alertbox.show();

					}
					else
					{
						Toast.makeText(SystemAppActivity.this, getString(R.string.info_failed), Toast.LENGTH_LONG).show();
						Log.e(TAG,"History: " + status.toString());
					}						
				}
				catch (Exception e)
				{
					Log.e(TAG, "Exception: " + Log.getStackTraceString(e));
					Toast.makeText(SystemAppActivity.this, getString(R.string.info_failed), Toast.LENGTH_LONG).show();
				}
				
				// refresh status of button
				setButtonText(buttonRemount);

			}
		});

		final Button buttonRecovery = (Button) findViewById(R.id.button3);


		buttonRecovery.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				String url = "";
				if (isDebuggable(SystemAppActivity.this))
				{
					// use XDA
					url = RECOVERY_XDA;
				}
				else
				{
					url = RECOVERY_GPLAY;
				}
				
            	Intent i = new Intent(Intent.ACTION_VIEW);
            	i.setData(Uri.parse(url));
            	startActivity(i);
			}
		});

	}

	void setButtonText(Button button)
	{
		if (!RootShell.getInstance().hasRootPermissions()) return;

		if (SystemAppInstaller.isSystemApp(systemAPKName))
		{
			button.setText(getString(R.string.uninstall_system_app));
		}
		else
		{
			button.setText(getString(R.string.install_system_app));
		}
	}
		
	private boolean hasBatteryStatsPermission(Context context)
	{
		return wasPermissionGranted(context, android.Manifest.permission.BATTERY_STATS);
	}

	private boolean hasDumpPermission(Context context)
	{
		return wasPermissionGranted(context, android.Manifest.permission.DUMP);
	}

	private boolean wasPermissionGranted(Context context, String permission)
	{
		PackageManager pm = context.getPackageManager();
		int hasPerm = pm.checkPermission(
		    permission, 
		    context.getPackageName());
		return (hasPerm == PackageManager.PERMISSION_GRANTED);
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
