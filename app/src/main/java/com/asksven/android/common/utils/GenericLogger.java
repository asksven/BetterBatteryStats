/**
 * 
 */
package com.asksven.android.common.utils;

import android.util.Log;

import com.asksven.android.common.utils.DataStorage;

/**
 * @author sven
 *
 */
public abstract class GenericLogger
{

	
	
	public static void d(String strLogfile, String strTag, String strMessage)
	{
		Log.d(strTag, strMessage);
		DataStorage.LogToFile(strLogfile, strMessage);
	}
	
	public static void e(String strLogfile, String strTag, String strMessage)
	{
		Log.e(strTag, strMessage);
		DataStorage.LogToFile(strLogfile, strMessage);
	}

	public static void i(String strLogFile, String strTag, String strMessage)
	{
		Log.i(strTag, strMessage);
		DataStorage.LogToFile(strLogFile, strMessage);
	}

	public static void e(String strLogFile, String strTag, StackTraceElement[] stack)
	{
		Log.e(strTag, "An Exception occured. Stacktrace:");
		for (int i=0; i < stack.length; i++)
		{
			Log.e(strTag, stack[i].toString());
		}
		DataStorage.LogToFile(strLogFile, stack);
	}
	
	public static void stackTrace(String strTag, StackTraceElement[] stack)
	{
		Log.e(strTag, "An Exception occured. Stacktrace:");
		for (int i=0; i < stack.length; i++)
		{
			Log.e(strTag, ">>> " + stack[i].toString());
		}
	}

	private static void writeLog(String strLogFile, String strTag, String strMessage)
	{
		DataStorage.LogToFile(strLogFile, strMessage);
	}
	
}
