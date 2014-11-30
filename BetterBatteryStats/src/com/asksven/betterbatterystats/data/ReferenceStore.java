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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

//import com.asksven.android.common.utils.DataStorage;


import com.asksven.betterbatterystats.LogSettings;

import android.content.Context;
import android.util.Log;

/**
 * @author sven
 * Static class as fassade to the cache of references
 */
public class ReferenceStore
{
	public static final String REF_UPDATED = "com.asksven.betterbatterystats.REF_UPDATED";
	
	/** the storage for references */
	private static  Map<String, Reference> m_refStore = new HashMap<String, Reference>();
	
	/** the logging tag */
	private static final String TAG = "ReferenceStore";

	/**
	 * Return the speaking reference names for references created after refFileName (or all if refFileName is empty or null)
	 * @param refName
	 * @param ctx
	 * @return
	 */
	public static List<String> getReferenceLabels(String refFileName, Context ctx)
	{
		List<String> ret = new ArrayList<String>();
		
		long time = 0;
		
		// if a ref name was passed get the time that ref was created
		if ((refFileName != null) && !refFileName.equals("") )
		{
			Reference ref = getReferenceByName(refFileName, ctx);
			if (ref != null)
			{
				time = ref.m_creationTime;
			}
		}
		ReferenceDBHelper db = ReferenceDBHelper.getInstance(ctx);
		ret = db.fetchAllLabels(time);
		
		return ret;
	}

	/**
	 * Return the internal reference names for references created after refFileName (or all if refFileName is empty or null)
	 * @param refName
	 * @param ctx
	 * @return
	 */
	public static List<String> getReferenceNames(String refFileName, Context ctx)
	{
		List<String> ret = new ArrayList<String>();
		
		long time = 0;
		
		// if a ref name was passed get the time that ref was created
		if ((refFileName != null) && !refFileName.equals("") )
		{
			Reference ref = getReferenceByName(refFileName, ctx);
			if (ref != null)
			{
				time = ref.m_creationTime;
			}
		}
		ReferenceDBHelper db = ReferenceDBHelper.getInstance(ctx);
		ret = db.fetchAllKeys(time);
		return ret;
	}

	/**
	 * Returns a reference given a name
	 * @param refName the name of the reference
	 * @param ctx
	 * @return
	 */
	public static Reference getReferenceByName(String refName, Context ctx)
	{
		ReferenceDBHelper db = ReferenceDBHelper.getInstance(ctx);

		// the reference names are lazily loaded too
		if (m_refStore.keySet().isEmpty())
		{
			populateReferenceNames(ctx);
		}
		
		// we use lazy loading so we must check if there is a reference there
		if (m_refStore.get(refName) == null)
		{
			Reference thisRef = db.fetchReferenceByKey(refName);
			m_refStore.put(refName, thisRef);
		
			if (LogSettings.DEBUG)
			{
				if (thisRef != null)
				{
					
					Log.i(TAG, "Retrieved reference from storage: " + thisRef.whoAmI());
				}
				else
				{
					Log.i(TAG, "Reference " + refName + " was not found");
				}
			}
		}
		else
		{
			if (LogSettings.DEBUG)
				Log.i(TAG, "Retrieved reference from cache: " + m_refStore.get(refName).whoAmI());
		}
		Reference ret = m_refStore.get(refName);
		return ret;
	}
	
	/**
	 * Adds a reference to the cache and persists it asynchronously
	 * @param refName the name
	 * @param ref
	 * @param ctx
	 */
	public static synchronized void put(String refName, final Reference ref, final Context ctx)
	{
		m_refStore.put(refName, ref);
		if (LogSettings.DEBUG)
			Log.i(TAG, "Serializing reference " + refName);
		
		// Do this asynchronously as the data is already in the cache
	    Runnable runnable = new Runnable()
	    {
	      @Override
	      public void run()
	      {
	  		serializeRef(ref, ctx);
	      }
	    };
	    
	    new Thread(runnable).start();

	}

	/**
	 * Invalidates a reference in the cache storage
	 * @param refName the name
	 * @param ref
	 * @param ctx
	 */
	public static void invalidate(String refName, Context ctx)
	{
		m_refStore.put(refName, null);
		ReferenceDBHelper db = ReferenceDBHelper.getInstance(ctx);
		db.deleteReference(refName);
	}

	/**
	 * Invalidates the whole cache
	 * @param ref
	 * @param ctx
	 */
	public static synchronized void rebuildCache(Context ctx)
	{
		m_refStore.clear();
		populateReferenceNames(ctx);
	}


	/**
	 * Returns whether a reference exists
	 * @param ref
	 * @param ctx
	 * @return
	 */
	public static boolean hasReferenceByName(String ref, Context ctx)
	{
		boolean ret = false;
		Reference myCheckRef = getReferenceByName(ref, ctx);

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
	private static void serializeRef(Reference refs, Context ctx)
	{
  		ReferenceDBHelper db = ReferenceDBHelper.getInstance(ctx);
		db.addOrUpdateReference(refs);
		Log.i(TAG, "Saved ref " + refs.m_fileName);
	}

	/**
	 * Fill the cache names of all existing refernces
	 * @param ctx
	 */
	private static void populateReferenceNames(Context ctx)
	{
		ReferenceDBHelper db = ReferenceDBHelper.getInstance(ctx);
		List<String> refs = db.fetchAllKeys(0);
		if (LogSettings.DEBUG)
			Log.i(TAG, "Populating cache");
		
		for (int i=0; i < refs.size(); i++)
		{
			m_refStore.put(refs.get(i), null);
			if (LogSettings.DEBUG)
				Log.i(TAG, "Added ref " + refs.get(i));
		}
		
		if (LogSettings.DEBUG)
			Log.i(TAG, "Finished populating cache");
	}
	
	/**
	 * Deletes (nulls) all serialized refererences
	 * @param ctx
	 */
	public static void deleteAllRefs(Context ctx)
	{
		if (LogSettings.DEBUG)
			Log.i(TAG, "Deleting all references");
		
		ReferenceDBHelper db = ReferenceDBHelper.getInstance(ctx);
		db.deleteReferences();
		m_refStore.clear();
		
	}
	
	public static void logReferences(Context ctx)
	{
		ReferenceDBHelper db = ReferenceDBHelper.getInstance(ctx);
		db.logCacheContent();		
	}
}

