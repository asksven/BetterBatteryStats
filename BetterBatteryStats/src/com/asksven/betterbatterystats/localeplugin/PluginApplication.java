/*
 * Copyright (C) 2012 asksven
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
 * 
 * This file was contributed by two forty four a.m. LLC <http://www.twofortyfouram.com>
 * unter the terms of the Apache License, Version 2.0
 */

package com.asksven.betterbatterystats.localeplugin;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Implements an application object for the plug-in.
 * <p>
 * This application is non-essential for the plug-in's operation; it simply enables debugging options globally for the app.
 */
public final class PluginApplication extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();

        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0)
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.v(Constants.LOG_TAG, "Application is debuggable.  Enabling additional debug logging"); //$NON-NLS-1$
            }

            /*
             * If using the Fragment compatibility library, enable debug logging here
             */
            // FragmentManager.enableDebugLogging(true);
            // LoaderManager.enableDebugLogging(true);

            if (Build.VERSION.SDK_INT >= 9)
            {
                try
                {
                    final Class<?> strictModeClass = Class.forName("android.os.StrictMode"); //$NON-NLS-1$
                    final Method enableDefaultsMethod = strictModeClass.getMethod("enableDefaults"); //$NON-NLS-1$
                    enableDefaultsMethod.invoke(strictModeClass);
                }
                catch (final ClassNotFoundException e)
                {
                    throw new RuntimeException(e);
                }
                catch (final SecurityException e)
                {
                    throw new RuntimeException(e);
                }
                catch (final NoSuchMethodException e)
                {
                    throw new RuntimeException(e);
                }
                catch (final IllegalArgumentException e)
                {
                    throw new RuntimeException(e);
                }
                catch (final IllegalAccessException e)
                {
                    throw new RuntimeException(e);
                }
                catch (final InvocationTargetException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}