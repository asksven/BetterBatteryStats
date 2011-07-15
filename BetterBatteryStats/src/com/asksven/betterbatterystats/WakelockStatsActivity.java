package com.asksven.betterbatterystats;

import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;

public class WakelockStatsActivity extends ListActivity
{
	/**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wakelock_stats);

        setListAdapter(new ArrayAdapter<String>(this,
                       android.R.layout.simple_list_item_1,  
                       getStatList()));
	}
	
	List<String> getStatList()
	{
		List<String> myStats = new Vector<String>();
		myStats.add("value 1");
		myStats.add("value 2");
		
		return myStats;
	}
}
