package com.asksven.betterbatterystats.data;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import com.asksven.android.common.privateapiproxies.StatElement;

import java.util.ArrayList;

import android.support.test.runner.AndroidJUnit4;
import android.util.Pair;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created on 12/26/16.
 */
@RunWith(AndroidJUnit4.class)
public class StatsProviderTest
{

	private StatsProvider mStats;
	static final String TAG = "StatsProviderTest";

	@Before
	public void createStatsProvider() throws Exception
	{
		Context ctx = InstrumentationRegistry.getContext();
		assertNotNull(ctx);
		mStats = StatsProvider.getInstance();

	}

	@Test
	public void test_getCurrentAlarmsStatList() throws Exception
	{
		ArrayList<StatElement> res = mStats.getCurrentAlarmsStatList(true);
		assertNotNull(res);
		Log.i(TAG, "Retrieved " + res.size() + " elements");
	}

	@Test
	public void getCurrentSensorStatList() throws Exception
	{
		ArrayList<StatElement> res = mStats.getCurrentSensorStatList(true);
		assertNotNull(res);
		Log.i(TAG, "Retrieved " + res.size() + " elements");

	}

	@Test
	public void getCurrentAlarmsStatList() throws Exception
	{
		ArrayList<StatElement> res = mStats.getCurrentAlarmsStatList(true);
		assertNotNull(res);
		Log.i(TAG, "Retrieved " + res.size() + " elements");

	}

	@Test
	public void getCurrentProcessStatList() throws Exception
	{
		ArrayList<StatElement> res = mStats.getCurrentProcessStatList(true, 0);
		assertNotNull(res);
		Log.i(TAG, "Retrieved " + res.size() + " elements");

	}

	@Test
	public void getCurrentWakelockStatList() throws Exception
	{
		ArrayList<StatElement> res = mStats.getCurrentWakelockStatList(true, 0, 0);
		assertNotNull(res);
		Log.i(TAG, "Retrieved " + res.size() + " elements");

	}

	@Test
	public void getCurrentKernelWakelockStatList() throws Exception
	{
		ArrayList<StatElement> res = mStats.getCurrentKernelWakelockStatList(true, 0, 0);
		assertNotNull(res);
		Log.i(TAG, "Retrieved " + res.size() + " elements");

	}

	@Test
	public void getCurrentNetworkUsageStatList() throws Exception
	{
		ArrayList<StatElement> res = mStats.getCurrentNetworkUsageStatList(true);
		assertNotNull(res);
		Log.i(TAG, "Retrieved " + res.size() + " elements");

	}

	@Test
	public void getCurrentCpuStateList() throws Exception
	{
		ArrayList<StatElement> res = mStats.getCurrentCpuStateList(true);
		assertNotNull(res);
		Log.i(TAG, "Retrieved " + res.size() + " elements");

	}

	@Test
	public void getRequestedPermissionListForPackage() throws Exception
	{
		ArrayList<String> res = mStats.getRequestedPermissionListForPackage(InstrumentationRegistry.getContext(), getPackageName());
		assertNotNull(res);
		assertTrue(res.size() > 0);
	}

	@Test
	public void getReceiverListForPackage() throws Exception
	{
		ArrayList<String> res = mStats.getReceiverListForPackage(InstrumentationRegistry.getContext(), getPackageName());
		assertNotNull(res);
		assertTrue(res.size() > 0);

	}

	@Test
	public void getServiceListForPackage() throws Exception
	{
		ArrayList<String> res = mStats.getServiceListForPackage(InstrumentationRegistry.getContext(), getPackageName());
		assertNotNull(res);
		assertTrue(res.size() > 0);

	}

	@Test
	public void getCurrentOtherUsageStatList() throws Exception
	{
		ArrayList<StatElement> res = mStats.getCurrentOtherUsageStatList(true, true, false);
		assertNotNull(res);
		Log.i(TAG, "Retrieved " + res.size() + " elements");

	}

	private String getPackageName()
	{
		String ret = InstrumentationRegistry.getTargetContext().getApplicationContext().getPackageName();
		Log.i(TAG, "Current package: " + ret);

		return ret;
	}
}