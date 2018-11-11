/*
 * Copyright (C) 2013 asksven
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
package com.asksven.android.common.utils;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import com.asksven.android.common.shellutils.Exec;
import com.asksven.android.common.shellutils.ExecResult;

/**
 * @author sven
 * A collection of system utilities
 *
 */
public class SysUtils
{

	/**
	 * Checks if we have BATTERY_STATS permission
	 * @param context
	 * @return true if the permission was granted
	 */
	public static boolean hasBatteryStatsPermission(Context context)
	{
		return wasPermissionGranted(context, android.Manifest.permission.BATTERY_STATS);
	}

	/**
	 * Checks if we have DUMP permission
	 * @param context
	 * @return true if the permission was granted
	 */

	public static boolean hasDumpsysPermission(Context context)
	{
		return wasPermissionGranted(context, android.Manifest.permission.DUMP);
	}

	/**
	 * Checks if we have PACKAGE_USAGE_STATS permission
	 * @param context
	 * @return true if the permission was granted
	 */

	public static boolean hasPackageUsageStatsPermission(Context context)
	{
		if (Build.VERSION.SDK_INT >= 21)
		{
            return wasPermissionGranted(context, android.Manifest.permission.PACKAGE_USAGE_STATS);
		}
		else
		{
			return true;
		}
	}

	private static boolean wasPermissionGranted(Context context, String permission)
	{
		PackageManager pm = context.getPackageManager();
		int hasPerm = pm.checkPermission(
		    permission, 
		    context.getPackageName());
		return (hasPerm == PackageManager.PERMISSION_GRANTED);
	}
	
	/** 
	 * Returns "n/a, Enforcing or Permissive", depending on the implemented policy
	 * @return
	 */
	public static String getSELinuxPolicy()
	{
		String ret = "";
		ExecResult res = Exec.execPrint(new String[]{"getenforce"});
		if (res.getSuccess())
		{
			ret = res.getResultLine();
		}
		else
		{
			ret = "n/a";
		}
		return ret;
	}

}
