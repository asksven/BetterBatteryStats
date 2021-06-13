package com.asksven.betterbatterystats.data;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import android.util.Log;

import com.asksven.android.common.privateapiproxies.StatElement;

import java.util.ArrayList;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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
		Context ctx = ApplicationProvider.getApplicationContext();
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
		ArrayList<String> res = mStats.getRequestedPermissionListForPackage(ApplicationProvider.getApplicationContext(), getPackageName());
		assertNotNull(res);
		assertTrue(res.size() > 0);
	}

	@Test
	public void getReceiverListForPackage() throws Exception
	{
		ArrayList<String> res = mStats.getReceiverListForPackage(ApplicationProvider.getApplicationContext(), getPackageName());
		assertNotNull(res);
		assertTrue(res.size() > 0);

	}

	@Test
	public void getServiceListForPackage() throws Exception
	{
		ArrayList<String> res = mStats.getServiceListForPackage(ApplicationProvider.getApplicationContext(), getPackageName());
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
		String ret = ApplicationProvider.getApplicationContext().getPackageName();
		Log.i(TAG, "Current package: " + ret);

		return ret;
	}
}