/*
 * Copyright (C) 2011-2012 asksven
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.List;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;


/**
 * DBHelper sigleton class.  
 * 
 * Database layer for cell log data
 */

public class ReferenceDBHelper
{
	private static final String DATABASE_NAME	= "betterbatterystats";
    private static final String TABLE_DBVERSION = "dbversion";
    private static final String TABLE_NAME 		= "samples";
    private static final int DATABASE_VERSION 	= 1;
    private static final String TAG 			= "EventDBHelper";
    private static final String[] COLS 			= new String[] {"ref_name", "ref_type", "ref_label", "time_created", "ref_blob"};

    Context m_context;
    static ReferenceDBHelper m_helper;

    private static final String DBVERSION_CREATE = 
    	"create table " + TABLE_DBVERSION + " (" + "version integer not null);";
    
    private static final String DBVERSION_DROP = " drop table " + TABLE_DBVERSION + ";";

    private static final String PURGE_EVENTS = " delete from " + TABLE_NAME + ";";

    private static final String TABLE_CREATE = "create table " + TABLE_NAME + " ("
    	    + "ref_name text primary key, "
            + "ref_type integer, "
    	    + "ref_label text, "
            + "time_created integer, "
            + "ref_blob blob"
            + ");";
    

    private static final String TABLE_DROP = "drop table " + TABLE_NAME + ";";

    private SQLiteDatabase db;

    protected static ReferenceDBHelper getInstance(Context context)
    {
    	if (m_helper == null)
    	{
    		m_helper = new ReferenceDBHelper(context); 
    	}
    	return m_helper;
    }
    
    /**
     * Hidden constructor, use as singleton
     * @param ctx
     */
    private ReferenceDBHelper(Context ctx)
    {
    	m_context = ctx;
		try
		{
			db = m_context.openOrCreateDatabase(DATABASE_NAME, 0,null);

			// Check for the existence of the DBVERSION table
			// If it doesn't exist than create the overall data,
			// otherwise double check the version
			Cursor c =
				db.query("sqlite_master", new String[] { "name" },
						"type='table' and name='" + TABLE_DBVERSION + "'", null, null, null, null);
			int numRows = c.getCount();
			if (numRows < 1)
			{
				createDatabase(db);
			}
			else
			{
				int version=0;
				Cursor vc = db.query(true, TABLE_DBVERSION, new String[] {"version"},
						null, null, null, null, null,null);
				if(vc.getCount() > 0)
				{
				    vc.moveToLast();
				    version=vc.getInt(0);
				}
				vc.close();
				if (version!=DATABASE_VERSION)
				{
					Log.e(TAG,"database version mismatch");
					migrateDatabase(db, version, DATABASE_VERSION);
				}

			}
			c.close();
			

		}
		catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
		finally
		{
			if (db.isOpen())
			{
				db.close();
			}
		}
    }

