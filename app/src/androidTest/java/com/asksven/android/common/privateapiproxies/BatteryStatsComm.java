package com.asksven.android.common.privateapiproxies;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Build;
import android.os.IBinder;
import android.os.MemoryFile;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;

import com.asksven.android.common.CommonLogSettings;

import org.junit.Before;
import org.junit.Test;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by sven on 01/05/2017.
 */

public class BatteryStatsComm {

    private BatteryStatsProxy mStats = null;
    private BatteryStatsProxy mStats2 = null;
    static final String TAG = "BatteryStatsProxyTest";




    @Test
    public void test_getService() throws Exception
    {
        try
        {
            Context context = ApplicationProvider.getApplicationContext();
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

            IBinder serviceBinder = (IBinder) methodGetService.invoke(serviceManagerClass, paramsGetService);

            Log.i(TAG, "android.os.ServiceManager.getService(\"" + service + "\") returned a service binder");

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

            Object iBatteryStatsInstance = methodAsInterface.invoke(iBatteryStatsStub, paramsAsInterface);

            // and finally we call getStatistics from that IBatteryStats to obtain a Parcel
            @SuppressWarnings("rawtypes")
            Class iBatteryStats = cl.loadClass("com.android.internal.app.IBatteryStats");

            // Enumerate methods
            Method[] methods = iBatteryStats.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
                System.out.println("The method is: " + methods[i].toString());
            }


            @SuppressWarnings("rawtypes")
            Class batteryStatsImpl = cl.loadClass("com.android.internal.os.BatteryStatsImpl");

            @SuppressWarnings("unchecked")
            // since there are yet undocumented changes in the signature of getStatisticsStream we need to implement this logic:
            // a) try with getStatisticsStream()
            // b) if a fails, retry with getStatisticsStream(boolean)
            Method methodGetStatisticsStream;
            boolean withBoolParam = false;
            methodGetStatisticsStream = iBatteryStats.getMethod("getStatisticsStream", boolean.class);
            // returns a ParcelFileDescriptor

            Log.i(TAG, "invoking getStatisticsStream");

            ParcelFileDescriptor pfd;
            pfd = (ParcelFileDescriptor) methodGetStatisticsStream.invoke(iBatteryStatsInstance, true);

            if (pfd != null)
            {
                FileInputStream fis = new ParcelFileDescriptor.AutoCloseInputStream(pfd);

                Log.i(TAG, "retrieving parcel");

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

                //---
                Constructor bsiEmptyConstructor = Class.forName("com.android.internal.os.BatteryStatsImpl").getConstructor();
                Object bsiInstance = bsiEmptyConstructor.newInstance();

                // Initialize the PowerProfile
                Constructor ppConstructor = Class.forName("com.android.internal.os.PowerProfile").getConstructor(Context.class);
                Object ppInstance = ppConstructor.newInstance(context);

                Method methodSetPowerProfileLocked = batteryStatsImpl.getMethod("setPowerProfileLocked", Class.forName("com.android.internal.os.PowerProfile"));

                // Parameter types
                Class[] paramTypesSetPowerProfileLocked= new Class[1];
                paramTypesSetPowerProfileLocked[0]= Class.forName("com.android.internal.os.PowerProfile");

                // Parameters
                Object[] paramSetPowerProfileLocked= new Object[1];
                paramSetPowerProfileLocked[0] = ppInstance;

                methodSetPowerProfileLocked.invoke(bsiInstance, paramSetPowerProfileLocked);

                Method methodReadFromParcel = batteryStatsImpl.getMethod("readFromParcel", Parcel.class);

                // Parameters
                Object[] paramReadFromParcel= new Object[1];
                paramReadFromParcel[0] = parcel;

                methodReadFromParcel.invoke(bsiInstance, paramReadFromParcel);

                Object m_Instance = bsiInstance;
                Log.i(TAG, "Service: " + m_Instance.toString());
                //---
/*
                Log.i(TAG, "reading CREATOR field");
                Field creatorField = batteryStatsImpl.getField("CREATOR");

                // From here on we don't need reflection anymore
                @SuppressWarnings("rawtypes")
                //Parcelable.Creator batteryStatsImpl_CREATOR = (Parcelable.Creator) creatorField.get(batteryStatsImpl);
                Parcelable.Creator batteryStatsImpl_CREATOR = (Parcelable.Creator) creatorField.get(bsiInstance);

                Object m_Instance = batteryStatsImpl_CREATOR.createFromParcel(parcel);
                Log.i(TAG, "Service: " + m_Instance.toString());
*/
            }

        }
        catch( Exception e )
        {
            Log.e(TAG, "An exception occured in BatteryStatsProxy(). Message: " + e.getCause());
            assertTrue(false);
        }
    }

    protected static byte[] readFully(FileInputStream stream, int avail) throws java.io.IOException {
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
