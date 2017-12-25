/**
 * 
 */
package com.asksven.android.common;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;



//import com.asksven.android.contrib.Shell;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;
import com.stericson.RootShell.execution.Shell;

/**
 * @author sven
 * Sigleton performing su operations
 *
 */
public class NonRootShell
{
    static final String TAG =" NonRootShell";
	static NonRootShell m_instance = null;
	static Shell m_shell = null;
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
				m_shell = RootTools.getShell(false);
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
                m_shell = RootTools.getShell(false);
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
		
		if (m_shell == null)
		{
			// reopen if for whatever reason the shell got closed
			NonRootShell.getInstance();
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
			m_shell.add(shellCommand);
			
			// we need to make this synchronous
			while (!shellCommand.isFinished())
			{
				Thread.sleep(100);
			}
		}
		catch (Exception e)
		{
		    Log.e(TAG, "An error occured while executiing command " + command + ". " + e.getMessage());
		}
		
		return res;
		
	}
	
}
