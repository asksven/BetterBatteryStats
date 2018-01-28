/**
 *
 */
package com.asksven.android.common;

import java.util.ArrayList;
import java.util.List;


import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;
import com.stericson.RootShell.execution.Shell;

/**
 * @author sven
 * Sigleton performing su operations
 *
 */
public class RootShell
{
	static RootShell m_instance = null;
	static Shell m_shell = null;

	private static boolean m_lastKnownIsRootAvailableStatus = false;

	private RootShell()
	{
	}

	public static RootShell getInstance()
	{
		if (m_instance == null)
		{
			m_instance = new RootShell();
			try
			{
				m_shell = RootTools.getShell(true);
			}
			catch (Exception e)
			{
				m_shell = null;
			}
		}

        // we need to take into account that the shell may be closed
        if ((m_shell == null) || (m_shell.isClosed))
        {
            try
            {
                m_shell = RootTools.getShell(true);
            }
            catch (Exception e)
            {
                m_shell = null;
            }
        }


        return m_instance;
	}

	public synchronized List<String> run(String command)
	{
		final List<String> res = new ArrayList<String>();

		if (!isRootAvailable())
		{
			return res;
		}

		if (m_shell == null)
		{
			// reopen if for whatever reason the shell got closed
			RootShell.getInstance();
		}

		Command shellCommand = new Command(0, command)
		{
		        @Override
		        public void commandOutput(int id, String line)
		        {
		        	res.add(line);
		        	super.commandOutput(id, line);
		        }

		};
		try
		{
			RootTools.getShell(true).add(shellCommand);

			// we need to make this synchronous
			while (!shellCommand.isFinished())
			{
				Thread.sleep(100);
			}
		}
		catch (Exception e)
		{

		}

		return res;

	}

	public boolean phoneRooted()
	{
		return isRootAvailable();
	}

	public boolean hasRootPermissions()
	{
        return ((m_shell != null) && isRootAvailable());
	}

    private boolean isRootAvailable()
    {
        if(!m_lastKnownIsRootAvailableStatus)
        { // Call to RootTools.isRootAvailable is time expensive, cache value
            m_lastKnownIsRootAvailableStatus = RootTools.isRootAvailable();
        }
        return m_lastKnownIsRootAvailableStatus;
    }
}
