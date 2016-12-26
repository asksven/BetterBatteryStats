package com.asksven.betterbatterystats.data;

import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by android on 12/26/16.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Log.class)
public class GraphSerieTest
{
	@Test
	public void testGraphSerie() throws Exception
	{
		PowerMockito.mockStatic(Log.class);

		ArrayList<Datapoint> nullSerie = null;
		ArrayList<Datapoint> emptySerie = new ArrayList();
		ArrayList<Datapoint> serie = new ArrayList();

		// populate serie
		for (int i = 0; i < 1000; i++)
		{
			serie.add(new Datapoint(i, 1000 - i));
		}

		GraphSerie gs1 = new GraphSerie("gs1", nullSerie);
		GraphSerie gs2 = new GraphSerie("gs2", emptySerie);
		GraphSerie gs3 = new GraphSerie("gs3", serie);

		assertTrue(gs1.size() == 0);
		assertTrue(gs2.size() == 0);
		assertTrue(gs3.size() > 0);

		assertTrue(gs1.getTitle().equals("gs1"));
		assertTrue(gs2.getTitle().equals("gs2"));
		assertTrue(gs3.getTitle().equals("gs3"));

		assertFalse(gs1.getValues() == nullSerie); // we expect the GraphSerie to have detected and replaced the empty array
		assertTrue(gs2.getValues() == emptySerie);
		assertTrue(gs3.getValues() == serie);
	}


}