    private void createDatabase(SQLiteDatabase db)
    {
		try
		{
			db = m_context.openOrCreateDatabase(DATABASE_NAME, 0,null);
			db.execSQL(DBVERSION_CREATE);
			ContentValues args = new ContentValues();
			args.put("version", DATABASE_VERSION);
			db.insert(TABLE_DBVERSION, null, args);

			db.execSQL(TABLE_CREATE);
		}
		catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		} 
    }
    

    private void deleteDatabase()
    {
        try
        {
			m_context.deleteDatabase(DATABASE_NAME);
			
        }
        catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
        finally 
		{
			db.close();
		}    	
    }
    
    protected void deleteReferences()
    {
        try
        {
			db = m_context.openOrCreateDatabase(DATABASE_NAME, 0,null);
			db.execSQL(PURGE_EVENTS);
        }
        catch (SQLException e)
		{
			Log.e(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
        finally 
		{
        	if (db.isOpen())
        	{
        		db.close();
        	}
		}    	
    }

	/**
	 * 
	 * @param entry
	 */
	protected void addOrUpdateReference(Reference entry)
	{

		ContentValues val = new ContentValues();
		val.put("ref_name", entry.m_fileName);
		val.put("ref_type", entry.m_refType);
		val.put("time_created", entry.m_creationTime);
		val.put("ref_label", entry.m_refLabel);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		try
		{
			out = new ObjectOutputStream(bos);
			out.writeObject(entry);
			byte[] refBytes = bos.toByteArray();
			val.put("ref_blob", refBytes);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				out.close();
				bos.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			
		}

		try
		{
			db = m_context.openOrCreateDatabase(DATABASE_NAME, 0, null);
			long lRes = db.replace(TABLE_NAME, null, val);
			if (lRes == -1)
			{
				Log.e(TAG, "Error inserting or updating row");
			}
		}
		catch (SQLException e)
		{
			Log.d(TAG, "SQLite exception: " + e.getLocalizedMessage());
		}
		finally
		{
			if (db.isOpen())
			{
				db.close();
			}
		}
	}	

	protected void deleteReference(String refName)
	{
		try
		{
			db = m_context.openOrCreateDatabase(DATABASE_NAME, 0, null);
			long lRes = db.delete(TABLE_NAME, "ref_name='" + refName + "'", null);
			if (lRes == 0)
			{
				Log.e(TAG, "No row with key '" + refName + "' was deleted");
			}
		}
		catch (SQLException e)
		{
			Log.d(TAG, "SQLite exception: " + e.getLocalizedMessage());
		}
		finally
		{
			if (db.isOpen())
			{
				db.close();
			}
		}
	}	

	/**
	 * 
	 * @return
	 */
	protected List<Reference> fetchAllRows()
	{
	    ArrayList<Reference> ret = new ArrayList<Reference>();
	    try
	    {
			db = m_context.openOrCreateDatabase(DATABASE_NAME, 0,null);
	        Cursor c;
	        c = db.query(TABLE_NAME, COLS, null, null, null, null, "time_created ASC");
	        int numRows = c.getCount();
	        c.moveToFirst();
	        for (int i = 0; i < numRows; ++i)
	        {

	        	// cctor with id, name, command, command_status
	            Reference row = createReferenceFromRow(c);
	           
	            ret.add(row);
	            c.moveToNext();
	        }
	        c.close();
		}
	    catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
	    finally 
		{
	    	if (db.isOpen())
	    	{
	    		db.close();
	    	}
		}
	    return ret;
	}

	public void logCacheContent()
	{
		List<Reference> refs = fetchAllRows();
		Log.i(TAG, "Reference store");
		for (int i=0; i < refs.size(); i++)
		{
			Reference ref = refs.get(i);
			Log.i(TAG, ref.whoAmI());
		}
	}
	protected List<String> fetchAllKeys(long time)
	{
	    ArrayList<String> ret = new ArrayList<String>();
	    try
	    {
			db = m_context.openOrCreateDatabase(DATABASE_NAME, 0,null);
	        Cursor c;
	        c = db.query(TABLE_NAME, new String[] {"ref_name", "time_created"}, null, null, null, null, "time_created ASC");
	        int numRows = c.getCount();
	        c.moveToFirst();
	        for (int i = 0; i < numRows; ++i)
	        {
	        	String name = c.getString(c.getColumnIndex("ref_name"));
	        	long timeCreated = c.getInt(c.getColumnIndex("time_created"));
	        	if (timeCreated > time)
	        	{
	        		ret.add(name);
	        	}
	            c.moveToNext();
	        }
	        c.close();
		}
	    catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
	    finally 
		{
	    	if (db.isOpen())
	    	{
	    		db.close();
	    	}
		}
	    return ret;
	}

	protected List<String> fetchAllLabels(long time)
	{
	    ArrayList<String> ret = new ArrayList<String>();
	    try
	    {
			db = m_context.openOrCreateDatabase(DATABASE_NAME, 0,null);
	        Cursor c;
	        c = db.query(TABLE_NAME, new String[] {"ref_label", "time_created"}, null, null, null, null, "time_created ASC");
	        int numRows = c.getCount();
	        c.moveToFirst();
	        for (int i = 0; i < numRows; ++i)
	        {
	        	String name = c.getString(c.getColumnIndex("ref_label"));
	        	long timeCreated = c.getInt(c.getColumnIndex("time_created"));
	        	if (timeCreated > time)
	        	{
	        		ret.add(name);
	        	}
	            c.moveToNext();
	        }
	        c.close();
		}
	    catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
	    finally 
		{
	    	if (db.isOpen())
	    	{
	    		db.close();
	    	}
		}
	    return ret;
	}

	protected Reference fetchReferenceByKey(String refName)
	{
	    Reference myRet = null;
	    try
	    {
			db = m_context.openOrCreateDatabase(DATABASE_NAME, 0,null);
	        Cursor c;
	        c = db.query(TABLE_NAME, COLS, "ref_name='" + refName + "'", null, null, null, null);
	        int numRows = c.getCount();
	        c.moveToFirst();
	        if (numRows == 1)
	        {

	        	// cctor with id, name, command, command_status
	            myRet = createReferenceFromRow(c);	           
	        }
	        c.close();
		}
	    catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
	    finally 
		{
			db.close();
		}
	    return myRet;
	}

	private Reference createReferenceFromRow(Cursor c)
	{
		ByteArrayInputStream bis = new ByteArrayInputStream(c.getBlob(c.getColumnIndex("ref_blob")));
		ObjectInput in = null;
		Reference ret = null;
		try
		{
			in = new ObjectInputStream(bis);
			Object o = in.readObject();
			ret = (Reference) o;
		}
		catch (StreamCorruptedException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				bis.close();
				in.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

		}
		return ret;
	}

    private void migrateDatabase(SQLiteDatabase db, int fromVersion, int toVersion)
    {
		try
		{
//			if ((fromVersion == 1)&&(toVersion == 2))
//			{
				deleteDatabase();
				createDatabase(db);
//			}
		}
		catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		} 
    }


}
	
