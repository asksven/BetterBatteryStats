package com.asksven.android.common.privateapiproxies;

import android.content.Context;
import android.os.Build;
import android.os.SystemClock;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static java.lang.Runtime.getRuntime;

/**
 * Created by sven on 01/05/2017.
 */

public class BatteryStatsProxyTest {

    private BatteryStatsProxy mStats = null;
    private BatteryStatsProxy mStats2 = null;
    static final String TAG = "BatteryStatsProxyTest";


    @Before
    public void createInstance() throws Exception
    {
        Context ctx = ApplicationProvider.getApplicationContext();
        assertNotNull(ctx);
        mStats = BatteryStatsProxy.getInstance(ctx);
        assertNotNull(mStats);
        assertFalse(mStats.initFailed());

    }

    @Test
    public void test_computeBatteryRealtime() throws Exception
    {
        int statsType = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            statsType = BatteryStatsTypesLolipop.STATS_CURRENT;
        }
        else
        {
            statsType = BatteryStatsTypes.STATS_CURRENT;
        }

        long whichRealtime = mStats.computeBatteryRealtime(SystemClock.elapsedRealtime() * 1000, statsType) / 1000;

        assertTrue(whichRealtime != 0);

    }

    @Test
    public void test_getBatteryRealtime() throws Exception
    {
        long whichRealtime = mStats.getBatteryRealtime(SystemClock.elapsedRealtime() * 1000) / 1000;

        assertTrue(whichRealtime != 0);

    }

    @Test
    public void getPrivateApiAccessible() throws Exception
    {
        getRuntime().Exec("settings get global hidden_api_policy")
    }

    /*
    @Test
    public void test_alarms() throws Exception
    {
        Context ctx = InstrumentationRegistry.getContext();
        assertNotNull(ctx);

        ClassLoader cl = ctx.getClassLoader();

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Class amClass = Class.forName(AlarmManager.class.getName());

        Field serviceField = amClass.getDeclaredField("mService");
        serviceField.setAccessible(true);

        Object service = serviceField.get(am);

        // get the IBinder to the "batteryinfo" service
        @SuppressWarnings("rawtypes")
        Class serviceManagerClass = cl.loadClass("android.os.ServiceManager");

        // parameter types
        @SuppressWarnings("rawtypes")
        Class[] paramTypesGetService= new Class[1];
        paramTypesGetService[0]= String.class;

        @SuppressWarnings("unchecked")
        Method methodGetService = serviceManagerClass.getMethod("getService", paramTypesGetService);

        // parameters
        Object[] paramsGetService= new Object[1];
        paramsGetService[0] = "alarmmanager";

        IBinder serviceBinder = (IBinder) methodGetService.invoke(serviceManagerClass, paramsGetService);
        Class iAlarmManagerStub = cl.loadClass("android.app.IAlarmManager$Stub");
        Class alarmManagerService = cl.loadClass("com.android.server" +
                ".AlarmManagerService");

        //Parameters Types
        @SuppressWarnings("rawtypes")
        Class[] paramTypesAsInterface= new Class[1];
        paramTypesAsInterface[0]= IBinder.class;

        @SuppressWarnings("unchecked")
        Method methodAsInterface = iAlarmManagerStub.getMethod("asInterface", paramTypesAsInterface);

        // Parameters
        Object[] paramsAsInterface= new Object[1];
        paramsAsInterface[0] = serviceBinder;

        Object iAlarmManagerInstance = methodAsInterface.invoke(iAlarmManagerStub, paramsAsInterface);

        Class iAlarmManager = cl.loadClass("android.app.IAlarmManager");
        Field broadcastStatsField = iAlarmManager.getDeclaredField("mBroadcastStats");
        broadcastStatsField.setAccessible(true);
        SparseArray<? extends Object> broadcastStats = (SparseArray<? extends Object>) broadcastStatsField.get(iAlarmManagerInstance);

        assertNotNull(broadcastStats);

    }
*/

}
