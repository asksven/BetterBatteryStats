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
public class RootShellTest
{

    static final String TAG = "rootShellTest";
    private RootShell shell = null;

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
