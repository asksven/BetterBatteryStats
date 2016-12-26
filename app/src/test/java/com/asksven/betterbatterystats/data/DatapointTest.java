package com.asksven.betterbatterystats.data;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Created by android on 12/26/16.
 */
public class DatapointTest
{
	@Test
	public void toStringReturnsValue() throws Exception
	{
		Datapoint p1 = new Datapoint();
		Datapoint p2 = new Datapoint(12, 45);

		String s1 = p1.toString();
		String s2 = p2.toString();

		assertTrue(p1.mX == 0);
		assertTrue(p1.mY == 0);

		assertTrue(s1.equals("X=" + p1.mX + ", Y=" + p1.mY));
		assertTrue(s2.equals("X=" + p2.mX + ", Y=" + p2.mY));

	}

}