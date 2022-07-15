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
import android.os.IBinder;
import android.os.MemoryFile;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.android.common.utils.SysUtils;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayInputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.x500.X500Principal;

public class DiagnosticsActivity extends BaseActivity
{

	final static String TAG = "DiagnosticsActivity";

    private View mLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagnosticsapp);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.label_diagnostics));

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayUseLogoEnabled(false);

    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.i(TAG, "OnResume called");

        final TextView tvDiags = (TextView) findViewById(R.id.textViewDiagnostics);
        ArrayList<String> list = diagGetService();
        tvDiags.setText("");
        for (int i=0; i < list.size(); i++)
        {
            tvDiags.append(list.get(i));
            tvDiags.append("\n");
        }
    }

    private ArrayList<String> diagGetService()
    {
        ArrayList<String> ret = new ArrayList<String>();
        try
        {

            Context context = getApplicationContext();
            ClassLoader cl = context.getClassLoader();

            Class m_ClassDefinition = cl.loadClass("com.android.internal.os.BatteryStatsImpl");

            // get the IBinder to the "batteryinfo" service
            @SuppressWarnings("rawtypes")
            Class serviceManagerClass = cl.loadClass("android.os.ServiceManager");

            // parameter types
            @SuppressWarnings("rawtypes")
            Class[] paramTypesGetService= new Class[1];
            paramTypesGetService[0]= String.class;

            @SuppressWarnings("unchecked")
            Method methodGetService = serviceManagerClass.getMethod("getService", paramTypesGetService);

            String service = "batterystats";

            // parameters
            Object[] paramsGetService= new Object[1];
            paramsGetService[0] = service;

            Log.i(TAG, "invoking android.os.ServiceManager.getService(\"" + service + "\")");
            ret.add("invoking android.os.ServiceManager.getService(\"" + service + "\")");

            IBinder serviceBinder = (IBinder) methodGetService.invoke(serviceManagerClass, paramsGetService);

            Log.i(TAG, "android.os.ServiceManager.getService(\"" + service + "\") returned a service binder");
            ret.add("android.os.ServiceManager.getService(\"" + service + "\") returned a service binder");

            // now we have a binder. Let's us that on IBatteryStats.Stub.asInterface
            // to get an IBatteryStats
            // Note the $-syntax here as Stub is a nested class
            @SuppressWarnings("rawtypes")
            Class iBatteryStatsStub = cl.loadClass("com.android.internal.app.IBatteryStats$Stub");

            //Parameters Types
            @SuppressWarnings("rawtypes")
            Class[] paramTypesAsInterface= new Class[1];
            paramTypesAsInterface[0]= IBinder.class;

            @SuppressWarnings("unchecked")
            Method methodAsInterface = iBatteryStatsStub.getMethod("asInterface", paramTypesAsInterface);

            // Parameters
            Object[] paramsAsInterface= new Object[1];
            paramsAsInterface[0] = serviceBinder;

            Log.i(TAG, "invoking com.android.internal.app.IBatteryStats$Stub.asInterface");
            ret.add("invoking com.android.internal.app.IBatteryStats$Stub.asInterface");
            Object iBatteryStatsInstance = methodAsInterface.invoke(iBatteryStatsStub, paramsAsInterface);

            // and finally we call getStatistics from that IBatteryStats to obtain a Parcel
            @SuppressWarnings("rawtypes")
            Class iBatteryStats = cl.loadClass("com.android.internal.app.IBatteryStats");

            @SuppressWarnings("unchecked")
            // since there are yet undocumented changes in the signature of getStatisticsStream we need to implement this logic:
            // a) try with getStatisticsStream()
            // b) if a fails, retry with getStatisticsStream(boolean)
            Method methodGetStatisticsStream;
            boolean withBoolParam = false;
            methodGetStatisticsStream = iBatteryStats.getMethod("getStatisticsStream", boolean.class);
            // returns a ParcelFileDescriptor

            Log.i(TAG, "invoking getStatisticsStream");
            ret.add("invoking getStatisticsStream");

            ParcelFileDescriptor pfd;
            pfd = (ParcelFileDescriptor) methodGetStatisticsStream.invoke(iBatteryStatsInstance, true);

            if (pfd != null)
            {
                FileInputStream fis = new ParcelFileDescriptor.AutoCloseInputStream(pfd);

                Log.i(TAG, "retrieving parcel");
                ret.add("retrieving parcel");

                @SuppressWarnings("rawtypes")
                Class[] paramTypes= new Class[1];
                paramTypes[0]= FileDescriptor.class;


                // we want to access MemoryFile.getSize(pfd.getFileDescriptor()) but this methos id hidden
                Method methodGetSize = MemoryFile.class.getMethod("getSize", paramTypes);
                methodGetSize.setAccessible(true);

                int size = (int) methodGetSize.invoke(null, /* null = static method */ pfd.getFileDescriptor());

                byte[] data = readFully(fis, size);

                Parcel parcel = Parcel.obtain();
                parcel.unmarshall(data, 0, data.length);
                parcel.setDataPosition(0);

                @SuppressWarnings("rawtypes")
                Class batteryStatsImpl = cl.loadClass("com.android.internal.os.BatteryStatsImpl");

                Log.i(TAG, "reading CREATOR field");
                Field creatorField = batteryStatsImpl.getField("CREATOR");

                // From here on we don't need reflection anymore
                @SuppressWarnings("rawtypes")
                Parcelable.Creator batteryStatsImpl_CREATOR = (Parcelable.Creator) creatorField.get(batteryStatsImpl);

                Object m_Instance = batteryStatsImpl_CREATOR.createFromParcel(parcel);
                ret.add("SUCCESS");
            }
        }
        catch( Exception e )
        {
            Log.e(TAG, "An exception occured in BatteryStatsProxy(). Message: " + e.getCause());
            ret.add("FAILURE: an exception occured");
            ret.add("");
            ret.add("STACK TRACE");
            ret.add(Log.getStackTraceString(e));
        }

        return(ret);
    }

    protected static byte[] readFully(FileInputStream stream, int avail) throws java.io.IOException
    {
        int pos = 0;
        byte[] data = new byte[avail];
        while (true) {
            int amt = stream.read(data, pos, data.length-pos);
            Log.i(TAG, "Read " + amt + " bytes at " + pos
                    + " of avail " + data.length);
            if (amt <= 0) {
                Log.i(TAG, "**** FINISHED READING: pos=" + pos
                        + " len=" + data.length);
                return data;
            }
            pos += amt;
            avail = stream.available();
            if (avail > data.length-pos) {
                byte[] newData = new byte[pos+avail];
                System.arraycopy(data, 0, newData, 0, pos);
                data = newData;
            }
        }
    }


}
