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
package com.asksven.betterbatterystats.data;

import java.util.HashMap;
import java.util.Map;

import com.asksven.android.common.privateapiproxies.BatteryStatsTypes;
import com.asksven.android.common.utils.DataStorage;

import android.content.Context;
import android.util.Log;

/**
 * @author sven
 * Static class as fassade to the cache of references
 */
public class ReferenceStore
{
	/** the storage for references */
	private static Map<String, Reference> m_refStore = new HashMap<String, Reference>();
	
	/** the logging tag */
	private static final String TAG = "ReferenceStore";

	/**
	 * Returns a reference given a name
	 * @param refName the name of the reference
	 * @param ctx
	 * @return
	 */
	public static Reference getReferenceByName(String refName, Context ctx)
	{
		// the reference names are lazily loaded too
		if (m_refStore.keySet().isEmpty())
		{
			populateReferenceNames(ctx);
		}
		
		if (m_refStore.containsKey(refName))
		{
			// we use lazy loading so we must check if there is a reference there
			if (m_refStore.get(refName) == null)
			{
				Reference thisRef = (Reference) DataStorage.fileToObject(ctx, refName);
				m_refStore.put(refName, thisRef);
			
				if (thisRef != null)
				{
					Log.i(TAG, "Retrieved reference from storage: " + thisRef.whoAmI());
				}
				else
				{
					Log.i(TAG, "Reference " + Reference.CUSTOM_REF_FILENAME
							+ " was not found");
				}
				
			}
			return m_refStore.get(refName);
		}
		else
		{
			Log.e(TAG, "getReference was called with an unknown name "
					+ refName + ". No reference found");
			return null;
		}
	}
	
	/**
	 * Adds a reference to the cache and persists it
	 * @param refName the name
	 * @param ref
	 * @param ctx
	 */
	public static void put(String refName, Reference ref, Context ctx)
	{
		m_refStore.put(refName, ref);
		serializeRefToFile(ref, ctx);
	}
	
	/**
	 * Returns a reference given a stat type
	 * @deprecated
	 * @param iStatType
	 * @param ctx
	 * @return
	 */
	public static Reference getReference(int iStatType, Context ctx)
	{
		switch (iStatType)
		{
		case StatsProvider.STATS_UNPLUGGED:
			return getReferenceByName(Reference.UNPLUGGED_REF_FILENAME, ctx);
		case StatsProvider.STATS_CHARGED:
			return getReferenceByName(Reference.CHARGED_REF_FILENAME, ctx);
		case StatsProvider.STATS_CUSTOM:
			return getReferenceByName(Reference.CUSTOM_REF_FILENAME, ctx);
		case StatsProvider.STATS_SCREEN_OFF:
			return getReferenceByName(Reference.SCREEN_OFF_REF_FILENAME, ctx);
		case StatsProvider.STATS_BOOT:
			return getReferenceByName(Reference.BOOT_REF_FILENAME, ctx);
		case BatteryStatsTypes.STATS_CURRENT:
			return null;
		default:
			Log.e(TAG, "getReference was called with an unknown StatType "
					+ iStatType + ". No reference found");
			break;
		}
		return null;
	
	}

	/**
	 * Returns whether a reference exists
	 * @deprecated
	 * @param iStatType
	 * @param ctx
	 * @return
	 */
	public static boolean hasReference(int iStatType, Context ctx)
	{
		boolean ret = false;
		Reference myCheckRef = getReference(iStatType, ctx);

		if ((myCheckRef != null) && (myCheckRef.m_refKernelWakelocks != null))
		{
			ret = true;
		} else
		{
			ret = false;
		}

		return ret;
	}

	/**
	 * Marshalls a reference to storage
	 * @param refs
	 * @param ctx
	 */
	private static void serializeRefToFile(Reference refs, Context ctx)
	{
		DataStorage.objectToFile(ctx, refs.m_fileName, refs);
		Log.i(TAG, "Saved ref " + refs.m_fileName);
	}

	/**
	 * Unmarshalls all refs from storage
	 * @param ctx
	 */
	private void deserializeFromFile(Context ctx)
	{
		String[] files = Reference.FILES;
		for (int i=0; i < files.length; i++)
		{
			Reference thisRef = (Reference) DataStorage.fileToObject(ctx, files[i]);
			m_refStore.put(files[i], thisRef);
		
			if (thisRef != null)
			{
				Log.i(TAG, "Retrieved ref: " + thisRef.whoAmI());
			}
			else
			{
				Log.i(TAG, "Reference " + Reference.CUSTOM_REF_FILENAME
						+ " was not found");
			}
		}
	}

	/**
	 * Fill the cache names of all existing refernces
	 * @param ctx
	 */
	private static void populateReferenceNames(Context ctx)
	{
		String[] files = Reference.FILES;
		for (int i=0; i < files.length; i++)
		{
			m_refStore.put(files[i], null);		
		}
	}
	
	/**
	 * Deletes (nulls) all serialized refererences
	 * @param ctx
	 */
	public static void deletedSerializedRefs(Context ctx)
	{
		Reference myEmptyRef = new Reference(Reference.CUSTOM_REF_FILENAME);
		myEmptyRef.setEmpty();
		DataStorage.objectToFile(ctx, Reference.CUSTOM_REF_FILENAME,
				myEmptyRef);

		myEmptyRef = new Reference(Reference.CHARGED_REF_FILENAME);
		myEmptyRef.setEmpty();
		DataStorage.objectToFile(ctx,
				Reference.CHARGED_REF_FILENAME, myEmptyRef);

		myEmptyRef = new Reference(Reference.SCREEN_OFF_REF_FILENAME);
		myEmptyRef.setEmpty();
		DataStorage.objectToFile(ctx,
				Reference.SCREEN_OFF_REF_FILENAME, myEmptyRef);

		myEmptyRef = new Reference(Reference.UNPLUGGED_REF_FILENAME);
		myEmptyRef.setEmpty();
		DataStorage.objectToFile(ctx,
				Reference.UNPLUGGED_REF_FILENAME, myEmptyRef);

		myEmptyRef = new Reference(Reference.BOOT_REF_FILENAME);
		myEmptyRef.setEmpty();
		DataStorage.objectToFile(ctx, Reference.BOOT_REF_FILENAME,
				myEmptyRef);

	}
}
