package com.demonslayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class KillAttributionTest
{
	@Test
	public void requiresRecentLocalHitAndDeduplicatesDeath()
	{
		KillAttribution tracker = new KillAttribution();
		tracker.spawn(1);
		assertFalse(tracker.death(1, 1));
		tracker.spawn(2);
		tracker.localHit(2, 2);
		assertTrue(tracker.death(2, 5));
		assertFalse(tracker.death(2, 5));
		tracker.spawn(3);
		tracker.localHit(3, 2);
		assertFalse(tracker.death(3, 20));
		tracker.spawn(4);
		tracker.localHit(4, 5);
		tracker.clear();
		assertFalse(tracker.death(4, 6));
	}
}
