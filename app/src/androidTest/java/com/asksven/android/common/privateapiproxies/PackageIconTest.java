package com.asksven.android.common.privateapiproxies;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.IBinder;
import android.os.MemoryFile;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;

import com.asksven.android.common.nameutils.UidNameResolver;
import com.asksven.betterbatterystats.BbsApplication;

import org.junit.Test;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Created by sven on 01/05/2017.
 */

public class PackageIconTest {

    private BatteryStatsProxy mStats = null;

    static final String TAG = "PackageIconTest";




    @Test
    public void test_getPackages() throws Exception
    {
        try
        {
            Context ctx = ApplicationProvider.getApplicationContext();
            assertNotNull(ctx);
            mStats = BatteryStatsProxy.getInstance(ctx);

            ArrayList<StatElement> elements = mStats.getWakeupStatsPost6(ctx, 2);
            for (int i=0; i < elements.size(); i++)
            {
                StatElement elem = elements.get(i);
                if (1==1 | elem.getPackageName().equals("me.bluemail.mail"))
                {
                    Log.i(TAG, "Found package " + elem.getPackageName());
                    //Drawable ic = elem.getIcon(UidNameResolver.getInstance());
                    //ApplicationInfo app = ctx.getPackageManager().getApplicationInfo(elem.getPackageName(), 0);
                    //Drawable ic = ctx.getPackageManager().getApplicationIcon(elem.getPackageName());
                    Drawable ic =ctx.getPackageManager().getDefaultActivityIcon();
                    if (ic != null)
                    {
                        Log.i(TAG, "Found");
                    }

                }

            }

        }
        catch( Exception e )
        {
            Log.e(TAG, "An exception occured in PackageIconTest(). Message: " + e.getCause());
            assertTrue(false);
        }
    }


}
