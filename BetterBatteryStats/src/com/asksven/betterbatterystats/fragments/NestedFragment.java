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
package com.asksven.betterbatterystats.fragments;

import java.lang.reflect.Field;

import android.support.v4.app.Fragment;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragment;

/**
 * @author sven
 * Implements some magic (see https://code.google.com/p/android/issues/detail?id=42601)
 * to avoid InvalidStateException: no Activity when using Fragment.replace on fragment containing other fragments
 */
public class NestedFragment extends SherlockFragment
{
	private static final Field sChildFragmentManagerField;
	private static String TAG = "CustomFragment";

	static
	{
		Field f = null;
		try
		{
			f = Fragment.class.getDeclaredField("mChildFragmentManager");
			f.setAccessible(true);
		}
		catch (NoSuchFieldException e)
		{
			Log.e(TAG, "Error getting mChildFragmentManager field", e);
		}
		sChildFragmentManagerField = f;
	}

	@Override
	public void onDetach()
	{
		super.onDetach();

		if (sChildFragmentManagerField != null)
		{
			try
			{
				sChildFragmentManagerField.set(this, null);
			}
			catch (Exception e)
			{
				Log.e(TAG, "Error setting mChildFragmentManager field", e);
			}
		}
	}
}
