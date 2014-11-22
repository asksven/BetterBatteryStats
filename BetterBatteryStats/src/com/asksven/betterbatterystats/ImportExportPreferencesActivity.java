/*
 * Copyright (C) 2014 asksven
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

import com.asksven.android.common.utils.SharedPreferencesUtils;
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
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ImportExportPreferencesActivity extends BaseActivity
{

	final static String TAG = "ImportExportPreferencesActivity";

	final static String BACKUP_FILE 	= "/sdcard/bbs_preferences.txt"; 
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.import_export_prefs);
		
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		toolbar.setTitle(getString(R.string.label_import_export));
	    setSupportActionBar(toolbar);


		
		final TextView explanation = (TextView) findViewById(R.id.textView1);
		explanation.setText(getString(R.string.label_import_export_prefs, BACKUP_FILE));
		
		final Button buttonExport = (Button) findViewById(R.id.buttonExport);
		final Button buttonImport = (Button) findViewById(R.id.buttonImport);

		buttonExport.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				SharedPreferences sharedPrefs = PreferenceManager
						.getDefaultSharedPreferences(ImportExportPreferencesActivity.this);
				if (SharedPreferencesUtils.saveSharedPreferencesToFile(sharedPrefs, BACKUP_FILE))
				{
					Toast.makeText(ImportExportPreferencesActivity.this, getString(R.string.info_pref_export_success, BACKUP_FILE),
							Toast.LENGTH_SHORT).show();
				}
				else
				{
					Toast.makeText(ImportExportPreferencesActivity.this, getString(R.string.info_pref_import_export_failed),
							Toast.LENGTH_SHORT).show();
				}
			}
		});
		buttonImport.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				SharedPreferences sharedPrefs = PreferenceManager
						.getDefaultSharedPreferences(ImportExportPreferencesActivity.this);
				if (SharedPreferencesUtils.loadSharedPreferencesFromFile(sharedPrefs, BACKUP_FILE))
				{
					Toast.makeText(ImportExportPreferencesActivity.this, getString(R.string.info_pref_import_success),
							Toast.LENGTH_SHORT).show();
					// restart
		        	Intent i = ImportExportPreferencesActivity.this.getBaseContext().getPackageManager()
		                    .getLaunchIntentForPackage( ImportExportPreferencesActivity.this.getBaseContext().getPackageName() );
		        	i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		        	startActivity(i);
				}
				else
				{
					Toast.makeText(ImportExportPreferencesActivity.this, getString(R.string.info_pref_import_export_failed, BACKUP_FILE),
							Toast.LENGTH_SHORT).show();
				}

			}
		});
	}
}
