package com.asksven.android.common;

import android.app.AlarmManager;
import android.content.Context;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.util.Log;
import android.util.SparseArray;

import com.asksven.android.common.CommonLogSettings;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by sven on 01/05/2017.
 */

public class RootShellTest
{

    private RootShell shell = null;
    static final String TAG = "rootShellTest";

    @Before
    public void createInstance() throws Exception
    {
        shell = RootShell.getInstance();
        assertNotNull(shell);

    }

    @Test
    public void test_run() throws Exception
    {
        List<String> res = new ArrayList<String>();
        String command = "ls -l /";
        res = shell.run(command);

        assertFalse(res.isEmpty());

        // if rooted the result should be different from the command, if not then it should be equal
        if (shell.phoneRooted())
        {
            assertTrue(!res.get(0).equals(command));
            assertTrue(res.size() > 1);
        }
        else
        {
            assertTrue(res.get(0).equals(command));
        }

    }

    @Test
    public void test_dumpsys() throws Exception
    {
        List<String> res = new ArrayList<String>();
        String command = "dumpsys alarm";
        res = shell.run(command);

        assertTrue(res != null);

    }



}
