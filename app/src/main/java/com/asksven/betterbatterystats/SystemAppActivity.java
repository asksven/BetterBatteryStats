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

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
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

    final static int CONST_READ_EXTERNAL_STORAGE    = 1001;
    final static int CONST_ACCESS_WIFI_STATE        = 1002;
    final static int CONST_ACCESS_NETWORK_STATE     = 1003;
    final static int CONST_INTERNET                 = 1004;
    final static int CONST_RECEIVE_BOOT_COMPLETED   = 1005;
    final static int CONST_READ_PHONE_STATE         = 1006;
    final static int CONST_BLUETOOTH                = 1007;
    final static int CONST_WAKE_LOCK                = 1008;
    final static int CONST_PACKAGE_USAGE_STATS      = 1009;
    final static int CONST_WRITE_EXTERNAL_STORAGE   = 1010;

    private View mLayout;

	String systemAPKName = "";

	@Override
	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_systemapp);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.label_system_app));

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayUseLogoEnabled(false);

        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);

        mLayout = findViewById(R.id.main_layout);
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
            } else
            {
                systemAPKName = BBS_DEBUG_APK;
            }
        } else
        {
            systemAPKName = BBS_XDA_APK;
        }

        final TextView tvADB = (TextView) findViewById(R.id.textViewAdb);

        // set the right adb text
        if (packageName.contains("xdaedition"))
        {
            tvADB.setText(getString(R.string.expl_adb_xda));

        }


        Log.i(TAG, "SystemAPKName = " + systemAPKName);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.i(TAG, "OnResume called");

		final TextView permBATTERY = (TextView) findViewById(R.id.textViewBATTERY_STATS);
        final TextView statusBATTERY = (TextView) findViewById(R.id.textViewBATTERY_STATS_STATUS);
        final TextView permDUMP = (TextView) findViewById(R.id.textViewDUMP);
        final TextView permPACKAGE = (TextView) findViewById(R.id.textViewPACKAGE_USAGE_STATS);
        final TextView permAPPOPS = (TextView) findViewById(R.id.textViewAPPOP_USAGE_STATS);
		final TextView permACCESS_WIFI_STATE = (TextView) findViewById(R.id.textViewACCESS_WIFI_STATE);
		final TextView permACCESS_NETWORK_STATE = (TextView) findViewById(R.id.textViewACCESS_NETWORK_STATE);
		final TextView permINTERNET = (TextView) findViewById(R.id.textViewINTERNET);
		final TextView permRECEIVE_BOOT_COMPLETED = (TextView) findViewById(R.id.textViewRECEIVE_BOOT_COMPLETED);
		final TextView permREAD_PHONE_STATE = (TextView) findViewById(R.id.textViewREAD_PHONE_STATE);
		final TextView permBLUETOOTH = (TextView) findViewById(R.id.textViewBLUETOOTH);
		final TextView permWAKE_LOCK = (TextView) findViewById(R.id.textViewWAKE_LOCK);

		String text = "";
		if (SysUtils.hasBatteryStatsPermission(this))
		{
			permBATTERY.setText("BATTERY_STATS " + getString(R.string.label_granted));
		}
		else
		{
            permBATTERY.setText("BATTERY_STATS  " + getString(R.string.label_not_granted));
            permBATTERY.setBackgroundColor(Color.RED);
        }

        BatteryStatsProxy stats = BatteryStatsProxy.getInstance(this);
		String status = "";
		if (stats.initFailed())
        {
            status = getString(R.string.label_failed);
            if (!stats.getError().equals(""))
            {
                status = status + ": " + stats.getError();
            }

        }
		else
        {
            status = getString(R.string.label_success);
            if (stats.isFallback())
            {
                status = status + " (" + getString(R.string.label_fallback) + ")";
            }
        }
        statusBATTERY.setText("STATUS: " + status);

        if (SysUtils.hasDumpsysPermission(this))
		{
			permDUMP.setText("DUMP " + getString(R.string.label_granted));
		}
		else
		{
            permDUMP.setText("DUMP  " + getString(R.string.label_not_granted));
            permDUMP.setBackgroundColor(Color.RED);
        }

        if (Build.VERSION.SDK_INT >= 21)
        {
            if (SysUtils.hasPackageUsageStatsPermission(this))
            {
                permPACKAGE.setText("PACKAGE_USAGE_STATS " + getString(R.string.label_granted));
                //permPACKAGE.setBackgroundColor(Color.WHITE);
            } else
            {
                permPACKAGE.setText("PACKAGE_USAGE_STATS  " + getString(R.string.label_not_granted));
                permPACKAGE.setBackgroundColor(Color.RED);
            }
        }
        else
        {
            permPACKAGE.setText("PACKAGE_USAGE_STATS " + getString(R.string.label_not_needed));

        }

        if (Build.VERSION.SDK_INT >= 21)
        {
            checkAndRequestPermissionAppOpsUsageStats();
            if (SystemAppActivity.hasPermissionAppOpsUsageStats(this))
            {
                permAPPOPS.setText("APPOPS_USAGE_STATS " + getString(R.string.label_granted));
                //permAPPOPS.setBackgroundColor(Color.WHITE);
            } else
            {
                permAPPOPS.setText("APPOPS_USAGE_STATS  " + getString(R.string.label_not_granted));
                permAPPOPS.setBackgroundColor(Color.RED);
            }
        }
        else
        {
            permAPPOPS.setText("APPOPS_USAGE_STATS " + getString(R.string.label_not_needed));

        }

        checkAndRequestPermission(Manifest.permission.ACCESS_WIFI_STATE, CONST_ACCESS_WIFI_STATE, getString(R.string.perm_rationale_ACCESS_WIFI_STATE));
		if (SystemAppActivity.hasPermission(Manifest.permission.ACCESS_WIFI_STATE, this))
		{
			permACCESS_WIFI_STATE.setText("ACCESS_WIFI_STATE " + getString(R.string.label_granted));
		}
		else
		{
			permACCESS_WIFI_STATE.setText("ACCESS_WIFI_STATE  " + getString(R.string.label_not_granted));
            permACCESS_WIFI_STATE.setBackgroundColor(Color.RED);
        }

        checkAndRequestPermission(Manifest.permission.ACCESS_NETWORK_STATE, CONST_ACCESS_NETWORK_STATE, getString(R.string.perm_rationale_ACCESS_NETWORK_STATE));
        if (SystemAppActivity.hasPermission(Manifest.permission.ACCESS_NETWORK_STATE, this))
        {
            permACCESS_NETWORK_STATE.setText("ACCESS_NETWORK_STATE " + getString(R.string.label_granted));
        }
        else
        {
            permACCESS_NETWORK_STATE.setText("ACCESS_NETWORK_STATE  " + getString(R.string.label_not_granted));
            permACCESS_NETWORK_STATE.setBackgroundColor(Color.RED);

        }

        checkAndRequestPermission(Manifest.permission.INTERNET, CONST_INTERNET, getString(R.string.perm_rationale_INTERNET));
        if (SystemAppActivity.hasPermission(Manifest.permission.INTERNET, this))
        {
            permINTERNET.setText("INTERNET " + getString(R.string.label_granted));
        }
        else
        {
            permINTERNET.setText("INTERNET  " + getString(R.string.label_not_granted));
            permINTERNET.setBackgroundColor(Color.RED);

        }

        checkAndRequestPermission(Manifest.permission.RECEIVE_BOOT_COMPLETED, CONST_RECEIVE_BOOT_COMPLETED, getString(R.string.perm_rationale_RECEIVE_BOOT_COMPLETED));
        if (SystemAppActivity.hasPermission(Manifest.permission.RECEIVE_BOOT_COMPLETED, this))
        {
            permRECEIVE_BOOT_COMPLETED.setText("RECEIVE_BOOT_COMPLETED " + getString(R.string.label_granted));
        }
        else
        {
            permRECEIVE_BOOT_COMPLETED.setText("RECEIVE_BOOT_COMPLETED  " + getString(R.string.label_not_granted));
            permRECEIVE_BOOT_COMPLETED.setBackgroundColor(Color.RED);

        }

        checkAndRequestPermission(Manifest.permission.READ_PHONE_STATE, CONST_READ_PHONE_STATE, getString(R.string.perm_rationale_READ_PHONE_STATE));
        if (SystemAppActivity.hasPermission(Manifest.permission.READ_PHONE_STATE, this))
        {
            permREAD_PHONE_STATE.setText("READ_PHONE_STATE " + getString(R.string.label_granted));
        }
        else
        {
            permREAD_PHONE_STATE.setText("READ_PHONE_STATE  " + getString(R.string.label_not_granted));
            permREAD_PHONE_STATE.setBackgroundColor(Color.RED);

        }

        checkAndRequestPermission(Manifest.permission.BLUETOOTH, CONST_BLUETOOTH, "");
        if (SystemAppActivity.hasPermission(Manifest.permission.BLUETOOTH, this))
        {
            permBLUETOOTH.setText("BLUETOOTH " + getString(R.string.label_granted));
        }
        else
        {
            permBLUETOOTH.setText("BLUETOOTH  " + getString(R.string.label_not_granted));
            permBLUETOOTH.setBackgroundColor(Color.RED);

        }

        checkAndRequestPermission(Manifest.permission.WAKE_LOCK, CONST_WAKE_LOCK, "");
        if (SystemAppActivity.hasPermission(Manifest.permission.WAKE_LOCK, this))
        {
            permWAKE_LOCK.setText("WAKE_LOCK " + getString(R.string.label_granted));
        }
        else
        {
            permWAKE_LOCK.setText("WAKE_LOCK  " + getString(R.string.label_not_granted));
            permWAKE_LOCK.setBackgroundColor(Color.RED);
        }


    }

    protected static boolean hasAllPermissions(Context ctx)
    {
        boolean hasPermissions = true;

        hasPermissions = hasPermissions && SysUtils.hasBatteryStatsPermission(ctx);
        hasPermissions = hasPermissions && SysUtils.hasDumpsysPermission(ctx);
        hasPermissions = hasPermissions && SysUtils.hasPackageUsageStatsPermission(ctx);
        hasPermissions = hasPermissions && SystemAppActivity.hasPermission(Manifest.permission.ACCESS_WIFI_STATE, ctx);
        hasPermissions = hasPermissions && SystemAppActivity.hasPermission(Manifest.permission.ACCESS_NETWORK_STATE, ctx);
        hasPermissions = hasPermissions && SystemAppActivity.hasPermission(Manifest.permission.INTERNET, ctx);
        hasPermissions = hasPermissions && SystemAppActivity.hasPermission(Manifest.permission.RECEIVE_BOOT_COMPLETED, ctx);
        hasPermissions = hasPermissions && SystemAppActivity.hasPermission(Manifest.permission.READ_PHONE_STATE, ctx);
        hasPermissions = hasPermissions && SystemAppActivity.hasPermission(Manifest.permission.BLUETOOTH, ctx);
        hasPermissions = hasPermissions && SystemAppActivity.hasPermission(Manifest.permission.WAKE_LOCK, ctx);
        hasPermissions = hasPermissions && SystemAppActivity.hasPermissionAppOpsUsageStats(ctx);

        return hasPermissions;

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

	private static boolean hasPermission(String perm, Context ctx)
	{
        if (ContextCompat.checkSelfPermission(ctx, perm) != PackageManager.PERMISSION_GRANTED)
        {
			return false;
		}
		else
		{
			return true;
		}
	}

	@TargetApi(21)
    private static boolean hasPermissionAppOpsUsageStats(Context ctx)
    {

        AppOpsManager appOps = (AppOpsManager) ctx.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), ctx.getPackageName());
        boolean granted = mode == AppOpsManager.MODE_ALLOWED;
        return granted;
    }

	private void checkAndRequestPermission(String permission, int perm_const, String perm_rationale)
    {
        if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED)
        {

            // Permission is not granted
            Log.i(TAG, "Permission is not granted: " + permission);
            requestPermission(permission, perm_const, perm_rationale);
        }
        else
        {
            // Permission has already been granted
            Log.i(TAG, "Permission was already granted: " + permission);
        }

    }

    @TargetApi(19)
    private void checkAndRequestPermissionAppOpsUsageStats()
    {
        boolean granted = SystemAppActivity.hasPermissionAppOpsUsageStats(this);

        if (!granted)
        {
            Log.i(TAG, " Displaying permission rationale to provide additional context.");
            Snackbar.make(mLayout, getString(R.string.perm_rationale_APPOPS_USAGE_STATS),
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction("OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View view)
                        {
                            Log.i(TAG, "User clicked OK.");
                            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                        }
                    })
                    .show();

        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            // permission was granted, yay! Do the
            // contacts-related task you need to do.
            Log.i(TAG, "Permissions were granted: " + this.toString(permissions));

        }
        else
        {
            // permission denied, boo! Disable the
            // functionality that depends on this permission.
            Log.i(TAG, "Permissions were denied. Request Code was " + requestCode);

        }

        this.recreate();
        return;
    }

    String toString(String arr[])
    {
        StringBuilder builder = new StringBuilder();
        for(String s : arr) {
            builder.append(s);
        }
        return builder.toString();
    }

    private void requestPermission(final String perm, final int const_perm, String perm_rationale)
    {
        final String SUBTAG="requestPermission";
        Log.i(TAG, SUBTAG + " entering method with params " + perm +  ", " + const_perm);

        if ((true) || (ActivityCompat.shouldShowRequestPermissionRationale(this, perm))) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // For example if the user has previously denied the permission.
            Log.i(TAG, SUBTAG + " Displaying permission rationale to provide additional context.");
            Snackbar.make(mLayout, perm_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction("OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View view)
                        {
                            Log.i(TAG, SUBTAG + " User clicked OK.");
                            ActivityCompat.requestPermissions(SystemAppActivity.this, new String[]{perm}, const_perm);
                        }
                    })
                    .show();
        } else {

            // Request permission directly.
            Log.i(TAG, SUBTAG + " Requesting permission directly: " + perm + ", " + const_perm);

            ActivityCompat.requestPermissions(this, new String[]{perm}, const_perm);
        }
    }

}
