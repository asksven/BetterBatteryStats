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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.asksven.betterbatterystats.R;

public class PackagesFragment extends SherlockFragment
{
	private static String TAG = "PackagesFragment";
	String m_packageName;

	public static PackagesFragment newInstance(Bundle args)
	{
		PackagesFragment fragment = new PackagesFragment();
		fragment.setArguments(args);
		return fragment;
	}


	/**
	 * When creating, retrieve this instance's number from its arguments.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Bundle b = getActivity().getIntent().getExtras();
		m_packageName = b.getString("package");
	}

	/**
	 * The Fragment's UI is just a simple text view showing its instance number.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.fragment_packages, container, false);
		Bundle b = getActivity().getIntent().getExtras();
		m_packageName = b.getString("package");

		TextView pName = (TextView) v.findViewById(R.id.TextViewPName);
		pName.setText(m_packageName);

		TextView pOnly4_3 = (TextView) v.findViewById(R.id.TextViewOnly43);

		final Button buttonSettings = (Button) v.findViewById(R.id.buttonSettings);
		buttonSettings.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				showInstalledAppDetails(getActivity(), PackagesFragment.this.m_packageName);
			}
		});

		final Button buttonAppOps = (Button) v.findViewById(R.id.ButtonAppOps);
		if (Build.VERSION.SDK_INT >= 18)
		{
			pOnly4_3.setVisibility(View.GONE);
			buttonAppOps.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{
					showAppOps(getActivity(), PackagesFragment.this.m_packageName);
				}
			});
		} else
		{
			// disable as app ops is not available
			buttonAppOps.setEnabled(false);
		}

		ImageView iconView = (ImageView) v.findViewById(R.id.icon);
		Drawable icon;
		PackageManager manager = getActivity().getPackageManager();
		try
		{
			icon = manager.getApplicationIcon(m_packageName);
			iconView.setImageDrawable(icon);
		}
		catch (Exception e)
		{
			// nop: no icon found
			icon = null;
		}

		return v;
	}

	private static final String SCHEME = "package";
	private static final String APP_PKG_NAME_21 = "com.android.settings.ApplicationPkgName";
	private static final String APP_PKG_NAME_22 = "pkg";
	private static final String APP_DETAILS_PACKAGE_NAME = "com.android.settings";
	private static final String APP_DETAILS_CLASS_NAME = "com.android.settings.InstalledAppDetails";

	public static void showInstalledAppDetails(Context context, String packageName)
	{
		Intent intent = new Intent();
		final int apiLevel = Build.VERSION.SDK_INT;
		if (apiLevel >= 9)
		{
			// above 2.3
			intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
			Uri uri = Uri.fromParts(SCHEME, packageName, null);
			intent.setData(uri);
		} else
		{
			// below 2.3
			final String appPkgName = (apiLevel == 8 ? APP_PKG_NAME_22 : APP_PKG_NAME_21);
			intent.setAction(Intent.ACTION_VIEW);
			intent.setClassName(APP_DETAILS_PACKAGE_NAME, APP_DETAILS_CLASS_NAME);
			intent.putExtra(appPkgName, packageName);
		}
		context.startActivity(intent);
	}

	public static void showAppOps(Context context, String packageName)
	{

		Intent intent = new Intent("android.settings.APP_OPS_SETTINGS");
		Uri uri = Uri.fromParts(SCHEME, packageName, null);
		// intent.setData(uri);
		context.startActivity(intent);
		// }
		// else
		// {
		// // below 2.3
		// final String appPkgName = (apiLevel == 8 ? APP_PKG_NAME_22 :
		// APP_PKG_NAME_21);
		// intent.setAction(Intent.ACTION_VIEW);
		// intent.setClassName(APP_DETAILS_PACKAGE_NAME,
		// APP_DETAILS_CLASS_NAME);
		// intent.putExtra(appPkgName, packageName);
		// }
		// context.startActivity(intent);
	}

}
