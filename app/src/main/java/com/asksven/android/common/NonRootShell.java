/**
 *
 */
package com.asksven.android.common;

import android.util.Log;

import com.asksven.betterbatterystats.LogSettings;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootShell.execution.Shell;
import com.stericson.RootTools.RootTools;

import java.util.LinkedList;
import java.util.List;

/**
 * @author sven
 * Sigleton performing su operations
 *
 */
public class NonRootShell
{
    static final String TAG = "BBSNonRootShell";
    static final boolean debugMode = LogSettings.DEBUG;

	static NonRootShell m_instance = null;
	static Shell m_shell = null;

    static {
        com.stericson.RootShell.RootShell.handlerEnabled  = false;
//        RootTools.debugMode = true;
//        com.stericson.RootShell.RootShell.debugMode = true;
    }

	private NonRootShell()
	{

	}

	public static NonRootShell getInstance()
	{
		if (m_instance == null)
		{
			m_instance = new NonRootShell();
			try
			{
				m_shell = RootTools.getShell(true);

			}
			catch (Exception e)
			{
				m_shell = null;
                Log.w(TAG,"Error ",e);
            }
		}

        // we need to take into account that the shell may be closed
        if ((m_shell == null) || (m_shell.isClosed))
        {
            try
            {
                m_shell = RootTools.getShell(false);
            }
            catch (Exception e)
            {
                m_shell = null;
                Log.w(TAG,"Error ",e);
            }
        }


        return m_instance;
	}

	public synchronized List<String> run(final String command)
	{
		final List<String> res = new LinkedList<>();

		if (m_shell == null)
		{
			// reopen if for whatever reason the shell got closed
			RootShell.getInstance();
		}

        final Thread currentThread = Thread.currentThread();

		Command shellCommand = new Command(0,1000, command)
		{
            @Override
            public void commandOutput(int id, String line)
            {
                super.commandOutput(id, line);
                if(debugMode) Log.d(TAG, "commandOutput command '"+command+"'" + " " + line);
                res.add(line);
            }

            @Override
            public void commandTerminated(int id, String reason)
            {
                Log.w(TAG, "commandTerminated "+reason + " command '"+command+"'");
                currentThread.interrupt();
            }

            @Override
            public void commandCompleted(int id, int exitcode)
            {
                if(debugMode) Log.d(TAG, "commandCompleted command '"+command+"'" + " exitCode "+exitcode);
                currentThread.interrupt();
            }
        };

		try
		{
			RootTools.getShell(true).add(shellCommand);

			// we need to make this synchronous
			while (!shellCommand.isFinished())
			{
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
		}
		catch (Exception e)
		{
            Log.w(TAG,"Error ",e);
		}

		return res;

	}
}
