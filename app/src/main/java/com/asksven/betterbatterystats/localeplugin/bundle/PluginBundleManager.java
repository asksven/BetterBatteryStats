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

package com.asksven.betterbatterystats.localeplugin.bundle;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.asksven.betterbatterystats.localeplugin.Constants;

/**
 * Class for managing the com.twofortyfouram.locale.Intent#EXTRA_BUNDLE for this plug-in.
 */
public final class PluginBundleManager
{
    /**
     * Private constructor prevents instantiation
     * 
     * @throws UnsupportedOperationException because this class cannot be instantiated.
     */
    private PluginBundleManager()
    {
        throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
    }

    /**
     * Type: {@code String}
     * <p>
     * String message to display in a Toast message.
     */
    public static final String BUNDLE_EXTRA_BOOL_SAVE_REF 	= "com.asksven.betterbatterystats.localeplugin.extra.SAVE_REF"; //$NON-NLS-1$
    public static final String BUNDLE_EXTRA_BOOL_SAVE_STAT 	= "com.asksven.betterbatterystats.localeplugin.extra.SAVE_STAT"; //$NON-NLS-1$
    public static final String BUNDLE_EXTRA_STRING_REF_NAME	= "com.asksven.betterbatterystats.localeplugin.extra.REF_NAME"; //$NON-NLS-1$

    
    /**
     * Type: {@code int}
     * <p>
     * versionCode of the plug-in that saved the Bundle.
     */
    /*
     * This extra is not strictly required, however it makes backward and forward compatibility significantly easier. For example,
     * suppose a bug is found in how some version of the plug-in stored its Bundle. By having the version, the plug-in can better
     * detect when such bugs occur.
     */
    public static final String BUNDLE_EXTRA_INT_VERSION_CODE = "com.asksven.betterbatterystats.localeplugin.extra.INT_VERSION_CODE"; //$NON-NLS-1$

    /**
     * Method to verify the content of the bundle are correct.
     * <p>
     * This method will not mutate {@code bundle}.
     * 
     * @param bundle bundle to verify. May be null, which will always return false.
     * @return true if the Bundle is valid, false if the bundle is invalid.
     */
    public static boolean isBundleValid(final Bundle bundle)
    {
        if (null == bundle)
        {
            return false;
        }

        /*
         * Make sure the expected extras exist
         */
        if (!bundle.containsKey(BUNDLE_EXTRA_BOOL_SAVE_REF))
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.e(Constants.LOG_TAG, String.format("bundle must contain extra %s", BUNDLE_EXTRA_BOOL_SAVE_REF)); //$NON-NLS-1$
            }
            return false;
        }

        if (!bundle.containsKey(BUNDLE_EXTRA_BOOL_SAVE_STAT))
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.e(Constants.LOG_TAG, String.format("bundle must contain extra %s", BUNDLE_EXTRA_BOOL_SAVE_STAT)); //$NON-NLS-1$
            }
            return false;
        }

        if (!bundle.containsKey(BUNDLE_EXTRA_STRING_REF_NAME))
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.e(Constants.LOG_TAG, String.format("bundle must contain extra %s", BUNDLE_EXTRA_STRING_REF_NAME)); //$NON-NLS-1$
            }
            return false;
        }

        if (!bundle.containsKey(BUNDLE_EXTRA_INT_VERSION_CODE))
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.e(Constants.LOG_TAG, String.format("bundle must contain extra %s", BUNDLE_EXTRA_INT_VERSION_CODE)); //$NON-NLS-1$
            }
            return false;
        }

        /*
         * Make sure the correct number of extras exist. Run this test after checking for specific Bundle extras above so that the
         * error message is more useful. (E.g. the caller will see what extras are missing, rather than just a message that there
         * is the wrong number).
         */
        if (4 != bundle.keySet().size())
        {
            Log.e(Constants.LOG_TAG, String.format("bundle must contain 5 keys, but currently contains %d keys: %s", Integer.valueOf(bundle.keySet().size()), bundle.keySet().toString())); //$NON-NLS-1$

            if (Constants.IS_LOGGABLE)
            {
                Log.e(Constants.LOG_TAG, String.format("bundle must contain 5 keys, but currently contains %d keys: %s", Integer.valueOf(bundle.keySet().size()), bundle.keySet() //$NON-NLS-1$
                                                                                                                                                                       .toString()));
            }
            return false;
        }

        
        /*
         * Make sure the extra is the correct type
         */
        if (bundle.getInt(BUNDLE_EXTRA_INT_VERSION_CODE, 0) != bundle.getInt(BUNDLE_EXTRA_INT_VERSION_CODE, 1))
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.e(Constants.LOG_TAG, String.format("bundle extra %s appears to be the wrong type.  It must be an int", BUNDLE_EXTRA_INT_VERSION_CODE)); //$NON-NLS-1$
            }

            return false;
        }

        return true;
    }
}