package com.asksven.android.common.privateapiproxies;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;

/**
 * Created by sven on 01/05/2017.
 */

public class PrivateApiTest {

    static final String TAG = "PrivateApiTest";


    @Test
    public void getPrivateApiAccessible() throws Exception
    {
        try
        {
            Class.forName("android.app.ActivityThread").getDeclaredField("mResourcesManager");
        }
        catch (Exception e)
        {
            Log.d("An error occured: ", e.getMessage());
            assertTrue(false);
        }
    }
}
