package com.asksven.android.common.privateapiproxies;

import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
        Context ctx = InstrumentationRegistry.getContext();
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


}
