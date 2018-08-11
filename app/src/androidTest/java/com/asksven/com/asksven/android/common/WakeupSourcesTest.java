package com.asksven.com.asksven.android.common;

import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import com.asksven.android.common.kernelutils.WakeupSources;
import com.asksven.android.common.privateapiproxies.BatteryStatsProxy;
import com.asksven.android.common.privateapiproxies.BatteryStatsTypes;
import com.asksven.android.common.privateapiproxies.BatteryStatsTypesLolipop;
import com.asksven.android.common.privateapiproxies.StatElement;

import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WakeupSourcesTest
{

    private Context ctx = null;
    private static String FILE_PATH = "/d/wakeup_sources";


    @Before
    public void createInstance() throws Exception
    {
        Context ctx = InstrumentationRegistry.getContext();
        assertNotNull(ctx);
    }

    @Test
    public void test_accessFile() throws Exception
    {
        boolean exists = false;
        FileReader fr = null;
        try
        {
            fr = new FileReader(FILE_PATH);
            exists = true;
        }
        catch (Exception e)
        {
            exists = false;
        }
        finally
        {
            if (exists)
            {
                try
                {
                    fr.close();
                }
                catch (IOException e)
                {
                    // do nothing
                }
            }
        }

        assertTrue(exists);
    }

    @Test
    public void test_accessFile2() throws Exception
    {
        boolean exists = false;
        byte[] buffer = new byte[32*1024];
        int len;

        try
        {
            FileInputStream is;
            is = new FileInputStream(FILE_PATH);
            len = is.read(buffer);
            is.close();

        }
        catch (Exception e)
        {
            Log.e("Test", "Test:" + e.getMessage());
            assertTrue(false);
        }


    }

    @Test
    public void test_parseWakeupSources() throws Exception
    {
        ArrayList<StatElement> result = WakeupSources.parseWakeupSources(ctx);
        assertNotNull(result);
        assertTrue(!result.isEmpty());
    }

}
