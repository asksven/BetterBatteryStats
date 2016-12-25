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
import java.util.List;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;


/**
* DBHelper class.  
* 
* Database layer for cell log data
*/

public class KbDbHelper
{
	private static final String DATABASE_NAME	= "better_battery_stats";
   private static final String TABLE_DBVERSION = "dbversion";
   private static final String TABLE_NAME 		= "kb";
   private static final int DATABASE_VERSION 	= 1;
   private static final String TAG 			= "KbDbHelper";
   private static final String[] COLS 			= new String[] {"fqn", "title", "url"};

   Context myCtx;

   private static final String DBVERSION_CREATE = 
   	"create table " + TABLE_DBVERSION + " ("
   		+ "version integer not null);";
   
   private static final String DBVERSION_DROP =
   	" drop table " + TABLE_DBVERSION + ";";

   private static final String TABLE_CREATE =
       "create table " + TABLE_NAME + " ("
           + "fqn not null, "
           + "title text, "
           + "url text"
           + ");";
   
   private static final String TABLE_MIGRATE_1_2 =
   	"alter table " + TABLE_NAME + " add column processresult int";

   private static final String TABLE_DROP =
   	"drop table " + TABLE_NAME + ";";

   private static KbDbHelper m_singleton = null;
   
   private SQLiteDatabase m_db;

   public static KbDbHelper getInstance(Context ctx)
   {
	   if (m_singleton != null)
	   {
		   return m_singleton;
	   }
	   else
	   {
		   m_singleton = new KbDbHelper(ctx);
		   return m_singleton;
	   }
   }
   /**
    * 
    * @param ctx
    */
   private KbDbHelper(Context ctx)
   {
   	myCtx = ctx;
		try
		{
			m_db = myCtx.openOrCreateDatabase(DATABASE_NAME, 0,null);

			// Check for the existence of the DBVERSION table
			// If it doesn't exist than create the overall data,
			// otherwise double check the version
			Cursor c =
				m_db.query("sqlite_master", new String[] { "name" },
						"type='table' and name='"+TABLE_DBVERSION+"'", null, null, null, null);
			int numRows = c.getCount();
			if (numRows < 1)
			{
				CreateDatabase(m_db);
			}
			else
			{
				int version=0;
				Cursor vc = m_db.query(true, TABLE_DBVERSION, new String[] {"version"},
						null, null, null, null, null,null);
				if(vc.getCount() > 0) {
				    vc.moveToLast();
				    version=vc.getInt(0);
				}
				vc.close();
				if (version!=DATABASE_VERSION)
				{
					Log.e(TAG,"database version mismatch");
					MigrateDatabase(m_db, version, DATABASE_VERSION);
//					deleteDatabase();
//					CreateDatabase(db);
//					populateDatabase();
				}
			}
			c.close();
			

		}
		catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
   }

   private void CreateDatabase(SQLiteDatabase db)
   {
		try
		{
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
   
   private void MigrateDatabase(SQLiteDatabase db, int fromVersion, int toVersion)
   {
		try
		{
			if ((fromVersion == 1)&&(toVersion == 2))
			{
				db.execSQL(TABLE_MIGRATE_1_2);
				ContentValues args = new ContentValues();
				args.put("version", DATABASE_VERSION);
				db.insert(TABLE_DBVERSION, null, args);
			}
		}
		catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		} 
   }

   void deleteDatabase()
   {
       try
       {
			m_db.execSQL(TABLE_DROP);
			m_db.execSQL(DBVERSION_DROP);
       }
       catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
   }
      
   
   /**
    * Populates a database buffer from a value object
    * @param val the database buffer
    * @param record a value object
    */
   void populateValues(ContentValues val, KbEntry record)
   {
   	val.put("fqn", record.getFqn());
   	val.put("title", record.getTitle());
   	val.put("url", record.getUrl());
   }
	/**
	 * 
	 * @param entry
	 */
	void addEnrty(KbEntry entry)
	{
		ContentValues initialValues = new ContentValues();
		populateValues(initialValues, entry);
	
	    try
	    {
	        long lRes =m_db.insert(TABLE_NAME, null, initialValues);
	        if (lRes == -1)
	        {
	        	Log.d(TAG,"Error inserting row");
	        }
		}
	    catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
	}
	
	/**
	 * 
	 */
	void deleteAll()
	{
	    try
	    {
			m_db.delete(TABLE_NAME, "", null);
		}
	    catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
	}


	/**
	 * 
	 * @return
	 */
	public List<KbEntry> fetchAllRows()
	{
		List<KbEntry> ret = new ArrayList<KbEntry>();
	    
	    try
	    {
	        Cursor c;
	        c = m_db.query(TABLE_NAME, COLS, null, null, null, null, null);
	        int numRows = c.getCount();
	        c.moveToFirst();
	        for (int i = 0; i < numRows; ++i)
	        {

	        	// cctor with id, name, command, command_status
	            KbEntry row = createEntryFromRow(c);
	           
	            ret.add(row);
	            c.moveToNext();
	        }
	        c.close();
		}
	    catch (SQLException e)
		{
			Log.d(TAG,"SQLite exception: " + e.getLocalizedMessage());
		}
	    return ret;
	}

	KbEntry createEntryFromRow(Cursor c)
	{
       KbEntry myRet = new KbEntry();
       myRet.setFqn(c.getString(c.getColumnIndex("fqn")));
       myRet.setTitle(c.getString(c.getColumnIndex("title")));
       myRet.setUrl(c.getString(c.getColumnIndex("url")));
       return myRet;
	}

	
	
	public void save(KbData items)
	{
		deleteAll();
		if (items == null)
		{
			return;
		}
		
		for (int i=0; i< items.getEntries().size(); i++)
		{
			// add
			Log.i(getClass().getSimpleName(), "adding commands to the database: " + items.getEntries().get(i).toString());
			addEnrty(items.getEntries().get(i));
		}
	}

}
	
