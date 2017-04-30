/*
 * Copyright (C) 2011 asksven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.asksven.android.common.shellutils;

import java.io.*;

import android.util.Log;

/** A class that eases the pain of running external processes
 *  from applications. Lets you run a program three ways:
 *  <OL>
 *     <LI><B>exec</B>: Execute the command, returning
 *         immediately even if the command is still running.
 *         This would be appropriate for printing a file.
 *     <LI><B>execWait</B>: Execute the command, but dont
 *         return until the command finishes. This would be
 *         appropriate for sequential commands where the first
 *         depends on the second having finished (e.g.,
 *         <CODE>javac</CODE> followed by <CODE>java</CODE>).
 *     <LI><B>execPrint</B>: Execute the command and print the
 *          output. This would be appropriate for the Unix
 *          command <CODE>ls</CODE>.
 *  </OL>
 *  Note that the PATH is not taken into account, so you must
 *  specify the <B>full</B> pathname to the command, and shell
 *  built-in commands will not work. For instance, on Unix the
 *  above three examples might look like:
 *  <OL>
 *    <LI><PRE>Exec.exec("/usr/ucb/lpr Some-File");</PRE>
 *    <LI><PRE>Exec.execWait("/usr/local/bin/javac Foo.java");
 *        Exec.execWait("/usr/local/bin/java Foo");</PRE>
 *    <LI><PRE>Exec.execPrint("/usr/bin/ls -al");</PRE>
 *  </OL>
 *
 *  Adapted from Core Web Programming from 
 *  Prentice Hall and Sun Microsystems Press,
 *  http://www.corewebprogramming.com/.
 *  &copy; 2001 Marty Hall and Larry Brown;
 *  may be freely used or adapted. 
 */

public class Exec
{

  /** Starts a process to execute the command. Returns
    * immediately, even if the new process is still running.
    *
    * @param command The <B>full</B> pathname of the command to
    * be executed. No shell built-ins (e.g., "cd") or shell
    * meta-chars (e.g. ">") are allowed.
    * @return false if a problem is known to occur, but since
    * this returns immediately, problems arent usually found
    * in time. Returns true otherwise.
    */

  public static ExecResult exec(String[] command)
  {
    return(exec(command, false, false));
  }

  /** Starts a process to execute the command. Waits for the
    * process to finish before returning.
    *
    * @param command The <B>full</B> pathname of the command to
    * be executed. No shell built-ins or shell metachars are
    * allowed.
    * @return false if a problem is known to occur, either due
    * to an exception or from the subprocess returning a
    * nonzero value. Returns true otherwise.
    */

  public static ExecResult execWait(String[] command)
  {
    return(exec(command, false, true));
  }

  /** Starts a process to execute the command. Prints any output
    * the command produces.
    *
    * @param command The <B>full</B> pathname of the command to
    * be executed. No shell built-ins or shell meta-chars are
    * allowed.
    * @return false if a problem is known to occur, either due
    * to an exception or from the subprocess returning a
    * nonzero value. Returns true otherwise.
    */

  public static ExecResult execPrint(String[] command)
  {
    return(exec(command, true, false));
  }

  /** This creates a Process object via Runtime.getRuntime.exec()
    * Depending on the flags, it may call waitFor on the process
    * to avoid continuing until the process terminates, and open
    * an input stream from the process to read the results.
    */

  private static ExecResult exec(String[] command,
                              boolean printResults,
                              boolean wait)
  {
	  ExecResult oRet = new ExecResult();
	  try
	  {
	      // Start running command, returning immediately.
		  Log.d("Exec.exec", "Executing command " + command);
	      Process p  = Runtime.getRuntime().exec(command);
	
	      // Print the output. Since we read until there is no more
	      // input, this causes us to wait until the process is
	      // completed.
	      if(printResults)
	      {
	    	  BufferedReader buffer = new BufferedReader(
	    			  new InputStreamReader(p.getInputStream()));
	    	  String s = null;
	    	  try
	    	  {
	    		  while ((s = buffer.readLine()) != null)
	    		  {
	    			  oRet.m_oResult.add(s);
	    		  }
	    		  buffer.close();
	    		  if (p.exitValue() != 0)
	    		  {
	    			  oRet.m_bSuccess=false;
	    			  return(oRet);
	    		  }
	    	  }
	    	  catch (Exception e)
	    	  {
	    		  // Ignore read errors; they mean the process is done.
	    	  }

	      // If not printing the results, then we should call waitFor
	      // to stop until the process is completed.
	      }
	      else if (wait)
	      {
	    	  try
	    	  {
	    		  int returnVal = p.waitFor();
	    		  if (returnVal != 0)
	    		  {
	    			  oRet.m_bSuccess=false;
	    			  return oRet;
	    		  }
	    	  }
	    	  catch (Exception e)
	    	  {
    			  oRet.m_oError.add(e.getMessage());
	    		  oRet.m_bSuccess=false;
	    		  return oRet;
	    	  }
	      }
	  }
	  catch (Exception e)
	  {
		  oRet.m_oError.add(e.getMessage());
		  oRet.m_bSuccess=false;
		  return oRet;
	  }
	  oRet.m_bSuccess=true;
	  return oRet;
  }
  
	public static void suExec(String strCommand)
	{
		try
		{
			// dirty hack: http://code.google.com/p/market-enabler/wiki/ShellCommands
			Process process = Runtime.getRuntime().exec("su");
			DataOutputStream os = new DataOutputStream(process.getOutputStream());
			Log.d("Exec.exec", "Executing command " + strCommand);

			os.writeBytes(strCommand + "\n");
			os.flush();
			os.writeBytes("exit\n");
			os.flush();
			process.waitFor();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void shExec(String strCommand)
	{
		try
		{
			// dirty hack: http://code.google.com/p/market-enabler/wiki/ShellCommands
			Process process = Runtime.getRuntime().exec("sh");
			DataOutputStream os = new DataOutputStream(process.getOutputStream());
			Log.d("Exec.exec", "Executing command " + strCommand);

			os.writeBytes(strCommand + "\n");
			os.flush();
			os.writeBytes("exit\n");
			os.flush();
			process.waitFor();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}	
}
