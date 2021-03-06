package com.asksven.android.common;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by sven on 01/05/2017.
 */

public class NonRootShellTest
{

    private NonRootShell shell = null;
    static final String TAG = "NonRootShellTest";

    @Before
    public void createInstance() throws Exception
    {
        shell = NonRootShell.getInstance();
        assertNotNull(shell);

    }

    @Test
    public void test_run() throws Exception
    {
        List<String> res = new ArrayList<String>();
        String command = "ls -l /";
        res = shell.run(command);

        assertFalse(res.isEmpty());

        assertTrue(!res.get(0).equals(command));
    }

    @Test
    public void test_dumpsys_alam() throws Exception
    {
        List<String> res = new ArrayList<String>();
        String command = "dumpsys alarm";
        res = shell.run(command);

        assertTrue(res != null);

    }


    @Test
    public void test_dumpsys() throws Exception
    {
        List<String> res = new ArrayList<String>();
        String command = "dumpsys";
        res = shell.run(command);

        assertTrue(res != null);

    }


}
