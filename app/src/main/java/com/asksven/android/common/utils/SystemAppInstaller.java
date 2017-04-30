/**
 * 
 */
package com.asksven.android.common.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.asksven.android.common.RootShell;
import com.stericson.RootTools.RootTools;

/**
 * @author sven
 *
 */
public class SystemAppInstaller
{
	static final String TAG = "SystemAppInstaller";
	
	static final String SYSTEM_DIR_4_4		= "/system/priv-app";
	static final String SYSTEM_DIR			= "/system/app";
	
	static final String REMOUNT_SYSTEM_RW 	= "mount -o rw,remount /system";
	static final String REMOUNT_SYSTEM_RO 	= "mount -o ro,remount /system";
	// returns ro or rw
//	static final String CHECK_MOUNT_STATE 	= "mount | grep /system | awk '{print $4}' | awk -F\",\" '{print $1}'";
	static final String CHECK_MOUNT_STATE 	= "mount | grep /system";
	
	
	public static boolean mountSystemRw()
	{
		if (isSystemRw()) return true;
		
		Log.i(TAG, "Remount system rw");
		RootShell.getInstance().run(REMOUNT_SYSTEM_RW);
		
		return isSystemRw();
		
	}
	
	public static boolean mountSystemRo()
	{
		if (!isSystemRw()) return true;
		
		Log.i(TAG, "Remount system ro");
		RootShell.getInstance().run(REMOUNT_SYSTEM_RO);
		
		return !isSystemRw();
		
	}

	public static boolean isSystemRw()
	{
		boolean ret = false;
		Log.i(TAG, "Checking if system is mounted rw");
		try
		{
			ret = RootTools.getMountedAs("/system").equals("rw");
		}
		catch (Exception e)
		{
			Log.e(TAG, "isSystemRw failed: " + e.getMessage());
			ret = false;
		}
				
//		List<String> res = RootShell.getInstance().run(CHECK_MOUNT_STATE);
//		if (res.size() > 0)
//		{
//			String[] tokens = res.get(0).split(" |,");
//			String mountState = tokens[3];
//			Log.i(TAG, "Mount status: " + mountState);
//			ret = (mountState.equals("rw"));
//		}
		
		return ret;
	}
	
	public static boolean isSystemApp(String apk)
	{
		boolean ret = false;
		List<String> res;
		
		String command = "";
		if (Build.VERSION.SDK_INT >= 19)
		{
			command = "ls " + SYSTEM_DIR_4_4 + "/" + apk;
		}
		else
		{
			command = "ls " + SYSTEM_DIR + "/" + apk;
		}

		Log.i(TAG, "Checking if " + apk + " is a system app");
		res = RootShell.getInstance().run(command);
		
		if (res.size() > 0)
		{
			Log.i(TAG, "Command returned "+ res.get(0));
			ret = !res.get(0).contains("No such file or directory");
		}
		
		return ret;
	}
	
//	static boolean installAsSystemApp(Context ctx, String apk)
//	{
//		String command = "";
//		String commandCopyBack = "";
//		String commandTouch = "";
//		
//		// get the original filename
//		command = "ls /data/app/" + apk + "*";
//		List<String> res = RootShell.getInstance().run(command);
//
//		// we copy all the instances of the file to /system (preserving timestamp)
//		// at that point the APK will get deleted from /data/app (by the system) 
//		// so we need to copy the APK back to /data/app (with a new timestamp)
//		for (int i=0; i < res.size(); i++)
//		{
//			String fileName = res.get(0).split("/")[3];
//			// remove the -1 from filename to make sure the target filename is different
//			
//			if (Build.VERSION.SDK_INT >= 19)
//			{
//				commandTouch	= "touch -t 20080801 " + SYSTEM_DIR_4_4 + "/" + fileName; 
//				command 		= "cp -p /data/app/" + fileName + " " +SYSTEM_DIR_4_4  
//						+ " && chmod 644 " + SYSTEM_DIR_4_4 + "/" + fileName 
//						+ " && chown root:root " + SYSTEM_DIR_4_4 + "/" + fileName;
//				commandCopyBack = "cp  " + SYSTEM_DIR_4_4 + "/" + fileName + " /data/app/"
//						+ " && chmod 644 /data/app/" + fileName 
//						+ " && chown system:system /data/app/" + fileName;
//			}
//			else
//			{
//				commandTouch	= "touch -t 20080801 " + SYSTEM_DIR + "/" + fileName;
//				command 		= "cp -p /data/app/" + fileName + " " + SYSTEM_DIR 
//						+ " && chmod 644 " + SYSTEM_DIR + "/" + fileName 
//						+ " && chown root:root " + SYSTEM_DIR + "/" + fileName;
//				commandCopyBack = "cp  " + SYSTEM_DIR + "/" + fileName + " /data/app/"
//						+ " && chmod 644 /data/app/" + fileName 
//						+ " && chown system:systems /data/app/" + fileName;
//			}
//
//
//			Log.i(TAG, "Installing app as system app: " + command);
//			RootShell.getInstance().run(command);
//
//			Log.i(TAG, "Copy APK back to /data/app: " + commandCopyBack);
//			RootShell.getInstance().run(commandCopyBack);
//			
//			Log.i(TAG, "Changing timestamp: " + commandTouch);
//			RootShell.getInstance().run(commandTouch);
//
//		}		
//		return isSystemApp(apk);
//	}

