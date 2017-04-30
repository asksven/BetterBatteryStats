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
 
package com.asksven.android.common.nameutils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;

/**
 * @author sven
 *
 */
public class UidNameResolver 
{
	
	protected String[] m_packages;
	protected String[] m_packageNames;
    private static Context m_context;
    private static UidNameResolver m_instance;
    
	private UidNameResolver(Context ctx)
	{
		m_context = ctx;
	}
	
	public static UidNameResolver getInstance(Context ctx)
	{
		if (m_instance == null)
		{
			m_instance = new UidNameResolver(ctx);
		}
		
		return m_instance;
	}
	
	public Drawable getIcon(String packageName)
	{
		Drawable icon = null;
		// retrieve and store the icon for that package
		String myPackage = packageName;
		if (!myPackage.equals(""))
		{
			PackageManager manager = m_context.getPackageManager();
			try
			{
				icon = manager.getApplicationIcon(myPackage);
			}
			catch (Exception e)
			{
				// nop: no icon found
				icon = null;
			}
		}				
		return icon;
	}

	public String getLabel(String packageName)
	{
		String ret = packageName;
		PackageManager pm = m_context.getPackageManager();
        try
        {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            CharSequence label = ai.loadLabel(pm);
            if (label != null)
            {
                ret = label.toString();
            }
        }
        catch (NameNotFoundException e)
        {
            ret = packageName;
        }
        
        return ret;
    }
	
	// Side effects: sets mName and mUniqueName
	// Sets mNamePackage, mName and mUniqueName
    public UidInfo getNameForUid(int uid)
    {
    	String uidName = "";
    	String uidNamePackage = "";
    	boolean uidUniqueName = false;
    	
    	UidInfo myInfo = new UidInfo();
    	myInfo.setUid(uid);
    	myInfo.setName(uidName);
    	myInfo.setNamePackage(uidNamePackage);
    	myInfo.setUniqueName(uidUniqueName);

        
        PackageManager pm = m_context.getPackageManager();
        m_packages = pm.getPackagesForUid(uid);
        
        if (m_packages == null)
        {
            uidName = Integer.toString(uid);

        	myInfo.setName(uidName);
            return myInfo;
        }
        
        m_packageNames = new String[m_packages.length];
        System.arraycopy(m_packages, 0, m_packageNames, 0, m_packages.length);
        
        // Convert package names to user-facing labels where possible
        for (int i = 0; i < m_packageNames.length; i++)
        {
            m_packageNames[i] = getLabel(m_packageNames[i]);
        }

        if (m_packageNames.length == 1)
        {
            uidNamePackage = m_packages[0];
            uidName = m_packageNames[0];
            uidUniqueName = true;
        }
        else
        {
            uidName = "UID"; // Default name
            // Look for an official name for this UID.
            for (String name : m_packages)
            {
                try
                {
                    PackageInfo pi = pm.getPackageInfo(name, 0);
                    if (pi.sharedUserLabel != 0)
                    {
                        CharSequence nm = pm.getText(name,
                                pi.sharedUserLabel, pi.applicationInfo);
                        if (nm != null)
                        {
                            uidName = nm.toString();
                            break;
                        }
                    }
                }
                catch (PackageManager.NameNotFoundException e)
                {
                }
            }
        }
    	myInfo.setName(uidName);
    	myInfo.setNamePackage(uidNamePackage);
    	myInfo.setUniqueName(uidUniqueName);

    	return myInfo;
    }
    
    /**
     * Returns the name for UIDs < 2000
     * @param uid
     * @return
     */
    String getName(int uid)
    {
    	String ret = "";
    	switch (uid)
    	{
    		case 0:
    			ret = "root";
    			break;
    		case 1000:
    			ret = "system";
    			break;
    		case 1001:
    			ret = "radio";
    			break;
    		case 1002:
    			ret = "bluetooth";
    			break;
    		case 1003:
    			ret = "graphics";
    			break;
    		case 1004:
    			ret = "input";
    			break;
    		case 1005:
    			ret = "audio";
    			break;
    		case 1006:
    			ret = "camera";
    			break;
    		case 1007:
    			ret = "log";
    			break;
    		case 1008:
    			ret = "compass";
    			break;
    		case 1009:
    			ret = "mount";
    			break;
    		case 1010:
    			ret = "wifi";
    			break;
    		case 1011:
    			ret = "adb";
    			break;
    		case 1013:
    			ret = "media";
    			break;
    		case 1014:
    			ret = "dhcp";
    			break;
    			
    	}
    	return ret;
    }

}