	static boolean installAsSystemApp(Context ctx, String apk)
	{
		String command 		= "";
		String tempPath 	= "/sdcard/";
		
		// actions:
		// copy apk from /sdcard to /system in order to be able to set ownership and perms
		// then copy the file to the target. The sequence is important as copying first and setting perms and ownership
		// afterward will cause PackageParser to fail parsing the package
		if (Build.VERSION.SDK_INT >= 19)
		{
			command = "cp " + tempPath + apk + " /system" + " && chmod 644 " + "/system/" + apk 
					+ " && chown root:root /system/" + apk + " && cp -p /system/" + apk + " " + SYSTEM_DIR_4_4 + " && rm " + tempPath + apk + " && rm /system/" + apk;
		}
		else
		{
			command = "cp " + tempPath + apk + " /system" + " && chmod 644 " + "/system/" + apk 
					+ " && chown root:root /system/" + apk + " && cp -p /system/" + apk + " " + SYSTEM_DIR + " && rm " + tempPath + apk + " && rm /system/" + apk;
		}


		copyAsset(ctx, apk, tempPath);
		Log.i(TAG, "Copying, setting permissions and owner and cleaning up: " + command);
		RootShell.getInstance().run(command);

		return isSystemApp(apk);
	}

	static boolean uninstallAsSystemApp(String apk)
	{
		String command = "";
	
		if (Build.VERSION.SDK_INT >= 19)
		{	
			command = "rm " + SYSTEM_DIR_4_4 + "/" + apk + "*";
		}
		else
		{
			command = "rm " + SYSTEM_DIR + "/" + apk + "*";
		}
		
		Log.i(TAG, "Uninstalling system app: " + command);
		RootShell.getInstance().run(command);
		
		return !isSystemApp(apk);
	}

	public static Status install(Context ctx, String apk)
	{
		Status status = new Status();
		
		SystemAppInstaller.mountSystemRw();
		if (SystemAppInstaller.isSystemRw())
		{
			status.add("Mounted system rw");
			SystemAppInstaller.installAsSystemApp(ctx, apk);
			status.add("Install as system app");
			if (SystemAppInstaller.isSystemApp(apk))
			{
				SystemAppInstaller.mountSystemRo();
				if (!SystemAppInstaller.isSystemRw())
				{
					status.add("Mounted system ro. Finished");
				}
				else
				{
					status.add("An error while remounting system to ro. Warning!");
					status.m_success = true;
				}
			}
			else
			{
				status.add("An error while installing app. Aborted");
				status.m_success = false;
			}
			
		}
		else
		{
			status.add("An error occured mounting system rw. Aborted");
			status.m_success = false;
		}
		
		return status;
	}

    private static void copyAsset(Context ctx, String assetName, String targetPath)
    {
        AssetManager assetManager = ctx.getAssets();
        String[] files = null;
        try
        {
            files = assetManager.list("");
        }
        catch (IOException e)
        {
            Log.e("tag", e.getMessage());
        }
        for(String filename : files)
        {
        	if (filename.equals(assetName))
        	{
	            InputStream in = null;
	            OutputStream out = null;
	            try
	            {
	              in = assetManager.open(filename);
	              String strOutFile = targetPath + "/" + filename;
	              out = new FileOutputStream(strOutFile);
	              copyFile(in, out);
	              in.close();
	              in = null;
	              out.flush();
	              out.close();
	              out = null;
	            }
	            catch(Exception e)
	            {
	                Log.e(TAG, "An error occured while reading " + filename);
	            }
        	}
        }
    }
    
    /**
     * Write a single file
     * @param in the source (in assets)
     * @param out the target
     * @throws IOException
     */
    private static void copyFile(InputStream in, OutputStream out) throws IOException
    {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1)
        {
          out.write(buffer, 0, read);
        }
    }

    /**
     * Value holder for status
     * @param apk
     * @return
     */
	public static Status uninstall(String apk)
	{
		Status status = new Status();
		SystemAppInstaller.mountSystemRw();
		if (SystemAppInstaller.isSystemRw())
		{
			status.add("Mounted system rw");
			SystemAppInstaller.uninstallAsSystemApp(apk);
			status.add("Uninstall as system app");
			if (!SystemAppInstaller.isSystemApp(apk))
			{
				SystemAppInstaller.mountSystemRo();
				if (!SystemAppInstaller.isSystemRw())
				{
					status.add("Mounted system ro. Finished");
				}
				else
				{
					status.add("An error while remounting system to ro. Aborted");
					status.m_success = false;
				}
			}
			else
			{
				status.add("An error while uninstalling app. Aborted");
				status.m_success = false;
			}	
		}
		else
		{
			status.add("An error occured mounting system rw. Aborted");
			status.m_success = false;
		}
		
		return status;
	}
		
	public static class Status
	{
		List<String> m_status = new ArrayList<String>();
		boolean m_success = true;
		
		void add(String text)
		{
			Log.i(TAG, "Status: " + text);
			m_status.add(text);
		}
		
		public boolean success()
		{
			return m_success;
		}
		
		public boolean getSuccess()
		{
			return m_success;
		}
		
		public String toString()
		{
			String ret = "";
			for (int i=0; i < m_status.size(); i++)
			{
				ret += m_status.get(i) + "\n";
			}
			
			return ret;
		}
	}	
	


}
